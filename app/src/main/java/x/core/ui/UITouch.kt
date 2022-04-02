package x.core.ui

import android.view.View
import x.core.graphics.Point
import x.spritekit.SKNode

open class UITouch(
	val x: Float, val y: Float, val nodeX: Float, val nodeY: Float, val pointerID: Int
) {

	companion object {
		private const val TAG = "UITouch"
	}

	override fun toString(): String {
		return "${javaClass.simpleName}$${hashCode()}=$x-$y|$nodeX-$nodeY|$pointerID"
	}

	open fun locationIn(view: View?): Point {
		return Point.zero
	}

	open fun previousLocation(inView: View?): Point {
		return Point.zero
	}

	// Allow conversion of UITouch coordinates to scene-space */
	open fun locationIn(node: SKNode): Point {
		return Point.zero
	}
}
