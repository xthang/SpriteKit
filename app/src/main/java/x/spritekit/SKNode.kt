package x.spritekit

import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.SoundPool
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.annotation.CallSuper
import org.andengine.opengl.vbo.VertexBufferObjectManager
import x.core.graphics.*
import x.core.ui.UIResponder
import x.core.ui.UITouch
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.*

open class SKNode : UIResponder(), Cloneable {

	companion object {
		private const val TAG = "SKNode"
	}

	var name: String? = null

	// -------
	protected var isClone = false
	private var isScaleSet = false   // prevent loop when set scale or size
	private var isSizeSet = false   // prevent loop when set scale or size

	protected var scaleIsValid = false
	internal var scaleIsValid2 = false
	private var colorIsValid = false
	private var alphaIsValid = false

	// protected open var glVerticesIsValid = false
	protected open var glColorIsValid = false

	// private var toBeRemoved = false

	var isUserInteractionEnabled = false
		set(value) {
			if (field == value) return
			field = value
			if (value) scene?.touchListeners?.add(this)
			else scene?.touchListeners?.remove(this)
		}
	var isPaused = true

	// -------
	/** A rectangle in the parent’s coordinate system that contains the node’s content, ignoring the node’s children. */
	open var frameHorizontalAlignment = RectHorizontalAlignmentMode.center
	open var frameVerticalAlignment = RectVerticalAlignmentMode.center
	open val frame: Rect
		get() = Rect(position, size).apply {
			verticalAlignmentMode = frameVerticalAlignment
			horizontalAlignmentMode = frameHorizontalAlignment
		}
	private val absFrame: Rect
		get() = Rect(getAbsPosition(false), size).apply {
			verticalAlignmentMode = frameVerticalAlignment
			horizontalAlignmentMode = frameHorizontalAlignment
		}

	private var rawSize: Size = Size.zero // this size is set by user
	protected open var size: Size = Size.zero // ! setting size does not affect scale
		set(value) {
			if (value == field && !isClone) return
			field = value.clone()
			if (!isScaleSet) rawSize = value.clone()
		}

	var xScale = 1f
		set(value) {
			if (value == field) return
			field = value
			isScaleSet = true
			if (!isSizeSet) size.width = rawSize.width * abs(value)
			isScaleSet = false
			scaleIsValid = false
			scaleIsValid2 = false
		}
	var yScale = 1f
		set(value) {
			if (value == field) return
			field = value
			isScaleSet = true
			if (!isSizeSet) size.height = rawSize.height * abs(value)
			isScaleSet = false
			scaleIsValid = false
			scaleIsValid2 = false
		}
	internal var scaleAbs: Pair<Float, Float> = 1f to 1f
	var scaleCenterX: Float? = null
	var scaleCenterY: Float? = null

	var position = Point.zero
	internal var positionAbs = Point.zero

	var zPosition: Float = 0f
	internal var zPositionAbs: Float = 0f

	internal var provisionalDeltaX: Float = 0f
	internal var provisionalDeltaXAbs: Float = 0f
	internal var provisionalDeltaY: Float = 0f
	internal var provisionalDeltaYAbs: Float = 0f

	var zRotation = 0f
	internal var zRotationAbs: Float = 0f
	internal val zRotationAbs360: Float get() = zRotationAbs / 2 / PI.toFloat() * 360
	var rotationCenterX: Float? = null
	var rotationCenterY: Float? = null

	var skewX = 0f
	var skewY = 0f
	var skewCenterX: Float? = null
	var skewCenterY: Float? = null

	open var alpha: Float = 1f
		set(value) {
			field = value
			alphaIsValid = false
			glColorIsValid = false
		}
	internal var alphaAbs: Float = 1f

	var isHidden = false
	internal var isHiddenAbs: Boolean? = null

	var speed: Float = 1f
	internal var speedAbs: Float = 1f

	var parent: SKNode? = null
		private set
	private var actualChildren: LinkedHashSet<SKNode> = linkedSetOf()
	val children: List<SKNode> get() = actualChildren.filter { it.parent != null }
	private var clonedChildren: LinkedHashSet<SKNode> = linkedSetOf()
	private var clonedChildren2: LinkedHashSet<SKNode> = linkedSetOf()

