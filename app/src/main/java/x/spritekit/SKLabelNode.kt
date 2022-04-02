package x.spritekit

import android.graphics.*
import android.graphics.Paint.Align
import android.opengl.GLES20
import android.text.TextPaint
import androidx.annotation.CallSuper
import androidx.annotation.ColorInt
import androidx.core.graphics.alpha
import org.andengine.engine.camera.Camera
import org.andengine.entity.text.AutoWrap
import org.andengine.entity.text.Text
import org.andengine.entity.text.exception.OutOfCharactersException
import org.andengine.entity.text.vbo.HighPerformanceTextVertexBufferObject
import org.andengine.opengl.font.Font
import org.andengine.opengl.font.FontFactory
import org.andengine.opengl.font.FontUtils
import org.andengine.opengl.shader.PositionColorTextureCoordinatesShaderProgram
import org.andengine.opengl.shader.ShaderProgram
import org.andengine.opengl.util.GLState
import org.andengine.opengl.vbo.DrawType
import org.andengine.opengl.vbo.HighPerformanceVertexBufferObject
import x.core.graphics.RectHorizontalAlignmentMode
import x.core.graphics.Size
import kotlin.math.roundToInt
import kotlin.properties.Delegates

enum class SKLabelVerticalAlignmentMode {
	baseline, center, top, bottom
}

enum class SKLabelHorizontalAlignmentMode {
	center, left, right
}

open class SKLabelNode : SKDrawableObject {

	companion object {
		private const val TAG = "SKLabelNode"
	}

	internal var fontIsValid = false
	private var tempFontIsValid = false

	var text: CharSequence? = null
		set(value) {
			if (field == value) return
			field = value
			onSetText("setText", value)
		}

	var typeface: Typeface = Typeface.DEFAULT
		set(value) {
			if (field == value) return
			field = value
			textPaint.typeface = value
			fontIsValid = false
			tempFontIsValid = false
			onSetText("setTypeface", text)
		}
	var fontSize: Float = 10f
		set(value) {
			val _value = (value * 2).roundToInt() / 2f
			if (field == _value) return
			field = _value
			textPaint.textSize = _value
			fontIsValid = false
			tempFontIsValid = false
			onSetText("setFontSize", text)
		}

	/**
	 * text block alignment
	 */
	var textAlign: Align
		set(value) {
			if (field == value) return
			field = value
			textPaint.textAlign = value
			frameHorizontalAlignment = when (value) {
				Align.LEFT -> RectHorizontalAlignmentMode.left
				Align.RIGHT -> RectHorizontalAlignmentMode.right
				else -> RectHorizontalAlignmentMode.center
			}
			updateVertices()
		}

	/**
	 * if text contains multiple lines, this is line alignment in text block
	 */
	var lineAlignmentMode: Align = Align.LEFT
		set(value) {
			if (field == value) return
			field = value
			updateVertices()
		}
	var verticalAlignmentMode = SKLabelVerticalAlignmentMode.baseline
		set(value) {
			if (field == value) return
			field = value
			fontIsValid = false
			updateVertices()
		}

	var autoWrap = AutoWrap.NONE
	var autoWrapWidth = 0f
	var leading = Text.LEADING_DEFAULT

	/**
	 * Base color that the text is rendered with (if supported by the font)
	 */
	@ColorInt
	var fontColor: Int = Color.WHITE
		set(value) {
			if (field == value) return
			field = value
			textPaint.color = value
			fontIsValid = false
			// tempFontIsValid = false
			glColorIsValid = false
			// updateColor("1")
		}

	var backgroundColor: Int? = null
		set(value) {
			field = value
			if (value != null) textPaint.bgColor = value
			fontIsValid = false
		}

	/**
	 * Controls the blending between the rendered text and a color. The valid interval of values is from 0.0 up to and including 1.0. A value above or below that interval is clamped to the minimum (0.0) if below or the maximum (1.0) if above.
	 */
	var colorBlendFactor: Float? = null

	var strokeWidth: Float? = null
		set(value) {
			if (field == value) return
			field = value!!
			textPaint.strokeWidth = value
			fontIsValid = false
			tempFontIsValid = false
		}
	var strokeColor: Int? = null
		set(value) {
			if (field == value) return
			field = value
			strokePaint?.color = value!!
			fontIsValid = false
			// tempFontIsValid = false
		}

	internal lateinit var font: Font
	private lateinit var tempFont: Font

	private var textPaint = TextPaint().apply {
		isAntiAlias = true
	}
	private var strokePaint: TextPaint? = null

	var lines = ArrayList<Pair<CharSequence, Float>>()

	// var lineWidths: ArrayList<Float> = arrayListOf()
	private var lineWidthMaximum = 0f
	var lineAlignmentWidth = 0f
	private var charactersToDraw: Int = 0
	private var vertexCountToDraw by Delegates.notNull<Int>()

	internal lateinit var bounds: RectF

