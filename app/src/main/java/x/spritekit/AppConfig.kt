package x.spritekit

import android.app.Application
import android.util.Log
import x.spritekit.R

class AppConfig private constructor() {

	companion object {
		private const val TAG = "⚒︎"

		var fileProviderAuthority: String = ""

		fun init(context: Application) {
			Log.i(TAG, "-------")

			fileProviderAuthority = context.getString(R.string.file_provider_authority)
		}
	}
}