	// private var clonedChildren3: LinkedHashSet<SKNode> = linkedSetOf()
	private var namedChildren: ConcurrentHashMap<String, MutableSet<SKNode>> = ConcurrentHashMap()
	val scene: SKScene?
		get() {
			var p = parent
			while (p != null) {
				if (p is SKScene) return p
				p = p.parent
			}
			return null
		}

	private var actions: LinkedHashSet<SKAction> = linkedSetOf()
	private var clonedActions: LinkedHashSet<SKAction> = linkedSetOf()

	var physicsBody: SKPhysicsBody? = null
		set(value) {
			field?.let {
				scene?.physicsWorld?.bodies?.remove(it)
				it.node = null
			}
			field = value
			field?.node = this // set node before add to world
			if (value != null) scene?.physicsWorld?.bodies?.add(value)
		}

	var userData: HashMap<String, Any>? = null

	protected var drawLayers: TreeMap<Float, ArrayList<IDrawable>>? = null

	@CallSuper
	public override fun clone(): SKNode {
		val c = super.clone() as SKNode
		c.isClone = true

		c.scaleIsValid = false
		c.scaleIsValid2 = false
		c.colorIsValid = false
		c.alphaIsValid = false
		c.glColorIsValid = false

		c.size = size
		c.rawSize = rawSize.clone()
		c.position = position.clone()
		c.positionAbs = Point.zero

		c.parent = null // set parent null before physicsBody
		c.actualChildren = linkedSetOf()
		c.namedChildren = ConcurrentHashMap()
		synchronized(actualChildren) {
			actualChildren.forEach {
				c.actualChildren.add(it.clone().apply {
					this.parent = c
					if (this.name.isNullOrEmpty().not())
						c.namedChildren.getOrPut(this.name!!) { mutableSetOf() }.add(this)
				})
			}
		}
		c.clonedChildren = linkedSetOf()
		c.clonedChildren2 = linkedSetOf()

		c.actions = linkedSetOf()
		actions.forEach { c.actions.add(it.clone()) }
		c.clonedActions = linkedSetOf()

		physicsBody?.let { c.physicsBody = it.clone() }
		userData?.let { c.userData = it.clone() as HashMap<String, Any> }

		c.isClone = false
		return c
	}

	override fun toString(): String {
		return "$TAG@${hashCode()} ($name | ${actualChildren.size})"
	}

	internal open fun dispose(tag: String) {}

	/**
	Sets both the x & y scale

	@param scale the uniform scale to set.
	 */
	fun setScale(scale: Float) {
		xScale = scale
		yScale = scale
	}

	private fun getAbsScale(parentComputed: Boolean = true): Pair<Float, Float> {
//		if (parentComputed && parent != null && parent!!.scaleAbs == null)
//			throw Exception("!-  parent's scaleAbs is null: $name | ${parent!!.name}")
		val pAbsScale = if (parentComputed) parent?.scaleAbs else parent?.getAbsScale(parentComputed)
		val xS = xScale * (pAbsScale?.first ?: 1f)
		val yS = yScale * (pAbsScale?.second ?: 1f)
		if (xS != scaleAbs.first || yS != scaleAbs.second) {
			scaleIsValid = false
			scaleIsValid2 = false
			scaleAbs = xS to yS
		}
		return scaleAbs
	}

	private fun getAbsZRotation(parentComputed: Boolean = true): Float {
		zRotationAbs = ((if (parentComputed) parent?.zRotationAbs else parent?.getAbsZRotation(parentComputed)) ?: 0f) + zRotation
		return zRotationAbs
	}

	private fun getAbsPosition(parentComputed: Boolean = true): Point {
//		if (parentComputed && parent != null && parent!!.positionAbs == null)
//			throw Exception("!-  parent's positionAbs is null: $name | ${parent!!.name}")
		val pAbsPoint = if (parentComputed) parent?.positionAbs else parent?.getAbsPosition(parentComputed)
		val pAbsScale = if (parentComputed) parent?.scaleAbs else parent?.getAbsScale(parentComputed)
		val pAbsZRotationAbs = (if (parentComputed) parent?.zRotationAbs else parent?.getAbsZRotation(parentComputed)) ?: 0f

		val scaledPosX = position.x * (pAbsScale?.first ?: 1f)
		val scaledPosY = position.y * (pAbsScale?.second ?: 1f)

		positionAbs.x = (pAbsPoint?.x ?: 0f) + scaledPosX * cos(pAbsZRotationAbs) - scaledPosY * sin(pAbsZRotationAbs)
		positionAbs.y = (pAbsPoint?.y ?: 0f) + scaledPosX * sin(pAbsZRotationAbs) + scaledPosY * cos(pAbsZRotationAbs)
		return positionAbs
	}

