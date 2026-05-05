package com.example.parkingzonemadrid.data.local.entity
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "usuarios",
    primaryKeys = ["id_usuario"]
)
data class UsuarioEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_usuario")
    val id_usuario: Int = 0,

    @ColumnInfo(name = "nom_usuario")
    val nom_usuario: String,

    @ColumnInfo(name = "correo")
    val correo: String,

    @ColumnInfo(name = "password")
    val password: String
)