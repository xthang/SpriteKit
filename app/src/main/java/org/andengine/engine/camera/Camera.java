package org.andengine.engine.camera;

import org.andengine.engine.handler.IUpdateHandler;
import org.andengine.engine.handler.UpdateHandlerList;
import org.andengine.opengl.util.GLState;

/**
 * (c) 2010 Nicolas Gramlich
 * (c) 2011 Zynga Inc.
 *
 * @author Nicolas Gramlich
 * @since 10:24:18 - 25.03.2010
 */
public class Camera implements IUpdateHandler {
    // ===========================================================
    // Constants
    // ===========================================================

    static final float[] VERTICES_TMP = new float[2];

    private static final int UPDATEHANDLERS_CAPACITY_DEFAULT = 4;

    // ===========================================================
    // Fields
    // ===========================================================

    protected float mXMin;
    protected float mXMax;
    protected float mYMin;
    protected float mYMax;

    private float mZNear = -1.0f;
    private float mZFar = 1.0f;

    protected float mRotation = 0;
    protected float mCameraSceneRotation = 0;

    protected int mSurfaceX;
    protected int mSurfaceY;
    protected int mSurfaceWidth;
    protected int mSurfaceHeight;

    protected boolean mResizeOnSurfaceSizeChanged;
    protected UpdateHandlerList mUpdateHandlers;

    // ===========================================================
    // Constructors
    // ===========================================================

    public Camera(final float pX, final float pY, final float pWidth, final float pHeight) {
        this.set(pX, pY, pX + pWidth, pY + pHeight);
    }

    // ===========================================================
    // Getter & Setter
    // ===========================================================

    public float getXMin() {
        return this.mXMin;
    }

    public void setXMin(final float pXMin) {
        this.mXMin = pXMin;
    }

    public float getXMax() {
        return this.mXMax;
    }

    public void setXMax(final float pXMax) {
        this.mXMax = pXMax;
    }

    public float getYMin() {
        return this.mYMin;
    }

    public void setYMin(final float pYMin) {
        this.mYMin = pYMin;
    }

    public float getYMax() {
        return this.mYMax;
    }

    public void setYMax(final float pYMax) {
        this.mYMax = pYMax;
    }

    public void set(final float pXMin, final float pYMin, final float pXMax, final float pYMax) {
        this.mXMin = pXMin;
        this.mXMax = pXMax;
        this.mYMin = pYMin;
        this.mYMax = pYMax;
    }

    public float getZNear() {
        return this.mZNear;
    }

    public float getZFar() {
        return this.mZFar;
    }

    public void setZNear(final float pZNear) {
        this.mZNear = pZNear;
    }

    public void setZFar(final float pZFar) {
        this.mZFar = pZFar;
    }

    public void setZClippingPlanes(final float pNearZClippingPlane, final float pFarZClippingPlane) {
        this.mZNear = pNearZClippingPlane;
        this.mZFar = pFarZClippingPlane;
    }

    public float getWidth() {
        return this.mXMax - this.mXMin;
    }

    public float getHeight() {
        return this.mYMax - this.mYMin;
    }

    public float getWidthRaw() {
        return this.mXMax - this.mXMin;
    }

    public float getHeightRaw() {
        return this.mYMax - this.mYMin;
    }

    public float getCenterX() {
        return (this.mXMin + this.mXMax) * 0.5f;
    }

    public float getCenterY() {
        return (this.mYMin + this.mYMax) * 0.5f;
    }

    public void setCenter(final float pCenterX, final float pCenterY) {
        final float dX = pCenterX - this.getCenterX();
        final float dY = pCenterY - this.getCenterY();

        this.mXMin += dX;
        this.mXMax += dX;
        this.mYMin += dY;
        this.mYMax += dY;
    }

    public void offsetCenter(final float pX, final float pY) {
        this.setCenter(this.getCenterX() + pX, this.getCenterY() + pY);
    }

    public boolean isRotated() {
        return this.mRotation != 0;
    }

    public float getRotation() {
        return this.mRotation;
    }

    public void setRotation(final float pRotation) {
        this.mRotation = pRotation;
    }

    public float getCameraSceneRotation() {
        return this.mCameraSceneRotation;
    }

    public void setCameraSceneRotation(final float pCameraSceneRotation) {
        this.mCameraSceneRotation = pCameraSceneRotation;
    }

    public int getSurfaceX() {
        return this.mSurfaceX;
    }

    public int getSurfaceY() {
        return this.mSurfaceY;
    }

    public int getSurfaceWidth() {
        return this.mSurfaceWidth;
    }

    public int getSurfaceHeight() {
        return this.mSurfaceHeight;
    }

