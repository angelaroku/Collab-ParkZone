package com.example.parkingzonemadrid.data.repository

import android.content.Context
import com.example.parkingzonemadrid.data.local.AppDatabase
import com.example.parkingzonemadrid.data.mapper.ZonaMapper
import com.example.parkingzonemadrid.data.parser.CsvParserSER
import com.example.parkingzonemadrid.model.Zona
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ZonaRepository(context: Context) {

    //  Obtenemos la instancia de la base de datos y el DAO
    private val database = AppDatabase.getInstance(context)
    private val zonaDao = database.zonaDao()

    //  Instanciamos nuestro lector de CSV
    private val csvParser = CsvParserSER()

    /**
     * Esta función lee el CSV desde assets, lo mapea a entidades de Room
     * y las guarda en la base de datos.
     */
    suspend fun importarZonasDesdeCsv(context: Context) = withContext(Dispatchers.IO) {
        try {
            //  Leer el CSV - obtenemos lista de ColumnasCsvSER)
            val datosCsv = csvParser.leerArchivo(context, "zonas_ser.csv")

            // Convertir cada fila del CSV a una Entidad de base de datos
            val entidadesParaRoom = datosCsv.map { fila ->
                val modeloZona = ZonaMapper.deCsvAModelo(fila)
                ZonaMapper.modeloAEntidad(modeloZona)
            }

            // Insertar todas en la base de datos
            if (entidadesParaRoom.isNotEmpty()) {
                zonaDao.insertarTodas(entidadesParaRoom)
            }

        } catch (e: Exception) {
            e.printStackTrace()

        }
    }

    /**
     * Recupera todas las zonas de la BD para pintarlas en el mapa.
     */
    suspend fun obtenerZonasGuardadas() = withContext(Dispatchers.IO) {
        zonaDao.obtenerTodas()
    }
}