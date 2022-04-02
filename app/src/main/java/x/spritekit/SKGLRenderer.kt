package x.spritekit

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import org.andengine.engine.camera.Camera
import org.andengine.engine.options.RenderOptions
import org.andengine.opengl.font.FontManager
import org.andengine.opengl.shader.ShaderProgramManager
import org.andengine.opengl.texture.TextureManager
import org.andengine.opengl.util.GLState
import org.andengine.opengl.vbo.VertexBufferObjectManager
import org.andengine.opengl.view.ConfigChooser
import x.core.graphics.Size
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


class SKGLRenderer : GLSurfaceView.Renderer {

	companion object {
		private val TAG = SKGLRenderer::class.simpleName!!
	}

	private val glState = GLState()
	private lateinit var camera: Camera

	private val shaderProgramManager = ShaderProgramManager()
	private val vertexBufferObjectManager = VertexBufferObjectManager()
	private val textureManager = TextureManager()
	internal val skTextureManager = SKTextureManager(textureManager)
	private val fontManager = FontManager()
	private val skFontManager = SKFontManager(fontManager, textureManager)

	internal lateinit var configChooser: ConfigChooser

	private var created = false
	private var paused = false
	private var destroyed = false

	@Volatile
	internal var scene: SKScene? = null
	private var engine = XEngine { secondsPassed, deltaTime -> update(secondsPassed, deltaTime) }
	private val engineLock: EngineLock = EngineLock(false)

	private var sceneSizeInView = Size.zero
	private var sceneSizeHasChanged = false

	init {
		Log.i(TAG, "-------")

		createResources("init")
	}

	override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
		Log.i(TAG, "--  onSurfaceCreated: $created - $paused - $destroyed | frame:${scene?.frame}")

		// Set the background frame color
		GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

		val renderOptions = RenderOptions()
		glState.reset(renderOptions, configChooser, config)

		glState.disableDepthTest()
		glState.enableBlend()
		glState.isDitherEnabled = renderOptions.isDithering

		if (!created) {
			created = true
			engine.valid = true
			// scene?.createScene()
		} else {
			scene?.renderResumed("$TAG|onSurfaceCreated")
		}

		if (destroyed) {
			destroyed = false
			createResources("onSurfaceCreated")
		}
		reloadResources("onSurfaceCreated")

