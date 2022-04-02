package x.spritekit

import android.util.Log

class XEngine(
	private val runnable: (secondsPassed: Double, deltaTime: Long) -> Unit
) {

	companion object {
		private val TAG = XEngine::class.simpleName

		private const val MAX_FRAME_TIME = (1000.0 / 60.0).toInt()
	}

	internal var valid = false

	@Volatile
	private var isRunning: Boolean = false
	private var isPaused: Boolean = false
	internal var updateThread: Thread? = null


	internal fun startThread(tag: String) {
		if (!valid) {
			Log.i(TAG, "--  startThread [$tag]: renderer is not created..")
			return
		}
		if (updateThread != null) {
			Log.i(TAG, "--  startThread [$tag]: thread is still active..")
			return
		}

		isRunning = true

		var millisecondsPassed = 0L
		updateThread = Thread {
			Log.i(TAG, "--  startThread [$tag]: ${Thread.currentThread().name} | ${hashCode()}")

			var frameStartTime = System.nanoTime()
			while (isRunning) {
				val deltaTime = (System.nanoTime() - frameStartTime) / 1000000
				frameStartTime = System.nanoTime()
				millisecondsPassed += deltaTime

				if (!isPaused) runnable(millisecondsPassed * 1e-3, deltaTime)

				// calculate the time required to draw the frame in ms
				val frameTime = System.nanoTime() - frameStartTime
//                Log.d(TAG, "~~~   Thread: ${Thread.currentThread().name} | frameTime: $frameTime")
				if (frameTime / 1000000 < MAX_FRAME_TIME) {
					try {
						Thread.sleep(MAX_FRAME_TIME - frameTime / 1000000)
					} catch (e: InterruptedException) {
						Log.e(TAG, "!!  ", e)
					}
				}
			}
			Log.i(TAG, "--  Thread: stopped")
		}
		updateThread!!.start()
	}

	internal fun pause(tag: String) {
		Log.i(TAG, "--  pause [$tag]")

		isPaused = true
	}

	internal fun resume(tag: String) {
		Log.i(TAG, "--  resume [$tag]")

		isPaused = false
	}

	internal fun notifyStop(tag: String) {
		if (isRunning) isRunning = false
		else Log.w(TAG, "!-  notifyStop [$tag]: isRunning: $isRunning")
	}

	internal fun stop(tag: String) {
		Log.i(TAG, "--  stop [$tag]: stopping thread ...")

		stopThread("stop|$tag")
	}

	private fun stopThread(tag: String) {
		if (!isRunning && updateThread == null) {
			Log.w(TAG, "--  stopThread [$tag]: thread is already stopped")
			return
		}
		isRunning = false
		try {
			updateThread?.join()
		} catch (e: Exception) {
			Log.e(TAG, "!-  stopThread [$tag]: Could not join with draw thread", e)
			Log.w(TAG, "--  stopThread [$tag]: Trying to manually interrupt UpdateThread.")
			updateThread?.interrupt()
		} finally {
			Log.d(TAG, "--  stopThread [$tag]: Request last frame done")
		}
		updateThread = null
	}
}