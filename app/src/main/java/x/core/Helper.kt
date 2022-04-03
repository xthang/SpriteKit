package x.core

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ConfigurationInfo
import android.content.pm.FeatureInfo


class Helper {

	class GL {
		companion object {
			fun getVersionFromActivityManager(context: Context): Int {
				val activityManager =
					context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
				val configInfo: ConfigurationInfo = activityManager.deviceConfigurationInfo
				return if (configInfo.reqGlEsVersion != ConfigurationInfo.GL_ES_VERSION_UNDEFINED) {
					configInfo.reqGlEsVersion
				} else {
					1 shl 16 // Lack of property means OpenGL ES version 1
				}
			}

			fun getVersionFromPackageManager(context: Context): Int {
				val packageManager = context.packageManager
				val featureInfos = packageManager.systemAvailableFeatures
				if (featureInfos.isNotEmpty()) {
					for (featureInfo in featureInfos) {
						// Null feature name means this feature is the open gl es version feature.
						if (featureInfo.name == null) {
							return if (featureInfo.reqGlEsVersion != FeatureInfo.GL_ES_VERSION_UNDEFINED) {
								featureInfo.reqGlEsVersion
							} else {
								1 shl 16 // Lack of property means OpenGL ES version 1
							}
						}
					}
				}
				return 1
			}

			/** @see FeatureInfo.getGlEsVersion
			 */
			fun getMajorVersion(glEsVersion: Int): Int {
				return glEsVersion and -0x10000 shr 16
			}

			/** @see FeatureInfo.getGlEsVersion
			 */
			fun getMinorVersion(glEsVersion: Int): Int {
				return glEsVersion and 0xffff
			}
		}
	}
}