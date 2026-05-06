package com.example.parkingzonemadrid.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.parkingzonemadrid.data.local.entity.FavoritoZonaEntity

@Dao
interface FavoritoZonaDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun agregarFavorito(favorito: FavoritoZonaEntity)

    @Query(
        "DELETE FROM favoritos_zona_ser WHERE correo_usuario = :correoUsuario AND id_zona = :idZona"
    )
    suspend fun eliminarFavorito(correoUsuario: String, idZona: Int)

    @Query(
        "SELECT id_zona FROM favoritos_zona_ser WHERE correo_usuario = :correoUsuario ORDER BY id_zona"
    )
    suspend fun obtenerIdsZonaFavoritos(correoUsuario: String): List<Int>

    @Query(
        "SELECT COUNT(*) FROM favoritos_zona_ser WHERE correo_usuario = :correoUsuario AND id_zona = :idZona"
    )
    suspend fun contarFavorito(correoUsuario: String, idZona: Int): Int

    @Transaction
    suspend fun alternarFavorito(correoUsuario: String, idZona: Int) {
        if (contarFavorito(correoUsuario, idZona) > 0) {
            eliminarFavorito(correoUsuario, idZona)
        } else {
            agregarFavorito(
                FavoritoZonaEntity(correo_usuario = correoUsuario, id_zona = idZona)
            )
        }
    }
}
