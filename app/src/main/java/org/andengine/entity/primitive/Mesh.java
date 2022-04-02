package org.andengine.entity.primitive;

import android.opengl.GLES20;

import org.andengine.opengl.shader.constants.ShaderProgramConstants;
import org.andengine.opengl.vbo.attribute.VertexBufferObjectAttributes;
import org.andengine.opengl.vbo.attribute.VertexBufferObjectAttributesBuilder;

/**
 * (c) Zynga 2012
 *
 * @author Nicolas Gramlich <ngramlich@zynga.com>
 * @since 16:44:50 - 09.02.2012
 */
public class Mesh {
    // ===========================================================
    // Constants
    // ===========================================================

    public static final int VERTEX_INDEX_X = 0;
    public static final int VERTEX_INDEX_Y = Mesh.VERTEX_INDEX_X + 1;
    public static final int COLOR_INDEX = Mesh.VERTEX_INDEX_Y + 1;

    public static final int VERTEX_SIZE = 2 + 1;

    public static final VertexBufferObjectAttributes VERTEXBUFFEROBJECTATTRIBUTES_DEFAULT = new VertexBufferObjectAttributesBuilder(2)
            .add(ShaderProgramConstants.ATTRIBUTE_POSITION_LOCATION, ShaderProgramConstants.ATTRIBUTE_POSITION, 2, GLES20.GL_FLOAT, false)
            .add(ShaderProgramConstants.ATTRIBUTE_COLOR_LOCATION, ShaderProgramConstants.ATTRIBUTE_COLOR, 4, GLES20.GL_UNSIGNED_BYTE, true)
            .build();
}