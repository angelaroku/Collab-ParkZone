package com.example.parkingzonemadrid.data.local.entity
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.example.parkingzonemadrid.model.TipoColor

@Entity(
    tableName = "tarifas",
    foreignKeys = [
        ForeignKey(
            entity = ZonaEntity::class,
            parentColumns = ["id_zona"],     // Nombre exacto en ZonaEntity
            childColumns = ["id_zona_fk"],    // Nombre exacto aquí abajo
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TarifaEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_tarifa")
    val id_tarifa: Int = 0,

    @ColumnInfo(name = "id_zona_fk")
    val id_zona_fk: Int,

    @ColumnInfo(name = "color")
    val color: TipoColor,

    @ColumnInfo(name = "precio")
    val precio: Float
)