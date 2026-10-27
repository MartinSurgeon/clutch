package com.example.data.local

import android.content.Context
import android.content.SharedPreferences

/**
 * Small settings that are not farm records: sign-in, sync cursor, reminder time and
 * end-of-lay prices. Farm records stay in Room.
 */
class AppPrefs(context: Context, name: String = "clutch_prefs") {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(name, Context.MODE_PRIVATE)

    // --- Account & sync ---
    var authToken: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    var userName: String?
        get() = prefs.getString(KEY_USER_NAME, null)
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()

    var userPhone: String?
        get() = prefs.getString(KEY_USER_PHONE, null)
        set(value) = prefs.edit().putString(KEY_USER_PHONE, value).apply()

    var syncCursor: Long
        get() = prefs.getLong(KEY_SYNC_CURSOR, 0L)
        set(value) = prefs.edit().putLong(KEY_SYNC_CURSOR, value).apply()

    var lastSyncedAt: Long
        get() = prefs.getLong(KEY_LAST_SYNCED, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SYNCED, value).apply()

    /** Debug builds only: lets a tester point the app at a server on the same Wi-Fi. */
    var serverUrlOverride: String?
        get() = prefs.getString(KEY_SERVER_URL, null)?.takeIf { it.isNotBlank() }
        set(value) = prefs.edit().putString(KEY_SERVER_URL, value).apply()

    fun clearAccount() {
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_USER_NAME)
            .remove(KEY_USER_PHONE)
            .remove(KEY_SYNC_CURSOR)
            .remove(KEY_LAST_SYNCED)
            .apply()
    }

    // --- Daily reminder ---
    var reminderEnabled: Boolean
        get() = prefs.getBoolean(KEY_REMINDER_ON, false)
        set(value) = prefs.edit().putBoolean(KEY_REMINDER_ON, value).apply()

    /** Minutes after midnight; default 6:00 PM, after the last egg pick. */
    var reminderMinuteOfDay: Int
        get() = prefs.getInt(KEY_REMINDER_TIME, 18 * 60)
        set(value) = prefs.edit().putInt(KEY_REMINDER_TIME, value).apply()

    var reminderPromptDismissed: Boolean
        get() = prefs.getBoolean(KEY_REMINDER_PROMPT_DISMISSED, false)
        set(value) = prefs.edit().putBoolean(KEY_REMINDER_PROMPT_DISMISSED, value).apply()

    // --- End-of-lay prices (per pen) ---
    fun eolPrice(flockId: String, field: String): Double? =
        prefs.getString("eol_${flockId}_$field", null)?.toDoubleOrNull()

    fun setEolPrice(flockId: String, field: String, value: Double?) {
        prefs.edit().putString("eol_${flockId}_$field", value?.toString()).apply()
    }

    companion object {
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_PHONE = "user_phone"
        private const val KEY_SYNC_CURSOR = "sync_cursor"
        private const val KEY_LAST_SYNCED = "last_synced_at"
        private const val KEY_SERVER_URL = "server_url_override"
        private const val KEY_REMINDER_ON = "reminder_enabled"
        private const val KEY_REMINDER_TIME = "reminder_minute_of_day"
        private const val KEY_REMINDER_PROMPT_DISMISSED = "reminder_prompt_dismissed"

        const val EOL_EGG_PRICE = "egg_price_crate"
        const val EOL_FEED_PRICE = "feed_price_bag"
        const val EOL_HEN_PRICE = "spent_hen_price"
        const val EOL_OTHER_COST = "other_cost_bird_month"
    }
}
