package x.spritekit

import x.spritekit.TimeInterval
import x.core.graphics.Point

enum class SKActionTimingMode {
	linear, easeIn, easeOut, easeInEaseOut
}

class SKAction private constructor(var type: Type) : Cloneable {

	enum class Type {
		DELAY, RUN_BLOCK, REMOVE, HIDE, UNHIDE,
		REPEAT, SEQUENCE, GROUP,
		MOVE_BY, MOVE_TO, ROTATE_BY, ROTATE_TO, FADE_TO, SCALE_TO,
		SET_TEXTURE, ANIMATE,
		COLORIZE,
		PLAY
	}

	var name: String? = null

	internal var duration: Double? = null
		set(value) {
			if (value!!.isNaN()) throw NumberFormatException("value isNaN")
			field = value
		}

	var timingMode: SKActionTimingMode? = null

	/** When set, prodives a custom timing via a block. Applies after
	the 'timingMode' property is taken into account, defaults to nil
	@see SKActionTimingFunction
	 */
	// open var timingFunction: SKActionTimingFunction

	/** A speed factor that modifies how fast an action runs. Default value is 1.0 */
	var speed = 1f

	// internal
	internal var completion: (() -> Unit)? = null
	internal var lastTime: Long? = null
	internal var newTime: Long? = null
	internal var isFinished = false

	internal var children: ArrayList<SKAction>? = null
	internal var childrenInRun: ArrayList<SKAction>? = null
		get() {
			if (field.isNullOrEmpty()) field = this.children?.map { it.clone() } as ArrayList<SKAction>
			return field
		}
	internal var repeatedAction: SKAction? = null
	internal var repeatCount: Int? = null

	internal var block: (() -> Unit)? = null
	internal var deltaX: Float? = null
	internal var deltaY: Float? = null
	internal var toX: Float? = null
	internal var toY: Float? = null
	internal var angle: Float? = null
	internal var fromScaleX: Float? = null
	internal var scaleX: Float? = null
	internal var fromScaleY: Float? = null
	internal var scaleY: Float? = null
	internal var alpha: Float? = null
	internal var texture: SKTexture? = null

	// internal var textures: List<SKTexture>? = null
	internal var resize: Boolean? = null

	internal var successfulExeCount = 0