	override var shaderProgram: ShaderProgram =
		PositionColorTextureCoordinatesShaderProgram.getInstance()
	override var vertexBufferObject: HighPerformanceVertexBufferObject =
		HighPerformanceTextVertexBufferObject(
			null, Text.LETTER_SIZE * (text?.length ?: 1), DrawType.STATIC, true, Text.VERTEXBUFFEROBJECTATTRIBUTES_DEFAULT
		)


	init {
		textAlign = Align.CENTER
	}

	constructor() : this(null)

	constructor(text: CharSequence?) {
		this.text = text

		// updateColor("2")
	}

	@CallSuper
	override fun clone(): SKLabelNode {
		val c = super.clone() as SKLabelNode

		// must set vertexBufferObject before others...
		c.vertexBufferObject = HighPerformanceTextVertexBufferObject(
			null, Text.LETTER_SIZE * (c.text?.length ?: 1), DrawType.STATIC, true, Text.VERTEXBUFFEROBJECTATTRIBUTES_DEFAULT
		)

		c.fontIsValid = false
		c.tempFontIsValid = false

		c.textPaint = TextPaint(textPaint)
		strokePaint?.let { c.strokePaint = TextPaint(it) }

		c.lines = arrayListOf()
		c.bounds = RectF(bounds)

		c.onSetText("clone", c.text)

		return c
	}

	override fun dispose(tag: String) {
		super.dispose(tag)

		// Log.d(TAG, "--  dispose [$tag]: $name")
	}

	// this is to compute size only
	@Throws(OutOfCharactersException::class)
	@CallSuper
	private fun onSetText(tag: String, text: CharSequence?) {
		// Log.i(TAG, "--  onSetText [$tag]: $name - $text - $fontSize")

		if (Text.LETTER_SIZE * (text?.length ?: 1) != vertexBufferObject.capacity) {
			vertexBufferObject.dispose()
			vertexBufferObject = HighPerformanceTextVertexBufferObject(
				null, Text.LETTER_SIZE * (text?.length ?: 1), DrawType.STATIC, true, Text.VERTEXBUFFEROBJECTATTRIBUTES_DEFAULT
			)
			glColorIsValid = false
		}

		lines.clear()
		// lineWidths.clear()

		if (text != null) {
			val font = if (this::font.isInitialized && fontIsValid) font
			else if (this::tempFont.isInitialized && tempFontIsValid) tempFont
			else if (strokeWidth == null && strokeColor == null) FontFactory.create(
				null, null,
				2048, 2048, typeface, fontSize, fontColor
			).also {
				tempFont = it
				tempFontIsValid = true
			}
			else FontFactory.createStroke(
				null, null,
				2048, 2048, typeface, fontSize, fontColor,
				strokeWidth!!, strokeColor!!, false
			).also {
				tempFont = it
				tempFontIsValid = true
			}

			val lines_: ArrayList<CharSequence> = arrayListOf()
			if (autoWrap == AutoWrap.NONE)
				FontUtils.splitLines(text, lines_) // TODO Add whitespace-trimming.
			else FontUtils.splitLines(font, text, lines_, autoWrap, autoWrapWidth)

			// if we use lines instead of lines_: when HighPerformanceTextVertexBufferObject.onUpdateVertices is called around this point: error happens (line 91)
			val numberOfLines: Int = lines_.size
			var maximumLineWidth = 0f
			for (i in 0 until numberOfLines) {
				val lineWidth = FontUtils.measureText(font, lines_[i])
				maximumLineWidth = maximumLineWidth.coerceAtLeast(lineWidth)
				lines.add(lines_[i] to lineWidth)
			}
			lineWidthMaximum = maximumLineWidth
			lineAlignmentWidth = if (autoWrap == AutoWrap.NONE) lineWidthMaximum
			else autoWrapWidth

			size = Size(
				lineAlignmentWidth,
				numberOfLines * font.lineHeight + (numberOfLines - 1) * leading
			)
			bounds = if (lines.size == 1) {
				FontUtils.getTextBounds(TAG, font, lines.single().first)
			} else RectF(0f, size.height, size.width, 0f)
			// Log.d(TAG, "--  [$tag] $text - $typeface - $name - $size - $bounds")
		} else {
			size = Size.zero
			bounds = RectF()
		}

		rotationCenterX = size.width * 0.5f
		rotationCenterY = size.height * 0.5f
		scaleCenterX = rotationCenterX
		scaleCenterY = rotationCenterY
	}