    public void setSurfaceSize(final int pSurfaceX, final int pSurfaceY, final int pSurfaceWidth, final int pSurfaceHeight) {
        if (this.mSurfaceHeight == 0 && this.mSurfaceWidth == 0) {
            this.onSurfaceSizeInitialized(pSurfaceX, pSurfaceY, pSurfaceWidth, pSurfaceHeight);
        } else if (this.mSurfaceWidth != pSurfaceWidth || this.mSurfaceHeight != pSurfaceHeight) {
            this.onSurfaceSizeChanged(this.mSurfaceX, this.mSurfaceY, this.mSurfaceWidth, this.mSurfaceHeight, pSurfaceX, pSurfaceY, pSurfaceWidth, pSurfaceHeight);
        }
    }

    public boolean isResizeOnSurfaceSizeChanged() {
        return this.mResizeOnSurfaceSizeChanged;
    }

    public void setResizeOnSurfaceSizeChanged(final boolean pResizeOnSurfaceSizeChanged) {
        this.mResizeOnSurfaceSizeChanged = pResizeOnSurfaceSizeChanged;
    }

    // ===========================================================
    // Methods for/from SuperClass/Interfaces
    // ===========================================================

    @Override
    public void onUpdate(final float pSecondsElapsed) {
        if (this.mUpdateHandlers != null) {
            this.mUpdateHandlers.onUpdate(pSecondsElapsed);
        }
    }

    @Override
    public void reset() {

    }

    // ===========================================================
    // Methods
    // ===========================================================

    public void onApplySceneMatrix(final GLState pGLState) {
        pGLState.orthoProjectionGLMatrixf(this.getXMin(), this.getXMax(), this.getYMax(), this.getYMin(), this.mZNear, this.mZFar);

        final float rotation = this.mRotation;
        if (rotation != 0) {
            Camera.applyRotation(pGLState, this.getCenterX(), this.getCenterY(), rotation);
        }
    }

    public void onApplySceneBackgroundMatrix(final GLState pGLState) {
        final float widthRaw = this.getWidthRaw();
        final float heightRaw = this.getHeightRaw();

        pGLState.orthoProjectionGLMatrixf(0, widthRaw, heightRaw, 0, this.mZNear, this.mZFar);

        final float rotation = this.mRotation;
        if (rotation != 0) {
            Camera.applyRotation(pGLState, widthRaw * 0.5f, heightRaw * 0.5f, rotation);
        }
    }

    public void onApplyCameraSceneMatrix(final GLState pGLState) {
        final float widthRaw = this.getWidthRaw();
        final float heightRaw = this.getHeightRaw();
        pGLState.orthoProjectionGLMatrixf(0, widthRaw, heightRaw, 0, this.mZNear, this.mZFar);

        final float cameraSceneRotation = this.mCameraSceneRotation;
        if (cameraSceneRotation != 0) {
            Camera.applyRotation(pGLState, widthRaw * 0.5f, heightRaw * 0.5f, cameraSceneRotation);
        }
    }

    private static void applyRotation(final GLState pGLState, final float pRotationCenterX, final float pRotationCenterY, final float pAngle) {
        pGLState.translateProjectionGLMatrixf(pRotationCenterX, pRotationCenterY, 0);
        pGLState.rotateProjectionGLMatrixf(pAngle, 0, 0, 1);
        pGLState.translateProjectionGLMatrixf(-pRotationCenterX, -pRotationCenterY, 0);
    }

    protected void onSurfaceSizeInitialized(final int pSurfaceX, final int pSurfaceY, final int pSurfaceWidth, final int pSurfaceHeight) {
        this.mSurfaceX = pSurfaceX;
        this.mSurfaceY = pSurfaceY;
        this.mSurfaceWidth = pSurfaceWidth;
        this.mSurfaceHeight = pSurfaceHeight;
    }

    protected void onSurfaceSizeChanged(final int pOldSurfaceX, final int pOldSurfaceY, final int pOldSurfaceWidth, final int pOldSurfaceHeight, final int pNewSurfaceX, final int pNewSurfaceY, final int pNewSurfaceWidth, final int pNewSurfaceHeight) {
        if (this.mResizeOnSurfaceSizeChanged) {
            final float surfaceWidthRatio = (float) pNewSurfaceWidth / pOldSurfaceWidth;
            final float surfaceHeightRatio = (float) pNewSurfaceHeight / pOldSurfaceHeight;

            final float centerX = this.getCenterX();
            final float centerY = this.getCenterY();

            final float newWidthRaw = this.getWidthRaw() * surfaceWidthRatio;
            final float newHeightRaw = this.getHeightRaw() * surfaceHeightRatio;

            final float newWidthRawHalf = newWidthRaw * 0.5f;
            final float newHeightRawHalf = newHeightRaw * 0.5f;

            final float xMin = centerX - newWidthRawHalf;
            final float yMin = centerY - newHeightRawHalf;
            final float xMax = centerX + newWidthRawHalf;
            final float yMax = centerY + newHeightRawHalf;

            this.set(xMin, yMin, xMax, yMax);
        }

        this.mSurfaceX = pNewSurfaceX;
        this.mSurfaceY = pNewSurfaceY;
        this.mSurfaceWidth = pNewSurfaceWidth;
        this.mSurfaceHeight = pNewSurfaceHeight;
    }

    // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================
}
