package com.example.parkingzonemadrid.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Usuario persistido en Room. El correo es la clave natural (coincide con sesión y favoritos del mapa).
 * El [FavoritoEntity] de dominio (dirección, etc.) de tu compañera es otro modelo y puede integrarse más adelante.
 */
@Entity(tableName = "usuarios")
data class UsuarioEntity(
    @PrimaryKey
    @ColumnInfo(name = "correo")
    val correo: String,

    @ColumnInfo(name = "nom_usuario")
    val nom_usuario: String,

    @ColumnInfo(name = "password")
    val password: String = ""
)