	internal fun updateAbsPositionAll(tag: String) {
		getAbsPosition()
		children.forEach {
			it.updateAbsPositionAll(tag)
		}
	}

	internal fun getAbsProvDelta(parentComputed: Boolean = true): Pair<Float, Float> {
		val pAbsDelta = parent?.let {
			if (parentComputed) it.provisionalDeltaXAbs to it.provisionalDeltaYAbs else it.getAbsProvDelta(parentComputed)
		}
		val pAbsZRotationAbs = (if (parentComputed) parent?.zRotationAbs else parent?.getAbsZRotation(parentComputed)) ?: 0f
		val pAbsScale = if (parentComputed) parent?.scaleAbs else parent?.getAbsScale(parentComputed)

		val scaledDX = provisionalDeltaX * (pAbsScale?.first ?: 1f)
		val scaledDY = provisionalDeltaY * (pAbsScale?.second ?: 1f)

		provisionalDeltaXAbs = (pAbsDelta?.first ?: 0f) + scaledDX * cos(pAbsZRotationAbs) - scaledDY * sin(pAbsZRotationAbs)
		provisionalDeltaYAbs = (pAbsDelta?.second ?: 0f) + scaledDX * sin(pAbsZRotationAbs) + scaledDY * cos(pAbsZRotationAbs)

		return provisionalDeltaXAbs to provisionalDeltaYAbs
	}

	private fun getAbsZPosition(parentComputed: Boolean = true): Float {
//		if (parent != null && parent!!.zPositionAbs == null)
//			throw Exception("!-  parent's zPositionAbs is null: $name | ${parent!!.name}")
		zPositionAbs = ((if (parentComputed) parent?.zPositionAbs else parent?.getAbsZPosition(parentComputed)) ?: 0f) + zPosition
		return zPositionAbs
	}

	private fun getAbsVisibility(parentComputed: Boolean = true): Boolean {
		isHiddenAbs = isHidden || ((if (parentComputed) parent?.isHiddenAbs else parent?.getAbsVisibility(parentComputed)) ?: false)
		return isHiddenAbs!!
	}

	private fun getAbsAlpha(parentComputed: Boolean = true): Float {
//		if (parentComputed && parent != null && parent!!.alphaAbs == null)
//			throw Exception("!-  parent's alphaAbs is null: $name | ${parent!!.name}")
		val _alphaAbs = alpha * ((if (parentComputed) parent?.alphaAbs else parent?.getAbsAlpha(parentComputed)) ?: 1f)
		if (_alphaAbs != alphaAbs) {
			alphaIsValid = false
			glColorIsValid = false
			alphaAbs = _alphaAbs
		}
		return alphaAbs
	}

	private fun getAbsSpeed(parentComputed: Boolean = true): Float {
//		if (parent != null && parent!!.speedAbs == null)
//			throw Exception("!-  parent's speedAbs is null: $name | ${parent!!.name}")
		speedAbs = speed * ((if (parentComputed) parent?.speedAbs else parent?.getAbsSpeed(parentComputed)) ?: 1f)
		return speedAbs
	}

	@Synchronized
	private fun getTouchableNodesAndPhysicsBodies(add: Boolean): Pair<Set<SKNode>, Set<SKPhysicsBody>> {
		val nodes: MutableSet<SKNode> = hashSetOf()
		val bodies: MutableSet<SKPhysicsBody> = hashSetOf()
		if (isUserInteractionEnabled) nodes.add(this)
		physicsBody?.let { bodies.add(it) }
		clonedChildren2.clear()
		synchronized(actualChildren) {
			clonedChildren2.addAll(if (add) children else actualChildren)
		}
		clonedChildren2.forEach {
			val x = it.getTouchableNodesAndPhysicsBodies(add)
			nodes.addAll(x.first)
			bodies.addAll(x.second)
		}
		return nodes to bodies
	}

