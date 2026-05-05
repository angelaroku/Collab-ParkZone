package com.example.parkingzonemadrid.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "favoritos",
    primaryKeys = ["id_favorito"],
    foreignKeys = [
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["id_usuario"],
            childColumns = ["id_usuario_fk"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class FavoritoEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_favorito")
    val id_favorito: Int = 0,

    @ColumnInfo(name = "id_usuario_fk")
    val id_usuario_fk: Int,

    @ColumnInfo(name = "id_direccion_fk")
    val id_direccion_fk: Int,

    @ColumnInfo(name = "nombre_favorito")
    val nombre_favorito: String
)