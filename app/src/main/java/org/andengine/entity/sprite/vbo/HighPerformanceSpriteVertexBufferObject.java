package org.andengine.entity.sprite.vbo;

import androidx.annotation.NonNull;

import org.andengine.entity.sprite.Sprite;
import org.andengine.opengl.vbo.DrawType;
import org.andengine.opengl.vbo.HighPerformanceVertexBufferObject;
import org.andengine.opengl.vbo.VertexBufferObjectManager;
import org.andengine.opengl.vbo.attribute.VertexBufferObjectAttributes;

import x.spritekit.SKSpriteNode;
import x.spritekit.SKTexture;

/**
 * (c) Zynga 2012
 *
 * @author Nicolas Gramlich <ngramlich@zynga.com>
 * @since 18:40:47 - 28.03.2012
 */
public class HighPerformanceSpriteVertexBufferObject extends HighPerformanceVertexBufferObject {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	// ===========================================================
	// Constructors
	// ===========================================================

	public HighPerformanceSpriteVertexBufferObject(final VertexBufferObjectManager pVertexBufferObjectManager, final int pCapacity, final DrawType pDrawType, final boolean pAutoDispose, final VertexBufferObjectAttributes pVertexBufferObjectAttributes) {
		super(pVertexBufferObjectManager, pCapacity, pDrawType, pAutoDispose, pVertexBufferObjectAttributes);
	}

	public void onUpdateColor(final float color) {
		final float[] bufferData = this.mBufferData;

		bufferData[0 * Sprite.VERTEX_SIZE + Sprite.COLOR_INDEX] = color;
		bufferData[1 * Sprite.VERTEX_SIZE + Sprite.COLOR_INDEX] = color;
		bufferData[2 * Sprite.VERTEX_SIZE + Sprite.COLOR_INDEX] = color;
		bufferData[3 * Sprite.VERTEX_SIZE + Sprite.COLOR_INDEX] = color;

		this.setDirtyOnHardware();
	}

	public void onUpdateVertices(final float width, final float height) {
		final float[] bufferData = this.mBufferData;

		final float x = 0;
		final float y = 0;
		final float x2 = width; // TODO Optimize with field access?
		final float y2 = height; // TODO Optimize with field access?

		bufferData[0 * Sprite.VERTEX_SIZE + Sprite.VERTEX_INDEX_X] = x;
		bufferData[0 * Sprite.VERTEX_SIZE + Sprite.VERTEX_INDEX_Y] = y;

		bufferData[1 * Sprite.VERTEX_SIZE + Sprite.VERTEX_INDEX_X] = x;
		bufferData[1 * Sprite.VERTEX_SIZE + Sprite.VERTEX_INDEX_Y] = y2;

		bufferData[2 * Sprite.VERTEX_SIZE + Sprite.VERTEX_INDEX_X] = x2;
		bufferData[2 * Sprite.VERTEX_SIZE + Sprite.VERTEX_INDEX_Y] = y;

		bufferData[3 * Sprite.VERTEX_SIZE + Sprite.VERTEX_INDEX_X] = x2;
		bufferData[3 * Sprite.VERTEX_SIZE + Sprite.VERTEX_INDEX_Y] = y2;

		this.setDirtyOnHardware();
	}

	public void onUpdateTextureCoordinates(@NonNull SKSpriteNode node) {
		final float[] bufferData = this.mBufferData;

		final SKTexture texture = node.getTexture();
		assert texture != null;

		final float u;
		final float v;
		final float u2;
		final float v2;

		// 2022-03-03: Thang.X fixed this
//		if (node.getXScale() == -1) { // TODO Optimize with field access?
//			u = texture.getU2();
//			u2 = texture.getU();
//		} else {
		u = texture.getU();
		u2 = texture.getU2();
//		}
//		if (node.getYScale() == -1) { // TODO Optimize with field access?
//			v = texture.getV2();
//			v2 = texture.getV();
//		} else {
		v = texture.getV();
		v2 = texture.getV2();
//		}

		if (texture.getRotate()) {
			bufferData[0 * Sprite.VERTEX_SIZE + Sprite.TEXTURECOORDINATES_INDEX_U] = u2;
			bufferData[0 * Sprite.VERTEX_SIZE + Sprite.TEXTURECOORDINATES_INDEX_V] = v;

			bufferData[1 * Sprite.VERTEX_SIZE + Sprite.TEXTURECOORDINATES_INDEX_U] = u;
			bufferData[1 * Sprite.VERTEX_SIZE + Sprite.TEXTURECOORDINATES_INDEX_V] = v;

			bufferData[2 * Sprite.VERTEX_SIZE + Sprite.TEXTURECOORDINATES_INDEX_U] = u2;
			bufferData[2 * Sprite.VERTEX_SIZE + Sprite.TEXTURECOORDINATES_INDEX_V] = v2;

			bufferData[3 * Sprite.VERTEX_SIZE + Sprite.TEXTURECOORDINATES_INDEX_U] = u;
			bufferData[3 * Sprite.VERTEX_SIZE + Sprite.TEXTURECOORDINATES_INDEX_V] = v2;
		} else {
			bufferData[0 * Sprite.VERTEX_SIZE + Sprite.TEXTURECOORDINATES_INDEX_U] = u;
			bufferData[0 * Sprite.VERTEX_SIZE + Sprite.TEXTURECOORDINATES_INDEX_V] = v;

			bufferData[1 * Sprite.VERTEX_SIZE + Sprite.TEXTURECOORDINATES_INDEX_U] = u;
			bufferData[1 * Sprite.VERTEX_SIZE + Sprite.TEXTURECOORDINATES_INDEX_V] = v2;

			bufferData[2 * Sprite.VERTEX_SIZE + Sprite.TEXTURECOORDINATES_INDEX_U] = u2;
			bufferData[2 * Sprite.VERTEX_SIZE + Sprite.TEXTURECOORDINATES_INDEX_V] = v;

			bufferData[3 * Sprite.VERTEX_SIZE + Sprite.TEXTURECOORDINATES_INDEX_U] = u2;
			bufferData[3 * Sprite.VERTEX_SIZE + Sprite.TEXTURECOORDINATES_INDEX_V] = v2;
		}

		this.setDirtyOnHardware();
	}

	// ===========================================================
	// Methods
	// ===========================================================

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}