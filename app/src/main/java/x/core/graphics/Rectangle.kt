package x.core.graphics

import android.graphics.Path
import android.graphics.RectF
import x.core.Utils
import kotlin.math.min
import kotlin.math.pow

class Rectangle(private val size: Size) : IShape() {

	companion object {
		private val TAG = Rectangle::class.simpleName
	}

	override val area: Float get() = size.width * size.height

	override fun getPath(): Path {
		return Path().apply {
			addRect(
				RectF(-size.width / 2, -size.height / 2, size.width / 2, size.height / 2),
				Path.Direction.CCW
			)
		}
	}

	override fun contact(other: IShape): Boolean {
		val nodeA = physicsBody.node!!
		val nodeB = other.physicsBody.node!!

		val provXA = nodeA.positionAbs.x + physicsBody.centerAbs.x + nodeA.provisionalDeltaXAbs
		val provYA = nodeA.positionAbs.y + physicsBody.centerAbs.y + nodeA.provisionalDeltaYAbs
		val provXB = nodeB.positionAbs.x + other.physicsBody.centerAbs.x + nodeB.provisionalDeltaXAbs
		val provYB = nodeB.positionAbs.y + other.physicsBody.centerAbs.y + nodeB.provisionalDeltaYAbs

		when (other) {
			is Rectangle -> {
				return provXA - size.width / 2 < provXB + other.size.width / 2
						&& provXB - other.size.width / 2 < provXA + size.width / 2
						&& provYA - size.height / 2 < provYB + other.size.height / 2
						&& provYB - other.size.height / 2 < provYA + size.height / 2
			}
			is Circle -> {
				// Find the nearest point on the rectangle to the center of the circle
				val nX = (provXA - size.width / 2).coerceAtLeast(provXB.coerceAtMost(provXA + size.width / 2))
				val nY = (provYA - size.height / 2).coerceAtLeast(provYB.coerceAtMost(provYA + size.height / 2))

				// Find the distance between the nearest point and the center of the circle
				// Distance between 2 points, (x1, y1) & (x2, y2) in 2D Euclidean space is
				// ((x1-x2)**2 + (y1-y2)**2)**0.5
				val Dx = nX - provXB
				val Dy = nY - provYB

				// if (true) Log.d(TAG, "--  contact 1: A($provXA, $provYA) | B($provXB, $provYB) | ($nX, $nY) | ($Dx, $Dy) | ${other.radius} | ${Dx * Dx + Dy * Dy - other.radius.pow(2)}")
				return Dx * Dx + Dy * Dy < other.radius.pow(2)
			}
			else -> {
				TODO("Not yet implemented")
			}
		}
	}

