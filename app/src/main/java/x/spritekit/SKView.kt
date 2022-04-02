package x.spritekit

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceView
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import androidx.annotation.CallSuper
import androidx.core.view.children
import x.spritekit.Helper


class SKView : FrameLayout {

	companion object {
		private val TAG = SKView::class.simpleName
	}

	private val surfaceView: ISKView
	var scene: SKScene?
		get() = surfaceView.scene
		private set(value) {
			surfaceView.scene = value
		}

	private var viewSizeIsDefined = false

	var showsFPS: Boolean = false
	var showsDrawCount: Boolean = false
	var showsNodeCount: Boolean = false
	var showsQuadCount: Boolean = false
	var showsPhysics: Boolean = false
	var showsFields: Boolean = false
	var showsLargeContentViewer: Boolean = false


	init {
		Log.i(TAG, "-------")

		surfaceView = if (Helper.GL.getVersionFromActivityManager(context) >= 0x20000) {
			SKGLView(context)
		} else {
			SKCanvasView(context)
		}

		addView(surfaceView as SurfaceView)

		viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
			override fun onGlobalLayout() {
				// IMPORTANT (by XT): remember to remove this Listener (for example: on viewDestroyed), or this view will not be garbage-cleaned
				viewTreeObserver.removeOnGlobalLayoutListener(this)
				if (!viewSizeIsDefined) {
					Log.d(TAG, "--  viewTreeObserver.OnGlobalLayout: $width x $height")
					viewSizeIsDefined = true
					scene?.let {
						it.willMoveTo("$TAG|onGlobalLayout", this@SKView, this@SKView.surfaceView)
						it.didMoveTo("$TAG|onGlobalLayout", this@SKView, this@SKView.surfaceView)
					}
				}
			}
		})
	}

	constructor(ctx: Context) : super(ctx)
	constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

	@CallSuper
	fun finalize() {
		Log.d(TAG, "~~~~~~~ ${hashCode()}")

		scene = null
	}

	override fun removeAllViews() {
		for (child in children) {
			if (child != surfaceView) removeView(child)
		}
	}

	override fun onAttachedToWindow() {
		Log.i(TAG, "--  onAttachedToWindow: $width x $height")
		super.onAttachedToWindow()

		surfaceView.onResume("$TAG|onAttachedToWindow|$tag")
	}

	override fun onDetachedFromWindow() {
		Log.i(TAG, "--  onDetachedFromWindow")
		super.onDetachedFromWindow()

		surfaceView.onPause("$TAG|onDetachedFromWindow|$tag")

		scene = null
	}

	override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
		Log.i(TAG, "--  onWindowFocusChanged: $hasWindowFocus")
		super.onWindowFocusChanged(hasWindowFocus)

		if (hasWindowFocus) surfaceView.onResume("$TAG|onWindowFocusChanged|$tag")
		else surfaceView.onPause("$TAG|onWindowFocusChanged|$tag")
	}

	fun presentScene(tag: String, scene: SKScene?, transition: SKTransition? = null) {
		val oldScene = this.scene
		oldScene?.let {
			it.willMoveFrom("$TAG|unsetScene|$tag", this)
			it.removeFromParent()
		}
		if (viewSizeIsDefined) scene?.willMoveTo("$TAG|presentScene|$tag", this, surfaceView)
		this.scene = scene
		if (viewSizeIsDefined) this.scene?.didMoveTo("$TAG|presentScene|$tag", this, surfaceView)
		oldScene?.didMoveFrom("$TAG|presentScene|$tag", this)
	}
}
