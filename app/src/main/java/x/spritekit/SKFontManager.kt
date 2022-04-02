package x.spritekit

import android.graphics.Typeface
import android.util.Log
import org.andengine.opengl.font.Font
import org.andengine.opengl.font.FontFactory
import org.andengine.opengl.font.FontManager
import org.andengine.opengl.texture.TextureManager

class SKFontManager(private val fontManager: FontManager, val textureManager: TextureManager) {

	data class Key<out K1, out K2, out K3, out K4, out K5, out K6, out K7>
		(
		val key1: K1, val key2: K2, val key3: K3, val key4: K4,
		val key5: K5?, val key6: K6?, val key7: K7?
	) {

		override fun equals(other: Any?): Boolean {
			if (this === other) {
				return true
			}

			if (other == null || javaClass != other.javaClass) {
				return false
			}

			val key = other as Key<*, *, *, *, *, *, *>
			if (if (key1 != null) key1 != key.key1 else key.key1 != null) {
				return false
			}
			if (if (key2 != null) key2 != key.key2 else key.key2 != null) {
				return false
			}
			if (if (key3 != null) key3 != key.key3 else key.key3 != null) {
				return false
			}
			if (if (key4 != null) key4 != key.key4 else key.key4 != null) {
				return false
			}
			if (if (key5 != null) key5 != key.key5 else key.key5 != null) {
				return false
			}
			if (if (key6 != null) key6 != key.key6 else key.key6 != null) {
				return false
			}
			if (if (key7 != null) key7 != key.key7 else key.key7 != null) {
				return false
			}
			return true
		}

		override fun hashCode(): Int {
			var result = key1?.hashCode() ?: 0
			result = 31 * result + (key2?.hashCode() ?: 0)
			result = 31 * result + (key3?.hashCode() ?: 0)
			result = 31 * result + (key4?.hashCode() ?: 0)
			result = 31 * result + (key5?.hashCode() ?: 0)
			result = 31 * result + (key6?.hashCode() ?: 0)
			result = 31 * result + (key7?.hashCode() ?: 0)
			return result
		}

		override fun toString(): String {
			return "($key1, $key2, $key3, $key4, $key5, $key6, $key7)"
		}
	}

	data class FontData(val font: Font, var count: Int)

	companion object {
		private val TAG = SKFontManager::class.simpleName!!
	}

	// major size, minor size,
	private var fonts: HashMap<Key<Typeface, Int, Int, Int, Int, Int, Int>, FontData> =
		hashMapOf()

	fun unCount(tag: String, font: Font) {
		fonts.forEach {
			if (it.value.font == font) it.value.count--
		}
	}

	fun destroy() {
		fonts.clear()
	}

	fun getFont(tag: String, typeface: Typeface, size: Float, color: Int): Font {

		val majorSize: Int = size.toInt()
		val minorSize: Int = ((size - majorSize) * 10).toInt()
		return fonts.getOrPut(
			Key(
				typeface, majorSize, minorSize, color,
				null, null, null
			)
		) {
			Log.d(TAG, "--  getFont [$tag]: $typeface - $size - $color")
			FontData(
				FontFactory.create(
					fontManager, textureManager,
					2000, 2000, typeface, majorSize + minorSize / 10f, color
				).also {
					it.load()
				}, 0
			)
		}.apply {
			count++
		}.font
	}

	fun getFont(
		tag: String, typeface: Typeface, size: Float, color: Int, strokeWidth: Float, strokeColor: Int
	): Font {
		val majorSize: Int = size.toInt()
		val minorSize: Int = ((size - majorSize) * 10).toInt()
		val strokeMajorWidth: Int = strokeWidth.toInt()
		val strokeMinorWidth: Int = ((strokeWidth - strokeMajorWidth) * 10).toInt()
		return fonts.getOrPut(
			Key(
				typeface, majorSize, minorSize, color,
				strokeMajorWidth, strokeMinorWidth, strokeColor
			)
		) {
			FontData(
				FontFactory.createStroke(
					fontManager, textureManager,
					2000, 2000, typeface, majorSize + minorSize / 10f, color,
					(strokeMajorWidth + strokeMinorWidth / 10f), strokeColor, false
				).also {
					it.load()
				}, 0
			)
		}.apply {
			count++
		}.font
	}
}