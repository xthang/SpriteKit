package org.andengine.entity.shape;

import android.opengl.GLES20;

import org.andengine.opengl.shader.ShaderProgram;
import org.andengine.opengl.vbo.IVertexBufferObject;
import org.andengine.opengl.vbo.VertexBufferObjectManager;

/**
 * (c) 2010 Nicolas Gramlich
 * (c) 2011 Zynga Inc.
 *
 * @author Nicolas Gramlich
 * @since 13:32:52 - 07.07.2010
 */
public interface IShape {
	// ===========================================================
	// Constants
	// ===========================================================

	public static final int BLENDFUNCTION_SOURCE_DEFAULT = GLES20.GL_SRC_ALPHA;
	public static final int BLENDFUNCTION_DESTINATION_DEFAULT = GLES20.GL_ONE_MINUS_SRC_ALPHA;

	// ===========================================================
	// Methods
	// ===========================================================

	public boolean collidesWith(final IShape pOtherShape);
}