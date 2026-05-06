package com.example.parkingzonemadrid.data.mapper

import com.example.parkingzonemadrid.data.local.entity.ZonaEntity
import com.example.parkingzonemadrid.data.parser.ColumnasCsvSER
import com.example.parkingzonemadrid.model.*

object ZonaMapper {

    /**
     * Convierte la fila del CSV a tu objeto de modelo Zona.
     */
    fun deCsvAModelo(csv: ColumnasCsvSER): Zona {

        //  Creamos la dirección con los datos literales del CSV
        val direccionCsvSER = Direccion(
            id_direccion = 0,
            tipo_via = extraerTipoVia(csv.calle),
            calle = csv.calle, // Se mantiene literal como en el CSV
            num_finca = csv.numero_finca,
            codigo_postal = 0,
            cod_distrito = csv.cod_distrito,
            distrito = csv.distrito,
            cod_barrio = csv.cod_barrio,
            num_barrio = csv.num_barrio,
            barrio = csv.barrio,
            // Comprobamos el literal exacto "Batería" que viene en tu CSV
            bateria_linea = if (csv.bateria_linea.contains("Batería"))
                TipoAparcamiento.BATERIA else TipoAparcamiento.LINEA
        )

        // Creamos la Zona con TODAS las variables para evitar el error "No value passed"
        val zona = Zona(
            id_zona = 0,
            gis_x = csv.gis_x,
            gis_y = csv.gis_y,
            color = traducirColor(csv.color),
            direcciones = mutableListOf(direccionCsvSER),
            horarios = generarHorariosMadrid().toMutableList(),
            tarifas = mutableListOf()
        )

        return zona
    }

    /**
     * Ajustamos lo que dice el CSV a los Enums del modelo.
     * Usamos los códigos exactos del fichero.
     */
    private fun traducirColor(colorCsv: String): TipoColor {
        val texto = colorCsv.trim()
        return when {
            texto.contains("077214010 Verde", ignoreCase = true) -> TipoColor.VERDE
            texto.contains("043000255 Azul", ignoreCase = true) -> TipoColor.AZUL
            texto.contains("255140000 Naranja", ignoreCase = true) -> TipoColor.NARANJA
            texto.contains("255000000 Rojo", ignoreCase = true) -> TipoColor.ROJOBLANCO
            else -> TipoColor.AZUL
        }
    }

    /**
     * Extrae el tipo de vía (PLAZA, CALLE, etc.)
     * Como el CSV viene: "NOMBRE, TIPO VIA, ARTICULO",
     * dividimos por la coma y cogemos la segunda parte.
     */
    private fun extraerTipoVia(calleCompleta: String): String {
        val partes = calleCompleta.split(",")
        return if (partes.size >= 2) {
            // Cogemos la segunda parte, quitamos espacios y a mayúsculas
            partes[1].trim().uppercase()
        } else {
            // Si no hay comas, devolvemos CALLE por defecto
            "CALLE"
        }
    }

    /**
     * Genera los horarios por defecto para Madrid.
     * Formato de hora: "inicio - fin"
     */
    private fun generarHorariosMadrid(): List<Horario> {
        return listOf(
            Horario(
                id_horario = 0,
                dias_semana = "L-V",
                hora = "09:00 - 21:00",
                detalles = "Servicio estándar"
            ),
            Horario(
                id_horario = 0,
                dias_semana = "Sábado",
                hora = "09:00 - 15:00",
                detalles = "Servicio reducido"
            )
        )
    }

    /**
     * Pasa del modelo Zona a la Entidad de Room para la base de datos.
     */
    fun modeloAEntidad(zona: Zona): ZonaEntity {
        return ZonaEntity(
            id_zona = zona.id_zona,
            gis_x = zona.gis_x,
            gis_y = zona.gis_y,
            color = zona.color
        )
    }
}


