package org.andengine.entity.sprite;

import android.opengl.GLES20;

import org.andengine.opengl.shader.constants.ShaderProgramConstants;
import org.andengine.opengl.vbo.attribute.VertexBufferObjectAttributes;
import org.andengine.opengl.vbo.attribute.VertexBufferObjectAttributesBuilder;

/**
 * (c) 2010 Nicolas Gramlich
 * (c) 2011 Zynga Inc.
 *
 * @author Nicolas Gramlich
 * @since 19:22:38 - 09.03.2010
 */
public class Sprite {
    // ===========================================================
    // Constants
    // ===========================================================

    public static final int VERTEX_INDEX_X = 0;
    public static final int VERTEX_INDEX_Y = Sprite.VERTEX_INDEX_X + 1;
    public static final int COLOR_INDEX = Sprite.VERTEX_INDEX_Y + 1;
    public static final int TEXTURECOORDINATES_INDEX_U = Sprite.COLOR_INDEX + 1;
    public static final int TEXTURECOORDINATES_INDEX_V = Sprite.TEXTURECOORDINATES_INDEX_U + 1;

    public static final int VERTEX_SIZE = 2 + 1 + 2;
    public static final int VERTICES_PER_SPRITE = 4;
    public static final int SPRITE_SIZE = Sprite.VERTEX_SIZE * Sprite.VERTICES_PER_SPRITE;

    public static final VertexBufferObjectAttributes VERTEXBUFFEROBJECTATTRIBUTES_DEFAULT = new VertexBufferObjectAttributesBuilder(3)
            .add(ShaderProgramConstants.ATTRIBUTE_POSITION_LOCATION, ShaderProgramConstants.ATTRIBUTE_POSITION, 2, GLES20.GL_FLOAT, false)
            .add(ShaderProgramConstants.ATTRIBUTE_COLOR_LOCATION, ShaderProgramConstants.ATTRIBUTE_COLOR, 4, GLES20.GL_UNSIGNED_BYTE, true)
            .add(ShaderProgramConstants.ATTRIBUTE_TEXTURECOORDINATES_LOCATION, ShaderProgramConstants.ATTRIBUTE_TEXTURECOORDINATES, 2, GLES20.GL_FLOAT, false)
            .build();
}
