package com.example.parkingzonemadrid.utils

import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.parkingzonemadrid.databinding.ActivityLoginBinding
import com.example.parkingzonemadrid.data.repository.ParkingLocalRepository
import com.example.parkingzonemadrid.model.Usuario
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var prefsManager: PreferencesManager
    private lateinit var repository: ParkingLocalRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefsManager = PreferencesManager(this)
        repository = ParkingLocalRepository(applicationContext)
        setupUI()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "Iniciar Sesión"
            setDisplayHomeAsUpEnabled(true)
        }

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.btnLogin.setOnClickListener { attemptLogin() }
    }

    private fun attemptLogin() {
        val name = binding.etName.text?.toString()?.trim().orEmpty()
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()

        if (name.isEmpty()) {
            binding.etName.error = "El nombre es obligatorio"
            binding.etName.requestFocus()
            return
        }

        if (email.isEmpty()) {
            binding.etEmail.error = "El email es obligatorio"
            binding.etEmail.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Email inválido"
            binding.etEmail.requestFocus()
            return
        }

        val user = Usuario(
            id_usuario = Usuario.generarId(),
            nom_usuario = name,
            correo = email,
            password = "",
            favoritos = mutableListOf()
        )
        prefsManager.saveUser(user)

        // Persistimos el usuario en Room para que el login “recuerde” y las favoritas queden asociadas a su email.
        lifecycleScope.launch(Dispatchers.IO) {
            repository.upsertUser(user)
        }

        Toast.makeText(this, "¡Bienvenido, $name!", Toast.LENGTH_SHORT).show()
        setResult(RESULT_OK)
        finish()
    }
}

