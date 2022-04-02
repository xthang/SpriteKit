package x.core.ui

import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.annotation.CallSuper

open class UIResponder {

	companion object {
		private const val TAG = "UIResponder"
	}

	open var next: UIResponder? = null

	internal var isTouched = false

	// Touch Event
	@CallSuper
	internal open fun onTouchEvent(touches: MutableSet<UITouch>, event: MotionEvent): Boolean {
//		if (this::class.simpleName == "ButtonNode")
//			Log.d(TAG, "--  onTouchEvent: ${this::class.simpleName} | $touches | $event")

		when (event.action) {
			MotionEvent.ACTION_DOWN,
			MotionEvent.ACTION_POINTER_DOWN -> {
				isTouched = true
				touchesBegan(touches, event)
			}
			MotionEvent.ACTION_MOVE -> {
				touchesMoved(touches, event)
			}
			MotionEvent.ACTION_UP,
			MotionEvent.ACTION_POINTER_UP -> {
				isTouched = false
				touchesEnded(touches, event)
			}
			MotionEvent.ACTION_CANCEL,
			MotionEvent.ACTION_OUTSIDE -> {
				isTouched = false
				touchesCancelled(touches, event)
			}
			else -> {
				Log.w(TAG, "--  onTouchEvent: wth is this action: ${event.action}")
			}
		}
		return true
	}

	open fun touchesBegan(touches: Set<UITouch>, event: MotionEvent) {}

	open fun touchesMoved(touches: Set<UITouch>, event: MotionEvent) {}

	open fun touchesEnded(touches: Set<UITouch>, event: MotionEvent) {}

	open fun touchesCancelled(touches: Set<UITouch>, event: MotionEvent) {}

	// Key Event
	internal open fun onKeyEvent(keyCode: Int, event: KeyEvent): Boolean {
		when (event.action) {
			KeyEvent.ACTION_DOWN -> {
				onKeyDown(keyCode, event)
			}
			KeyEvent.ACTION_UP -> {
				onKeyUp(keyCode, event)
			}
			KeyEvent.ACTION_MULTIPLE -> {
				Log.d(TAG, "--  onKeyEvent: ACTION_MULTIPLE [${event.action}]")
			}
			else -> {
				Log.w(TAG, "--  onKeyEvent: wth is this action: ${event.action}")
			}
		}
		return true
	}

	open fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
		return false
	}

	open fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
		return false
	}
}
