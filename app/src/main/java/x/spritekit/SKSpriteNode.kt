package x.spritekit

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.opengl.GLES20
import android.util.Log
import androidx.annotation.CallSuper
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import org.andengine.engine.camera.Camera
import org.andengine.entity.sprite.Sprite
import org.andengine.entity.sprite.vbo.HighPerformanceSpriteVertexBufferObject
import org.andengine.opengl.shader.PositionColorTextureCoordinatesShaderProgram
import org.andengine.opengl.shader.ShaderProgram
import org.andengine.opengl.util.GLState
import org.andengine.opengl.vbo.DrawType
import org.andengine.opengl.vbo.HighPerformanceVertexBufferObject
import org.andengine.util.adt.io.`in`.IInputStreamOpener
import org.andengine.util.color.Color
import x.core.graphics.Point
import x.core.graphics.Size
import kotlin.math.max

open class SKSpriteNode : SKDrawableObject {

	companion object {
		private const val TAG = "SKSpriteNode"
	}

	private var textureIsValid = false

	private var sizeIsFirstSet = false

	/**
	Used to choose the location in the sprite that maps to its 'position' in the parent's coordinate space. The valid interval for each input is from 0.0 up to and including 1.0.
	 */
	open var anchorPoint = Point(0.5f, 0.5f)

	public final override var size: Size
		get() = super.size
		set(value) {
			super.size = value
			sizeIsFirstSet = true
		}

	var texture: SKTexture? = null
		set(value) {
			if (value == field) return
			field = value

			if (!sizeIsFirstSet) { // set Size for the first time only
				size = value?.size() ?: Size.zero
			}

			bitmapToBeDrawn = null
			textureIsValid = false

			updateTextureCoordinates()
		}

	private var bitmapToBeDrawn: Bitmap? = null

	override var shaderProgram: ShaderProgram =
		PositionColorTextureCoordinatesShaderProgram.getInstance()
	override var vertexBufferObject: HighPerformanceVertexBufferObject =
		HighPerformanceSpriteVertexBufferObject(
			null, Sprite.SPRITE_SIZE, DrawType.STATIC, true,
			Sprite.VERTEXBUFFEROBJECTATTRIBUTES_DEFAULT
		)


	init {
		// why updateColor in init?
		// updateColor("1")
	}

	constructor(texture: SKTexture? = null) : super() {
		this.texture = texture
	}

	constructor(resources: Resources, @DrawableRes textureId: Int) : this(SKTexture(resources, textureId))

	@Deprecated("")
	private constructor(inputStreamOpener: IInputStreamOpener) : this(SKTexture(inputStreamOpener))

	constructor(@ColorInt color: Int, size: Size) : this() {
		this.size = size
		val bmp = Bitmap.createBitmap(max(size.width.toInt(), 1), max(size.height.toInt(), 1), Bitmap.Config.ARGB_8888);
		val canvas = Canvas(bmp)
		canvas.drawColor(color)
		texture = SKTexture(bmp)
	}

	@CallSuper
	override fun clone(): SKSpriteNode {
		val c = super.clone() as SKSpriteNode
		c.isClone = true

		c.anchorPoint = anchorPoint.clone()

		// must set vertexBufferObject before texture
		c.vertexBufferObject = HighPerformanceSpriteVertexBufferObject(
			null, Sprite.SPRITE_SIZE, DrawType.STATIC, true,
			Sprite.VERTEXBUFFEROBJECTATTRIBUTES_DEFAULT
		)
		c.updateVertices()

		c.textureIsValid = false

		texture?.let { c.texture = it.clone() }
		bitmapToBeDrawn?.let { c.bitmapToBeDrawn = it.copy(it.config, true) }

		c.isClone = false
		return c
	}

	@CallSuper
	override fun dispose(tag: String) {
		super.dispose(tag)

		texture?.let { if (!it.disposed) it.dispose("$TAG|$tag") }
		// texture = null
		bitmapToBeDrawn?.recycle()
	}

	fun setTexture(resources: Resources, textureId: Int) {
		texture = SKTexture(resources, textureId)
	}

	@CallSuper
	internal open fun load(textureManager: SKTextureManager, type: Int) {
		if (isHiddenAbs!!) return
		texture?.load(textureManager, type)
	}

