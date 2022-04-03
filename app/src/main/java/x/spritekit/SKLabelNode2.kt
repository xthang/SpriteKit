package x.spritekit

import android.util.Log
import x.core.NSLineBreakMode
import x.core.graphics.Size

class SKLabelNode2() : SKNode() {

	companion object {
		private val TAG = SKLabelNode2::class.simpleName!!
	}

	var verticalAlignmentMode = SKLabelVerticalAlignmentMode.baseline
	var horizontalAlignmentMode = SKLabelHorizontalAlignmentMode.center

	var numberOfLines: Int = 0

	var lineBreakMode = NSLineBreakMode.byTruncatingTail

	var preferredMaxLayoutWidth = 0f

	lateinit var fontTextureAtlas: SKTextureAtlas
	lateinit var fontMap: ((SKTextureAtlas, Char) -> SKTexture)

	var text: String? = null
		set(value) {
			field = value
			update("text")
		}

	var fontSize = 0f
		set(value) {
			field = value
			update("fontSize")
		}

	var textSpace = 0f
		set(value) {
			field = value
			update("space")
		}

	private var textNodes = SKNode()


	init {
		this.addChild(textNodes)
	}

	constructor(fontTextureAtlas: SKTextureAtlas, fontMap: (SKTextureAtlas, Char) -> SKTexture, text: String? = null) : this() {
		Log.i(TAG, "-------  | $fontMap | $text")

		this.fontTextureAtlas = fontTextureAtlas
		this.fontMap = fontMap
		this.text = text
	}

	override fun clone(): SKLabelNode2 {
		// Log.i(TAG, "--  clone")

		val n = super.clone() as SKLabelNode2
		// n.textNodes = this.textNodes.copy() as! SKNode

		return n
	}

	private fun update(tag: String) {
		// Log.i(TAG, "--  update [$tag]: $text | $fontSize | $textSpace")

		textNodes.removeAllChildren()

		if (fontSize == 0f) {
			return
		}

		var width = 0f
		var isFirst = true
		text?.forEach {
			val texture = fontMap(fontTextureAtlas, it)
			val textNode = SKSpriteNode(texture)
			textNode.size = Size(fontSize * texture.size().width / texture.size().height, fontSize)
			textNode.position.x = width + (if (isFirst) 0f else textSpace) + textNode.size.width / 2
			textNodes.addChild(textNode)

			width += (if (isFirst) 0f else textSpace) + textNode.size.width
			if (isFirst) {
				isFirst = false
			}
		}

		when (horizontalAlignmentMode) {
			SKLabelHorizontalAlignmentMode.center -> textNodes.position.x = -width / 2
			SKLabelHorizontalAlignmentMode.right -> textNodes.position.x = -width
			else -> {}
		}
	}
}