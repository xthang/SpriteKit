package x.spritekit

import android.graphics.Color
import android.graphics.Paint
import android.text.TextPaint

object CommonConfig {

	val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG)
	val textPaintSceneInfo = TextPaint().apply {
		textSize = 40f;
		color = Color.MAGENTA;
		textAlign = Paint.Align.RIGHT
		baselineShift = (textSize / 2 - descent()).toInt()
	}

	val strokePaint =
		Paint().apply { style = Paint.Style.STROKE; strokeWidth = 3f; color = Color.GREEN }
	val strokePaint2 =
		Paint().apply { style = Paint.Style.STROKE; strokeWidth = 3f; color = Color.YELLOW }
}
