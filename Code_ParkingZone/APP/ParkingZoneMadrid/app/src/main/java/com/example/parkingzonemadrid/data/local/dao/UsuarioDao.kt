package com.example.parkingzonemadrid.data.local.dao

import androidx.room.*
import com.example.parkingzonemadrid.data.local.entity.UsuarioEntity

@Dao
interface UsuarioDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(usuario: UsuarioEntity)

    @Query("SELECT * FROM usuarios WHERE id_usuario = :id")
    suspend fun obtenerPorId(id: Int): UsuarioEntity?

    @Query("DELETE FROM usuarios WHERE id_usuario = :id")
    suspend fun borrarPorId(id: Int)

    @Update
    suspend fun actualizar(usuario: UsuarioEntity)
}