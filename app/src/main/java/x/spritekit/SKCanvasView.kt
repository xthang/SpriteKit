package x.spritekit

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import x.spritekit.TimeInterval
import x.core.graphics.Size


internal class SKCanvasView : SurfaceView, SurfaceHolder.Callback, ISKView {

	companion object {
		private val TAG = SKCanvasView::class.simpleName!!
	}

	@Volatile
	override var scene: SKScene? = null
		set(value) {
			field = value
			field?.surfaceView = this
			setOnTouchListener(field)
			setOnKeyListener(field)
		}
	var engine = XEngine { secondsPassed, deltaTime ->
		update(secondsPassed, deltaTime)
		render()
	}

	var created = false
	var paused = false
	var destroyed = false

	override val textureManager = SKTextureManager()


	init {
		Log.i(TAG, "-------")

		isFocusable = true
		isFocusableInTouchMode = true

		holder.addCallback(this)
	}

	constructor(context: Context?) : super(context)
	constructor(context: Context?, attributes: AttributeSet?) : super(context, attributes)

	override fun surfaceCreated(holder: SurfaceHolder) {
		Log.i(
			TAG,
			"--  surfaceCreated: $created - $paused - $destroyed | ($width x $height) | ${scene!!.frame}"
		)

		if (!created) {
			created = true
			engine.valid = true
			// scene!!.createScene()
		} else {
			scene?.renderResumed("$TAG|surfaceCreated")
		}

		if (destroyed) {
			destroyed = false
			// createResources()
		}

		engine.startThread("$TAG|surfaceCreated")
	}

	override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
		Log.i(
			TAG, "--  surfaceChanged: ($width x $height) | (${this.width} x ${this.height})"
		)

		if (engine.updateThread?.isAlive != true) {
			Log.i(TAG, "--  drawThread isNotAlive")
			val canvas = holder.lockCanvas()
			scene?.loadToManagers(textureManager)
			scene?.draw(canvas)
			holder.unlockCanvasAndPost(canvas)
		}
		scene?.viewSizeChanged("$TAG|surfaceChanged", Size(0, 0))
	}

	override fun surfaceDestroyed(holder: SurfaceHolder) {
		Log.i(TAG, "--  surfaceDestroyed: $created - $paused - $destroyed")

		destroyed = true
		holder.surface.release()
	}

	override fun onResume(tag: String) {
		Log.i(TAG, "--  onResume [$tag]: $created - $paused - $destroyed")

		if (created && paused && !destroyed) {
			scene?.renderResumed("$TAG|resume|$tag")
			engine.startThread("$TAG|onResume|$tag")
		}
		paused = false
	}

	override fun onPause(tag: String) {
		Log.i(TAG, "--  onPause [$tag]: $created - $paused - $destroyed")

		paused = true
		// NOTE: do not call surface.release() --> the surface will not be resumed after app resume
		scene?.renderPaused("$TAG|onPause|$tag")
		stop("onPause|$tag")
	}

	private fun stop(tag: String) {
		scene?.renderDestroyed("$TAG|stop|$tag")
		engine.stop("$TAG|stop|$tag")
	}

	fun update(secondsPassed: TimeInterval, deltaTime: Long) {
		scene?.update(secondsPassed, deltaTime)
	}

	private var sceneSizeInView = Size.zero
	private var canvasCleared = 3

	private fun render() {
		val canvas = holder.lockCanvas()
		canvas?.let {
			if (canvasCleared < 3) {
				Log.d(TAG, "--  render: sceneSizeHasChanged | $canvasCleared")
				canvasCleared++

				it.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
			}

			scene?.loadToManagers(textureManager)
			scene?.draw(it)

			holder.unlockCanvasAndPost(it)
		}
	}

	override fun notifyPauseEngine(tag: String) {
		engine.pause(tag)
	}

	override fun notifyResumeEngine(tag: String) {
		engine.resume(tag)
	}

	override fun onSceneSizeChanged(tag: String, sceneSize: Size, sceneSizeInView: Size) {
		if (this.sceneSizeInView != sceneSizeInView) {
			this.sceneSizeInView = sceneSizeInView
			canvasCleared = 0
		}
	}
}
