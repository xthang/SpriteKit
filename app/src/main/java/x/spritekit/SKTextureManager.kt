package x.spritekit

import android.content.res.AssetManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.annotation.CallSuper
import androidx.annotation.IdRes
import org.andengine.opengl.texture.TextureManager
import org.andengine.opengl.texture.bitmap.AssetBitmapTexture
import org.andengine.opengl.texture.bitmap.ResourceBitmapTexture

class SKTextureManager(internal val textureManager: TextureManager? = null) {

	interface TextureData {
		var count: Int
	}

	internal data class BitmapTextureData(val bitmap: Bitmap, override var count: Int) : TextureData
	internal data class ResourceBitmapTextureData(val resourceBitmapTexture: ResourceBitmapTexture, override var count: Int) :
		TextureData

	internal data class AssetBitmapTextureData(val assetBitmapTexture: AssetBitmapTexture, override var count: Int) :
		TextureData

	companion object {
		private val TAG = SKTextureManager::class.simpleName!!
	}

	private var bitmapTextures: HashMap<Int, BitmapTextureData> = hashMapOf()
	private var bitmapTextures2: HashMap<String, BitmapTextureData> = hashMapOf()
	private var resourceBitmapTextures: HashMap<Int, ResourceBitmapTextureData> = hashMapOf()
	private var assetBitmapTextures: HashMap<String, AssetBitmapTextureData> = hashMapOf()

	@CallSuper
	fun finalize() {
		Log.d(TAG, "~~~~~~~")
	}

	internal fun destroy(tag: String) {
		Log.d(TAG, "--  destroy [$tag]")

		// do not recycle. The bitmap maybe the same after TextureManager is recreated
		//bitmapTextures.forEach {
		//	it.value.bitmap.recycle()
		//}
		bitmapTextures.clear()
		resourceBitmapTextures.clear()
	}

	internal fun unCount(tag: String, @IdRes id: Int) {
		(bitmapTextures[id] ?: resourceBitmapTextures[id])!!.count--
	}

	internal fun getResourceBitmap(tag: String, resources: Resources, @IdRes textureId: Int): Bitmap {
		return bitmapTextures.getOrPut(textureId) {
			Log.d(TAG, "--  getResourceBitmap [$tag]: ${resources.getResourceEntryName(textureId)}")
			BitmapTextureData(
				BitmapFactory.decodeResource(resources, textureId, BitmapFactory.Options().apply { inScaled = false }),
				0
			)
		}.apply {
			count++
		}.bitmap
	}

	internal fun getAssetBitmap(tag: String, assetManager: AssetManager, filePath: String): Bitmap {
		return bitmapTextures2.getOrPut(filePath) {
			Log.d(TAG, "--  getAssetBitmap [$tag]: $filePath")
			BitmapTextureData(
				BitmapFactory.decodeStream(assetManager.open(filePath), null, BitmapFactory.Options().apply { inScaled = false })!!,
				0
			)
		}.apply {
			count++
		}.bitmap
	}

	internal fun getResourceBitmapTexture(tag: String, resources: Resources, @IdRes textureId: Int): ResourceBitmapTexture {
		return resourceBitmapTextures.getOrPut(textureId) {
			Log.d(TAG, "--  getResourceBitmapTexture [$tag]: ${resources.getResourceEntryName(textureId)}")
			ResourceBitmapTextureData(ResourceBitmapTexture(textureManager, resources, textureId), 0)
		}.apply {
			count++
		}.resourceBitmapTexture
	}

	internal fun getAssetBitmapTexture(tag: String, assetManager: AssetManager, filePath: String): AssetBitmapTexture {
		return assetBitmapTextures.getOrPut(filePath) {
			Log.d(TAG, "--  getAssetBitmapTexture [$tag]: $filePath")
			AssetBitmapTextureData(AssetBitmapTexture(textureManager, assetManager, filePath), 0)
		}.apply {
			count++
		}.assetBitmapTexture
	}
}