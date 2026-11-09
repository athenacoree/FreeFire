package com.athenacoree.music

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Email/password account actions. Supabase handles password hashing and tokens.
 * The app never stores or logs the user's password itself.
 */
internal object MusicAuthRepository {
    suspend fun signUp(email: String, password: String) {
        MusicSupabase.client.auth.signUpWith(Email) {
            this.email = email
            this.password = password
            data = buildJsonObject { put("app", "music") }
        }
    }

    suspend fun signIn(email: String, password: String) {
        MusicSupabase.client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signOut() {
        MusicSupabase.client.auth.signOut()
    }
}
