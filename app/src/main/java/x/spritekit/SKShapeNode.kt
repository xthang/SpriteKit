package x.spritekit

import android.graphics.*
import androidx.annotation.ColorInt
import androidx.core.graphics.alpha
import x.core.graphics.Size
import kotlin.math.max

open class SKShapeNode : SKSpriteNode {

	companion object {
		private const val TAG = "SKShapeNode"
	}

	private var bitmapIsValid = false

	/**
	The CGPath to be drawn (in the Node's coordinate space)
	 */
	open var path: Path? = null
		set(value) {
			field = value

			size = if (value != null) {
				val pathRect = RectF()
				value.computeBounds(pathRect, true)
				Size(pathRect.width(), pathRect.height())
			} else {
				Size.zero
			}

			bitmapIsValid = false
		}
	private var pathToDraw: Path? = null

	/**
	The color to draw the path with. (for no stroke use [SKColor clearColor]). Defaults to [SKColor whiteColor].
	 */
	@ColorInt
	open var strokeColor: Int
		set(value) {
			if (field == value) return
			field = value
			strokePaint.color = value
			bitmapIsValid = false
		}

	/**
	The color to fill the path with. Defaults to [SKColor clearColor] (no fill).
	 */
	@ColorInt
	open var fillColor: Int
		set(value) {
			if (field == value) return
			field = value
			paint.color = value
			bitmapIsValid = false
		}

	/**
	Sets the blend mode to use when composing the shape with the final framebuffer.
	@see SKNode.SKBlendMode
	 */
//	open var blendMode: SKBlendMode

	/**
	If set to YES, the path stroke edges and caps is smoothed (antialiased) when drawn.
	 */
	open var isAntialiased: Boolean
		set(value) {
			if (field == value) return
			field = value
			paint.isAntiAlias = value
			strokePaint.isAntiAlias = value
			bitmapIsValid = false
		}

	/**
	The width used to stroke the path. Widths larger than 2.0 may result in artifacts. Defaults to 1.0.
	 */
	open var lineWidth: Float
		set(value) {
			if (field == value) return
			field = value
			paint.strokeWidth = value
			strokePaint.strokeWidth = value
			bitmapIsValid = false
		}

	/**
	Add a glow to the path stroke of the specified width. Defaults to 0.0 (no glow)
	 */
	open var glowWidth: Float
		set(value) {
			if (field == value) return
			field = value
			bitmapIsValid = false
		}

	override var shadowRadius: Float? = null
		set(value) {
			if (field == value) return
			field = value!!
			if (shadowColor != null) strokePaint.setShadowLayer(value, 0f, 0f, shadowColor!!)
			bitmapIsValid = false
		}

	@ColorInt
	override var shadowColor: Int? = null
		set(value) {
			if (field == value) return
			field = value!!
			if (shadowRadius != null) strokePaint.setShadowLayer(shadowRadius!!, 0f, 0f, value)
			bitmapIsValid = false
		}

	/**
	The cap type that should be used when stroking a non-closed path
	 */
//	open var lineCap: CGLineCap

	/**
	The join type that should be used when stroking a path
	 */
//	open var lineJoin: CGLineJoin

	/**
	When a miter join is used, the maximum ratio of miter length to line with to be used
	 */
//	open var miterLimit: Float

	/**
	The length of the node's path if it were to be stroked
	 */
//	open var lineLength: CGFloat { get }

	/* An optional Texture used for the filling the Shape */
	open var fillTexture: SKTexture? = null

	/* An optional SKShader used for the filling the Shape */
//	open var fillShader: SKShader?

	/* An optional Texture used for the Shape's stroke. */
	open var strokeTexture: SKTexture? = null

	/* An optional SKShader used for the Shape's Stroke. */
//	open var strokeShader: SKShader?

	private var centered = false

	private var paint = Paint().apply {
		style = Paint.Style.FILL
	}
	private var strokePaint: Paint = Paint().apply {
		style = Paint.Style.STROKE
		isDither = true
		strokeJoin = Paint.Join.ROUND
		strokeCap = Paint.Cap.ROUND
	}

	init {
		strokeColor = Color.WHITE
		fillColor = 0x00FFFFFF
		isAntialiased = true
		lineWidth = 1f
		glowWidth = 0f
		// updateColor(1)
	}

	constructor() : super()

	constructor(rectOf: Size, cornerRadius: Float) : super() {
		this.path = Path().apply {
			addRoundRect(
				RectF(0F, 0F, rectOf.width, rectOf.height),
				cornerRadius,
				cornerRadius,
				Path.Direction.CCW
			)
		}
	}

