package org.andengine.opengl.vbo.attribute;


import androidx.annotation.NonNull;

/**
 * (c) Zynga 2011
 *
 * @author Nicolas Gramlich <ngramlich@zynga.com>
 * @since 14:22:06 - 15.08.2011
 */
public class VertexBufferObjectAttributes implements Cloneable {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	private final int mStride;
	private VertexBufferObjectAttribute[] mVertexBufferObjectAttributes;

	// ===========================================================
	// Constructors
	// ===========================================================

	public VertexBufferObjectAttributes(final int pStride, final VertexBufferObjectAttribute... pVertexBufferObjectAttributes) {
		this.mVertexBufferObjectAttributes = pVertexBufferObjectAttributes;
		this.mStride = pStride;
	}

	// Thang.X
	@NonNull
	public VertexBufferObjectAttributes clone() throws CloneNotSupportedException {
		VertexBufferObjectAttributes c = (VertexBufferObjectAttributes) super.clone();
		c.mVertexBufferObjectAttributes = new VertexBufferObjectAttribute[mVertexBufferObjectAttributes.length];
		for (int i = 0; i < mVertexBufferObjectAttributes.length; i++) {
			c.mVertexBufferObjectAttributes[i] = mVertexBufferObjectAttributes[i].clone();
		}
		return c;
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	// ===========================================================
	// Methods
	// ===========================================================

	public void glVertexAttribPointers() {
		for (VertexBufferObjectAttribute vertexBufferObjectAttribute : this.mVertexBufferObjectAttributes) {
			vertexBufferObjectAttribute.glVertexAttribPointer(this.mStride);
		}
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}