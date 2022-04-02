package x.core.graphics

import android.graphics.Path
import android.graphics.RectF
import android.graphics.Region
import android.graphics.RegionIterator

class Polygon(private val path: Path) : IShape() {

	override val area: Float

	init {
		val region = Region()
		val rect = RectF()
		path.computeBounds(rect, true)
		region.setPath(path, Region(rect.left.toInt(), rect.top.toInt(), rect.right.toInt(), rect.bottom.toInt()))
		val regionIterator = RegionIterator(region)
		val tmpRect = android.graphics.Rect()
		var size = 0 // amount of Rects
		var area = 0f // units of area
		while (regionIterator.next(tmpRect)) {
			size++;
			area += tmpRect.width() * tmpRect.height();
		}
		this.area = area
	}


	override fun getPath(): Path = Path(path)

	override fun contact(other: IShape): Boolean {
		TODO("Not yet implemented")
	}

	override fun revertMove(other: IShape) {
		TODO("Not yet implemented")
	}
}