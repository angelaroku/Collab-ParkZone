package com.example.parkingzonemadrid.data.repository

import android.content.Context
import com.example.parkingzonemadrid.data.local.AppDatabase
import com.example.parkingzonemadrid.data.local.entity.UsuarioEntity
import com.example.parkingzonemadrid.model.Usuario
import com.example.parkingzonemadrid.utils.PasswordHasher

class ParkingLocalRepository(
    context: Context
) {
    private val database = AppDatabase.getInstance(context)

    suspend fun upsertUser(user: Usuario) {
        database.usuarioDao().insertarOActualizar(
            UsuarioEntity(
                correo = user.correo.trim().lowercase(),
                nom_usuario = user.nom_usuario,
                password = user.password
            )
        )
    }

    suspend fun registerUser(nombre: String, correo: String, password: String): AuthResult {
        val email = correo.trim().lowercase()
        val existing = database.usuarioDao().obtenerPorCorreo(email)
        if (existing != null) {
            return AuthResult.Error("Ya existe una cuenta con este correo. Usa Iniciar sesión.")
        }
        val usuario = Usuario(
            id_usuario = Usuario.generarId(),
            nom_usuario = nombre.trim(),
            correo = email,
            password = PasswordHasher.hash(password, email),
            favoritos = mutableListOf()
        )
        upsertUser(usuario)
        return AuthResult.Success(usuario)
    }

    suspend fun signInUser(correo: String, password: String): AuthResult {
        val email = correo.trim().lowercase()
        val entity = database.usuarioDao().obtenerPorCorreo(email)
            ?: return AuthResult.Error("No hay cuenta con este correo. Crea una cuenta primero.")

        if (entity.password.isBlank()) {
            return AuthResult.Error(
                "Esta cuenta no tiene contraseña guardada. Regístrate de nuevo o contacta soporte."
            )
        }

        if (!PasswordHasher.verify(password, email, entity.password)) {
            return AuthResult.Error("Contraseña incorrecta. Vuelve a intentarlo.")
        }

        val usuario = Usuario(
            id_usuario = Usuario.generarId(),
            nom_usuario = entity.nom_usuario,
            correo = entity.correo,
            password = entity.password,
            favoritos = mutableListOf()
        )
        return AuthResult.Success(usuario)
    }

    suspend fun getFavoriteZoneIds(userEmail: String): Set<Int> {
        return database.favoritoZonaDao().obtenerIdsZonaFavoritos(userEmail).toSet()
    }

    suspend fun toggleFavorite(userEmail: String, zoneId: Int) {
        database.favoritoZonaDao().alternarFavorito(userEmail, zoneId)
    }

    suspend fun isFavorite(userEmail: String, zoneId: Int): Boolean {
        return database.favoritoZonaDao().contarFavorito(userEmail, zoneId) > 0
    }

    fun getDatabaseForTesting(): AppDatabase = database
}