/*
object ZonaMapper {

    /**
     * Convierte la fila del CSV a tu objeto de modelo Zona.
     */
    fun deCsvAModelo(csv: ColumnasCsvSER): Zona {

        // Dirección con los datos del CSV
        val direccionCsvSER = Direccion(
            id_direccion = 0,
            tipo_via = extraerTipoVia(csv.calle),
            calle = csv.calle,
            codigo_postal = 0,
            num_finca = csv.numFinca,
            cod_distrito = csv.codDistrito,
            distrito = csv.distrito,
            cod_barrio = csv.codBarrio,
            num_barrio = csv.numBarrio,
            barrio = csv.barrio,
            bateria_linea = if (csv.bateriaLinea.uppercase().contains("BATERIA"))
                TipoAparcamiento.BATERIA else TipoAparcamiento.LINEA
        )

        // Creamos la Zona y asignamos el color usando nuestra función traductora
        val zona = Zona(
            id_zona = 0,
            gis_x = csv.gisX,
            gis_y = csv.gisY,
            color = traducirColor(csv.color),
            direcciones = mutableListOf(direccionCsvSER), // La metemos ya en la lista
            horarios = generarHorariosMadrid().toMutableList(), // Metemos los horarios aquí
            tarifas = mutableListOf()
        )


    //Ajustamos lo que dice el CSV a los Enums del modelo

    private fun traducirColor(colorCsv: String): TipoColor {
        return when (colorCsv.uppercase().trim()) {
            "077214010 Verde" -> TipoColor.VERDE
            "043000255 Azul" -> TipoColor.AZUL
            "255140000 Naranja" -> TipoColor.NARANJA
            "255000000 Rojo" -> TipoColor.ROJOBLANCO
            else -> TipoColor.AZUL // Color por defecto si no se entiende
        }
    }

    private fun extraerTipoVia(calleCompleta: String): String {
        return calleCompleta.trim().substringBefore(" ", "CALLE")
    }

    private fun generarHorariosMadrid(): List<Horario> {
        return listOf(
            Horario(
                id_horario = 0,
                dias_semana = "L-V",
                hora = "09:00 - 21:00",
                detalles = "Tarifa ordinaria"
            ),
            Horario(
                id_horario = 0,
                dias_semana = "Sábado",
                hora = "09:00 - 15:00",
                detalles = "Tarifa reducida"
            )
        )
    }

    /**
     * Para cuando necesitemos pasar de la Zona (model) a la Entidad de Room (local/entity)
     */
    fun modeloAEntidad(zona: Zona): ZonaEntity {
        return ZonaEntity(
            id_zona = zona.id_zona,
            gis_x = zona.gis_x,
            gis_y = zona.gis_y,
            color = zona.color
        )
    }
}
*/

/*

object ZonaMapper {

    /**
     * Convierte la fila cruda del CSV a tu objeto de modelo Zona.
     * Reparte los datos de 'ColumnasCsvSER' en Zona y Direccion.
     */
    fun deCsvAModelo(csv: ColumnasCsvSER): Zona {

        // 1. Creamos la dirección con los datos del CSV
        val direccionFiel = Direccion(
            id_direccion = 0,
            tipo_via = csv.calle.substringBefore(" "),
            calle = csv.calle,
            num_finca = csv.numFinca,
            cod_distrito = csv.codDistrito,
            distrito = csv.distrito,
            cod_barrio = csv.codBarrio,
            num_barrio = csv.numBarrio,
            barrio = csv.barrio,
            bateria_linea = if (csv.bateriaLinea.uppercase() == "BATERIA")
                TipoAparcamiento.BATERIA else TipoAparcamiento.LINEA
        )

        // 2. Creamos la Zona (los datos "padre")
        val zona = Zona(
            id_zona = 0, // Autogenerado por la base de datos
            gis_x = csv.gisX,
            gis_y = csv.gisY,
            color = when(csv.color.uppercase()) {
                "077214010 Verde" -> TipoColor.VERDE
                "043000255 Azul" -> TipoColor.AZUL
                else -> TipoColor.AZUL
            }
        ).apply {
            // 3. Añadimos la dirección a la lista de la zona
            direcciones.add(direccionFiel)
            // Añadimos horarios base de Madrid (puedes crear una función para esto)
            horarios.addAll(generarHorariosMadrid())
        }

        return zona
    }

    private fun generarHorariosMadrid() = listOf(
        Horario(0, "L-V", "09:00", "21:00", false),
        Horario(0, "S", "09:00", "15:00", false)
    )
}
*/
