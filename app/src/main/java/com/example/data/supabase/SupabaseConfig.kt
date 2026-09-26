package com.example.data.supabase

import android.content.Context
import android.content.SharedPreferences

object SupabaseConfig {
    private const val PREFS_NAME = "supabase_settings"
    private const val KEY_URL = "supabase_url"
    private const val KEY_ANON_KEY = "supabase_anon_key"

    // =========================================================================
    // CREDENCIAIS EMBUTIDAS NO CÓDIGO DO APLICATIVO
    // =========================================================================
    // Cole abaixo a URL do seu projeto Supabase (ex: "https://xyzcompany.supabase.co")
    const val EMBEDDED_SUPABASE_URL = "https://xjclpnjejumqujhhtyxo.supabase.co"

    // Cole abaixo a Anon Key pública do seu projeto Supabase (ex: "eyJhbGciOiJIUzI1Ni...")
    const val EMBEDDED_SUPABASE_ANON_KEY = "sb_publishable_XhKxyeTQLt4XWZVlAgimsg_92nrux43"

    var supabaseUrl: String = EMBEDDED_SUPABASE_URL
        private set
    var supabaseAnonKey: String = EMBEDDED_SUPABASE_ANON_KEY
        private set

    val isConfigured: Boolean
        get() = supabaseUrl.isNotBlank() &&
                supabaseAnonKey.isNotBlank() &&
                !supabaseUrl.contains("xyzcompany") &&
                supabaseUrl.startsWith("http")

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedUrl = prefs.getString(KEY_URL, "") ?: ""
        val savedKey = prefs.getString(KEY_ANON_KEY, "") ?: ""

        // Prioridade: se houver credenciais embutidas no código, usa-as
        supabaseUrl = when {
            EMBEDDED_SUPABASE_URL.isNotBlank() -> EMBEDDED_SUPABASE_URL.trim().removeSuffix("/")
            savedUrl.isNotBlank() -> savedUrl.trim().removeSuffix("/")
            else -> ""
        }

        supabaseAnonKey = when {
            EMBEDDED_SUPABASE_ANON_KEY.isNotBlank() -> EMBEDDED_SUPABASE_ANON_KEY.trim()
            savedKey.isNotBlank() -> savedKey.trim()
            else -> ""
        }
    }

    fun updateCredentials(context: Context, url: String, anonKey: String) {
        supabaseUrl = url.trim().removeSuffix("/")
        supabaseAnonKey = anonKey.trim()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_URL, supabaseUrl)
            .putString(KEY_ANON_KEY, supabaseAnonKey)
            .apply()
    }
}
