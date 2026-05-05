package com.example.parkingzonemadrid.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.parkingzonemadrid.data.local.dao.*
import com.example.parkingzonemadrid.data.local.entity.*
import com.example.parkingzonemadrid.data.local.converter.AppConverters

@Database(
    entities = [
        UsuarioEntity::class,
        FavoritoEntity::class,
        ZonaEntity::class,
        DireccionEntity::class,
        HorarioEntity::class,
        TarifaEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(AppConverters::class) // para que Room entienda los Enums (Colores, etc.)
abstract class AppDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao
    abstract fun favoritoDao(): FavoritoDao
    abstract fun zonaDao(): ZonaDao
    abstract fun direccionDao(): DireccionDao
    abstract fun horarioDao(): HorarioDao
    abstract fun tarifaDao(): TarifaDao

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