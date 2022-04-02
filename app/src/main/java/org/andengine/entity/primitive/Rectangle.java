package org.andengine.entity.primitive;

import android.opengl.GLES20;

import org.andengine.opengl.shader.constants.ShaderProgramConstants;
import org.andengine.opengl.vbo.attribute.VertexBufferObjectAttributes;
import org.andengine.opengl.vbo.attribute.VertexBufferObjectAttributesBuilder;

/**
 * (c) 2010 Nicolas Gramlich
 * (c) 2011 Zynga Inc.
 *
 * @author Nicolas Gramlich
 * @since 12:18:49 - 13.03.2010
 */
public class Rectangle {
    // ===========================================================
    // Constants
    // ===========================================================

    public static final int VERTEX_INDEX_X = 0;
    public static final int VERTEX_INDEX_Y = Rectangle.VERTEX_INDEX_X + 1;
    public static final int COLOR_INDEX = Rectangle.VERTEX_INDEX_Y + 1;

    public static final int VERTEX_SIZE = 2 + 1;
    public static final int VERTICES_PER_RECTANGLE = 4;
    public static final int RECTANGLE_SIZE = Rectangle.VERTEX_SIZE * Rectangle.VERTICES_PER_RECTANGLE;

    public static final VertexBufferObjectAttributes VERTEXBUFFEROBJECTATTRIBUTES_DEFAULT = new VertexBufferObjectAttributesBuilder(2)
            .add(ShaderProgramConstants.ATTRIBUTE_POSITION_LOCATION, ShaderProgramConstants.ATTRIBUTE_POSITION, 2, GLES20.GL_FLOAT, false)
            .add(ShaderProgramConstants.ATTRIBUTE_COLOR_LOCATION, ShaderProgramConstants.ATTRIBUTE_COLOR, 4, GLES20.GL_UNSIGNED_BYTE, true)
            .build();
}