	companion object {
		private const val TAG = "SKAction"

		fun wait(duration: Double): SKAction {
			val a = SKAction(Type.DELAY)
			a.duration = duration
			return a
		}

		fun run(block: () -> Unit): SKAction {
			val a = SKAction(Type.RUN_BLOCK)
			a.block = block
			return a
		}

		fun removeFromParent(): SKAction {
			return SKAction(Type.REMOVE)
		}

		fun hide(): SKAction {
			return SKAction(Type.HIDE)
		}

		fun unhide(): SKAction {
			return SKAction(Type.UNHIDE)
		}

		fun repeat(action: SKAction, count: Int): SKAction {
			val a = SKAction(Type.REPEAT)
			a.repeatedAction = action
			a.repeatCount = count
			return a
		}

		fun repeatForever(action: SKAction): SKAction {
			return repeat(action, -1)
		}

		fun sequence(vararg actions: SKAction): SKAction {
			val a = SKAction(Type.SEQUENCE)
			a.children = ArrayList()
			a.children!!.addAll(actions)
			return a
		}

		fun group(vararg actions: SKAction): SKAction {
			val a = SKAction(Type.GROUP)
			a.children = ArrayList()
			a.children!!.addAll(actions)
			return a
		}

		fun moveBy(deltaX: Float, deltaY: Float, duration: Double): SKAction {
			val a = SKAction(Type.MOVE_BY)
			a.deltaX = deltaX
			a.deltaY = deltaY
			a.duration = duration
			return a
		}

		fun moveTo(x: Float, y: Float, duration: Double): SKAction {
			val a = SKAction(Type.MOVE_TO)
			a.toX = x
			a.toY = y
			a.duration = duration
			return a
		}

		fun moveTo(location: Point, duration: Double): SKAction {
			return moveTo(location.x, location.y, duration)
		}

		fun moveToX(x: Float, duration: Double): SKAction {
			val a = SKAction(Type.MOVE_TO)
			a.toX = x
			a.duration = duration
			return a
		}

		fun moveToY(y: Float, duration: Double): SKAction {
			val a = SKAction(Type.MOVE_TO)
			a.toY = y
			a.duration = duration
			return a
		}

		/** Creates an action that rotates the node by a relative value
		 * @param radians The amount to rotate the node, in radians
		 * @param duration The duration of the animation, in seconds
		 */
		fun rotateBy(angle: Float, duration: Double): SKAction {
			val a = SKAction(Type.ROTATE_BY)
			a.angle = angle
			a.duration = duration
			return a
		}

		/** Creates an action that rotates the node counterclockwise to an absolute angle
		 * @param radians The angle to rotate the node to, in radians
		 * @param duration The duration of the animation
		 */
		fun rotateTo(angle: Float, duration: Double): SKAction {
			val a = SKAction(Type.ROTATE_TO)
			a.angle = angle
			a.duration = duration
			return a
		}

		fun fadeIn(duration: Double, completion: (() -> Unit)? = null): SKAction {
			return fadeTo(1f, duration, completion)
		}

		fun fadeOut(duration: Double, completion: (() -> Unit)? = null): SKAction {
			return fadeTo(0f, duration, completion)
		}

		fun fadeTo(alpha: Float, duration: Double, completion: (() -> Unit)? = null): SKAction {
			val a = SKAction(Type.FADE_TO)
			a.alpha = alpha
			a.duration = duration
			a.completion = completion
			return a
		}

		fun scaleTo(scale: Float, duration: Double, completion: (() -> Unit)? = null): SKAction {
			val a = SKAction(Type.SCALE_TO)
			a.scaleX = scale
			a.scaleY = scale
			a.duration = duration
			a.completion = completion
			return a
		}

		fun setTexture(texture: SKTexture, resize: Boolean): SKAction {
			val a = SKAction(Type.SET_TEXTURE)
			a.texture = texture.clone()
			a.resize = resize
			return a
		}

		fun animate(with: List<SKTexture>, timePerFrame: TimeInterval): SKAction {
			if (with.isEmpty()) throw Error("!-  animate with 0 texture")

			val seq: ArrayList<SKAction> = arrayListOf()
			with.forEach {
				seq.add(setTexture(it, false))
				seq.add(wait(timePerFrame))
			}
			return sequence(*seq.toTypedArray())
		}

		fun colorize(withColorBlendFactor: Float, duration: TimeInterval): SKAction {
			val a = SKAction(Type.COLORIZE)
			a.duration = duration
			return a
		}

		fun play(): SKAction {
			return SKAction(Type.PLAY)
		}
	}

	public override fun clone(): SKAction {
		val c = super.clone() as SKAction

		if (repeatedAction != null) c.repeatedAction = repeatedAction!!.clone()
		if (this.children != null) c.children = this.children!!.map { it.clone() } as ArrayList<SKAction>
		this.childrenInRun = null

		// c.texture = texture?.clone() // TODO: clone or not?
		// c.textures = textures?.map { it.clone() } as List<SKTexture>

		return c
	}

	fun reversed(): SKAction {
		val a = SKAction(type)

		a.name = name

		a.duration = duration

		if (deltaX != null) a.deltaX = -deltaX!!
		if (deltaY != null) a.deltaY = -deltaY!!
		if (toX != null) a.toX = -toX!!
		if (toY != null) a.toY = -toY!!
		if (fromScaleX != null) a.fromScaleX = -fromScaleX!!
		if (fromScaleY != null) a.fromScaleY = -fromScaleY!!
		if (scaleX != null) a.scaleX = -scaleX!!
		if (scaleY != null) a.scaleY = -scaleY!!
		if (alpha != null) a.alpha = -alpha!!

		return a
	}

	override fun toString(): String {
		return "(${hashCode()} : $type | $duration | $ | $lastTime | $isFinished | $repeatCount | $repeatedAction | ${this.children})"
	}

	internal fun getActionTimeToRun(full: Boolean): Long {
		return if (type == Type.REPEAT) {
			if (repeatCount!! <= -1) Long.MAX_VALUE
			else repeatCount!! * repeatedAction!!.getActionTimeToRun(full)
		} else if (type == Type.SEQUENCE) {
			var t = 0L
			val s = if (full) this.children!!
			else (if (this.childrenInRun != null && this.childrenInRun!!.isNotEmpty()) this.childrenInRun else this.children)!!
			s.forEach {
				t += it.getActionTimeToRun(full)
			}
			t
		} else {
			((duration ?: 0.0) * 1000).toLong()
		}
	}
}
