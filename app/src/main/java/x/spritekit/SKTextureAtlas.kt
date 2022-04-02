package x.spritekit

import android.content.Context
import androidx.annotation.DrawableRes
import x.core.graphics.Rect
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class SKTextureAtlas {

	companion object {
		private val TAG = SKTextureAtlas::class.simpleName!!
	}

	private var textures: HashMap<String, SKTexture> = HashMap()
	var textureNames: ArrayList<String> = arrayListOf()


	constructor(context: Context, @DrawableRes name: Int) {
		val resources = context.resources
		val atlas = SKTexture(resources, name)
		val atlasWidth = atlas.size().width
		val atlasHeight = atlas.size().height
		val sheet = resources.getResourceEntryName(name)
		val sheetId = resources.getIdentifier(sheet, "raw", context.packageName)
		val inputStream = resources.openRawResource(sheetId)
		val bufferedReader = BufferedReader(InputStreamReader(inputStream))
		var eachline = bufferedReader.readLine()
		while (eachline != null) {
			val comps = eachline.split(" ".toRegex()).toTypedArray()

			val imgName = comps[0]
			val width = comps[1].toFloat()
			val height = comps[2].toFloat()
			val x = comps[3].toFloat() * atlasWidth
			val y = comps[4].toFloat() * atlasHeight
			val rect = Rect(x + width / 2, y + height / 2, width, height)

			textures[imgName] = SKTexture(rect, atlas).apply { this.name = imgName }
			textureNames.add(imgName)

			eachline = bufferedReader.readLine()
		}
	}

	constructor(context: Context, atlasName: String) {
		val assetManager = context.assets
		val files = assetManager.list(atlasName) ?: throw Error("!-  Atlas not exist")
		files.forEach {
			val name = File(it).nameWithoutExtension
			textures[name] = SKTexture(assetManager, "$atlasName/$it").apply { this.name = name }
			textureNames.add(name)
		}
	}

	fun textureNamed(name: String): SKTexture {
		return textures[name]!!
	}
}