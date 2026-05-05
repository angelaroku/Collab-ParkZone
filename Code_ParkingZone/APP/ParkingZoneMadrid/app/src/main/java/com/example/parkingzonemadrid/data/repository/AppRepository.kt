package com.example.parkingzonemadrid.data.repository

import android.content.Context
import com.example.parkingzonemadrid.data.local.AppDatabase
import com.example.parkingzonemadrid.data.local.entity.UsuarioEntity
import com.example.parkingzonemadrid.data.parser.CsvParserSER
import com.example.parkingzonemadrid.data.mapper.ZonaMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppRepository(context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val usuarioDao = db.usuarioDao()
    private val zonaDao = db.zonaDao()
    private val csvParser = CsvParserSER()

    //  GESTIÓN DE USUARIOS

    suspend fun crearUsuario(nombre: String, correo: String, pass: String) = withContext(Dispatchers.IO) {
        val nuevo = UsuarioEntity(nom_usuario = nombre, correo = correo, password = pass)
        usuarioDao.insertar(nuevo)
    }

    suspend fun consultarUsuario(id: Int) = withContext(Dispatchers.IO) {
        usuarioDao.obtenerPorId(id)
    }

    suspend fun eliminarUsuario(id: Int) = withContext(Dispatchers.IO) {
        usuarioDao.borrarPorId(id)
    }

    //  GESTIÓN DE ZONAS (Importación del CSV)

    suspend fun importarDatosSiEsNecesario(context: Context) = withContext(Dispatchers.IO) {
        val cuenta = zonaDao.contarZonas() // Necesitas añadir este método al DAO
        if (cuenta == 0) {
            val datos = csvParser.leerArchivo(context, "zonas_ser.csv")
            val entidades = datos.map { ZonaMapper.modeloAEntidad(ZonaMapper.deCsvAModelo(it)) }
            zonaDao.insertarTodas(entidades)
        }
    }
}