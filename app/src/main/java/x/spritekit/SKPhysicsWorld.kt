package x.spritekit

import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import org.andengine.engine.camera.Camera
import org.andengine.opengl.util.GLState
import x.spritekit.TimeInterval
import x.core.graphics.Vector
import java.lang.ref.WeakReference

interface SKPhysicsContactDelegate {

	fun didBegin(contact: SKPhysicsContact)
	fun didEnd(contact: SKPhysicsContact) {}
}

class SKPhysicsWorld : IDrawable {

	companion object {
		private val TAG = SKPhysicsWorld::class.simpleName!!
	}

	var gravity = Vector(0f, -9.8f)
	var speed: Float = 1f

	var contactDelegateRef: WeakReference<SKPhysicsContactDelegate>? = null
	var contactDelegate: SKPhysicsContactDelegate?
		get() = contactDelegateRef?.get()
		set(value) {
			contactDelegateRef = WeakReference(value)
		}

	internal var bodies: MutableSet<SKPhysicsBody> = hashSetOf()

	/** bodies updated each frame */
	internal var bodies2: MutableSet<SKPhysicsBody> = hashSetOf()


	internal fun add(body: SKPhysicsBody) {
		synchronized(bodies) {
			bodies.add(body)
		}
	}

	internal fun addAll(bodies: Set<SKPhysicsBody>) {
		synchronized(bodies) {
			this.bodies.addAll(bodies)
		}
	}

	internal fun removeAll(bodies: Set<SKPhysicsBody>) {
		synchronized(bodies) {
			this.bodies.removeAll(bodies)
		}
	}

	private val mainLooperHandler = Handler(Looper.getMainLooper())
	fun simulatePhysics(tag: String, deltaTime: TimeInterval) {
		// if (speed == 0f) return // need update bodies contacts

		val bodies = bodies2.toList()

		bodies.forEach {
			it.simulatePhysics(deltaTime, gravity, speed)
		}

		for (i in bodies.indices) {
			val a = bodies[i]

			for (j in i + 1 until bodies.size) {
				val b = bodies[j]

				val c = a.contact("$TAG|$tag", b)
				if (c == true) {
					mainLooperHandler.post {
						contactDelegate?.didBegin(SKPhysicsContact(a, b))
					}
				} else if (c == false) {
					mainLooperHandler.post {
						contactDelegate?.didEnd(SKPhysicsContact(a, b))
					}
				}
			}

			a.node!!.let {
				it.provisionalDeltaX += (a.shape.kX - 1) * it.provisionalDeltaXAbs
				it.provisionalDeltaY += (a.shape.kY - 1) * it.provisionalDeltaYAbs

				it.position.x += it.provisionalDeltaX
				it.position.y += it.provisionalDeltaY

				it.updateAbsPositionAll(TAG)
			}

			a.isFirstAdd = false
		}
	}

	override fun drawSelf(canvas: Canvas, sceneXScale: Float, sceneYScale: Float, sceneScaleHasChanged: Boolean) {
		for (a in bodies2.toList()) {
			a.drawSelf(canvas, sceneXScale, sceneYScale, sceneScaleHasChanged)
		}
	}

	override fun drawSelf(glState: GLState, camera: Camera) {
		for (a in bodies2.toList()) {
			a.drawSelf(glState, camera)
		}
	}
}