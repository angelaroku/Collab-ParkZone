package com.example.parkingzonemadrid.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.parkingzonemadrid.data.local.entity.ZonaEntity
@Dao
interface ZonaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodas(zonas: List<ZonaEntity>)

    @Query("SELECT * FROM zonas")
    suspend fun obtenerTodas(): List<ZonaEntity>

    @Query("DELETE FROM zonas")
    suspend fun borrarTodo()

    @Query("SELECT COUNT(*) FROM zonas")
    suspend fun contarZonas(): Int
}