package x.spritekit

import android.graphics.Canvas
import org.andengine.engine.camera.Camera
import org.andengine.opengl.util.GLState

interface IDrawable : IPresentable {
	// Canvas
	fun drawSelf(canvas: Canvas, sceneXScale: Float, sceneYScale: Float, sceneScaleHasChanged: Boolean)

	// GL
	fun drawSelf(glState: GLState, camera: Camera)
}