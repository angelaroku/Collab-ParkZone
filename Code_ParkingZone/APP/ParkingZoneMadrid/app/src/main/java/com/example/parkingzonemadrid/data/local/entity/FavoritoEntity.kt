package com.example.parkingzonemadrid.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Favorito ligado a una dirección (modelo relacional de tu compañera).
 * Aún no está registrado en [AppDatabase]; cuando lo integréis, encajará con [UsuarioEntity.correo] como padre.
 */
@Entity(
    tableName = "favoritos",
    foreignKeys = [
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["correo"],
            childColumns = ["correo_usuario_fk"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class FavoritoEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_favorito")
    val id_favorito: Int = 0,

    @ColumnInfo(name = "correo_usuario_fk")
    val correo_usuario_fk: String,

    @ColumnInfo(name = "id_direccion_fk")
    val id_direccion_fk: Int,

    @ColumnInfo(name = "nombre_favorito")
    val nombre_favorito: String
)
