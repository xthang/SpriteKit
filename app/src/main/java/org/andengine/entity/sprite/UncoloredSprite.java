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
public class UncoloredSprite extends Sprite {
    // ===========================================================
    // Constants
    // ===========================================================

    public static final int VERTEX_INDEX_X = 0;
    public static final int VERTEX_INDEX_Y = UncoloredSprite.VERTEX_INDEX_X + 1;
    public static final int TEXTURECOORDINATES_INDEX_U = UncoloredSprite.VERTEX_INDEX_Y + 1;
    public static final int TEXTURECOORDINATES_INDEX_V = UncoloredSprite.TEXTURECOORDINATES_INDEX_U + 1;

    public static final int VERTEX_SIZE = 2 + 2;
    public static final int VERTICES_PER_SPRITE = 4;
    public static final int SPRITE_SIZE = UncoloredSprite.VERTEX_SIZE * UncoloredSprite.VERTICES_PER_SPRITE;

    public static final VertexBufferObjectAttributes VERTEXBUFFEROBJECTATTRIBUTES_DEFAULT = new VertexBufferObjectAttributesBuilder(2)
            .add(ShaderProgramConstants.ATTRIBUTE_POSITION_LOCATION, ShaderProgramConstants.ATTRIBUTE_POSITION, 2, GLES20.GL_FLOAT, false)
            .add(ShaderProgramConstants.ATTRIBUTE_TEXTURECOORDINATES_LOCATION, ShaderProgramConstants.ATTRIBUTE_TEXTURECOORDINATES, 2, GLES20.GL_FLOAT, false)
            .build();
}
