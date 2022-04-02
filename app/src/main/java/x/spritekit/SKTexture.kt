package x.spritekit

import android.content.res.AssetManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.CallSuper
import androidx.annotation.DrawableRes
import org.andengine.opengl.texture.Texture
import org.andengine.opengl.texture.bitmap.BitmapTexture
import org.andengine.opengl.texture.bitmap.RawBitmapTexture
import org.andengine.util.IDisposable
import org.andengine.util.adt.io.`in`.IInputStreamOpener
import x.core.graphics.Rect
import x.core.graphics.Size

enum class SKTextureFilteringMode {
	nearest, linear
}

class SKTexture : Cloneable {

	companion object {
		private const val TAG = "SKTexture"
	}

	internal var name: String? = null

	internal var disposed: Boolean = false
		private set
	private var disposeInfo: String? = null

	//
	@DrawableRes
	private var textureId: Int? = null
	private var resources: Resources? = null

	private var assetManager: AssetManager? = null
	private var filePath: String? = null

	private var inputStreamOpener: IInputStreamOpener? = null

	internal var bitmap: Bitmap? = null
		private set
	internal var bitmapStream: Texture? = null

	private var texture: SKTexture? = null

	private val rect: Rect
	fun size(): Size {
		return rect.size.clone()
	}

	var rotate = false

	private var x = 0f
	private var y = 0f

	var u = 0f
	var u2 = 1f
	var v = 0f
	var v2 = 1f

	var filteringMode = SKTextureFilteringMode.linear


	constructor(bitmap: Bitmap) {
		this.bitmap = bitmap.copy(bitmap.config, true)
		rect = Rect(0, 0, bitmap.width, bitmap.height)
	}

	constructor(resources: Resources, textureId: Int) {
		this.resources = resources
		this.textureId = textureId

		val decodeOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
		BitmapFactory.decodeResource(resources, textureId, decodeOptions)
		rect = Rect(0, 0, decodeOptions.outWidth, decodeOptions.outHeight)
	}

	constructor(assetManager: AssetManager, filePath: String) {
		this.assetManager = assetManager
		this.filePath = filePath

		val decodeOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
		BitmapFactory.decodeStream(assetManager.open(filePath), null, decodeOptions)
		rect = Rect(0, 0, decodeOptions.outWidth, decodeOptions.outHeight)
	}

	@Deprecated("")
	internal constructor(inputStreamOpener: IInputStreamOpener) {
		this.inputStreamOpener = inputStreamOpener

		val decodeOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
		BitmapFactory.decodeStream(inputStreamOpener.open(), null, decodeOptions)
		rect = Rect(0, 0, decodeOptions.outWidth, decodeOptions.outHeight)
	}

	/**
	Create a texture that is a subrect of an existing texture. See textureRect property for details.

	@param rect the source rectangle to use in creating a logical copy of the given texture.
	@param texture the existing texture to reference in the copy.
	 */
	constructor(rect: Rect, texture: SKTexture) {
		this.texture = texture
		this.rect = rect

		updateUV()
	}

	@CallSuper
	public override fun clone(): SKTexture {
		// Log.d(TAG, "--  clone [$] $name@${hashCode()}")

		val c = super.clone() as SKTexture
		// c.rect = rect.clone()
		bitmap?.let {
			c.bitmap = if (!it.isRecycled) it.copy(it.config, true) else null
		}
		inputStreamOpener?.let { c.inputStreamOpener = it.clone() }
		c.bitmapStream = null

		return c
	}

	@CallSuper
	fun finalize() {
		if (!disposed) {
			dispose("finalize")
		}
	}

	@CallSuper
	internal fun dispose(tag: String) {
		// Log.d(TAG, "--  dispose [$tag] $name@${hashCode()}")
		if (disposed) throw IDisposable.AlreadyDisposedException("-- $tag | ${this::class.simpleName}@${hashCode()} | $name | $disposeInfo")

		disposed = true
		disposeInfo = tag

		// recycle bitmap if it is not managed by manager / textureAtlas
		if (textureId == null && filePath == null && texture == null) {
			bitmapStream?.unload("dispose [$tag] $name@${hashCode()}")
			bitmap?.recycle() // recycle bitmap after unload bitmapStream: for mTexturesToBeUnloaded to be added first
		}
	}

	@CallSuper
	internal fun load(textureManager: SKTextureManager, type: Int) {
		if (type == 1) {
			// load for Canvas rendering
			if (bitmap == null) {
				if (textureId != null)
					bitmap = textureManager.getResourceBitmap(TAG, resources!!, textureId!!)
				else if (filePath != null)
					bitmap = textureManager.getAssetBitmap(TAG, assetManager!!, filePath!!)
				else if (texture != null) {
					texture!!.load(textureManager, type)
					bitmap = Bitmap.createBitmap(
						texture!!.bitmap!!,
						x.toInt(), y.toInt(), rect.width.toInt(), rect.height.toInt()
					)
				}
			}
		} else if (type == 2) {
			// load for GL rendering
			if (bitmapStream == null) {
				// Log.d(TAG, "--  toLoad [$] $name@${hashCode()}")
				if (inputStreamOpener != null) {
					bitmapStream = BitmapTexture(textureManager.textureManager, inputStreamOpener)
				} else if (textureId != null) {
					bitmapStream = textureManager.getResourceBitmapTexture("$TAG|load|$name", resources!!, textureId!!)
				} else if (filePath != null) {
					bitmapStream = textureManager.getAssetBitmapTexture("$TAG|load|$name", assetManager!!, filePath!!)
				} else if (bitmap != null) {
					bitmapStream = RawBitmapTexture(textureManager.textureManager, bitmap).apply {
						name = "${this@SKTexture.name}@${this@SKTexture.hashCode()}"
					}
				} else if (texture != null) {
					texture!!.load(textureManager, type)
					bitmapStream = texture!!.bitmapStream
				}
			}
			if (!bitmapStream!!.isLoadedToHardware)
				bitmapStream!!.load()
		}
	}

	@CallSuper
	private fun updateUV() {
		texture?.let {
			val textureWidth = it.rect.width
			val textureHeight = it.rect.height

			val width = rect.width
			val height = rect.height

			x = rect.minX
			y = rect.minY

			u = x / textureWidth
			u2 = (x + (if (rotate) height else width)) / textureWidth

			v = y / textureHeight
			v2 = (y + (if (rotate) width else height)) / textureHeight
		}
	}
}