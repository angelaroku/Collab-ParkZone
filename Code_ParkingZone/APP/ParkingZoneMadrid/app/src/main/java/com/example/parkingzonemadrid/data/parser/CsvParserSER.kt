package com.example.parkingzonemadrid.data.parser

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

class CsvParserSER {

    fun leerArchivo(context: Context, nombreArchivo: String): List<ColumnasCsvSER> {
        val lista = mutableListOf<ColumnasCsvSER>()

        try {
            // Abrimos el archivo desde la carpeta assets
            val reader = BufferedReader(InputStreamReader(context.assets.open(nombreArchivo)))
            // Leemos todas las líneas
            val lineas = reader.readLines()
            // Saltamos la primera línea (la de los títulos)
            lineas.drop(1).forEach { linea ->
                val columnas = linea.split(";")

                if (columnas.size >= 12) {
                    lista.add(
                        ColumnasCsvSER(
                            gis_x = columnas[0],
                            gis_y = columnas[1],
                            cod_distrito = columnas[2].toIntOrNull() ?: 0,
                            distrito = columnas[3],
                            cod_barrio = columnas[4].toIntOrNull() ?: 0,
                            num_barrio = columnas[5].toIntOrNull() ?: 0,
                            barrio = columnas[6],
                            calle = columnas[7],
                            numero_finca = columnas[8], // Antes era numFinca
                            color = columnas[9],
                            bateria_linea = columnas[10], // Antes era bateriaLinea
                            numero_plazas = columnas[11]
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return lista
    }
}