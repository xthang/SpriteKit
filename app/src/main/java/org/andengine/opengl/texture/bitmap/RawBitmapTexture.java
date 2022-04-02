package org.andengine.opengl.texture.bitmap;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import org.andengine.opengl.texture.PixelFormat;
import org.andengine.opengl.texture.Texture;
import org.andengine.opengl.texture.TextureManager;
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.util.GLState;
import org.andengine.util.exception.NullBitmapException;
import org.andengine.util.math.MathUtils;

import java.io.IOException;

/**
 * @author Thang.X
 * @since 2021-11-08 21:30
 */
public class RawBitmapTexture extends Texture {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	private final int mWidth;
	private final int mHeight;
	public Integer outWidth;
	public Integer outHeight;
	private final Bitmap bitmap;

	// ===========================================================
	// Constructors
	// ===========================================================

	public RawBitmapTexture(final TextureManager pTextureManager, final Bitmap bitmap) {
		super(pTextureManager, BitmapTextureFormat.RGBA_8888.getPixelFormat(), TextureOptions.DEFAULT, null);

		this.bitmap = bitmap;

		final BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
		decodeOptions.inJustDecodeBounds = true;

		this.mWidth = bitmap.getWidth();
		this.mHeight = bitmap.getHeight();
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	@Override
	public int getWidth() {
		return this.mWidth;
	}

	public void setWidth(int width) {
		this.outWidth = width;
	}

	@Override
	public int getHeight() {
		return this.mHeight;
	}

	public void getHeight(int height) {
		this.outHeight = height;
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	@Override
	protected void writeTextureToHardware(final GLState pGLState) throws IOException {
		if (bitmap == null) {
			throw new NullBitmapException("Caused by: '" + this.toString() + "'.");
		}

		final boolean useDefaultAlignment = MathUtils.isPowerOfTwo(bitmap.getWidth()) && MathUtils.isPowerOfTwo(bitmap.getHeight()) && (this.mPixelFormat == PixelFormat.RGBA_8888);
		if (!useDefaultAlignment) {
			/* Adjust unpack alignment. */
			GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);
		}

		final boolean preMultipyAlpha = this.mTextureOptions.mPreMultiplyAlpha;
		if (preMultipyAlpha) {
			GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
		} else {
			pGLState.glTexImage2D(name, GLES20.GL_TEXTURE_2D, 0, bitmap, 0, this.mPixelFormat);
		}

		if (!useDefaultAlignment) {
			/* Restore default unpack alignment. */
			GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, GLState.GL_UNPACK_ALIGNMENT_DEFAULT);
		}

		// bitmap.recycle(); not recycle here, it can might be used elsewhere or to be ¬cloned
	}

	// ===========================================================
	// Methods
	// ===========================================================

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
