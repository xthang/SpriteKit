package x.core.graphics

import android.graphics.Path
import android.util.Log
import x.core.Utils
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

class Circle(internal val radius: Float) : IShape() {

	companion object {
		private val TAG = Circle::class.simpleName
	}

	override val area: Float get() = PI.toFloat() * radius.pow(2)

	override fun getPath(): Path {
		return Path().apply { addCircle(0f, 0f, radius, Path.Direction.CCW) }
	}

	override fun contact(other: IShape): Boolean {
		return when (other) {
			is Circle -> {
				val nodeA = physicsBody.node!!
				val nodeB = other.physicsBody.node!!
				val distance = sqrt(
					(nodeA.positionAbs.x + physicsBody.centerAbs.x + nodeA.provisionalDeltaXAbs - (nodeB.positionAbs.x + other.physicsBody.centerAbs.x + nodeB.provisionalDeltaXAbs))
						.pow(2) +
							(nodeA.positionAbs.y + physicsBody.centerAbs.y + nodeA.provisionalDeltaYAbs - (nodeB.positionAbs.y + other.physicsBody.centerAbs.y + nodeB.provisionalDeltaYAbs))
								.pow(2)
				)
				radius + other.radius > distance
			}
			is Rectangle -> {
				other.contact(this)
			}
			else -> TODO("Not yet implemented")
		}
	}

	override fun revertMove(other: IShape) {
		when (other) {
			is Circle -> {
				// find min k: 0 <= k <= 1:
				// ((xa + k.dxa) - (xb + k.dxb))2 + ((ya + k.dya) - (yb + k.dyb))2 = (ra + rb)2

				//   (xa-xb)2 + 2k.(xa-xb).(dxa-dxb) + k2.(dxa-dxb)2
				// + (ya-yb)2 + 2k.(ya-yb).(dya-dyb) + k2.(dya-dyb)2
				// = (ra + rb)2
				//   ((dxa-dxb)2 + (dya-dyb)2).k2 + 2k.((xa-xb).(dxa-dxb) + (ya-yb).(dya-dyb)) + (xa-xb)2 + (ya-yb)2 - (ra + rb)2 = 0

				val nodeA = physicsBody.node!!
				val nodeB = other.physicsBody.node!!

				nodeA.getAbsProvDelta(false)
				nodeB.getAbsProvDelta(false)

				val pxa = nodeA.positionAbs.x + physicsBody.centerAbs.x
				val pya = nodeA.positionAbs.y + physicsBody.centerAbs.y
				val dxa = nodeA.provisionalDeltaXAbs
				val dya = nodeA.provisionalDeltaYAbs
				val pxb = nodeB.positionAbs.x + other.physicsBody.centerAbs.x
				val pyb = nodeB.positionAbs.y + other.physicsBody.centerAbs.y
				val dxb = nodeB.provisionalDeltaXAbs
				val dyb = nodeB.provisionalDeltaYAbs

				val a = (dxa - dxb).pow(2) + (dya - dyb).pow(2)
				val b = (pxa - pxb) * (dxa - dxb) + (pya - pyb) * (dya - dyb)
				val c = (pxa - pxb).pow(2) + (pya - pyb).pow(2) - (radius + other.radius).pow(2)
				val k = if (a == 0f && dxa != 0f && dya != 0f) {
					Log.e(TAG, "!-- 2 object move in the same way and collide: ${nodeA.name} - ${nodeB.name}")
					return
				} else {
					val xx = Utils.solveEquation2(a, b, c)
					if (xx == null) 1f
					else if (xx.first < 0 && xx.second!! < 0) throw Error("!-- both var is < 0: $xx")
					else if (xx.first >= 0 && xx.second!! >= 0) min(1f, min(xx.first, xx.second!!))
					else min(1f, xx.first)
				}

				kX = min(kX, k)
				kY = min(kY, k)
				other.kX = min(other.kX, k)
				other.kY = min(other.kY, k)

				// Log.d(TAG, "--  revertMove: $k | $physicsRevertCount")
			}
			is Rectangle -> {
				other.revertMove(this)
			}
			else -> TODO("Not yet implemented")
		}
	}
}