	fun addChild(node: SKNode) {
		// node.toBeRemoved = false

		synchronized(actualChildren) {
			actualChildren.add(node) // TODO if (!children.contains(node))
		}

		if (node.name.isNullOrEmpty().not())
			namedChildren.getOrPut(node.name!!) { mutableSetOf() }.add(node)

		// set parent after added
		node.parent = this

		val x = node.getTouchableNodesAndPhysicsBodies(true)
		(if (this is SKScene) this else scene)?.let {
			it.touchListeners.addAll(x.first)
			it.physicsWorld.addAll(x.second)
		}
	}

	/** If more than one child share the same name, the first node discovered is returned. */
	fun childNode(withName: String): SKNode? {
		return namedChildren[withName]?.single { it.parent != null }
	}

	open fun removeFromParent(tag: String? = null) {
		// toBeRemoved = true
		parent = null
		// parent?.removeChild("$tag|removeFromParent", this)

		// removeAllChildren("removeFromParent") // cannot removeAllChildren here: the children might be used/retained elsewhere
	}

	//@Deprecated("")
	//private fun removeChild(tag: String, node: SKNode) {
	//	synchronized(actualChildren) {
	//		// node.dispose("$tag|removeChild") // cannot call dispose here: the node might be retained elsewhere
	//		actualChildren.remove(node) // call remove() after dispose() because `node` can be finalized right after remove()
	//		node.parent = null
	//
	//		val x = node.getTouchableNodesAndPhysicsBodies(false)
	//		(if (this is SKScene) this else scene)?.let {
	//			it.touchListeners.removeAll(x.first)
	//			it.physicsWorld.bodies.removeAll(x.second)
	//		}
	//	}
	//}

	fun removeAllChildren(tag: String? = null) {
		synchronized(actualChildren) {
			val itr = actualChildren.iterator()
			while (itr.hasNext()) {
				val it = itr.next()
				// it.dispose("$tag|removeAllChildren")   // cannot call dispose here: the node might be retained elsewhere
				// itr.remove()  // call remove() after dispose() because it can be finalized right after remove
				// why toBeRemoved? : ConcurrentModification error when a button pressed clear a scene's children
				// it.toBeRemoved = true
				it.parent = null
				// it.removeAllChildren(tag)
			}
		}
		synchronized(namedChildren) {
			namedChildren.clear()
		}

		// (if (this is SKScene) this else scene)?.touchListeners?.removeAll(getTouchableNodes().toSet())
	}

	fun run(action: SKAction, completion: (() -> Unit)? = null) {
		this.actions.add(action.clone().apply { this.completion = completion })
	}

	fun run(action: SKAction, withKey: String) {
		removeAction(withKey)
		this.actions.add(action.clone().apply { name = withKey })
	}

	fun hasActions(): Boolean {
		return actions.isNotEmpty()
	}

	fun action(forKey: String): SKAction? {
		return this.actions.firstOrNull { it.name == forKey }
	}

	fun removeAllActions(tag: String? = null) {
		this.actions.clear()
	}

	fun removeAction(forKey: String) {
		this.actions.removeAll { it.name == forKey }
	}

	internal fun update(
		timeToRun: Long,
		drawLayers: TreeMap<Float, ArrayList<IDrawable>>, sp: SoundPool,
		physicsBodies: MutableSet<SKPhysicsBody>,
		runnables: ArrayList<(() -> Unit)>
	) {
		provisionalDeltaX = 0f
		provisionalDeltaY = 0f

		if (getAbsSpeed() != 0f) {
			// if (name == "tray-frame") Log.i(TAG, "--  update Node: $name | $action | $timeToRun")
			clonedActions.clear()
			synchronized(actions) {
				clonedActions.addAll(actions)
			}
			clonedActions.forEach {
				recurredUpdate(it, false, timeToRun, 1f, runnables)
				if (it.isFinished) actions.remove(it)
			}
		}

		// update before children
		// get zRotation before Position
		getAbsScale()
		getAbsZRotation()
		getAbsPosition()
		getAbsProvDelta()
		val zPosition = getAbsZPosition()
		getAbsVisibility()
		getAbsAlpha()

		if (parent != null) {
			if (this is IDrawable)
				drawLayers.getOrPut(zPosition) { arrayListOf() }.add(this)
			if (this is SKAudioNode)
				load(sp)
			physicsBody?.let { physicsBodies.add(it) }
		}

		clonedChildren.clear()
		synchronized(actualChildren) {
			clonedChildren.addAll(actualChildren)
		}
		for (el in clonedChildren) {
			if (el.parent != null)
				el.update(timeToRun, drawLayers, sp, physicsBodies, runnables)
			if (el.parent == null) {
				// it.dispose("update-toRemoved")
				synchronized(actualChildren) {
					actualChildren.remove(el)
				}
				el.name?.let { namedChildren.remove(it) }
				el.parent = null

				val x = el.getTouchableNodesAndPhysicsBodies(false)
				(if (this is SKScene) this else scene)?.let {
					it.touchListeners.removeAll(x.first)
					it.physicsWorld.removeAll(x.second)
				}
				x.second.forEach { it.removeFromWorld() }
			}
		}
	}

