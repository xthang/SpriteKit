package org.andengine.entity.text;

import android.opengl.GLES20;

import org.andengine.opengl.shader.constants.ShaderProgramConstants;
import org.andengine.opengl.vbo.attribute.VertexBufferObjectAttributes;
import org.andengine.opengl.vbo.attribute.VertexBufferObjectAttributesBuilder;
import org.andengine.util.adt.DataConstants;

/**
 * TODO Try Degenerate Triangles?
 * <p>
 * (c) 2010 Nicolas Gramlich
 * (c) 2011 Zynga Inc.
 *
 * @author Nicolas Gramlich
 * @since 10:54:59 - 03.04.2010
 */
public class Text {
    // ===========================================================
    // Constants
    // ===========================================================

    public static final float LEADING_DEFAULT = 0;

    public static final int VERTEX_INDEX_X = 0;
    public static final int VERTEX_INDEX_Y = Text.VERTEX_INDEX_X + 1;
    public static final int COLOR_INDEX = Text.VERTEX_INDEX_Y + 1;
    public static final int TEXTURECOORDINATES_INDEX_U = Text.COLOR_INDEX + 1;
    public static final int TEXTURECOORDINATES_INDEX_V = Text.TEXTURECOORDINATES_INDEX_U + 1;

    public static final int VERTEX_SIZE = 2 + 1 + 2;
    public static final int VERTICES_PER_LETTER = 6;
    public static final int LETTER_SIZE = Text.VERTEX_SIZE * Text.VERTICES_PER_LETTER;
    public static final int VERTEX_STRIDE = Text.VERTEX_SIZE * DataConstants.BYTES_PER_FLOAT;

    public static final VertexBufferObjectAttributes VERTEXBUFFEROBJECTATTRIBUTES_DEFAULT = new VertexBufferObjectAttributesBuilder(3)
            .add(ShaderProgramConstants.ATTRIBUTE_POSITION_LOCATION, ShaderProgramConstants.ATTRIBUTE_POSITION, 2, GLES20.GL_FLOAT, false)
            .add(ShaderProgramConstants.ATTRIBUTE_COLOR_LOCATION, ShaderProgramConstants.ATTRIBUTE_COLOR, 4, GLES20.GL_UNSIGNED_BYTE, true)
            .add(ShaderProgramConstants.ATTRIBUTE_TEXTURECOORDINATES_LOCATION, ShaderProgramConstants.ATTRIBUTE_TEXTURECOORDINATES, 2, GLES20.GL_FLOAT, false)
            .build();
}