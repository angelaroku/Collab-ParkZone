package com.example.parkingzonemadrid.ui.login

import android.app.Application
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.parkingzonemadrid.data.repository.AuthResult
import com.example.parkingzonemadrid.data.repository.ParkingLocalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class LoginMode { SIGN_IN, REGISTER }

sealed class LoginUiState {
    data object Idle : LoginUiState()
    data object Loading : LoginUiState()
    data class Success(val userName: String) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ParkingLocalRepository(application)

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    var mode: LoginMode = LoginMode.SIGN_IN
        private set

    fun setMode(newMode: LoginMode) {
        mode = newMode
        _uiState.value = LoginUiState.Idle
    }

    fun submitSignIn(email: String, password: String) {
        validateSignIn(email, password)?.let {
            _uiState.value = LoginUiState.Error(it)
            return
        }
        runAuth {
            repository.signInUser(email, password)
        }
    }

    fun submitRegister(name: String, email: String, password: String, confirmPassword: String) {
        validateRegister(name, email, password, confirmPassword)?.let {
            _uiState.value = LoginUiState.Error(it)
            return
        }
        runAuth {
            repository.registerUser(name, email, password)
        }
    }

    fun clearError() {
        if (_uiState.value is LoginUiState.Error) {
            _uiState.value = LoginUiState.Idle
        }
    }

    private fun runAuth(block: suspend () -> AuthResult) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            when (val result = block()) {
                is AuthResult.Success -> _uiState.value = LoginUiState.Success(result.usuario.nom_usuario)
                is AuthResult.Error -> _uiState.value = LoginUiState.Error(result.message)
            }
        }
    }

    private fun validateSignIn(email: String, password: String): String? {
        if (email.isBlank()) return "El correo es obligatorio"
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) return "Correo inválido"
        if (password.isBlank()) return "La contraseña es obligatoria"
        if (password.length < 6) return "La contraseña debe tener al menos 6 caracteres"
        return null
    }

    private fun validateRegister(
        name: String,
        email: String,
        password: String,
        confirmPassword: String
    ): String? {
        if (name.isBlank()) return "El nombre de usuario es obligatorio"
        if (email.isBlank()) return "El correo es obligatorio"
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) return "Correo inválido"
        if (password.length < 6) return "La contraseña debe tener al menos 6 caracteres"
        if (password != confirmPassword) return "Las contraseñas no coinciden"
        return null
    }
}
