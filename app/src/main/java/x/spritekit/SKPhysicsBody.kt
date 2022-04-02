package x.spritekit

import android.graphics.*
import android.opengl.GLES20
import android.util.Log
import androidx.annotation.CallSuper
import org.andengine.entity.primitive.Rectangle
import org.andengine.entity.primitive.vbo.HighPerformanceMeshVertexBufferObject
import org.andengine.opengl.shader.PositionColorShaderProgram
import org.andengine.opengl.shader.ShaderProgram
import org.andengine.opengl.util.GLState
import org.andengine.opengl.vbo.DrawType
import org.andengine.opengl.vbo.HighPerformanceVertexBufferObject
import org.andengine.util.color.ColorUtils
import x.spritekit.CommonConfig
import x.spritekit.TimeInterval
import x.core.graphics.*
import x.core.graphics.Point
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

class SKPhysicsBody : IDrawableObject {

	companion object {
		private const val TAG = "SKPhysicsBody"
	}

	internal val shape: IShape

	private var pathToDraw: Path? = null
	override var size: Size

	private val center: Point
	internal val centerAbs = Point.zero

	var isDynamic: Boolean = true
	var usesPreciseCollisionDetection: Boolean = false
	var allowsRotation: Boolean = true
	var pinned: Boolean = false

	var isResting: Boolean = false
	var friction: Float = 0.2f
	var charge: Float = 0f
	var restitution: Float = 0.2f
	var linearDamping: Float = 0.1f
	var angularDamping: Float = 0.1f

	private var isDensitySet = false
	private var isMassSet = false

	var density: Float = 1f
		set(value) {
			field = value
			isDensitySet = true
			if (!isMassSet) mass = density * area
			isDensitySet = false
		}

	/**
	The mass of the body.
	@discussion
	The unit is arbitrary, as long as the relative masses are consistent throughout the application. Note that density and mass are inherently related (they are directly proportional), so changing one also changes the other. Both are provided so either can be used depending on what is more relevant to your usage.
	 */
	var mass: Float = 0f
		set(value) {
			field = value
			isMassSet = true
			if (!isDensitySet) density = value / area
			isMassSet = false
		}

	/**
	The area of the body.
	@discussion
	The unit is arbitrary, as long as the relative areas are consistent throughout the application.
	 */
	// 1unit (m?,...) ~ 150px
	val area: Float get() = shape.area / 150f.pow(2)

	var affectedByGravity: Boolean = true

	var fieldBitMask = 0xFFFFFFFF.toInt()
	var categoryBitMask = 0xFFFFFFFF.toInt()
	var collisionBitMask = 0xFFFFFFFF.toInt()
	var contactTestBitMask = 0x00000000

	var node: SKNode? = null
		internal set

	/**
	 * The physics body’s velocity vector, measured in meters per second.
	 * XT: m/s ? really Apple?
	 */
	var velocity = Vector.zero
	var angularVelocity: Float = 0f

	private val contactedBodies: MutableSet<SKPhysicsBody> = hashSetOf()

	override var name: String? = null
		get() = node?.name

	internal var isFirstAdd = true

	override var glColorIsValid = false

	override var shaderProgram: ShaderProgram = PositionColorShaderProgram.getInstance()
	private val bufferData: FloatArray = FloatArray(Rectangle.RECTANGLE_SIZE)
	override var vertexBufferObject: HighPerformanceVertexBufferObject =
		HighPerformanceMeshVertexBufferObject(// HighPerformanceRectangleVertexBufferObject(
			null, bufferData, 4, DrawType.STATIC, true, Rectangle.VERTEXBUFFEROBJECTATTRIBUTES_DEFAULT
		)

	override var disposed = false
	override var disposeInfo: String? = null


	constructor(rectangleOf: Size, center: Point = Point.zero) {
		shape = Rectangle(rectangleOf)
		shape.physicsBody = this
		size = rectangleOf
		this.center = center
		updateVertices()
	}

	constructor(circleOfRadius: Float, center: Point = Point.zero) {
		shape = Circle(circleOfRadius)
		shape.physicsBody = this
		size = Size(circleOfRadius * 2, circleOfRadius * 2)
		this.center = center
		updateVertices()
	}

	constructor(path: Path, center: Point = Point.zero) {
		shape = Polygon(path)
		shape.physicsBody = this
		val pathRect = RectF()
		path.computeBounds(pathRect, true)
		size = Size(pathRect.width(), pathRect.height())
		this.center = center
		updateVertices()
	}


	override fun clone(): SKPhysicsBody {
		TODO("Not yet implemented")
	}

	// apply Forces
	fun applyForce(force: Vector) {}

	fun applyForce(force: Vector, point: Point) {}

	fun applyTorque(torque: Float) {}

	fun applyImpulse(impulse: Vector) {
		velocity.dx += impulse.dx / mass
		velocity.dy += impulse.dy / mass
	}