	private fun recurredUpdate(
		action: SKAction, isRepeated: Boolean, timeToRun: Long, parentSpeed: Float, runnables: ArrayList<(() -> Unit)>
	): Long {
		// if (this is BaseButtonNode) Log.d(TAG, "--  recurredUpdate: $name | $action | $isRepeated | $timeToRun")
		if (action.isFinished && !isRepeated) {
			Log.w(TAG, "--  recurredUpdate: action is already finished: $name | $action")
			return timeToRun
		}

		val actionAbsSpeed = action.speed * parentSpeed
		if (actionAbsSpeed == 0f) return timeToRun

		var timeLeft = timeToRun

		when (action.type) {
			SKAction.Type.REPEAT -> {
				if (action.repeatCount!! <= -1 && action.repeatedAction!!.getActionTimeToRun(true) == 0L)
					throw Exception("!-  repeat of 0-length action: $name | $action")

				while (timeLeft > 0 && (action.repeatCount!! <= -1 || action.repeatCount!! >= 1)) {
					timeLeft = recurredUpdate(action.repeatedAction!!, true, timeLeft, actionAbsSpeed, runnables)

					if (timeLeft > 0) action.repeatCount = action.repeatCount!! - 1
				}
				action.isFinished = action.repeatCount == 0 && action.repeatedAction!!.isFinished
			}
			SKAction.Type.SEQUENCE -> {
				val seq = if (isRepeated) action.childrenInRun!! else action.children!!
				val itr = seq.iterator()
				while (timeLeft > 0 && itr.hasNext()) {
					val a = itr.next()

					timeLeft = recurredUpdate(a, isRepeated, timeLeft, actionAbsSpeed, runnables)

					if (a.isFinished) { // && !isRepeated && (a.type != SKAction.Type.REPEAT)) {
						itr.remove()
					}
				}
				action.isFinished = timeLeft > 0 && seq.isEmpty()
			}
			SKAction.Type.GROUP -> {
				val gr = if (isRepeated) action.childrenInRun!! else action.children!!
				val itr = gr.iterator()
				var tLeft = timeLeft
				while (timeLeft > 0 && itr.hasNext()) {
					val a = itr.next()

					tLeft = min(tLeft, recurredUpdate(a, isRepeated, timeLeft, actionAbsSpeed, runnables))

					if (a.isFinished) {
						itr.remove()
					}
				}
				timeLeft = tLeft
				action.isFinished = timeLeft > 0 && gr.isEmpty()
			}
			else -> {
				val frameTime = (timeLeft * actionAbsSpeed).toLong()
				val actionTimeToRun = action.getActionTimeToRun(false)
				action.newTime = min((action.lastTime ?: 0) + frameTime, actionTimeToRun)
				timeLeft = (action.lastTime ?: 0) + frameTime - actionTimeToRun // max(0, ...)

				performAction(action)

				if (action.newTime == actionTimeToRun) { // ~ timeLeft >= 0
					action.lastTime = 0
					action.isFinished = action.successfulExeCount == 0
				} else
					action.lastTime = action.newTime
			}
		}

		if (action.isFinished && action.completion != null)
			runnables.add(action.completion!!)

		return timeLeft
	}

