package x.spritekit

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceHolder
import org.andengine.opengl.view.ConfigChooser
import x.core.graphics.Size


internal class SKGLView : GLSurfaceView, ISKView {

	companion object {
		private val TAG = SKGLView::class.simpleName
	}

	private val renderer: SKGLRenderer

	override var scene: SKScene?
		get() = renderer.scene
		set(value) {
			value?.surfaceView = this
			renderer.scene = value

			setOnTouchListener(value)
			setOnKeyListener(value)
		}

	override val textureManager: SKTextureManager get() = renderer.skTextureManager


	init {
		Log.i(TAG, "-------")

		// Create an OpenGL ES 2.0 context
		setEGLContextClientVersion(2)
//        setZOrderOnTop(false)
//        setZOrderMediaOverlay(true)

		val configChooser = ConfigChooser(false)
		setEGLConfigChooser(configChooser)

		renderer = SKGLRenderer()
		renderer.configChooser = configChooser

		// Set the Renderer for drawing on the GLSurfaceView
		setRenderer(renderer)

		// Render the view only when there is a change in the drawing data
		renderMode = RENDERMODE_WHEN_DIRTY
	}

	constructor(context: Context?) : super(context)
	constructor(context: Context?, attributes: AttributeSet?) : super(context, attributes)

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		val measuredWidth = MeasureSpec.getSize(widthMeasureSpec)
		val measuredHeight = MeasureSpec.getSize(heightMeasureSpec)
		val widthMode = MeasureSpec.getMode(widthMeasureSpec)
		val heightMode = MeasureSpec.getMode(heightMeasureSpec)
		Log.i(
			TAG,
			"--  onMeasure: ($widthMeasureSpec x $heightMeasureSpec) | ($measuredWidth x $measuredHeight) | ($widthMode x $heightMode) | ${scene?.frame}"
		)

		super.onMeasure(widthMeasureSpec, heightMeasureSpec)
	}

	/** keep this, be called when screen is turned on */
	// need to be call by developer
	override fun onResume(tag: String) {
		super.onResume()

		renderer.resume("$TAG|onResume|$tag")
	}

	/** Need to be called when screen turned-off */
	// need to be call by developer
	override fun onPause(tag: String) {
		// todo: call super after renderer.pause
		super.onPause()

		renderer.pause("$TAG|onPause|$tag")
	}

	override fun surfaceDestroyed(holder: SurfaceHolder) {
		// call super first so app is not so lag
		super.surfaceDestroyed(holder)

		renderer.destroy("$TAG|surfaceDestroyed")
	}

	override fun notifyPauseEngine(tag: String) {
		renderer.pauseEngine("$TAG|notifyPauseEngine|$tag")
	}

	override fun notifyResumeEngine(tag: String) {
		renderer.resumeEngine("$TAG|notifyResumeEngine|$tag")
	}

	override fun onSceneSizeChanged(tag: String, sceneSize: Size, sceneSizeInView: Size) {
		renderer.updateViewport("$TAG|onSceneSizeInViewChanged|$tag", width, height, sceneSize, sceneSizeInView)
	}
}
