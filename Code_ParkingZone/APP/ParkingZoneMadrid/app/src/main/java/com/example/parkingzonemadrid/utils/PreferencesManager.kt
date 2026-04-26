package com.example.parkingzonemadrid.utils

import android.content.Context
import com.example.parkingzonemadrid.model.Usuario

class PreferencesManager(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveUser(user: Usuario) {
        prefs.edit()
            .putInt(KEY_USER_ID, user.id_usuario)
            .putString(KEY_NAME, user.nom_usuario)
            .putString(KEY_EMAIL, user.correo)
            .apply()
    }

    fun getUser(): Usuario? {
        val userId = prefs.getInt(KEY_USER_ID, -1)
        val name = prefs.getString(KEY_NAME, null) ?: return null
        val email = prefs.getString(KEY_EMAIL, null) ?: return null
        return Usuario(
            id_usuario = if (userId > 0) userId else Usuario.generarId(),
            nom_usuario = name,
            correo = email,
            password = "",
            favoritos = mutableListOf()
        )
    }

    fun clearUser() {
        prefs.edit()
            .remove(KEY_USER_ID)
            .remove(KEY_NAME)
            .remove(KEY_EMAIL)
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "parking_zone_prefs"
        const val KEY_USER_ID = "user_id"
        const val KEY_NAME = "user_name"
        const val KEY_EMAIL = "user_email"
    }
}
