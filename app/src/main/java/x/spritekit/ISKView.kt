package x.spritekit

import x.core.graphics.Size

interface ISKView {

	var scene: SKScene?

	val textureManager: SKTextureManager

	fun onSceneSizeChanged(tag: String, sceneSize: Size, sceneSizeInView: Size) {}

	fun onResume(tag: String)
	fun onPause(tag: String)

	fun notifyPauseEngine(tag: String)
	fun notifyResumeEngine(tag: String)
}