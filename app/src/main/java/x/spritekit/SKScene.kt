package x.spritekit

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.net.Uri
import android.opengl.GLES20
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import androidx.annotation.CallSuper
import androidx.annotation.ColorInt
import androidx.core.content.FileProvider
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import org.andengine.engine.camera.Camera
import org.andengine.opengl.util.GLState
import org.andengine.opengl.vbo.VertexBufferObjectManager
import x.core.TimeInterval
import x.core.graphics.SKSceneScaleMode
import x.core.graphics.Size
import x.core.ui.UITouch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import java.util.concurrent.ConcurrentHashMap


open class SKScene : SKNode(), IDrawable, View.OnTouchListener, View.OnKeyListener {

	private val TAG: String get() = "${SKScene::class.simpleName}|${this::class.simpleName}"

	private var didMoveToView = false
	private var frameCount = 0

	val view: SKView? get() = (surfaceView as? SurfaceView)?.parent as? SKView
	internal var surfaceView: ISKView? = null

	public override var size: Size
		get() = super.size
		set(value) {
			val oldSize = super.size
			super.size = value
			if (oldSize != super.size) didChangeSize("setSize", oldSize)
		}

	var scaleMode = SKSceneScaleMode.fill
		set(value) {
			field = value
			configChange("scaleMode", surfaceView)
		}
	private var xScaleInView = 1f
	private var yScaleInView = 1f
	private var scaleHasChanged = false
	internal var sizeInView: Size = Size.zero

	@ColorInt
	protected var backgroundColor: Int? = null

	val physicsWorld: SKPhysicsWorld = SKPhysicsWorld()

	private var sp: SoundPool? = null

	private val runnables: ArrayList<(() -> Unit)> = arrayListOf()

	internal val touchListeners: MutableSet<SKNode> = Collections.newSetFromMap(ConcurrentHashMap())
	internal val keyListeners: MutableSet<SKNode> = Collections.newSetFromMap(ConcurrentHashMap())

	private val mainLooperHandler = Handler(Looper.getMainLooper())

	init {
		Log.i(TAG, "-------")

		initSFX("init")

		drawLayers = TreeMap()
	}

	/** This is called once after the scene has been initialized or decoded,
	this is the recommended place to perform one-time setup */
	// todo: subclasses need to call this themselves ...
	protected open fun sceneDidLoad(tag: String) {}

	@CallSuper
	internal fun willMoveTo(tag: String, view: SKView, surfaceView: ISKView) {
		initSFX("willMoveTo|$tag")
		willMoveTo("willMoveTo|$tag", view)
		// call willMoveTo before configChange so scaleMode that set in willMoveTo can be applied
		configChange("willMoveTo|$tag", surfaceView)
	}

	protected open fun willMoveTo(tag: String, view: SKView) {}

	internal fun didMoveTo(tag: String, view: SKView, surfaceView: ISKView) {
		didMoveTo(tag, view)
		didMoveToView = true
	}

	protected open fun didMoveTo(tag: String, view: SKView) {}

	internal fun viewSizeChanged(tag: String, oldViewSize: Size) {
		configChange("viewSizeChanged|$tag", surfaceView)
	}

