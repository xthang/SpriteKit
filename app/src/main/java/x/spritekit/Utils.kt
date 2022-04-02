package x.spritekit

import kotlin.math.pow
import kotlin.math.sqrt

object Utils {

	fun solveEquation2(a: Float, b: Float, c: Float): Pair<Float, Float?>? {
		return if (a == 0f)
			if (b == 0f) throw Error("!-  equation: a = 0, b = 0, c = $c")
			else -c / b to null
		else {
			val delta = b.pow(2) - a * c
			if (delta == 0f) null
			else (-b + sqrt(delta)) / a to (-b - sqrt(delta)) / a
		}
	}
}