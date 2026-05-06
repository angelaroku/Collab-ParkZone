package com.example.parkingzonemadrid.data.repository

import android.content.Context
import com.example.parkingzonemadrid.data.local.AppDatabase
import com.example.parkingzonemadrid.data.local.entity.UsuarioEntity
import com.example.parkingzonemadrid.model.Usuario

class ParkingLocalRepository(
    context: Context
) {
    private val database = AppDatabase.getInstance(context)

    suspend fun upsertUser(user: Usuario) {
        database.usuarioDao().insertarOActualizar(
            UsuarioEntity(
                correo = user.correo,
                nom_usuario = user.nom_usuario,
                password = user.password
            )
        )
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