	@CallSuper
	private fun load(sceneXScale: Float, sceneYScale: Float) {
		if (!fontIsValid || !scaleIsValid) {
			fontIsValid = true
			scaleIsValid = true

			textPaint.apply {
				typeface = this@SKLabelNode.typeface
				textSize = this@SKLabelNode.fontSize * scaleAbs.second * sceneYScale // TODO: this is not scaled correctly
				textAlign = this@SKLabelNode.textAlign
				color = this@SKLabelNode.fontColor
				this@SKLabelNode.strokeWidth?.let {
					strokeWidth = it * scaleAbs.second * sceneYScale
				} // TODO: this is not scaled correctly
				// strokeJoin = Paint.Join.ROUND;
				// strokeMiter = 10f;
				// isAntiAlias = true

//				if (verticalAlignmentMode == SKLabelVerticalAlignmentMode.center) {
//					baselineShift = (textSize / 2 - textPaint.descent()).toInt()
//				} else {
//					baselineShift = 0
//				}
			}

			if (strokeColor != null) strokePaint = TextPaint(textPaint).apply {
				style = Paint.Style.STROKE
				color = strokeColor!!
			}
		}
	}

	@CallSuper
	internal fun toLoad(fontManager: SKFontManager) {
		if (!this::font.isInitialized || !fontIsValid || !font.isLoaded) {
			fontIsValid = true

			if (this::font.isInitialized) {
				fontManager.unCount(name ?: "", font)
			}

			font = if (strokeWidth == null && strokeColor == null)
				fontManager.getFont(name ?: "", typeface, fontSize, fontColor)
			else
				fontManager.getFont(name ?: "", typeface, fontSize, fontColor, strokeWidth!!, strokeColor!!)
			// todo: getFont by background
			if (backgroundColor != null) font.setBackgroundColor(backgroundColor!!)

			updateVertices()
		}
	}

	@CallSuper
	fun setCharactersToDraw(charactersToDraw: Int) {
		if (charactersToDraw > text?.length ?: 0) {
			throw OutOfCharactersException("!-  Characters: maximum: '${text?.length ?: 0}' required: '$charactersToDraw'.")
		}
		this.charactersToDraw = charactersToDraw
		this.vertexCountToDraw = charactersToDraw * Text.VERTICES_PER_LETTER
	}

	// -- Canvas
	@CallSuper
	override fun drawSelf(canvas: Canvas, sceneXScale: Float, sceneYScale: Float, sceneScaleHasChanged: Boolean) {
		this.text ?: return
		super.drawSelf(canvas, sceneXScale, sceneYScale, sceneScaleHasChanged)
	}

	override fun doDraw(canvas: Canvas, sceneXScale: Float, sceneYScale: Float, sceneScaleHasChanged: Boolean) {
		load(sceneXScale, sceneYScale)

		val absPosition = positionAbs
		val a = alphaAbs
		textPaint.alpha = (a * 255).toInt()
		strokePaint?.alpha = (a * 255).toInt()

		val fontMetrics = textPaint.fontMetrics

		val rawHeight: Float = bounds.height()
		val lineHeight = fontMetrics.descent - fontMetrics.ascent
		val textHeight: Float = frame.height
		val bottom: Float = bounds.bottom
		val descend = fontMetrics.descent

		val lineCount = lines.size
		for (row in 0 until lineCount) {
			val line = lines[row]

			val xBase = when (lineAlignmentMode) {
				Align.RIGHT -> lineAlignmentWidth - lines[row].second
				Align.CENTER -> (lineAlignmentWidth - lines[row].second) * 0.5f
				else -> 0f
			}

			// Thang.X
			var yBase: Float = row * (lineHeight + leading)
			// todo: fix this
			when (verticalAlignmentMode) {
				SKLabelVerticalAlignmentMode.center -> yBase -= textHeight / 2 - lineHeight / 3 - descend - rawHeight / 2 + bottom
				else -> {
				}
			}

			canvas.drawText(
				line.first.toString(),
				(canvas.width) / 2 + (xBase + absPosition.x) * sceneXScale,
				(canvas.height) / 2 + (yBase - absPosition.y) * sceneYScale,
				textPaint
			)
			strokePaint?.let {
				canvas.drawText(
					line.first.toString(),
					(canvas.width) / 2 + (xBase + absPosition.x) * sceneXScale,
					(canvas.height) / 2 + (yBase - absPosition.y) * sceneYScale,
					it
				)
			}
		}
	}

	// -- GL
	@CallSuper
	override fun updateVertices() {
		if (text == null || !this::font.isInitialized) return
		(vertexBufferObject as HighPerformanceTextVertexBufferObject).onUpdateVertices(this)
	}

	@CallSuper
	override fun updateColor(tag: String) {
		if (text == null) return
		val a = alphaAbs
		(vertexBufferObject as HighPerformanceTextVertexBufferObject)
			.onUpdateColor(
				org.andengine.util.color.Color(
					1f, 1f, 1f,
					if (a != 1f) a else fontColor.alpha / 255f
				).abgrPackedFloat,
				text?.length ?: 0
			)
	}

	@CallSuper
	override fun drawSelf(glState: GLState, camera: Camera) {
		if (text != null) {
			super.drawSelf(glState, camera)
		}
	}

	@CallSuper
	override fun preDraw(glState: GLState) {
		super.preDraw(glState)

		font.texture.bind(glState)
	}

	override fun doDraw() {
		vertexBufferObject.draw(GLES20.GL_TRIANGLES, vertexCountToDraw)
	}
}