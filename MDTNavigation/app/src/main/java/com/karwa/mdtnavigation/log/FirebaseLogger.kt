package com.karwa.mdtnavigation.log

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import com.google.firebase.analytics.FirebaseAnalytics

class FirebaseLogger private constructor(context: Context) {

    private var firebaseAnalytics: FirebaseAnalytics = FirebaseAnalytics.getInstance(context)

    init {
        firebaseAnalytics.setUserId(Settings.Secure.ANDROID_ID)
        firebaseAnalytics.setUserProperty("DEVICE", Build.DEVICE)
        firebaseAnalytics.setUserProperty("MODEL", Build.MODEL)
        firebaseAnalytics.setUserProperty("MANUFACTURER", Build.MANUFACTURER)
        firebaseAnalytics.setUserProperty("BRAND", Build.BRAND)
        firebaseAnalytics.setUserProperty("SDK_VERSION", Build.VERSION.SDK_INT.toString())

        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val appVersion = packageInfo.versionName
        firebaseAnalytics.setUserProperty("APP_VERSION", appVersion)
    }

    companion object {
        @Volatile
        private var INSTANCE: FirebaseLogger? = null

        fun getInstance(context: Context): FirebaseLogger {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirebaseLogger(context.applicationContext).also { INSTANCE = it }
            }
        }
    }


    fun logEvent(eventName: String, params: Bundle? = null) {
        firebaseAnalytics.logEvent(eventName, params)
    }


    fun logSelectContent(itemId: String, itemName: String, content: String) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.ITEM_ID, itemId)
            putString(FirebaseAnalytics.Param.ITEM_NAME, itemName)
            putString(FirebaseAnalytics.Param.CONTENT_TYPE, "text")
            putString(FirebaseAnalytics.Param.CONTENT, content)
        }
        logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)
    }
}