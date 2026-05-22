package com.example.parkingzonemadrid.ui.login

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.parkingzonemadrid.databinding.ActivityLoginBinding
import com.example.parkingzonemadrid.model.Usuario
import com.example.parkingzonemadrid.utils.PreferencesManager
import com.google.android.material.button.MaterialButtonToggleGroup
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var prefsManager: PreferencesManager
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefsManager = PreferencesManager(this)
        setupToolbar()
        setupModeToggle()
        setupActions()
        observeUiState()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupModeToggle() {
        binding.toggleAuthMode.addOnButtonCheckedListener { _: MaterialButtonToggleGroup, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            when (checkedId) {
                binding.btnModeSignIn.id -> applyMode(LoginMode.SIGN_IN)
                binding.btnModeRegister.id -> applyMode(LoginMode.REGISTER)
            }
        }
        applyMode(LoginMode.SIGN_IN)
    }

    private fun applyMode(mode: LoginMode) {
        viewModel.setMode(mode)
        val isRegister = mode == LoginMode.REGISTER
        binding.layoutName.visibility = if (isRegister) View.VISIBLE else View.GONE
        binding.layoutConfirmPassword.visibility = if (isRegister) View.VISIBLE else View.GONE
        binding.tvSubtitle.setText(
            if (isRegister) com.example.parkingzonemadrid.R.string.login_subtitle_register
            else com.example.parkingzonemadrid.R.string.login_subtitle_sign_in
        )
        binding.btnSubmit.setText(
            if (isRegister) com.example.parkingzonemadrid.R.string.login_btn_register
            else com.example.parkingzonemadrid.R.string.login_btn_sign_in
        )
        hideError()
    }

    private fun setupActions() {
        binding.btnSubmit.setOnClickListener { onSubmit() }
        listOf(binding.etEmail, binding.etPassword, binding.etName, binding.etConfirmPassword)
            .forEach { field ->
                field.doAfterTextChanged { hideError() }
            }
    }

    private fun onSubmit() {
        val email = binding.etEmail.text?.toString().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()
        when (viewModel.mode) {
            LoginMode.SIGN_IN -> viewModel.submitSignIn(email, password)
            LoginMode.REGISTER -> {
                val name = binding.etName.text?.toString().orEmpty()
                val confirm = binding.etConfirmPassword.text?.toString().orEmpty()
                viewModel.submitRegister(name, email, password, confirm)
            }
        }
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        LoginUiState.Idle -> {
                            binding.progress.visibility = View.GONE
                            binding.btnSubmit.isEnabled = true
                        }
                        LoginUiState.Loading -> {
                            binding.progress.visibility = View.VISIBLE
                            binding.btnSubmit.isEnabled = false
                            hideError()
                        }
                        is LoginUiState.Success -> {
                            binding.progress.visibility = View.GONE
                            binding.btnSubmit.isEnabled = true
                            onAuthSuccess(state.userName)
                        }
                        is LoginUiState.Error -> {
                            binding.progress.visibility = View.GONE
                            binding.btnSubmit.isEnabled = true
                            showError(state.message)
                        }
                    }
                }
            }
        }
    }

    private fun onAuthSuccess(displayName: String) {
        val email = binding.etEmail.text?.toString()?.trim()?.lowercase().orEmpty()
        prefsManager.saveUser(
            Usuario(
                id_usuario = Usuario.generarId(),
                nom_usuario = displayName,
                correo = email,
                password = "",
                favoritos = mutableListOf()
            )
        )
        Toast.makeText(this, "¡Bienvenido, $displayName!", Toast.LENGTH_SHORT).show()
        setResult(RESULT_OK)
        finish()
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
    }

    private fun hideError() {
        binding.tvError.visibility = View.GONE
        viewModel.clearError()
    }
}