	fun applyImpulse(impulse: Vector, point: Point) {}

	fun applyAngularImpulse(impulse: Float) {}

	fun allContactedBodies(): Set<SKPhysicsBody> {
		return contactedBodies
	}

	@CallSuper
	internal fun removeFromWorld() {
		contactedBodies.forEach {
			it.contactedBodies.remove(this)
		}
		contactedBodies.clear()
	}

	@CallSuper
	internal fun simulatePhysics(deltaTime: TimeInterval, gravity: Vector, speed: Float) {
		// update by impulse & gravity
		if (isDynamic) {
			// Log.d(TAG, "~~  simulatePhysics: $deltaTime | ${gravity.dy} | $speed | ${node!!.position} | $velocity")

			val secBySpeed = deltaTime * speed
			// we can merge these 2 conditional blocks into 1
			if (affectedByGravity) {
				// s = v0.t + g.t^2/2
				node!!.provisionalDeltaX += (velocity.dx * secBySpeed + 0.5 * gravity.dx * 150 * secBySpeed.pow(2)).toFloat()
				node!!.provisionalDeltaY += (velocity.dy * secBySpeed + 0.5 * gravity.dy * 150 * secBySpeed.pow(2)).toFloat()

				velocity.dx += (gravity.dx * 150 * secBySpeed).toFloat()
				velocity.dy += (gravity.dy * 150 * secBySpeed).toFloat()
			} else if (velocity.dx != 0f || velocity.dy != 0f) {
				node!!.provisionalDeltaX += velocity.dx * secBySpeed.toFloat()
				node!!.provisionalDeltaY += velocity.dy * secBySpeed.toFloat()
			}
		}

		val scaledCenterX = center.x * (node!!.parent?.scaleAbs?.first ?: 1f)
		val scaledCenterY = center.y * (node!!.parent?.scaleAbs?.second ?: 1f)
		centerAbs.x = scaledCenterX * cos(node!!.zRotationAbs) - scaledCenterY * sin(node!!.zRotationAbs)
		centerAbs.y = scaledCenterX * sin(node!!.zRotationAbs) + scaledCenterY * cos(node!!.zRotationAbs)

		node!!.getAbsProvDelta(false)

		// shape.position.x = node!!.positionAbs.x
		// shape.position.y = node!!.positionAbs.y
		shape.physicsRevertCount = 0
		shape.kX = 1f
		shape.kY = 1f
	}

	@CallSuper
	internal fun contact(tag: String, b: SKPhysicsBody): Boolean? {
		if (b == this) throw Error("!-  PhysicsBody contacting itself")
		if (node!!.speedAbs == 0f && b.node!!.speedAbs == 0f) return null

		val collidable = collisionBitMask and b.categoryBitMask != 0
				|| b.collisionBitMask and categoryBitMask != 0
		val contactAlert = contactTestBitMask and b.categoryBitMask != 0
				|| b.contactTestBitMask and categoryBitMask != 0

		if (!collidable && !contactAlert) {
			return null
		}

		val intersect = shape.contact(b.shape)

		if (intersect) {
			if (collidable) shape.revertMove(b.shape)

			if (contactedBodies.add(b) && b.contactedBodies.add(this) && !isFirstAdd && !b.isFirstAdd) {
				Log.d(
					TAG,
					"--  contact [$tag]: ${node!!.name}@${hashCode()} | $isFirstAdd | ${node!!.positionAbs} >><< ${b.node!!.name} | ${b.node!!.positionAbs} | ${b.isFirstAdd}"
				)

				if (contactAlert) return true
			}
		} else {
			if (contactedBodies.remove(b) && b.contactedBodies.remove(this)) {
				Log.d(
					TAG,
					"--  contact [$tag]: X: ${node!!.name}@${hashCode()} | ${node!!.position} >><< ${b.node!!.name}"
				)

				if (contactAlert) return false
			}
		}

		return null
	}

	@CallSuper
	override fun drawSelf(canvas: Canvas, sceneXScale: Float, sceneYScale: Float, sceneScaleHasChanged: Boolean) {
		if (node!!.parent == null || node!!.isHiddenAbs!!) return
		super.drawSelf(canvas, sceneXScale, sceneYScale, sceneScaleHasChanged)
	}

	@CallSuper
	override fun doDraw(canvas: Canvas, sceneXScale: Float, sceneYScale: Float, sceneScaleHasChanged: Boolean) {
		val absPosition = node!!.positionAbs
		if (pathToDraw == null || absPosition.changed2 || !node!!.scaleIsValid2 || sceneScaleHasChanged) {
			absPosition.changed2 = false
			node!!.scaleIsValid2 = true

			val parentAbsScale = node!!.parent?.scaleAbs ?: 1f to 1f

			pathToDraw = shape.getPath()
			pathToDraw!!.transform(Matrix().apply {
				setScale(parentAbsScale.first * sceneXScale, parentAbsScale.second * sceneYScale)
			})
			pathToDraw!!.offset(
				canvas.width / 2f + (absPosition.x + centerAbs.x) * sceneXScale,
				canvas.height / 2f - (absPosition.y + centerAbs.y) * sceneYScale
			)
		}

		canvas.drawPath(pathToDraw!!, CommonConfig.strokePaint)
//        Log.i(TAG, "--  ${node!!.name} | ${hashCode()} | $absPosition")
	}

