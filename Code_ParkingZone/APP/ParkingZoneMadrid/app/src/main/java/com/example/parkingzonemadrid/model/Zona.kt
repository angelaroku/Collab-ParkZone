package com.example.parkingzonemadrid.model

import kotlin.text.isNotBlank

data class Zona(
    // val(solo get) var -> get y set automáticos
    //public final  por defecto
    var id_zona: Int,
    var gis_x: String,
    var gis_y: String,
    var color: TipoColor,
    var direcciones:  MutableList<Direccion>,
    var horarios:  MutableList<Horario>,
    var tarifas:  MutableList<Tarifa>

) {
    // CRUD de Tarifa
    fun crearTarifa(color: TipoColor, precio: Float){
        // id automatico
        val id_tarifa = tarifas.size+1

        val nueva_tarifa = Tarifa(id_tarifa, color, precio)
        tarifas.add(nueva_tarifa)
    }


    fun consultarTarifa(id_tarifa_consultar: Int) : Tarifa? {
        return tarifas.find { it.id_tarifa == id_tarifa_consultar }
        /* version compleja con posibilidad de descartar
        val tarifa_consultada = tarifas.find { it.id_tarifa == id_tarifaZona }

        if (tarifa_consultada != null)
            return tarifa_consultada
        else
            return null*/
    }

    fun modificarTarifa(id_tarifa_modificar: Int, nuevo_precio: Float, nuevo_color:TipoColor): Boolean{
        val tarifa = tarifas.find{ it.id_tarifa == id_tarifa_modificar }

        return if (tarifa != null) {
            tarifa.precio = nuevo_precio
            tarifa.color = nuevo_color
            true
        } else{
            false
        }
    }

    fun eliminarTarifa(id_tarifa_eliminar: Int): Boolean{
        return tarifas.removeIf{ it.id_tarifa == id_tarifa_eliminar }
    }

    // CRUD de Horario
    fun crearHorario(hora:String, dias_semana:String, detalles:String){
        // id automatico
        val id_horario = horarios.size+1

        val nuevo_horario = Horario(id_horario, hora , dias_semana, detalles)
        horarios.add(nuevo_horario)
    }

    fun consultarHorario(id_horario_consultar: Int) : Horario? {
        return horarios.find { it.id_horario == id_horario_consultar }
        /* version compleja con posibilidad de descartar
        val horario_consultado = horarios.find { it.id_horario == id_horarioZona }


        if (horario_consultado != null)
            return horario_consultado
        else
            return null*/
    }

    fun modificarHorario(id_horario_modificar: Int, nueva_hora:String, nuevos_dias_semana:String, nuevos_detalles:String): Boolean{
       val horario = horarios.find{ it.id_horario == id_horario_modificar }

        return if (horario != null) {
            horario.hora = nueva_hora
            horario.dias_semana = nuevos_dias_semana
            horario.detalles= nuevos_detalles
            true
        } else{
            false
        }
    }

    fun eliminarHorario(id_horario_eliminar: Int): Boolean{
        return horarios.removeIf { it.id_horario == id_horario_eliminar }
    }
}
