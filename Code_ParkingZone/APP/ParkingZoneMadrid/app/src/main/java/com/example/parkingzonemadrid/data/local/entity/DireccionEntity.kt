package com.example.parkingzonemadrid.data.local.entity
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.example.parkingzonemadrid.model.TipoAparcamiento

@Entity(
    tableName = "direcciones",
    primaryKeys = ["id_direccion"],
    foreignKeys = [
        ForeignKey(
            entity = ZonaEntity::class,
            parentColumns = ["id_zona"],
            childColumns = ["id_zona_fk"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class DireccionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_direccion")
    val id_direccion: Int = 0,

    @ColumnInfo(name = "id_zona_fk")
    val id_zona_fk: Int,

    @ColumnInfo(name = "tipo_via")
    val tipo_via: String,

    @ColumnInfo(name = "calle")
    val calle: String,

    @ColumnInfo(name = "codigo_postal")
    val codigo_postal: Int,

    @ColumnInfo(name = "num_finca")
    val num_finca: String,

    @ColumnInfo(name = "cod_distrito")
    val cod_distrito: Int,

    @ColumnInfo(name = "distrito")
    val distrito: String,

    @ColumnInfo(name = "cod_barrio")
    val cod_barrio: Int,

    @ColumnInfo(name = "num_barrio")
    val num_barrio: Int,

    @ColumnInfo(name = "barrio")
    val barrio: String,

    @ColumnInfo(name = "bateria_linea")
    val bateria_linea: TipoAparcamiento
)