	private fun configChange(tag: String, surfaceView: ISKView?) {
		if (surfaceView == null) return

		val view = surfaceView as SurfaceView

		// val oldSize = size
		val oldSizeInView = sizeInView

		when (scaleMode) {
			SKSceneScaleMode.aspectFit -> {
				val fitX = size.width / size.height > view.width.toFloat() / view.height.toFloat()
				xScaleInView =
					if (fitX) view.width.toFloat() / size.width else view.height.toFloat() / size.height
				yScaleInView = xScaleInView
				sizeInView =
					if (fitX) Size(view.width.toFloat(), view.width * size.height / size.width)
					else Size(view.height * size.width / size.height, view.height.toFloat())
			}
			SKSceneScaleMode.aspectFill -> {
				val fitX = size.width / size.height < view.width.toFloat() / view.height.toFloat()
				xScaleInView =
					if (fitX) view.width.toFloat() / size.width else view.height.toFloat() / size.height
				yScaleInView = xScaleInView
				sizeInView =
					if (fitX) Size(view.width.toFloat(), view.width * size.height / size.width)
					else Size(view.height * size.width / size.height, view.height.toFloat())
			}
			SKSceneScaleMode.resizeFill -> {
				xScaleInView = 1f
				yScaleInView = 1f
				size = Size(view.width, view.height) // Todo be careful here -> unexpected behavior
				sizeInView = Size(view.width, view.height)
			}
			SKSceneScaleMode.fill -> {
				xScaleInView = view.width.toFloat() / size.width
				yScaleInView = view.height.toFloat() / size.height
				sizeInView = Size(view.width, view.height)
			}
		}

		Log.i(
			TAG,
			"--  config changed [$tag]: $scaleMode | view(${view.width} x ${view.height}) | $size | ($xScale:$yScale) | $oldSizeInView -> $sizeInView"
		)

		scaleHasChanged = true

		if (size.changed || sizeInView != oldSizeInView) {
			size.changed = false
			surfaceView.onSceneSizeChanged("$TAG|configChange|$tag", size, sizeInView)
		}

		// if (!oldSize.equals(size)) didChangeSize(tag, oldSize)
	}

	protected open fun didChangeSize(tag: String, oldSize: Size) {}

	open fun willMoveFrom(tag: String, view: SKView) {}

	internal fun didMoveFrom(tag: String, view: SKView) {
		didMoveToView = false
		frameCount = 0
		renderDestroyed("didMoveFrom|$tag")
		releaseSFX("didMoveFrom|$tag")
	}