	@CallSuper
	override fun updateVertices() {
		val x = 0f
		val y = 0f
		val x2: Float = size.width
		val y2: Float = size.height

		bufferData[0 * Rectangle.VERTEX_SIZE + Rectangle.VERTEX_INDEX_X] = x
		bufferData[0 * Rectangle.VERTEX_SIZE + Rectangle.VERTEX_INDEX_Y] = y

		bufferData[1 * Rectangle.VERTEX_SIZE + Rectangle.VERTEX_INDEX_X] = x
		bufferData[1 * Rectangle.VERTEX_SIZE + Rectangle.VERTEX_INDEX_Y] = y2

		bufferData[2 * Rectangle.VERTEX_SIZE + Rectangle.VERTEX_INDEX_X] = x2
		bufferData[2 * Rectangle.VERTEX_SIZE + Rectangle.VERTEX_INDEX_Y] = y2

		bufferData[3 * Rectangle.VERTEX_SIZE + Rectangle.VERTEX_INDEX_X] = x2
		bufferData[3 * Rectangle.VERTEX_SIZE + Rectangle.VERTEX_INDEX_Y] = y

		(vertexBufferObject as HighPerformanceMeshVertexBufferObject)
			.onUpdateVertices()
	}

	@CallSuper
	override fun updateColor(tag: String) {
		(vertexBufferObject as HighPerformanceMeshVertexBufferObject)
			.onUpdateColor(ColorUtils.convertPackedIntToPackedFloat(Color.GREEN))
	}

	// ----------------------
	@CallSuper
	final override fun applyTranslation(pGLState: GLState, viewSize: Size) {
		val absPosition = node!!.positionAbs
		pGLState.translateModelViewGLMatrixf(
			(viewSize.width - size.width) / 2f + absPosition.x + centerAbs.x,
			(viewSize.height - size.height) / 2f - absPosition.y - centerAbs.y,
			0f
		)
	}

	@CallSuper
	final override fun applyRotation(pGLState: GLState, viewSize: Size) {
		if (node!!.zRotationAbs != 0f) {
			pGLState.translateModelViewGLMatrixf(
				node!!.rotationCenterX ?: size.width / 2,
				node!!.rotationCenterY ?: size.height / 2,
				0f
			)
			pGLState.rotateModelViewGLMatrixf(-node!!.zRotationAbs360, 0f, 0f, 1f)
			pGLState.translateModelViewGLMatrixf(
				-(node!!.rotationCenterX ?: size.width / 2),
				-(node!!.rotationCenterY ?: size.height / 2),
				0f
			)

			/* TODO There is a special, but very likely case when mRotationCenter and mScaleCenter are the same.
			 * In that case the last glTranslatef of the rotation and the first glTranslatef of the scale is superfluous.
			 * The problem is that applyRotation and applyScale would need to be "merged" in order to efficiently check for that condition.  */
		}
	}

	@CallSuper
	final override fun applySkew(pGLState: GLState, viewSize: Size) {
		if (node!!.skewX != 0f || node!!.skewY != 0f) {
			pGLState.translateModelViewGLMatrixf(node!!.skewCenterX ?: size.width / 2, node!!.skewCenterY ?: size.height / 2, 0f)
			pGLState.skewModelViewGLMatrixf(node!!.skewX, node!!.skewY)
			pGLState.translateModelViewGLMatrixf(
				-(node!!.skewCenterX ?: size.width / 2),
				-(node!!.skewCenterY ?: size.height / 2),
				0f
			)
		}
	}

	@CallSuper
	final override fun applyScale(pGLState: GLState, viewSize: Size) {
		val absScale = node!!.parent?.scaleAbs ?: 1f to 1f
		if (absScale.first != 1f || absScale.second != 1f) {
			pGLState.translateModelViewGLMatrixf(node!!.scaleCenterX ?: size.width / 2, node!!.scaleCenterY ?: size.height / 2, 0f)
			pGLState.scaleModelViewGLMatrixf(absScale.first, absScale.second, 1)
			pGLState.translateModelViewGLMatrixf(
				-(node!!.scaleCenterX ?: size.width / 2),
				-(node!!.scaleCenterY ?: size.height / 2),
				0f
			)
		}
	}

	@CallSuper
	override fun preDraw(glState: GLState) {
		super.preDraw(glState)
		glState.lineWidth(2f)
	}

	override fun doDraw() {
		vertexBufferObject.draw(GLES20.GL_LINE_LOOP, Rectangle.VERTICES_PER_RECTANGLE)
	}
}