package com.universallive.app.streaming.security

/**
 * Platform-neutral contract. Android uses Android Keystore; iOS will use Keychain.
 * The app should persist only returned references/aliases outside the vault.
 */
interface CredentialVault {
    fun put(alias: String, secret: String)
    fun get(alias: String): String?
    fun remove(alias: String)
    fun contains(alias: String): Boolean
}

class InMemoryCredentialVault : CredentialVault {
    private val secrets = mutableMapOf<String, String>()
    override fun put(alias: String, secret: String) { secrets[alias] = secret }
    override fun get(alias: String): String? = secrets[alias]
    override fun remove(alias: String) { secrets.remove(alias) }
    override fun contains(alias: String): Boolean = alias in secrets
}