	override fun revertMove(other: IShape) {
		val nodeA = physicsBody.node!!
		val nodeB = other.physicsBody.node!!

		val pxa = nodeA.positionAbs.x + physicsBody.centerAbs.x
		val pya = nodeA.positionAbs.y + physicsBody.centerAbs.y
		val dxa = nodeA.provisionalDeltaXAbs
		val dya = nodeA.provisionalDeltaYAbs
		val pxb = nodeB.positionAbs.x + other.physicsBody.centerAbs.x
		val pyb = nodeB.positionAbs.y + other.physicsBody.centerAbs.y
		val dxb = nodeB.provisionalDeltaXAbs
		val dyb = nodeB.provisionalDeltaYAbs

		// Log.d(TAG, "--  revertMove: $physicsRevertCount || $pa | $size | $dxa-$dya >><< $pb | $ | $dxb-$dyb")

		val possibilities = mutableSetOf<Float>()
		var k = 1f

		// contact point is left/right / top/bot / both
		var isLeft = false
		var isRight = false
		var isTop = false
		var isBot = false

		when (other) {
			is Rectangle -> {
				// find k: 0 <= k <= 1
				//    leftA + k.dXA = rightB + k.dXB
				// or leftB + k.dXB = rightA + k.dXA
				// or topA + k.dYA = botB + k.dYB
				// or topB + k.dYB = botA + k.dYA

				if (dxa != dxb) {
					val k1 = (pxa - size.width / 2 - (pxb + other.size.width / 2)) / (dxb - dxa)
					if (k1 >= 0) {
						possibilities.add(k1)
						if (k1 <= k) {
							k = k1
							isLeft = true
						}
					}
					val k2 = (pxa + size.width / 2 - (pxb - other.size.width / 2)) / (dxb - dxa)
					if (k2 >= 0) {
						possibilities.add(k2)
						if (k2 <= k) {
							k = k2
							isRight = true
						}
					}
				}
				if (dya != dyb) {
					val k1 = (pya - size.height / 2 - (pyb + other.size.height / 2)) / (dyb - dya)
					if (k1 >= 0) {
						possibilities.add(k1)
						if (k1 <= k) {
							k = k1
							isBot = true
						}
					}
					val k2 = (pya + size.height / 2 - (pyb - other.size.height / 2)) / (dyb - dya)
					if (k2 >= 0) {
						possibilities.add(k2)
						if (k2 <= k) {
							k = k2
							isTop = true
						}
					}
				}

				if (possibilities.isEmpty()) k = 0f

				if ((isLeft && dxa < 0) || (isRight && dxa > 0)) kX = min(kX, k)
				if ((isTop && dya > 0) || (isBot && dya < 0)) kY = min(kY, k)
				if ((isLeft && dxb > 0) || (isRight && dxb < 0)) other.kX = min(other.kX, k)
				if ((isTop && dyb < 0) || (isBot && dyb > 0)) other.kY = min(other.kY, k)

				// Log.d(TAG, "--  revertMove DONE: $possibilities | $k | $isLeft-$isRight-$isTop-$isBot")
			}
			is Circle -> {
				// nearestX = max(leftA + k.dXC, min(rightA + k.dXA, xC + k.dXC))
				// nearestY = max(botA  + k.dYC, min(topA   + k.dYA, yC + k.dYC))
				// find min k: 0 <= k <= 1
				// (nearestX - (xC + k.dXC)) ^2 + (nearestY - (yC + k.dYC)) ^2 = r^2

				// ~ max(leftA - xC + k.(dXA - dXC), min(rightA - xC + k.(dXA - dXC), 0)) ^2
				// + max(botA  - yC + k.(dYA - dYC), min(topA   - yC + k.(dYA - dYC), 0)) ^2
				// = r^2
				// --> 8 possibilities

				val leftA_xC = pxa - size.width / 2 - pxb
				val rightA_xC = pxa + size.width / 2 - pxb
				val dXA_dXC = dxa - dxb
				val botA_yC = pya - size.height / 2 - pyb
				val topA_yC = pya + size.height / 2 - pyb
				val dYA_dYC = dya - dyb
				val r = other.radius

				// Log.d(TAG, "--  revertMove BEGIN: $leftA_xC | $rightA_xC | $dXA_dXC || $botA_yC | $topA_yC | $dYA_dYC")

				// if (dXA_dXC == 0f && dxa != 0f && dYA_dYC == 0f && dya != 0f) {
				if (dXA_dXC == 0f && dYA_dYC == 0f) {
					// Log.e(TAG, "!-- 2 object move/stay in the same way and collide: ${nodeA.name} - ${nodeB.name}")
					return
				}

				/*
				_2_|_6_|_4_
				_7_|___|_8_
				 1 | 5 | 3
				 */

				// case 1:
				// 0 < leftA - xC + k.(dXA - dXC)
				// 0 < botA - yC + k.(dYA - dYC)
				// ((dXA - dXC)2 + (dYA - dYC)2).k2 + 2k.((leftA - xC)(dXA - dXC) + (botA - yC)(dYA - dYC)) + (leftA - xC)2 + (botA - yC)2 - r2 == 0
				Utils.solveEquation2(
					dXA_dXC.pow(2) + dYA_dYC.pow(2),
					leftA_xC * dXA_dXC + botA_yC * dYA_dYC,
					leftA_xC.pow(2) + botA_yC.pow(2) - r.pow(2)
				)?.let { xx ->
					if (xx.first in 0.0..1.0 && leftA_xC + xx.first * dXA_dXC > 0 && botA_yC + xx.first * dYA_dYC > 0) {
						possibilities.add(xx.first)
						if (xx.first <= k) {
							k = xx.first
							isLeft = true
							isRight = false
							isTop = false
							isBot = true
						}
					}
					if (xx.second!! in 0.0..1.0 && leftA_xC + xx.second!! * dXA_dXC > 0 && botA_yC + xx.second!! * dYA_dYC > 0) {
						possibilities.add(xx.second!!)
						if (xx.second!! <= k) {
							k = xx.second!!
							isLeft = true
							isRight = false
							isTop = false
							isBot = true
						}
					}
				}

				// case 2:
				// 0 < leftA - xC + k.(dXA - dXC)
				// topA - yC + k.(dYA - dYC) < 0
				// ((dXA - dXC)2 + (dYA - dYC)2).k2 + 2k.((leftA - xC)(dXA - dXC) + (topA - yC)(dYA - dYC)) + (leftA - xC)2 + (topA - yC)2 - r2 == 0
				Utils.solveEquation2(
					dXA_dXC.pow(2) + dYA_dYC.pow(2),
					leftA_xC * dXA_dXC + topA_yC * dYA_dYC,
					leftA_xC.pow(2) + topA_yC.pow(2) - r.pow(2)
				)?.let { xx ->
					if (xx.first in 0.0..1.0 && leftA_xC + xx.first * dXA_dXC > 0 && topA_yC + xx.first * dYA_dYC < 0) {
						possibilities.add(xx.first)
						if (xx.first <= k) {
							k = xx.first
							isLeft = true
							isRight = false
							isTop = true
							isBot = false
						}
					}
					if (xx.second!! in 0.0..1.0 && leftA_xC + xx.second!! * dXA_dXC > 0 && topA_yC + xx.second!! * dYA_dYC < 0) {
						possibilities.add(xx.second!!)
						if (xx.second!! <= k) {
							k = xx.second!!
							isLeft = true
							isRight = false
							isTop = true
							isBot = false
						}
					}
				}

				// case 3:
				// rightA - xC + k.(dXA - dXC) < 0
				// 0 < botA - yC + k.(dYA - dYC)
				// ((dXA - dXC)2 + (dYA - dYC)2).k2 + 2k.((rightA - xC)(dXA - dXC) + (botA - yC)(dYA - dYC)) + (rightA - xC)2 + (botA - yC)2 - r2 == 0
				Utils.solveEquation2(
					dXA_dXC.pow(2) + dYA_dYC.pow(2),
					rightA_xC * dXA_dXC + botA_yC * dYA_dYC,
					rightA_xC.pow(2) + botA_yC.pow(2) - r.pow(2)
				)?.let { xx ->
					if (xx.first in 0.0..1.0 && rightA_xC + xx.first * dXA_dXC < 0 && botA_yC + xx.first * dYA_dYC > 0) {
						possibilities.add(xx.first)
						if (xx.first <= k) {
							k = xx.first
							isLeft = false
							isRight = true
							isTop = false
							isBot = true
						}
					}
					if (xx.second!! in 0.0..1.0 && rightA_xC + xx.second!! * dXA_dXC < 0 && botA_yC + xx.second!! * dYA_dYC > 0) {
						possibilities.add(xx.second!!)
						if (xx.second!! <= k) {
							k = xx.second!!
							isLeft = false
							isRight = true
							isTop = false
							isBot = true
						}
					}
				}

				// case 4:
				// rightA - xC + k.(dXA - dXC) < 0
				// topA - yC + k.(dYA - dYC) < 0
				// ((dXA - dXC)2 + (dYA - dYC)2).k2 + 2k.((rightA - xC)(dXA - dXC) + (topA - yC)(dYA - dYC)) + (rightA - xC)2 + (topA - yC)2 - r2 == 0
				Utils.solveEquation2(
					dXA_dXC.pow(2) + dYA_dYC.pow(2),
					rightA_xC * dXA_dXC + topA_yC * dYA_dYC,
					rightA_xC.pow(2) + topA_yC.pow(2) - r.pow(2)
				)?.let { xx ->
					if (xx.first in 0.0..1.0 && rightA_xC + xx.first * dXA_dXC < 0 && topA_yC + xx.first * dYA_dYC < 0) {
						possibilities.add(xx.first)
						if (xx.first <= k) {
							k = min(k, xx.first)
							isLeft = false
							isRight = true
							isTop = true
							isBot = false
						}
					}
					if (xx.second!! in 0.0..1.0 && rightA_xC + xx.second!! * dXA_dXC < 0 && topA_yC + xx.second!! * dYA_dYC < 0) {
						possibilities.add(xx.second!!)
						if (xx.second!! <= k) {
							k = xx.second!!
							isLeft = false
							isRight = true
							isTop = true
							isBot = false
						}
					}
				}

				// case 5:
				// leftA - xC + k.(dXA - dXC) <= 0 <= rightA - xC + k.(dXA - dXC)
				// 0 < botA - yC + k.(dYA - dYC)
				// botA - yC + k.(dYA - dYC) = r
				((r - botA_yC) / dYA_dYC).let {
					if (it in 0.0..1.0 && leftA_xC + it * dXA_dXC <= 0 && rightA_xC + it * dXA_dXC >= 0) {
						possibilities.add(it)
						if (it <= k) {
							k = it
							isLeft = false
							isRight = false
							isTop = false
							isBot = true
						}
					}
				}

				// case 6:
				// leftA - xC + k.(dXA - dXC) <= 0 <= rightA - xC + k.(dXA - dXC)
				// topA - yC + k.(dYA - dYC) < 0
				// topA - yC + k.(dYA - dYC) = -r
				((-r - topA_yC) / dYA_dYC).let {
					if (it in 0.0..1.0 && leftA_xC + it * dXA_dXC <= 0 && rightA_xC + it * dXA_dXC >= 0) {
						possibilities.add(it)
						if (it <= k) {
							k = it
							isLeft = false
							isRight = false
							isTop = true
							isBot = false
						}
					}
				}

				// case 7:
				// 0 < leftA - xC + k.(dXA - dXC)
				// botA - yC + k.(dYA - dYC) <= 0 <= topA - yC + k.(dYA - dYC)
				// leftA - xC + k.(dXA - dXC) = r
				((r - leftA_xC) / dXA_dXC).let {
					if (it in 0.0..1.0 && botA_yC + it * dYA_dYC <= 0 && topA_yC + it * dYA_dYC >= 0) {
						possibilities.add(it)
						if (it <= k) {
							k = it
							isLeft = true
							isRight = false
							isTop = false
							isBot = false
						}
					}
				}

				// case 8:
				// rightA - xC + k.(dXA - dXC) < 0
				// botA - yC + k.(dYA - dYC) <= 0 <= topA - yC + k.(dYA - dYC)
				// rightA - xC + k.(dXA - dXC) = -r
				((-r - rightA_xC) / dXA_dXC).let {
					if (it in 0.0..1.0 && botA_yC + it * dYA_dYC <= 0 && topA_yC + it * dYA_dYC >= 0) {
						possibilities.add(it)
						if (it <= k) {
							k = it
							isLeft = false
							isRight = true
							isTop = false
							isBot = false
						}
					}
				}

				if (possibilities.size > 2) throw Error("!-- possibilities.size: ${possibilities.size}")

				if (possibilities.isEmpty()) k = 0f

				if ((isLeft && dxa < 0) || (isRight && dxa > 0)) kX = min(kX, k)
				if ((isTop && dya > 0) || (isBot && dya < 0)) kY = min(kY, k)
				other.kX = min(other.kX, k)
				other.kY = min(other.kY, k)

				// Log.d(TAG, "--  revertMove DONE: $possibilities | $k | $isLeft-$isRight-$isTop-$isBot")
			}
			else -> TODO("Not yet implemented")
		}
	}
}