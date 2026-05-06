package com.example.parkingzonemadrid.data.repository

import android.content.Context
import com.example.parkingzonemadrid.data.local.AppDatabase
import com.example.parkingzonemadrid.data.local.entity.UsuarioEntity
import com.example.parkingzonemadrid.data.mapper.ZonaMapper
import com.example.parkingzonemadrid.data.parser.CsvParserSER
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repositorio de app: usuarios en [UsuarioEntity], favoritos del mapa en [FavoritoZonaEntity],
 * importación CSV en [ZonaEntity].
 */
class AppRepository(context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val usuarioDao = db.usuarioDao()
    private val zonaDao = db.zonaDao()
    private val csvParser = CsvParserSER()

    suspend fun crearUsuario(nombre: String, correo: String, pass: String) = withContext(Dispatchers.IO) {
        usuarioDao.insertarOActualizar(
            UsuarioEntity(
                correo = correo,
                nom_usuario = nombre,
                password = pass
            )
        )
    }

    suspend fun consultarUsuarioPorCorreo(correo: String) = withContext(Dispatchers.IO) {
        usuarioDao.obtenerPorCorreo(correo)
    }

    suspend fun importarDatosSiEsNecesario(context: Context) = withContext(Dispatchers.IO) {
        val cuenta = zonaDao.contarZonas()
        if (cuenta == 0) {
            val datos = csvParser.leerArchivo(context, "zonas_ser.csv")
            val entidades = datos.map { ZonaMapper.modeloAEntidad(ZonaMapper.deCsvAModelo(it)) }
            if (entidades.isNotEmpty()) {
                zonaDao.insertarTodas(entidades)
            }
        }
    }
}