	@CallSuper
	private fun initSFX(tag: String) {
		if (sp == null) {
			sp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				val attrs = AudioAttributes.Builder()
					.setUsage(AudioAttributes.USAGE_GAME)
					.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
					.build()
				SoundPool.Builder()
					.setMaxStreams(10)
					.setAudioAttributes(attrs)
					.build()
			} else {
				SoundPool(10, AudioManager.STREAM_MUSIC, 0)
			}

//			sp!!.setOnLoadCompleteListener { soundPool, sampleId, status ->
//				Log.d(TAG, "--  sp.onLoadComplete: $soundPool | $sampleId | $status")
//			}
		}
	}

	@CallSuper
	private fun releaseSFX(tag: String) {
		sp!!.release()
		sp = null
	}

	// RENDERING
	open fun renderPaused(tag: String) {
		Log.i(TAG, "--  renderPaused [$tag]")
	}

	internal fun renderResumed(tag: String) {
		Log.i(TAG, "--  viewResumed [$tag]")
	}

	internal fun renderDestroyed(tag: String) {
		Log.i(TAG, "--  renderDestroyed [$tag]")
//		removeFromParent("$tag|destroyScene")
//		removeAllChildren("$tag|destroyScene")
	}

	internal fun update(secondsPassed: TimeInterval, timeToRun: Long) {
		// Log.d(TAG, "--  update: $this | $didMoveToView | $frameCount | $timeToRun")

		if (!didMoveToView) return
		// ignore the first frame because maybe timeToRun is accumulated from before this is set to view
		if (frameCount == 0) {
			frameCount = 1
			// to first loadToManagers (time costing) right after this, test: Numbers - first GameScene
			// note: this make loadToManagers draw some weird images in GL (and 'draw' does not run yet to overwrite this)
			drawLayers!!.clear()
			update(drawLayers!!)
			return
		} else if (frameCount == 1) frameCount = 2

		update(secondsPassed)

		physicsWorld.bodies2.clear()
		drawLayers!!.clear()
		runnables.clear()

		drawLayers!!.getOrPut(zPosition) { arrayListOf() }.add(this)
		update(timeToRun, drawLayers!!, sp!!, physicsWorld.bodies2, runnables)

		runnables.forEach {
			mainLooperHandler.post { // post inside forEach to prevent ConcurrentModificationException
				it.invoke()
			}
		}

		didEvaluateActions()

		physicsWorld.simulatePhysics("$TAG|update", timeToRun * 1e-3)

		didSimulatePhysics()

		//...
		didApplyConstraints()

		//...
		didFinishUpdate()
	}

	protected open fun update(secondsPassed: TimeInterval) {}

	/**
	Override this to perform game logic. Called exactly once per frame after any actions have been evaluated but before any physics are simulated. Any additional actions applied is not evaluated until the next update.
	 */
	protected open fun didEvaluateActions() {}

	/**
	Override this to perform game logic. Called exactly once per frame after any actions have been evaluated and any physics have been simulated. Any additional actions applied is not evaluated until the next update. Any changes to physics bodies is not simulated until the next update.
	 */
	protected open fun didSimulatePhysics() {}

	/**
	Override this to perform game logic. Called exactly once per frame after any enabled constraints have been applied. Any additional actions applied is not evaluated until the next update. Any changes to physics bodies is not simulated until the next update. Any changes to constraints will not be applied until the next update.
	 */
	protected open fun didApplyConstraints() {}

	/**
	Override this to perform game logic. Called after all update logic has been completed. Any additional actions applied are not evaluated until the next update. Any changes to physics bodies are not simulated until the next update. Any changes to constraints will not be applied until the next update.

	No futher update logic will be applied to the scene after this call. Any values set on nodes here will be used when the scene is rendered for the current frame.
	 */
	protected open fun didFinishUpdate() {}

	// Draw to Canvas (SurfaceView)
	override fun drawSelf(canvas: Canvas, sceneXScale: Float, sceneYScale: Float, sceneScaleHasChanged: Boolean) {
		backgroundColor?.let { canvas.drawColor(it) }
	}

	override fun draw(canvas: Canvas) {
		if (!didMoveToView) return

		val rect = RectF(
			(canvas.width - sizeInView.width) / 2,
			(canvas.height - sizeInView.height) / 2,
			(canvas.width + sizeInView.width) / 2 - 1,
			(canvas.height + sizeInView.height) / 2 - 1
		)

		canvas.clipRect(rect)

		synchronized(drawLayers!!) {
			for (layer in drawLayers!!) {
				for (n in layer.value) {
					n.drawSelf(canvas, xScaleInView, yScaleInView, scaleHasChanged)
				}
			}
		}

		if (view?.showsPhysics == true) {
			physicsWorld.drawSelf(canvas, xScaleInView, yScaleInView, scaleHasChanged)
		}

		scaleHasChanged = false
	}

	override fun loadToManagers(
		vertexBufferObjectManager: VertexBufferObjectManager,
		textureManager: SKTextureManager, fontManager: SKFontManager
	) {
		super.loadToManagers(vertexBufferObjectManager, textureManager, fontManager)
		// this draws some weird images in GL without 'draw' running

		if (view?.showsPhysics == true)
			synchronized(physicsWorld.bodies2) {
				for (x in physicsWorld.bodies2)
					x.attachVertexBufferObject(vertexBufferObjectManager)
			}
	}

	override fun drawSelf(glState: GLState, camera: Camera) {
		backgroundColor?.let {
			glState.pushModelViewGLMatrix()
			camera.onApplySceneBackgroundMatrix(glState)
			glState.loadModelViewGLMatrixIdentity()

			val view = this.surfaceView as SurfaceView
			// paint color inside the specified size only, not the whole window
			GLES20.glScissor(
				((view.width - sizeInView.width) / 2).toInt(),
				((view.height - sizeInView.height) / 2).toInt(),
				sizeInView.width.toInt(),
				sizeInView.height.toInt()
			)
			GLES20.glEnable(GLES20.GL_SCISSOR_TEST)

			GLES20.glClearColor(
				it.red / 255f,
				it.green / 255f,
				it.blue / 255f,
				it.alpha / 255f * alpha
			)
			GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

			GLES20.glDisable(GLES20.GL_SCISSOR_TEST)

			// Set the background size color
			GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

			glState.popModelViewGLMatrix()
		}
	}

	fun draw(glState: GLState, camera: Camera) {
		if (!didMoveToView) return

		glState.pushProjectionGLMatrix()
		camera.onApplySceneMatrix(glState)
		glState.loadModelViewGLMatrixIdentity()

		synchronized(drawLayers!!) {
			for (layer in drawLayers!!) {
				for (n in layer.value) {
					n.drawSelf(glState, camera)
				}
			}
		}

		if (view?.showsPhysics == true) {
			synchronized(physicsWorld) {
				physicsWorld.drawSelf(glState, camera)
			}
		}

		glState.popProjectionGLMatrix()
	}

	@SuppressLint("MissingSuperCall")
	@CallSuper
	override fun onTouchEvent(touches: MutableSet<UITouch>, event: MotionEvent): Boolean {
		if (!isUserInteractionEnabled || isHidden) return false

		if (!isTouched && arrayOf(
				MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP,
				MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_OUTSIDE
			).contains(event.action)
		)
			return false

		return superOnTouchEvent(touches, event)
	}

	@CallSuper
	final override fun onTouch(v: View?, event: MotionEvent?): Boolean {
		// Log.d(TAG, "--  onTouch: $v | $event | $isUserInteractionEnabled")
		v?.performClick()
		if (v == null || event == null) return false

		val touches: MutableSet<UITouch> = mutableSetOf()

		for (pointerIndex in event.pointerCount - 1 downTo 0) {
			val pointerID: Int = event.getPointerId(pointerIndex)
			// calculate x, y in scene's coordination
			val x = event.getX(pointerIndex)
			val y = event.getY(pointerIndex)
			touches.add(
				UITouch(
					x, y,
					(x - v.width / 2) / sizeInView.width * size.width,
					(v.height / 2 - y) / sizeInView.height * size.height,
					pointerID
				)
			)
		}

		touchListeners.toSortedSet { o1, o2 ->
			return@toSortedSet if (o1.zPositionAbs - o2.zPositionAbs >= 0) -1 else 1
		}.forEach {
			if (it.onTouchEvent(touches, event))
				return true
		}

		return onTouchEvent(touches, event)
	}

	@CallSuper
	final override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
		Log.d(TAG, "--  onKey: $v | $keyCode | $event | $isUserInteractionEnabled")
		if (v == null || event == null) return false

		keyListeners.toSortedSet { o1, o2 ->
			return@toSortedSet if (o1.zPositionAbs - o2.zPositionAbs >= 0) -1 else 1
		}.forEach {
			if (it.onKeyEvent(keyCode, event))
				return true
		}

		return onKeyEvent(keyCode, event)
	}

	open fun takeScreenShot(context: Context, onComplete: (Uri) -> Unit) {
		val view = this.surfaceView as SurfaceView
		val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
		val file = File(context.cacheDir, "game-screenshot.png")
		Log.i(TAG, "--  takeScreenShot to file: ${file.absolutePath}")

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			Thread.sleep(100)
			PixelCopy.request(view, bitmap, { it_ ->
				Log.i(TAG, "--  takeScreenShot: PixelCopy completed: $it_")

				initUri(context, bitmap, file)?.let { onComplete(it) }
			}, mainLooperHandler)
		} else {
			Thread.sleep(70) // for engine to update the latest before pausing
			surfaceView?.notifyPauseEngine("takeScreenShot")
			Thread.sleep(100) // for engine to pause
			loadToManagers(surfaceView!!.textureManager, surfaceView !is SKCanvasView)
			draw(Canvas(bitmap))
			surfaceView?.notifyResumeEngine("takeScreenShot")

			initUri(context, bitmap, file)?.let { onComplete(it) }
		}
	}

	open fun imageFromNode(node: SKNode): Bitmap {
		Thread.sleep(70) // for engine to update the latest before pausing
		surfaceView?.notifyPauseEngine("takeScreenShot")
		Thread.sleep(100) // for engine to pause
		val bitmap = node.texture(surfaceView!!.textureManager, surfaceView !is SKCanvasView)
		surfaceView?.notifyResumeEngine("takeScreenShot")

		return bitmap
	}

	private fun initUri(context: Context, bitmap: Bitmap, file: File): Uri? {
		return try {
			val outputStream = FileOutputStream(file)
			bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
			outputStream.close()

			FileProvider.getUriForFile(
				context, AppConfig.fileProviderAuthority, file
			)
		} catch (e: IOException) {
			Log.d(TAG, "!-  takeScreenShot: IOException while trying to write file for sharing: $e")
			null
		}
	}
}
