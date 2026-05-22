package com.example.parkingzonemadrid.data.repository

import com.example.parkingzonemadrid.model.Usuario

sealed class AuthResult {
    data class Success(val usuario: Usuario) : AuthResult()
    data class Error(val message: String) : AuthResult()
}
