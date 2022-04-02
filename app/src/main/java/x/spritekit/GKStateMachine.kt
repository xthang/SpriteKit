package x.spritekit

import kotlin.reflect.KClass

open class GKStateMachine(vararg states: GKState) {

	companion object {
		private const val TAG = "GKStateMachine"
	}

	private val states: ArrayList<GKState> = arrayListOf(*states)

	/**
	 * The current state that the state machine is in.
	 * Prior to the first called to enterState this is equal to nil.
	 */
	open var currentState: GKState? = null

	/**
	 * Updates the current state machine.
	 *
	 * @param sec the time, in seconds, since the last frame
	 */
	open fun update(deltaTime: Double) {
		currentState?.update(deltaTime)
	}

	/**
	 * Returns YES if the indicated class is a a valid next state or if currentState is nil
	 *
	 * @param stateClass the class of the state to be tested
	 */
	open fun canEnterState(stateClass: KClass<out GKState>): Boolean {
		return currentState == null || currentState!!.isValidNextState(stateClass)
	}

	/**
	 * Calls canEnterState to check if we can enter the given state and then enters that state if so.
	 * [GKState willExitWithNextState:] is called on the old current state.
	 * [GKState didEnterWithPreviousState:] is called on the new state.
	 *
	 * @param stateClass the class of the state to switch to
	 * @return YES if state was entered.  NO otherwise.
	 */
	open fun enter(stateClass: KClass<out GKState>): Boolean {
		if (!canEnterState(stateClass)) return false

		val current = currentState
		val nextState = states.single { it::class == stateClass }
		currentState?.willExitTo(nextState)
		currentState = nextState
		currentState!!.didEnterFrom(current)
		return true
	}
}