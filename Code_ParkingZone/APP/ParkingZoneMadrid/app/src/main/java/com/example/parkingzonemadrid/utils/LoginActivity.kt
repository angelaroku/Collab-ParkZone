package com.example.parkingzonemadrid.utils

import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.parkingzonemadrid.databinding.ActivityLoginBinding
import com.example.parkingzonemadrid.model.User

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var prefsManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefsManager = PreferencesManager(this)
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

        val user = User(name = name, email = email)
        prefsManager.saveUser(user)

        Toast.makeText(this, "¡Bienvenido, $name!", Toast.LENGTH_SHORT).show()
        setResult(RESULT_OK)
        finish()
    }
}

