package org.andengine.entity.text.vbo;

import org.andengine.entity.text.Text;
import org.andengine.opengl.font.Font;
import org.andengine.opengl.font.Letter;
import org.andengine.opengl.vbo.DrawType;
import org.andengine.opengl.vbo.HighPerformanceVertexBufferObject;
import org.andengine.opengl.vbo.VertexBufferObjectManager;
import org.andengine.opengl.vbo.attribute.VertexBufferObjectAttributes;

import java.util.ArrayList;

import kotlin.Pair;
import x.spritekit.SKLabelNode;

/**
 * (c) Zynga 2012
 *
 * @author Nicolas Gramlich <ngramlich@zynga.com>
 * @since 12:38:43 - 29.03.2012
 */
public class HighPerformanceTextVertexBufferObject extends HighPerformanceVertexBufferObject {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	// ===========================================================
	// Constructors
	// ===========================================================

	public HighPerformanceTextVertexBufferObject(final VertexBufferObjectManager pVertexBufferObjectManager, final int pCapacity, final DrawType pDrawType, final boolean pAutoDispose, final VertexBufferObjectAttributes pVertexBufferObjectAttributes) {
		super(pVertexBufferObjectManager, pCapacity, pDrawType, pAutoDispose, pVertexBufferObjectAttributes);
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	public void onUpdateColor(float packedColor, final int charactersMaximum) {
		final float[] bufferData = this.mBufferData;

		int bufferDataOffset = 0;
		for (int i = 0; i < charactersMaximum; i++) {
			bufferData[bufferDataOffset + 0 * Text.VERTEX_SIZE + Text.COLOR_INDEX] = packedColor;
			bufferData[bufferDataOffset + 1 * Text.VERTEX_SIZE + Text.COLOR_INDEX] = packedColor;
			bufferData[bufferDataOffset + 2 * Text.VERTEX_SIZE + Text.COLOR_INDEX] = packedColor;
			bufferData[bufferDataOffset + 3 * Text.VERTEX_SIZE + Text.COLOR_INDEX] = packedColor;
			bufferData[bufferDataOffset + 4 * Text.VERTEX_SIZE + Text.COLOR_INDEX] = packedColor;
			bufferData[bufferDataOffset + 5 * Text.VERTEX_SIZE + Text.COLOR_INDEX] = packedColor;

			bufferDataOffset += Text.LETTER_SIZE;
		}

		this.setDirtyOnHardware();
	}

	public void onUpdateVertices(final SKLabelNode pText) {
		final float[] bufferData = this.mBufferData;

		// TODO Optimize with field access?
		final Font font = pText.font;
		final float rawHeight = pText.bounds.height();
		final ArrayList<Pair<CharSequence, Float>> lines = pText.getLines();
		// final ArrayList<Float> lineWidths = pText.getLineWidths();
		final float lineHeight = font.getLineHeight();
		final float textHeight = pText.getFrame().getSize().getHeight();
		final float lineAlignmentWidth = pText.getLineAlignmentWidth();
		final float bottom = pText.bounds.bottom;
		final float descend = font.getDescent();

		int charactersToDraw = 0;
		int bufferDataOffset = 0;

		final int lineCount = lines.size();
		for (int row = 0; row < lineCount; row++) {
			final CharSequence line = lines.get(row).getFirst();

			float xBase;
			switch (pText.getTextAlign()) {
				case RIGHT:
					xBase = -lineAlignmentWidth * 0.5f;
					break;
				case CENTER:
					xBase = 0;
					break;
				case LEFT:
				default:
					xBase = lineAlignmentWidth * 0.5f;
			}
			switch (pText.getLineAlignmentMode()) {
				case RIGHT:
					xBase += lineAlignmentWidth - lines.get(row).getSecond();
					break;
				case CENTER:
					xBase += (lineAlignmentWidth - lines.get(row).getSecond()) * 0.5f;
					break;
				case LEFT:
				default:
					break;
			}

			// Thang.X
			float yBase = row * (lineHeight + pText.getLeading());
			switch (pText.getVerticalAlignmentMode()) {
				case center:
					yBase -= textHeight / 2 - descend - rawHeight / 2 + bottom;
					break;
				default:
					break;
			}

			final int lineLength = line.length();
			Letter previousLetter = null;
			for (int i = 0; i < lineLength; i++) {
				final Letter letter = font.getLetter(line.charAt(i));
				if (previousLetter != null) {
					xBase += previousLetter.getKerning(letter.mCharacter);
				}

				if (!letter.isWhitespace()) {
					final float x = xBase + letter.mOffsetX;
					final float y = yBase + letter.mOffsetY;

					final float y2 = y + letter.mHeight;
					final float x2 = x + letter.mWidth;

					final float u = letter.mU;
					final float v = letter.mV;
					final float u2 = letter.mU2;
					final float v2 = letter.mV2;

					bufferData[bufferDataOffset + 0 * Text.VERTEX_SIZE + Text.VERTEX_INDEX_X] = x;
					bufferData[bufferDataOffset + 0 * Text.VERTEX_SIZE + Text.VERTEX_INDEX_Y] = y;
					bufferData[bufferDataOffset + 0 * Text.VERTEX_SIZE + Text.TEXTURECOORDINATES_INDEX_U] = u;
					bufferData[bufferDataOffset + 0 * Text.VERTEX_SIZE + Text.TEXTURECOORDINATES_INDEX_V] = v;

					bufferData[bufferDataOffset + 1 * Text.VERTEX_SIZE + Text.VERTEX_INDEX_X] = x;
					bufferData[bufferDataOffset + 1 * Text.VERTEX_SIZE + Text.VERTEX_INDEX_Y] = y2;
					bufferData[bufferDataOffset + 1 * Text.VERTEX_SIZE + Text.TEXTURECOORDINATES_INDEX_U] = u;
					bufferData[bufferDataOffset + 1 * Text.VERTEX_SIZE + Text.TEXTURECOORDINATES_INDEX_V] = v2;

					bufferData[bufferDataOffset + 2 * Text.VERTEX_SIZE + Text.VERTEX_INDEX_X] = x2;
					bufferData[bufferDataOffset + 2 * Text.VERTEX_SIZE + Text.VERTEX_INDEX_Y] = y2;
					bufferData[bufferDataOffset + 2 * Text.VERTEX_SIZE + Text.TEXTURECOORDINATES_INDEX_U] = u2;
					bufferData[bufferDataOffset + 2 * Text.VERTEX_SIZE + Text.TEXTURECOORDINATES_INDEX_V] = v2;

					bufferData[bufferDataOffset + 3 * Text.VERTEX_SIZE + Text.VERTEX_INDEX_X] = x2;
					bufferData[bufferDataOffset + 3 * Text.VERTEX_SIZE + Text.VERTEX_INDEX_Y] = y2;
					bufferData[bufferDataOffset + 3 * Text.VERTEX_SIZE + Text.TEXTURECOORDINATES_INDEX_U] = u2;
					bufferData[bufferDataOffset + 3 * Text.VERTEX_SIZE + Text.TEXTURECOORDINATES_INDEX_V] = v2;

					bufferData[bufferDataOffset + 4 * Text.VERTEX_SIZE + Text.VERTEX_INDEX_X] = x2;
					bufferData[bufferDataOffset + 4 * Text.VERTEX_SIZE + Text.VERTEX_INDEX_Y] = y;
					bufferData[bufferDataOffset + 4 * Text.VERTEX_SIZE + Text.TEXTURECOORDINATES_INDEX_U] = u2;
					bufferData[bufferDataOffset + 4 * Text.VERTEX_SIZE + Text.TEXTURECOORDINATES_INDEX_V] = v;

					bufferData[bufferDataOffset + 5 * Text.VERTEX_SIZE + Text.VERTEX_INDEX_X] = x;
					bufferData[bufferDataOffset + 5 * Text.VERTEX_SIZE + Text.VERTEX_INDEX_Y] = y;
					bufferData[bufferDataOffset + 5 * Text.VERTEX_SIZE + Text.TEXTURECOORDINATES_INDEX_U] = u;
					bufferData[bufferDataOffset + 5 * Text.VERTEX_SIZE + Text.TEXTURECOORDINATES_INDEX_V] = v;

					bufferDataOffset += Text.LETTER_SIZE;
					charactersToDraw++;
				}

				xBase += letter.mAdvance;

				previousLetter = letter;
			}
		}
		pText.setCharactersToDraw(charactersToDraw);

		this.setDirtyOnHardware();
	}
}