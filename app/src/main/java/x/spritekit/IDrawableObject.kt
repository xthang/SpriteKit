package x.spritekit

import android.graphics.Canvas
import androidx.annotation.CallSuper
import org.andengine.engine.camera.Camera
import org.andengine.entity.shape.IShape
import org.andengine.opengl.shader.ShaderProgram
import org.andengine.opengl.util.GLState
import org.andengine.opengl.vbo.HighPerformanceVertexBufferObject
import org.andengine.opengl.vbo.VertexBufferObjectManager
import org.andengine.util.IDisposable
import x.core.graphics.Size

interface IDrawableObject : IDrawable {

	companion object {
		private val TAG = IDrawableObject::class.simpleName!!
	}

	var name: String?
	// var isHidden: Boolean

	var size: Size

	// var glVerticesIsValid: Boolean
	var glColorIsValid: Boolean

	var shaderProgram: ShaderProgram
	var vertexBufferObject: HighPerformanceVertexBufferObject
	var disposed: Boolean
	var disposeInfo: String?

	fun clone(): IDrawableObject

	@CallSuper
	fun finalize() {
		if (!disposed) {
			dispose("finalize")
		}
	}

	@CallSuper
	fun dispose(tag: String) {
		// Log.i(TAG, "--  dispose: [$tag] $name @${hashCode()}")
		if (disposed) throw IDisposable.AlreadyDisposedException("-- $tag | ${this::class.simpleName}@${hashCode()} | $name | $disposeInfo")

		disposed = true
		disposeInfo = tag

		if (vertexBufferObject.isAutoDispose && !vertexBufferObject.isDisposed) {
			vertexBufferObject.dispose()
		}
	}

	@CallSuper
	override fun drawSelf(canvas: Canvas, sceneXScale: Float, sceneYScale: Float, sceneScaleHasChanged: Boolean) {
		doDraw(canvas, sceneXScale, sceneYScale, sceneScaleHasChanged)
	}

	fun doDraw(canvas: Canvas, sceneXScale: Float, sceneYScale: Float, sceneScaleHasChanged: Boolean)

	// GL
	@CallSuper
	fun attachVertexBufferObject(vertexBufferObjectManager: VertexBufferObjectManager) {
		if (vertexBufferObject.vertexBufferObjectManager == null)
			vertexBufferObject.vertexBufferObjectManager = vertexBufferObjectManager
	}

	// ----------------------
	fun updateVertices()
	fun updateColor(tag: String)

	// ----------------------
	@CallSuper
	fun applyTransformations(pGLState: GLState, viewSize: Size) {
		this.applyTranslation(pGLState, viewSize)
		this.applyRotation(pGLState, viewSize)
		this.applySkew(pGLState, viewSize)
		this.applyScale(pGLState, viewSize)
	}

	fun applyTranslation(pGLState: GLState, viewSize: Size)
	fun applyRotation(pGLState: GLState, viewSize: Size)
	fun applySkew(pGLState: GLState, viewSize: Size)
	fun applyScale(pGLState: GLState, viewSize: Size)

	@CallSuper
	override fun drawSelf(glState: GLState, camera: Camera) {
		// if (isHidden) return

		glState.pushModelViewGLMatrix()

		if (size.changed) {
			size.changed = false
			updateVertices()
		}
		if (!glColorIsValid) {
			glColorIsValid = true
			updateColor("gl-draw") // to update new abs alpha in each drawing
		}
		applyTransformations(glState, Size(camera.widthRaw, camera.heightRaw))

		preDraw(glState)
		doDraw()
		postDraw(glState)

		glState.popModelViewGLMatrix()
	}

	@CallSuper
	fun preDraw(glState: GLState) {
		glState.enableBlend()
		glState.blendFunction(
			IShape.BLENDFUNCTION_SOURCE_DEFAULT,
			IShape.BLENDFUNCTION_DESTINATION_DEFAULT
		)
		vertexBufferObject.bind(glState, shaderProgram)
	}

	fun doDraw()

	@CallSuper
	fun postDraw(glState: GLState) {
		vertexBufferObject.unbind(glState, shaderProgram)
		glState.disableBlend()
	}
}