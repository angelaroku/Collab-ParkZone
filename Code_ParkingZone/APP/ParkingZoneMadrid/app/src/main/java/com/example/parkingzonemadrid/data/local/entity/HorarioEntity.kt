package com.example.parkingzonemadrid.data.local.entity
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
@Entity(
    tableName = "horarios",
    primaryKeys = ["id_horario"],
    foreignKeys = [
        ForeignKey(
            entity = ZonaEntity::class,      // Tabla a la que apunta
            parentColumns = ["id_zona"],     // La columna en ZonaEntity
            childColumns = ["id_zona_fk"],   // La columna aquí en HorarioEntity
            onDelete = ForeignKey.CASCADE    // Si borro la zona, borro sus horarios
        )
    ]
)
data class HorarioEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_horario")
    val id_horario: Int = 0,

    @ColumnInfo(name = "id_zona_fk")
    val id_zona_fk: Int,

    @ColumnInfo(name = "dia_semana")
    val dia_semana: String,

    @ColumnInfo(name = "hora_inicio")
    val hora_inicio: String,

    @ColumnInfo(name = "hora_fin")
    val hora_fin: String,

    @ColumnInfo(name = "es_festivo")
    val es_festivo: Boolean
)