	private fun performAction(action: SKAction) {
		// Log.i(TAG, "--  performAction: $name | $action")

		when (action.type) {
			SKAction.Type.DELAY -> {}
			SKAction.Type.RUN_BLOCK -> {
				action.block!!.invoke()
			}
			SKAction.Type.REMOVE -> {
				// toBeRemoved = true
				parent = null
				// Log.i(TAG, "--  REMOVE: $name | $position")
			}
			SKAction.Type.HIDE -> {
				isHidden = true
			}
			SKAction.Type.UNHIDE -> {
				isHidden = false
			}
			SKAction.Type.MOVE_BY -> {
				val timePercent = speed *
						if (action.duration != 0.0) (action.newTime!! - (action.lastTime ?: 0))
							.div(action.duration!! * 1000).toFloat()
						else 1f
				if (physicsBody == null) {
					action.deltaX?.let { position.x += it * timePercent }
					action.deltaY?.let { position.y += it * timePercent }
				} else {
					action.deltaX?.let { provisionalDeltaX += it * timePercent }
					action.deltaY?.let { provisionalDeltaY += it * timePercent }
				}
				// Log.d(TAG, "--  MOVE: $name | ${position.y} | $timePercent")
			}
			SKAction.Type.MOVE_TO -> {
				val timeLeft = action.duration!! * 1000 - (action.lastTime ?: 0)
				val timePercent =
					if (timeLeft != 0.0) (action.newTime!! - (action.lastTime ?: 0)).div(timeLeft).toFloat()
					else 1f
				if (physicsBody == null) {
					action.toX?.let { position.x += (it - position.x) * timePercent }
					action.toY?.let { position.y += (it - position.y) * timePercent }
				} else {
					action.toX?.let { provisionalDeltaX += (it - position.x) * timePercent }
					action.toY?.let { provisionalDeltaY += (it - position.y) * timePercent }
				}
				// Log.d(TAG, "--  MOVE_TO: $name | ${position.y} | $timePercent")
			}
			SKAction.Type.ROTATE_BY -> {
				val timePercent = speed *
						if (action.duration != 0.0) (action.newTime!! - (action.lastTime ?: 0))
							.div(action.duration!! * 1000).toFloat()
						else 1f
				zRotation += action.angle!! * timePercent
				// Log.d(TAG, "--  ROTATE_BY: $name | $zRotation | $timePercent")
			}
			SKAction.Type.ROTATE_TO -> {
				val timeLeft = action.duration!! * 1000 - (action.lastTime ?: 0)
				val timePercent =
					if (timeLeft != 0.0) (action.newTime!! - (action.lastTime ?: 0)).div(timeLeft).toFloat()
					else 1f
				zRotation += (action.angle!! - zRotation) * timePercent
				// Log.d(TAG, "--  ROTATE_TO: $name | $zRotation | $timePercent")
			}
			SKAction.Type.FADE_TO -> {
				val timeLeft = action.duration!! * 1000 - (action.lastTime ?: 0)
				val timePercent =
					if (timeLeft != 0.0) (action.newTime!! - (action.lastTime ?: 0)).div(timeLeft).toFloat()
					else 1f
				alpha += (action.alpha!! - alpha) * timePercent
				// if (name == "") Log.d(TAG, "--  FADE_TO: $name | to: ${action.alpha} | fr: ${action.fromAlpha} | dur: ${action.duration} | %: $timePercent | $alpha")
			}
			SKAction.Type.SCALE_TO -> {
				if (action.fromScaleX == null) action.fromScaleX = xScale
				if (action.fromScaleY == null) action.fromScaleY = yScale
				val timePercent =
					if (action.duration != 0.0) action.newTime!!.div(action.duration!! * 1000).toFloat()
					else 1f
				xScale = action.fromScaleX!! + (action.scaleX!! - action.fromScaleX!!) * timePercent
				yScale = action.fromScaleY!! + (action.scaleY!! - action.fromScaleY!!) * timePercent
				// Log.d(TAG, "--  SCALE_TO: $name | $scaleX x $scaleY")
			}
			SKAction.Type.SET_TEXTURE -> {
				if (this is SKSpriteNode)
					texture = action.texture
			}
			SKAction.Type.COLORIZE -> {
			}
			SKAction.Type.PLAY -> {
				if (this is SKAudioNode) {
					if (play()) action.successfulExeCount = 0 else action.successfulExeCount--
					if (action.successfulExeCount <= -20) action.successfulExeCount = 0
				} else throw Error("!-  Action ${action.type} is not supported for ${this::class.simpleName}")
			}
			else -> {
				throw Error("!-  Action ${action.type} is not supported")
			}
		}
	}

	// input events
	internal fun superOnTouchEvent(touches: MutableSet<UITouch>, event: MotionEvent): Boolean {
		return super.onTouchEvent(touches, event)
	}

