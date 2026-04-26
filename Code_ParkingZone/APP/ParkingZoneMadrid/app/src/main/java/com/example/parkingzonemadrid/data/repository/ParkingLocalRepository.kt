package com.example.parkingzonemadrid.data.repository

import android.content.Context
import com.example.parkingzonemadrid.data.local.AppDatabase
import com.example.parkingzonemadrid.data.local.entity.FavoriteEntity
import com.example.parkingzonemadrid.data.local.entity.UserEntity
import com.example.parkingzonemadrid.model.Usuario

class ParkingLocalRepository(
    context: Context
) {
    private val database = AppDatabase.getInstance(context)

    suspend fun upsertUser(user: Usuario) {
        database.userDao().upsert(
            UserEntity(
                email = user.correo,
                name = user.nom_usuario
            )
        )
    }

    suspend fun getFavoriteZoneIds(userEmail: String): Set<Int> {
        return database.favoriteDao().getFavoriteZoneIds(userEmail).toSet()
    }

    suspend fun toggleFavorite(userEmail: String, zoneId: Int) {
        database.favoriteDao().toggleFavorite(userEmail, zoneId)
    }

    /**
     * Para usar cuando más adelante tengáis polígonos en el mapa.
     */
    suspend fun isFavorite(userEmail: String, zoneId: Int): Boolean {
        return database.favoriteDao().isFavorite(userEmail, zoneId) > 0
    }

    fun getDatabaseForTesting(): AppDatabase = database
}

