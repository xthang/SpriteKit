package x.spritekit

import android.content.Context
import android.media.SoundPool
import androidx.annotation.RawRes
import java.lang.ref.WeakReference

class SKAudioNode(private val context: Context, @RawRes private val id: Int) : SKNode(), IPresentable {

	companion object {
		private val TAG = SKAudioNode::class.simpleName
	}

	private var poolID: Int? = null
	private var sp: WeakReference<SoundPool>? = null

	/**Specifies whether the node is to automatically play sound when added to a scene.
	 * If autoplaysLooped is NO, the node and its sound must be explicitly scheduled and played using
	 * the scene's engine.
	 *
	 * If YES, the node will automatically play sound when added to a scene.
	 *
	 * Defaults to YES.
	 * @see SKView.paused
	 */
	var autoplayLooped: Boolean = true

	/**Marks the audio source as positional so that the audio mix considers relative position and velocity
	 * with regards to the scene's current listener node.
	 *
	 * @see AVAudio3DMixing
	 * @see SKScene.listener
	 */
	var isPositional: Boolean = false

	var soundVol: Float = 1f

	internal fun load(sp: SoundPool) {
		if (sp != this.sp?.get()) {
			this.sp = WeakReference(sp)
			poolID = sp.load(context, id, 1)
		}
	}

	internal fun play(): Boolean {
		return sp?.get()?.let {
			it.play(poolID!!, soundVol, soundVol, 1, 0, 1f) != 0
		} ?: false
	}

	fun stop() {
		sp?.get()?.stop(poolID!!)
	}
}