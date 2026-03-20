package com.example.parkingzonemadrid.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.parkingzonemadrid.data.local.entity.FavoriteEntity

@Dao
interface FavoriteDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorite_zones WHERE user_email = :userEmail AND zone_id = :zoneId")
    suspend fun removeFavorite(userEmail: String, zoneId: Int)

    @Query("SELECT zone_id FROM favorite_zones WHERE user_email = :userEmail ORDER BY zone_id")
    suspend fun getFavoriteZoneIds(userEmail: String): List<Int>

    @Query("SELECT COUNT(*) FROM favorite_zones WHERE user_email = :userEmail AND zone_id = :zoneId")
    suspend fun isFavorite(userEmail: String, zoneId: Int): Int

    @Transaction
    suspend fun toggleFavorite(userEmail: String, zoneId: Int) {
        val favoriteCount = isFavorite(userEmail, zoneId)
        if (favoriteCount > 0) {
            removeFavorite(userEmail, zoneId)
        } else {
            addFavorite(FavoriteEntity(userEmail = userEmail, zoneId = zoneId))
        }
    }
}

