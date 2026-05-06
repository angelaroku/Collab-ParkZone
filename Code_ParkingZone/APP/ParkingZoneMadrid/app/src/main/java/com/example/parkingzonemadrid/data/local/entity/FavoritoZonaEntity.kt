package com.example.parkingzonemadrid.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

/**
 * Favoritos del mapa SER: calle/zona identificada por [id_zona] (hash del CSV) y usuario por [correo_usuario].
 * Separado de [FavoritoEntity] (tabla `favoritos` ligada a direcció)
 */
@Entity(
    tableName = "favoritos_zona_ser",
    primaryKeys = ["correo_usuario", "id_zona"]
)
data class FavoritoZonaEntity(
    @ColumnInfo(name = "correo_usuario")
    val correo_usuario: String,

    @ColumnInfo(name = "id_zona")
    val id_zona: Int
)
