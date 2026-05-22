package com.example.parkingzonemadrid.utils

import java.security.MessageDigest

/**
 * Hash local para credenciales en Room (no sustituye un backend con bcrypt/Argon2).
 * Usa el correo como sal para que dos usuarios con la misma contraseña no compartan hash.
 */
object PasswordHasher {

    fun hash(password: String, correo: String): String {
        val normalized = "${correo.trim().lowercase()}|$password"
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(normalized.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun verify(password: String, correo: String, storedHash: String): Boolean =
        hash(password, correo) == storedHash
}