		engine.startThread("$TAG|onSurfaceCreated")
	}

	override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
		Log.i(TAG, "--  onSurfaceChanged: ($width x $height) | ${scene?.size}")

		sceneSizeHasChanged = true

		scene?.let {
			it.viewSizeChanged("$TAG|onSurfaceChanged", Size(0, 0))

			// updateViewport("onSurfaceChanged", width, height, it.sizeInView)
		} ?: run {
			camera = Camera(0f, 0f, width.toFloat(), height.toFloat())
			camera.setSurfaceSize(0, 0, width, height)

			GLES20.glViewport(0, 0, width, height)
		}
	}

	override fun onDrawFrame(unused: GL10) {
		if (destroyed) {  // fix bug: view is destroyed and renderer still renders. WTH Android?
			Log.wtf(TAG, "!-  onDrawFrame: this is already destroyed ?!")
			return
		}

		// workaround: in case scene size changes itself, not by calling from `onSurfaceChanged`
		if (sceneSizeHasChanged) scene?.let {
			Log.d(TAG, "--  onDrawFrame: sceneSizeHasChanged")
			sceneSizeHasChanged = false

			GLES20.glViewport(
				((it.view!!.width - it.sizeInView.width) / 2).toInt(),
				((it.view!!.height - it.sizeInView.height) / 2).toInt(),
				it.sizeInView.width.toInt(),
				it.sizeInView.height.toInt()
			)

			glState.loadProjectionGLMatrixIdentity()
		}

		// Redraw background color
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT) // todo: understand error when setting 'and GLES20.GL_DEPTH_BUFFER_BIT'

		vertexBufferObjectManager.updateVertexBufferObjects(glState)
		textureManager.updateTextures(glState)
		fontManager.updateFonts(glState)

		render()
	}

	fun updateViewport(tag: String, width: Int, height: Int, sceneSize: Size, sceneSizeInView: Size) {
		Log.d(
			TAG,
			"--  updateViewport [$tag]: ($width x $height) | $sceneSize | ${this.sceneSizeInView} -> $sceneSizeInView| !-${GLES20.glGetError()}"
		)

		camera = Camera(0f, 0f, sceneSize.width, sceneSize.height)
		camera.setSurfaceSize(0, 0, sceneSize.width.toInt(), sceneSize.height.toInt()) // this does nothing !??

		// this only works when calling from `onSurfaceChanged`, not when calling when scene changes size itself
		// still dont know why, maybe because of thread (?)
		// so I found a workaround by calling inside `onDrawFrame` once after
		// 2022-03-02: NOTE 2: do not call this here, when a new scene is set to skView, it affects the current scene being drawn
		//GLES20.glViewport(
		//	((width - sceneSizeInView.width) / 2).toInt(),
		//	((height - sceneSizeInView.height) / 2).toInt(),
		//	sceneSizeInView.width.toInt(),
		//	sceneSizeInView.height.toInt()
		//)
		//
		//glState.loadProjectionGLMatrixIdentity()

		if (this.sceneSizeInView != sceneSizeInView) {
			this.sceneSizeInView = sceneSizeInView
			sceneSizeHasChanged = true
		}
	}

	// --
	private fun createResources(tag: String) {
		Log.d(TAG, "--  createResources [$tag]")

		shaderProgramManager.onCreate()
		vertexBufferObjectManager.onCreate()
		textureManager.onCreate()
		fontManager.onCreate()
	}

	private fun reloadResources(tag: String) {
		Log.d(TAG, "--  reloadResources [$tag]")

		shaderProgramManager.onReload()
		vertexBufferObjectManager.onReload()
		textureManager.onReload()
		fontManager.onReload()
	}

	// --
	private fun update(secondsPassed: Double, forTime: Long) {
		engineLock.lock()
		try {
			scene?.let {
				it.update(secondsPassed, forTime)
				it.loadToManagers(vertexBufferObjectManager, skTextureManager, skFontManager)
				(it.surfaceView as? GLSurfaceView)?.requestRender()
			}

			engineLock.notifyCanDraw()
			engineLock.waitUntilCanUpdate()
		} finally {
			engineLock.unlock()
		}
	}

	private fun render() {
		engineLock.lock()
		try {
			engineLock.waitUntilCanDraw()

			scene?.draw(glState, camera)

			engineLock.notifyCanUpdate()
		} finally {
			engineLock.unlock()
		}
	}

	// --
	internal fun resume(tag: String) {
		Log.i(TAG, "--  onResume [$tag]: $created - $paused - $destroyed")

		if (created && paused && !destroyed) {
			scene?.renderResumed("$TAG|resume|$tag")
			engine.startThread("$TAG|resume|$tag")
		}
		paused = false
	}

	internal fun pause(tag: String) {
		Log.i(TAG, "--  onPause [$tag]: $created - $paused - $destroyed")

		paused = true

		scene?.renderPaused("$TAG|pause|$tag")
		scene?.renderDestroyed("$TAG|pause|$tag")
		engine.notifyStop("$TAG|pause|$tag") // in case thread still runs after engineLock.notifyCanUpdate

		// must release update() so engine's updateThread can be released
		engineLock.lock()
		try {
			engineLock.notifyCanUpdate()
		} finally {
			engineLock.unlock()
		}

		engine.stop("$TAG|pause|$tag")
	}

	internal fun destroy(tag: String) {
		Log.i(TAG, "--  destroy [$tag]: $created - $paused - $destroyed")

		destroyed = true

		textureManager.onDestroy()
		vertexBufferObjectManager.onDestroy()
		shaderProgramManager.onDestroy()
		fontManager.onDestroy()
		skTextureManager.destroy(TAG)
		skFontManager.destroy()
	}

	internal fun pauseEngine(tag: String) {
		engine.pause(tag)
	}

	internal fun resumeEngine(tag: String) {
		engine.resume(tag)
	}
}

internal class EngineLock(pFair: Boolean) : ReentrantLock(pFair) {
	private val mDrawingCondition = newCondition()
	private val mDrawing = AtomicBoolean(false)

	internal fun notifyCanDraw() {
		mDrawing.set(true)
		mDrawingCondition.signalAll()
	}

	internal fun notifyCanUpdate() {
		mDrawing.set(false)
		mDrawingCondition.signalAll()
	}

	@Throws(InterruptedException::class)
	internal fun waitUntilCanDraw() {
		while (!mDrawing.get()) {
			mDrawingCondition.await()
		}
	}

	@Throws(InterruptedException::class)
	internal fun waitUntilCanUpdate() {
		while (mDrawing.get()) {
			mDrawingCondition.await()
		}
	}
}