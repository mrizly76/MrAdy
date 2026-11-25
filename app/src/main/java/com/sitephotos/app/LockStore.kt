package com.sitephotos.app

import android.content.Context
import java.security.MessageDigest
import java.security.SecureRandom

/** Stores the app PIN (salted SHA-256 hash only, never the PIN itself) and the fingerprint setting. */
class LockStore(ctx: Context) {
    private val prefs = ctx.getSharedPreferences("app_lock", Context.MODE_PRIVATE)

    val hasPin: Boolean get() = prefs.contains("pin_hash")

    var fingerprintEnabled: Boolean
        get() = prefs.getBoolean("fingerprint", false)
        set(v) { prefs.edit().putBoolean("fingerprint", v).apply() }

    fun setPin(pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }.toHex()
        prefs.edit()
            .putString("pin_salt", salt)
            .putString("pin_hash", hash(salt, pin))
            .putInt("fails", 0)
            .apply()
    }

    fun checkPin(pin: String): Boolean {
        val salt = prefs.getString("pin_salt", null) ?: return false
        val ok = hash(salt, pin) == prefs.getString("pin_hash", null)
        if (ok) {
            prefs.edit().putInt("fails", 0).putLong("locked_until", 0).apply()
        } else {
            val fails = prefs.getInt("fails", 0) + 1
            val edit = prefs.edit().putInt("fails", fails)
            // After 5 wrong PINs: wait 30 s, doubling each further 5 wrong tries.
            if (fails % 5 == 0) {
                val waitMs = 30_000L shl ((fails / 5) - 1).coerceAtMost(6)
                edit.putLong("locked_until", System.currentTimeMillis() + waitMs)
            }
            edit.apply()
        }
        return ok
    }

    /** Seconds the user must still wait before trying another PIN (0 = can try now). */
    fun waitSeconds(): Long {
        val left = prefs.getLong("locked_until", 0) - System.currentTimeMillis()
        return if (left > 0) (left + 999) / 1000 else 0
    }

    private fun hash(salt: String, pin: String) =
        MessageDigest.getInstance("SHA-256").digest("$salt:$pin".toByteArray()).toHex()

    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }
}
