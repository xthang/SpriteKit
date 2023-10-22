package x.spritekit

import android.app.Application
import android.util.Log

class AppConfig private constructor() {

	companion object {
		private const val TAG = "⚒SK︎"

		lateinit var fileProviderAuthority: String

		fun init(context: Application) {
			Log.i(TAG, "-------")

			fileProviderAuthority = context.getString(x.lib.spritekit.R.string.file_provider_authority)
		}
	}
}