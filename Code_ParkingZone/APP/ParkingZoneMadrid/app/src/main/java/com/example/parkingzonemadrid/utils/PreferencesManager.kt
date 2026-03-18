package com.example.parkingzonemadrid.utils

import android.content.Context
import com.example.parkingzonemadrid.model.User

class PreferencesManager(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveUser(user: User) {
        prefs.edit()
            .putString(KEY_NAME, user.name)
            .putString(KEY_EMAIL, user.email)
            .apply()
    }

    fun getUser(): User? {
        val name = prefs.getString(KEY_NAME, null) ?: return null
        val email = prefs.getString(KEY_EMAIL, null) ?: return null
        return User(name = name, email = email)
    }

    fun clearUser() {
        prefs.edit().remove(KEY_NAME).remove(KEY_EMAIL).apply()
    }

    private companion object {
        const val PREFS_NAME = "parking_zone_prefs"
        const val KEY_NAME = "user_name"
        const val KEY_EMAIL = "user_email"
    }
}
