package x.core.graphics

import android.graphics.Path
import x.spritekit.SKPhysicsBody

abstract class IShape {

	// internal val position = Point.zero
	internal abstract val area: Float

	internal lateinit var physicsBody: SKPhysicsBody
	internal var physicsRevertCount = 0
	internal var kX = 1f
	internal var kY = 1f


	internal abstract fun getPath(): Path

	internal abstract fun contact(other: IShape): Boolean

	internal abstract fun revertMove(other: IShape)
}