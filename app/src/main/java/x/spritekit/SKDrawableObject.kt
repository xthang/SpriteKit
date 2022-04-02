package x.spritekit

import android.graphics.Canvas
import androidx.annotation.CallSuper
import androidx.annotation.ColorInt
import org.andengine.engine.camera.Camera
import org.andengine.opengl.util.GLState
import x.core.graphics.Size

abstract class SKDrawableObject : SKNode(), IDrawableObject {

	override var glColorIsValid = false

	override var size: Size
		get() = super.size
		set(value) {
			super.size = value
		}

	override var disposed = false
	override var disposeInfo: String? = null

	open var shadowRadius: Float? = null

	@ColorInt
	open var shadowColor: Int? = null

	@CallSuper
	override fun clone(): SKDrawableObject {
		val c = super.clone() as SKDrawableObject
		// TODO can not know if vertexBufferObjectAttributes in vertexBufferObject needs to be clone or new
		c.vertexBufferObject = vertexBufferObject.clone()
		return c
	}

	@CallSuper
	override fun dispose(tag: String) {
		super<IDrawableObject>.dispose(tag)
		super<SKNode>.dispose(tag)
	}

	@CallSuper
	override fun drawSelf(canvas: Canvas, sceneXScale: Float, sceneYScale: Float, sceneScaleHasChanged: Boolean) {
		if (parent == null || isHiddenAbs!!) return
		super.drawSelf(canvas, sceneXScale, sceneYScale, sceneScaleHasChanged)
	}

	@CallSuper
	override fun drawSelf(glState: GLState, camera: Camera) {
		if (parent == null || isHiddenAbs!!) return
		super.drawSelf(glState, camera)
	}

	override fun applyTranslation(pGLState: GLState, viewSize: Size) {
		val absPosition = this.positionAbs
		pGLState.translateModelViewGLMatrixf(
			(viewSize.width - size.width) / 2f + absPosition.x,
			(viewSize.height - size.height) / 2f - absPosition.y,
			0f
		)
	}

	override fun applyRotation(pGLState: GLState, viewSize: Size) {
		if (zRotationAbs != 0f) {
			pGLState.translateModelViewGLMatrixf(
				rotationCenterX ?: size.width / 2,
				rotationCenterY ?: size.height / 2,
				0f
			)
			pGLState.rotateModelViewGLMatrixf(-zRotationAbs360, 0f, 0f, 1f)
			pGLState.translateModelViewGLMatrixf(
				-(rotationCenterX ?: size.width / 2),
				-(rotationCenterY ?: size.height / 2),
				0f
			)

			/* TODO There is a special, but very likely case when mRotationCenter and mScaleCenter are the same.
		 * In that case the last glTranslatef of the rotation and the first glTranslatef of the scale is superfluous.
		 * The problem is that applyRotation and applyScale would need to be "merged" in order to efficiently check for that condition.  */
		}
	}

	@CallSuper
	final override fun applySkew(pGLState: GLState, viewSize: Size) {
		if (skewX != 0f || skewY != 0f) {
			pGLState.translateModelViewGLMatrixf(skewCenterX ?: size.width / 2, skewCenterY ?: size.height / 2, 0f)
			pGLState.skewModelViewGLMatrixf(skewX, skewY)
			pGLState.translateModelViewGLMatrixf(-(skewCenterX ?: size.width / 2), -(skewCenterY ?: size.height / 2), 0f)
		}
	}

	@CallSuper
	final override fun applyScale(pGLState: GLState, viewSize: Size) {
		val pAbsScale = this.parent?.scaleAbs ?: 1f to 1f
		if (pAbsScale.first != 1f || pAbsScale.second != 1f || scaleAbs.first < 0 || scaleAbs.second < 0) {
			pGLState.translateModelViewGLMatrixf(scaleCenterX ?: size.width / 2, scaleCenterY ?: size.height / 2, 0f)
			pGLState.scaleModelViewGLMatrixf(
				pAbsScale.first * if (scaleAbs.first < 0) -1 else 1,
				pAbsScale.second * if (scaleAbs.second < 0) -1 else 1,
				1
			)
			pGLState.translateModelViewGLMatrixf(-(scaleCenterX ?: size.width / 2), -(scaleCenterY ?: size.height / 2), 0f)
		}
	}
}