	constructor(path: Path, centered: Boolean) : super() {
		this.path = path
		this.centered = centered
	}

	// TODO: this
	override fun clone(): SKShapeNode {
		val c = super.clone() as SKShapeNode
		c.isClone = true

		c.bitmapIsValid = false

		path?.let { c.path = Path(it) }
		c.pathToDraw = null

		c.paint = Paint(paint)
		c.strokePaint = Paint(strokePaint)
		c.texture = null

		c.isClone = false
		return c
	}

	override fun dispose(tag: String) {
		super.dispose(tag)

		path = null
	}

	// -- canvas
	override fun drawSelf(canvas: Canvas, sceneXScale: Float, sceneYScale: Float, sceneScaleHasChanged: Boolean) {
		if (path == null) return
		super.drawSelf(canvas, sceneXScale, sceneYScale, sceneScaleHasChanged)
	}

	override fun doDraw(canvas: Canvas, sceneXScale: Float, sceneYScale: Float, sceneScaleHasChanged: Boolean) {
		if (pathToDraw == null || !bitmapIsValid || positionAbs.changed || !scaleIsValid || sceneScaleHasChanged) {
			bitmapIsValid = true
			positionAbs.changed = false
			scaleIsValid = true

			// must get self.absScale
			// val parentAbsScale = getParentAbsScale()!!

			pathToDraw = Path(path)
			pathToDraw!!.transform(Matrix().apply {
				setScale(scaleAbs.first * sceneXScale, scaleAbs.second * sceneYScale)
			})
			val pathRect = RectF()
			pathToDraw!!.computeBounds(pathRect, true)
			pathToDraw!!.offset(
				(canvas.width - pathRect.width()) / 2f + positionAbs.x * sceneXScale,
				(canvas.height - pathRect.height()) / 2f - positionAbs.y * sceneYScale
			)
			// Log.d(TAG, "--  reload pathToDraw: $name - $texture - $bitmapIsValid - ($scaleX x $scaleY) | $rawSize | $size")
		}

		val a = alphaAbs

		paint.alpha = if (a != 1f) (a * 255).toInt() else fillColor.alpha
		canvas.drawPath(pathToDraw!!, paint)
		if (lineWidth != 0f) {
			strokePaint.alpha = if (a != 1f) (a * 255).toInt() else fillColor.alpha
			canvas.drawPath(pathToDraw!!, strokePaint)
		}
	}

	// GL
	override fun load(textureManager: SKTextureManager, type: Int) {
		if (path == null || isHiddenAbs!!) return
		if (type == 2) loadBitmap()
		super.load(textureManager, type)
	}

	private fun loadBitmap() {
		path?.let {
			if (texture == null || !bitmapIsValid) {
				bitmapIsValid = true
				// Log.d(TAG, "--  loadBitmap: $name@${hashCode()} - $texture - $bitmapIsValid - ($scaleX x $scaleY) | $rawSize | $size")
				val bitmap = createBitmap(it)
				texture = SKTexture(bitmap).apply { name = this@SKShapeNode.name }
			}
		}
	}

	private fun createBitmap(path: Path): Bitmap {
		pathToDraw = Path(path)
		pathToDraw!!.offset(max(lineWidth / 2, shadowRadius ?: 0f), max(lineWidth / 2, shadowRadius ?: 0f))

		val bitmap = Bitmap.createBitmap(
			max(1f, size.width + max(lineWidth, 2 * (shadowRadius ?: 0f))).toInt(),
			max(1f, size.height + max(lineWidth, 2 * (shadowRadius ?: 0f))).toInt(),
			Bitmap.Config.ARGB_8888
		)
		val canvas = Canvas(bitmap)

		// val a = getAbsAlpha()

		// paint.alpha = if (a != 1f) (a * 255).toInt() else fillColor.alpha
		if (shadowRadius != null && shadowColor != null) paint.setShadowLayer(shadowRadius!!, 0f, 0f, shadowColor!!)
		else paint.clearShadowLayer()
		canvas.drawPath(pathToDraw!!, paint)
		if (lineWidth != 0f) {
			// strokePaint.alpha = if (a != 1f) (a * 255).toInt() else fillColor.alpha
			canvas.drawPath(pathToDraw!!, strokePaint)
		}

//		bitmap = bitmap.scale(
//			(bitmap.width * scaleX).toInt(), (bitmap.width * scaleY).toInt(), false
//		)

		return bitmap
	}
}