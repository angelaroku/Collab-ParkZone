package com.example.parkingzonemadrid.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.parkingzonemadrid.data.local.converter.AppConverters
import com.example.parkingzonemadrid.data.local.dao.FavoritoZonaDao
import com.example.parkingzonemadrid.data.local.dao.UsuarioDao
import com.example.parkingzonemadrid.data.local.dao.ZonaDao
import com.example.parkingzonemadrid.data.local.entity.FavoritoZonaEntity
import com.example.parkingzonemadrid.data.local.entity.UsuarioEntity
import com.example.parkingzonemadrid.data.local.entity.ZonaEntity

/**
 * Base de datos activa: usuarios (español), favoritos del mapa SER, zonas importables desde CSV.
 * [com.example.parkingzonemadrid.data.local.entity.FavoritoEntity] (tabla favoritos por dirección)
 * puede añadirse cuando enlacéis el modelo relacional completo.
 */
@Database(
    entities = [
        UsuarioEntity::class,
        FavoritoZonaEntity::class,
        ZonaEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(AppConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao
    abstract fun favoritoZonaDao(): FavoritoZonaDao
    abstract fun zonaDao(): ZonaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "parkingzone_madrid.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
