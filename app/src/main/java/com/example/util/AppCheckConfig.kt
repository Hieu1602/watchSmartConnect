package com.example.util

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory

private const val TAG = "AppCheckInit"

private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
private const val RECAPTCHA_SITE_KEY_RES = "recaptcha_site_key"

// UUID pattern: 8-4-4-4-12 hex chars
private val UUID_REGEX = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

/**
 * Configures Firebase App Check based on build variant and available tokens/keys.
 * Seamlessly handles Intent extras, onNewIntent, and falls back to provisioned reCAPTCHA site key.
 */
fun configureAppCheck(context: Context, intent: Intent?) {
    try {
        // 1. Ensure FirebaseApp is initialized
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }

        if (BuildConfig.DEBUG) {
            // 2. Debug: Read debug token from Intent extra
            val debugToken = extractDebugToken(intent)

            if (!debugToken.isNullOrEmpty()) {
                val persistenceKey = FirebaseApp.getInstance().persistenceKey
                val storeNames = listOf(
                    "$DEBUG_STORE_PREFIX$persistenceKey",
                    "${DEBUG_STORE_PREFIX}+[DEFAULT]+"
                )
                for (storeName in storeNames) {
                    val prefs = context.getSharedPreferences(storeName, Context.MODE_PRIVATE)
                    prefs.edit()
                        .putString(DEBUG_SECRET_KEY, debugToken)
                        .commit()
                }
                Log.d(TAG, "App Check debug secret configured successfully.")
            } else {
                Log.w(TAG, "No debug token found in Intent extra.")
            }

            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
            Log.d(TAG, "DebugAppCheckProviderFactory installed.")
        } else {
            // 3. Release: Look up recaptcha_site_key safely without throwing Resources.NotFoundException
            val resId = context.resources.getIdentifier(RECAPTCHA_SITE_KEY_RES, "string", context.packageName)
            if (resId != 0) {
                val siteKey = context.getString(resId)
                if (siteKey.isNotBlank()) {
                    FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                        RecaptchaAppCheckProviderFactory.getInstance(siteKey)
                    )
                    Log.d(TAG, "RecaptchaAppCheckProviderFactory installed.")
                } else {
                    Log.e(TAG, "recaptcha_site_key resource string is blank.")
                }
            } else {
                Log.e(TAG, "recaptcha_site_key string resource not found in package.")
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to initialize Firebase App Check", e)
    }
}

private fun extractDebugToken(intent: Intent?): String? {
    if (intent == null) return null

    val direct = intent.getStringExtra(DEBUG_TOKEN_EXTRA)
    if (!direct.isNullOrBlank()) return direct

    val extras = intent.extras ?: return null

    val variations = listOf(
        DEBUG_TOKEN_EXTRA,
        "debug_token",
        "DEBUG_TOKEN",
        "firebase_appcheck_debug_token",
        "app_check_debug_token"
    )
    for (key in variations) {
        val value = extras.getString(key)
        if (!value.isNullOrBlank()) return value
    }

    for (key in extras.keySet()) {
        val value = extras.getString(key)
        if (value != null && UUID_REGEX.matches(value.trim())) {
            Log.d(TAG, "Extracted debug token from extra '$key'")
            return value.trim()
        }
    }

    val sysProp = System.getProperty(DEBUG_TOKEN_EXTRA) ?: System.getenv(DEBUG_TOKEN_EXTRA)
    if (!sysProp.isNullOrBlank()) return sysProp

    return null
}
