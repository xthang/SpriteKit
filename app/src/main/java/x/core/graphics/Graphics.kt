package x.core.graphics

import android.graphics.RectF

class Size : Cloneable {
	companion object {
		val zero: Size get() = Size(0, 0)
	}

	var width: Float = 0f
		set(value) {
			if (field != value) {
				field = value
				changed = true
			}
		}
	var height: Float = 0f
		set(value) {
			if (field != value) {
				field = value
				changed = true
			}
		}

	internal var changed = true

	constructor(width: Float, height: Float) {
		this.width = width
		this.height = height
	}

	constructor(width: Int, height: Int) : this(width.toFloat(), height.toFloat())
	constructor(width: Double, height: Double) : this(width.toFloat(), height.toFloat())

	public override fun clone(): Size {
		// Log.i("Size", "clone")
		return Size(width, height)
	}

	override fun equals(other: Any?): Boolean {
		return other != null && other is Size && width == other.width && height == other.height
	}

	override fun toString(): String {
		return "S($width, $height)"
	}
}

class Point : Cloneable {

	companion object {
		val zero: Point get() = Point(0, 0)
	}

	var x: Float = 0f
		set(value) {
			if (field != value) {
				field = value
				changed = true
				changed2 = true
			}
		}
	var y: Float = 0f
		set(value) {
			if (field != value) {
				field = value
				changed = true
				changed2 = true
			}
		}
	internal var changed = true
	internal var changed2 = true

	constructor(x: Float, y: Float) {
		this.x = x
		this.y = y
	}

	constructor(x: Int, y: Int) : this(x.toFloat(), y.toFloat())
	constructor(x: Double, y: Double) : this(x.toFloat(), y.toFloat())

	public override fun clone(): Point {
		// Log.i("Point", "clone")
		return Point(x, y)
	}

	override fun toString(): String {
		return "P($x, $y)"
	}
}

class Vector : Cloneable {

	companion object {
		val zero: Vector get() = Vector(0, 0)
	}

	var dx: Float = 0f
		set(value) {
			field = value
			changed = true
		}
	var dy: Float = 0f
		set(value) {
			field = value
			changed = true
		}
	var changed = true

	constructor(dx: Float, dy: Float) {
		this.dx = dx
		this.dy = dy
	}

	constructor(dx: Int, dy: Int) : this(dx.toFloat(), dy.toFloat())
	constructor(dx: Double, dy: Double) : this(dx.toFloat(), dy.toFloat())

	public override fun clone(): Point {
		return Point(dx, dy)
	}

	override fun toString(): String {
		return "V($dx, $dy)"
	}
}

enum class SKSceneScaleMode {
	fill, /* Scale the SKScene to fill the entire SKView. */
	aspectFill, /* Scale the SKScene to fill the SKView while preserving the scene's aspect ratio. Some cropping may occur if the view has a different aspect ratio. */
	aspectFit, /* Scale the SKScene to fit within the SKView while preserving the scene's aspect ratio. Some letterboxing may occur if the view has a different aspect ratio. */
	resizeFill /* Modify the SKScene's actual size to exactly match the SKView. */
}

enum class RectHorizontalAlignmentMode {
	center, left, right
}

enum class RectVerticalAlignmentMode {
	center, top, bottom
}

class Rect(var origin: Point, var size: Size) : RectF(
	origin.x - size.width / 2, origin.y - size.height / 2,
	origin.x + size.width / 2, origin.y + size.height / 2,
), Cloneable {

	companion object {
		val zero: Rect get() = Rect(0, 0, 0, 0)
	}

	var horizontalAlignmentMode = RectHorizontalAlignmentMode.center
	var verticalAlignmentMode = RectVerticalAlignmentMode.center

	val width: Float get() = size.width
	val height: Float get() = size.height
	val midX: Float
		get() = when (horizontalAlignmentMode) {
			RectHorizontalAlignmentMode.left -> origin.x + size.width / 2
			RectHorizontalAlignmentMode.right -> origin.x - size.width / 2
			else -> origin.x
		}
	val midY: Float
		get() = when (verticalAlignmentMode) {
			RectVerticalAlignmentMode.bottom -> origin.y + size.height / 2
			RectVerticalAlignmentMode.top -> origin.y - size.height / 2
			else -> origin.y
		}
	val maxX: Float
		get() = when (horizontalAlignmentMode) {
			RectHorizontalAlignmentMode.left -> origin.x + size.width
			RectHorizontalAlignmentMode.right -> origin.x
			else -> origin.x + size.width / 2f
		}
	val maxY: Float
		get() = when (verticalAlignmentMode) {
			RectVerticalAlignmentMode.bottom -> origin.y + size.height
			RectVerticalAlignmentMode.top -> origin.y
			else -> origin.y + size.height / 2f
		}
	val minX: Float
		get() = when (horizontalAlignmentMode) {
			RectHorizontalAlignmentMode.left -> origin.x
			RectHorizontalAlignmentMode.right -> origin.x - size.width
			else -> origin.x - size.width / 2f
		}
	val minY: Float
		get() = when (verticalAlignmentMode) {
			RectVerticalAlignmentMode.bottom -> origin.y
			RectVerticalAlignmentMode.top -> origin.y - size.height
			else -> origin.y - size.height / 2f
		}


	constructor(x: Float, y: Float, width: Float, height: Float) : this(
		Point(x, y), Size(width, height)
	)

	constructor(x: Double, y: Double, width: Double, height: Double) : this(
		x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat()
	)

	constructor(x: Int, y: Int, width: Int, height: Int) : this(
		x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat()
	)

	public override fun clone(): Rect {
		return Rect(origin, size)
	}

	override fun toString(): String {
		return "R($origin, $size, $horizontalAlignmentMode, $verticalAlignmentMode)"
	}
}
