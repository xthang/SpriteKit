package x.spritekit

import x.spritekit.TimeInterval
import kotlin.reflect.KClass

open class GKState {
	var stateMachine: GKStateMachine? = null

	open fun isValidNextState(stateClass: KClass<out GKState>): Boolean {
		return true
	}

	open fun didEnterFrom(previousState: GKState?) {}
	open fun update(seconds: TimeInterval) {}
	open fun willExitTo(nextState: GKState) {}
}