	// -- canvas
	override fun doDraw(canvas: Canvas, sceneXScale: Float, sceneYScale: Float, sceneScaleHasChanged: Boolean) {
		val bitmap = texture?.bitmap ?: return

		if (bitmapToBeDrawn == null || !textureIsValid || !scaleIsValid || size.changed || sceneScaleHasChanged) {
			textureIsValid = true
			scaleIsValid = true
			size.changed = false

			// Log.d(TAG, "--  createScaledBitmap: $name - $bitmapToBeDrawn - $textureIsValid - $size - ($xScale x $yScale) | ${texture.name}")

			try {
				bitmapToBeDrawn = Bitmap.createScaledBitmap(
					bitmap.copy(bitmap.config, true),
					max(size.width, 1f).toInt(),
					max(size.height, 1f).toInt(),
					false
				)
			} catch (e: Throwable) {
				Log.e("$TAG|${this::class.simpleName}", "!-- createScaledBitmap: $e | $name | ${texture?.name} | $size")
				throw e
			}
		}
		val a = alphaAbs
		val paint: Paint? = if (a != 1f) {
			Paint().apply {
				alpha = (a * 255).toInt()
				// colorFilter =
			}
		} else null

		val pAbsScale = this.parent!!.scaleAbs
		val absPosition = positionAbs

		canvas.drawBitmap(
			bitmapToBeDrawn!!,
			Matrix().apply {  // IMPORTANT: scale -> rotate (in case scale == -1 for example)
				if (this@SKSpriteNode.zRotationAbs != 0f) preRotate(
					-this@SKSpriteNode.zRotationAbs360,
					this@SKSpriteNode.rotationCenterX ?: size.width * pAbsScale.first * anchorPoint.x,
					this@SKSpriteNode.rotationCenterY ?: size.height * pAbsScale.second * (1 - anchorPoint.y)
				)
				preScale(
					pAbsScale.first * sceneXScale * if (scaleAbs.first < 0) -1 else 1,
					pAbsScale.second * sceneYScale * if (scaleAbs.second < 0) -1 else 1,
					scaleCenterX ?: (size.width * anchorPoint.x),
					scaleCenterY ?: (size.height * (1 - anchorPoint.y))
				)
				postTranslate(
					// do not multiply with pAbsScale here (test with scaled BaseButtonNode)
					canvas.width / 2f + absPosition.x * sceneXScale - size.width * anchorPoint.x,
					canvas.height / 2f - absPosition.y * sceneYScale - size.height * (1 - anchorPoint.y)
				)
			},
			paint
		)
	}

	// -- GL
	@CallSuper
	override fun updateVertices() {
		(vertexBufferObject as HighPerformanceSpriteVertexBufferObject)
			.onUpdateVertices(size.width, size.height)
	}

	private fun updateTextureCoordinates() {
		if (texture == null) return
		(vertexBufferObject as HighPerformanceSpriteVertexBufferObject)
			.onUpdateTextureCoordinates(this)
	}

	@CallSuper
	override fun updateColor(tag: String) {
		(vertexBufferObject as HighPerformanceSpriteVertexBufferObject)
			.onUpdateColor(Color(1f, 1f, 1f, alphaAbs).abgrPackedFloat)
	}

	override fun applyTranslation(pGLState: GLState, viewSize: Size) {
		val absPosition = this.positionAbs
		// do not multiply with parent's absScale here (test with scaled BaseButtonNode)
		pGLState.translateModelViewGLMatrixf(
			viewSize.width / 2f + absPosition.x - size.width * anchorPoint.x,
			viewSize.height / 2f - absPosition.y - size.height * (1 - anchorPoint.y),
			0f
		)
	}

	final override fun applyRotation(pGLState: GLState, viewSize: Size) {
		if (zRotationAbs != 0f) {
			val pAbsScale = this.parent!!.scaleAbs

			pGLState.translateModelViewGLMatrixf(
				rotationCenterX ?: size.width * pAbsScale.first * anchorPoint.x,
				rotationCenterY ?: size.height * pAbsScale.second * (1 - anchorPoint.y),
				0f
			)
			pGLState.rotateModelViewGLMatrixf(-zRotationAbs360, 0f, 0f, 1f)
			pGLState.translateModelViewGLMatrixf(
				-(rotationCenterX ?: size.width * pAbsScale.first * anchorPoint.x),
				-(rotationCenterY ?: size.height * pAbsScale.second * (1 - anchorPoint.y)),
				0f
			)
		}
	}

	@CallSuper
	override fun drawSelf(glState: GLState, camera: Camera) {
		if (texture == null || texture!!.bitmapStream == null) return
		super.drawSelf(glState, camera)
	}

	@CallSuper
	override fun preDraw(glState: GLState) {
		super.preDraw(glState)

		texture!!.bitmapStream?.bind(glState)   // the bitmap can be null when change new texture
	}

	@CallSuper
	override fun doDraw() {
		vertexBufferObject.draw(GLES20.GL_TRIANGLE_STRIP, Sprite.VERTICES_PER_SPRITE)
	}
}