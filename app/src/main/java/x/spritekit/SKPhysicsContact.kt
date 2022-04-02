package x.spritekit

import x.core.graphics.Point
import java.util.*
import kotlin.properties.Delegates

class SKPhysicsContact {
	var bodyA: SKPhysicsBody
		private set

	var bodyB: SKPhysicsBody
		private set

	lateinit var contactPoint: Point
		private set

	lateinit var contactNormal: Vector<Float>
		private set

	var collisionImpulse by Delegates.notNull<Float>()
		private set

	constructor(bodyA: SKPhysicsBody, bodyB: SKPhysicsBody) {
		this.bodyA = bodyA
		this.bodyB = bodyB
	}
}