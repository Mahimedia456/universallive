# Phase 16 — Secure persistence foundation

- CredentialVault shared contract added.
- AndroidCredentialVault uses Android Keystore AES-256-GCM.
- Encrypted ciphertext is stored in private SharedPreferences; the AES key never leaves Android Keystore.
- Stream keys must not be persisted as plaintext in ordinary preferences or logs.
- iOS will implement the same contract with Keychain.
- Full profile/settings repository wiring is finalized during Phase 20 compile/device QA and backend phases.