	override fun onTouchEvent(touches: MutableSet<UITouch>, event: MotionEvent): Boolean {
		if (!isUserInteractionEnabled || isHidden) return false
		val contain = touches.any { absFrame.contains(it.nodeX, it.nodeY) }
		// if (this::class.simpleName == "ButtonNode")
		//    Log.d(TAG, "--  onTouchEvent: ${this::class.simpleName} | $name | $isUserInteractionEnabled | $contain | $absFrame2 | $touches | $event")

//		clonedChildren2.clear()
//		synchronized(trueChildren) {
//			clonedChildren2.addAll(children)
//		}
//		clonedChildren2.forEach {
//			if (!it.onTouchEvent(touches, event))
//				return false
//		}

		if (!contain) {
			if (isTouched) {
				if (arrayOf(
						MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP,
						MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_OUTSIDE
					).contains(event.action)
				) {
					isTouched = false
					touchesCancelled(touches, event)
				}
				return true
			}
			return false
		} else {
			if (!isTouched && arrayOf(
					MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP,
					MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_OUTSIDE
				).contains(event.action)
			)
				return false
		}

		return super.onTouchEvent(touches, event)
	}

	@CallSuper
	final override fun onKeyEvent(keyCode: Int, event: KeyEvent): Boolean {
		Log.d(TAG, "--  onKeyEvent: $name | $keyCode | $event")

//		clonedChildren3.clear()
//		synchronized(trueChildren) {
//			clonedChildren3.addAll(children)
//		}
//		clonedChildren3.forEach {
//			if (!it.onKeyEvent(keyCode, event))
//				return false
//		}
		if (!isUserInteractionEnabled) return true
		return super.onKeyEvent(keyCode, event)
	}

	// draw node to Canvas
	internal fun texture(textureManager: SKTextureManager, forced: Boolean): Bitmap {
		val p = parent
		parent = null
		val origin = position
		position = Point.zero

		if (drawLayers == null)
			drawLayers = TreeMap { o1, o2 ->
				return@TreeMap if (o1 >= o2) 1 else -1
			}
		else drawLayers!!.clear()

		update(drawLayers!!)

		loadToManagers(textureManager, forced)

		val bitmap = Bitmap.createBitmap(
			(frame.width + 2 * ((this as? SKDrawableObject)?.shadowRadius ?: 0f)).toInt(),
			(frame.height + 2 * ((this as? SKDrawableObject)?.shadowRadius ?: 0f)).toInt(),
			Bitmap.Config.ARGB_8888
		)

		draw(Canvas(bitmap))

		parent = p
		position = origin

		return bitmap
	}

	protected fun update(drawLayers: TreeMap<Float, ArrayList<IDrawable>>) {
		// get zRotation before Position
		getAbsScale()
		getAbsZRotation()
		getAbsPosition()
		val zPosition = getAbsZPosition()
		getAbsVisibility()
		getAbsAlpha()

		if (this is IDrawable)
			drawLayers.getOrPut(zPosition) { arrayListOf() }.add(this)

		clonedChildren.clear()
		synchronized(actualChildren) {
			clonedChildren.addAll(children)
		}
		for (el in clonedChildren) {
			el.update(drawLayers)
		}
	}

	@CallSuper
	internal fun loadToManagers(textureManager: SKTextureManager, forced: Boolean = false) {
		synchronized(drawLayers!!) {
			for (layer in drawLayers!!) {
				for (n in layer.value) {
					if (n is SKSpriteNode)
						n.load(textureManager, 1)
					if (n is SKLabelNode && forced)
						n.fontIsValid = false
				}
			}
		}
	}

	internal open fun draw(canvas: Canvas) {
		// need to drawSelf here because parent is null
		(this as? IDrawableObject)?.doDraw(canvas, 1f, 1f, true)

		for (layer in drawLayers!!) {
			for (n in layer.value) {
				n.drawSelf(canvas, 1f, 1f, true)
			}
		}
	}

	@CallSuper
	internal open fun loadToManagers(
		vertexBufferObjectManager: VertexBufferObjectManager,
		textureManager: SKTextureManager, fontManager: SKFontManager
	) {
		synchronized(drawLayers!!) {
			for (layer in drawLayers!!) {
				for (n in layer.value) {
					if (n is SKDrawableObject)
						n.attachVertexBufferObject(vertexBufferObjectManager)
					if (n is SKSpriteNode)
						n.load(textureManager, 2)
					if (n is SKLabelNode)
						n.toLoad(fontManager)
				}
			}
		}
	}
}