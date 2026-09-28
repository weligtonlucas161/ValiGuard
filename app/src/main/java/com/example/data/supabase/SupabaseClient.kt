package com.example.data.supabase

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID
import java.util.concurrent.TimeUnit

class SupabaseException(message: String) : Exception(message)

/**
 * Cliente Supabase configurado com a URL e a chave pública (via BuildConfig),
 * implementando as instâncias para Postgrest e GoTrue (auth) conforme necessário
 * para persistir os dados no banco de dados Supabase.
 */
class SupabaseClient(
    var baseUrl: String = BuildConfig.SUPABASE_URL.ifBlank { SupabaseConfig.supabaseUrl },
    var anonKey: String = BuildConfig.SUPABASE_ANON_KEY.ifBlank { SupabaseConfig.supabaseAnonKey }
) {
    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val postgrest: PostgrestClient
        get() = PostgrestClient(baseUrl, anonKey, okHttpClient, moshi)

    val auth: GoTrueClient
        get() = GoTrueClient(baseUrl, anonKey, okHttpClient, moshi)

    fun from(table: String): PostgrestTable = postgrest.from(table)

    fun updateCredentials(newUrl: String, newKey: String) {
        baseUrl = newUrl.trim().removeSuffix("/")
        anonKey = newKey.trim()
    }

    /**
     * Semeia dados iniciais (loja e usuários padrão) caso a tabela esteja vazia.
     */
    suspend fun seedInitialData(): String = withContext(Dispatchers.IO) {
        val cleanUrl = baseUrl.trim().removeSuffix("/")
        val cleanKey = anonKey.trim()

        if (!cleanUrl.startsWith("http") || cleanKey.isBlank()) {
            throw SupabaseException("Configure a URL e a Anon Key do Supabase primeiro.")
        }

        // 1. Verificar ou criar Loja Matriz
        val existingLojas = try {
            from("lojas").select().decodeList<Loja>()
        } catch (e: Exception) {
            emptyList()
        }

        val lojaId = if (existingLojas.isEmpty()) {
            val novaLoja = NovaLoja(nome_loja = "Loja Matriz Centro", cor_borda = "#2563EB", ativa = true)
            val inserted = from("lojas").insert(novaLoja) { select() }.decodeSingle<Loja>()
            inserted.id
        } else {
            existingLojas.first().id
        }

        // 2. Verificar e criar usuários padrão
        val existingUsers = try {
            from("usuarios").select().decodeList<Usuario>()
        } catch (e: Exception) {
            emptyList()
        }

        val standardUsers = listOf(
            NovoUsuario(matricula = "111111", nome = "Administrador Geral", cargo = "adm", loja_id = lojaId, ativo = true),
            NovoUsuario(matricula = "123456", nome = "Gerente de Validade", cargo = "master", loja_id = lojaId, ativo = true),
            NovoUsuario(matricula = "654321", nome = "Operador de Perecíveis", cargo = "operador", loja_id = lojaId, ativo = true)
        )

        var createdCount = 0
        for (u in standardUsers) {
            if (existingUsers.none { it.matricula == u.matricula }) {
                try {
                    from("usuarios").insert(u)
                    createdCount++
                } catch (e: Exception) {
                    Log.w("SupabaseClient", "Usuário ${u.matricula} já pode existir ou erro: ${e.message}")
                }
            }
        }

        "Conexão com Postgrest ativa! Loja id: ${lojaId.take(8)}... ($createdCount usuários adicionados)"
    }

    companion object {
        val instance = SupabaseClient()
    }
}

/**
 * Instância para operações Postgrest (tabelas e consultas).
 */
class PostgrestClient(
    private val baseUrl: String,
    private val anonKey: String,
    private val okHttpClient: OkHttpClient,
    val moshi: Moshi
) {
    fun from(tableName: String): PostgrestTable = PostgrestTable(tableName, baseUrl, anonKey, okHttpClient, moshi)
}

/**
 * Instância para GoTrue (Auth do Supabase).
 */
class GoTrueClient(
    private val baseUrl: String,
    private val anonKey: String,
    private val okHttpClient: OkHttpClient,
    private val moshi: Moshi
) {
    private var currentAccessToken: String? = null

    fun setToken(token: String?) {
        currentAccessToken = token
    }

    fun getToken(): String? = currentAccessToken

    suspend fun pingAuth(): Boolean = withContext(Dispatchers.IO) {
        val cleanUrl = baseUrl.trim().removeSuffix("/")
        val request = Request.Builder()
            .url("$cleanUrl/auth/v1/health")
            .addHeader("apikey", anonKey)
            .get()
            .build()
        try {
            okHttpClient.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            false
        }
    }
}

class PostgrestTable(
    val tableName: String,
    private val baseUrl: String,
    private val anonKey: String,
    private val okHttpClient: OkHttpClient,
    val moshi: Moshi
) {
    suspend fun <T : Any> insert(
        item: T,
        builder: (InsertOptions.() -> Unit)? = null
    ): PostgrestInsertResult {
        val options = InsertOptions().apply { builder?.invoke(this) }
        return withContext(Dispatchers.IO) {
            val cleanUrl = baseUrl.trim().removeSuffix("/")
            val cleanKey = anonKey.trim()

            if (!cleanUrl.startsWith("http") || cleanKey.isBlank()) {
                throw SupabaseException("Credenciais do Supabase não configuradas no BuildConfig.")
            }

            val jsonBody = moshi.adapter(item.javaClass).toJson(item)
            val mediaType = "application/json; charset=utf-8".toMediaType()

            val requestBuilder = Request.Builder()
                .url("$cleanUrl/rest/v1/$tableName")
                .addHeader("apikey", cleanKey)
                .addHeader("Authorization", "Bearer $cleanKey")
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")

            if (options.shouldSelect) {
                requestBuilder.addHeader("Prefer", "return=representation")
            }

            val request = requestBuilder
                .post(jsonBody.toRequestBody(mediaType))
                .build()

            try {
                okHttpClient.newCall(request).execute().use { response ->
                    val respString = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        val detail = if (respString.isNotBlank()) respString else response.message
                        throw SupabaseException("Falha ao inserir na tabela '$tableName' (HTTP ${response.code}): $detail")
                    }
                    PostgrestInsertResult(respString, moshi)
                }
            } catch (e: SupabaseException) {
                throw e
            } catch (e: Exception) {
                throw SupabaseException("Erro de conexão ao inserir em '$tableName': ${e.localizedMessage ?: e.message}")
            }
        }
    }

    suspend fun select(
        filterBuilder: (FilterBuilder.() -> Unit)? = null
    ): PostgrestSelectResult {
        val filter = FilterBuilder().apply { filterBuilder?.invoke(this) }
        return withContext(Dispatchers.IO) {
            val cleanUrl = baseUrl.trim().removeSuffix("/")
            val cleanKey = anonKey.trim()

            if (!cleanUrl.startsWith("http") || cleanKey.isBlank()) {
                throw SupabaseException("Credenciais do Supabase não configuradas no BuildConfig.")
            }

            val queryParams = StringBuilder("select=*")
            filter.filters.forEach { (col, opValue) ->
                queryParams.append("&").append(col).append("=").append(opValue)
            }
            filter.orderParam?.let {
                queryParams.append("&order=").append(it)
            }
            filter.limitParam?.let {
                queryParams.append("&limit=").append(it)
            }

            val request = Request.Builder()
                .url("$cleanUrl/rest/v1/$tableName?$queryParams")
                .addHeader("apikey", cleanKey)
                .addHeader("Authorization", "Bearer $cleanKey")
                .addHeader("Accept", "application/json")
                .get()
                .build()

            try {
                okHttpClient.newCall(request).execute().use { response ->
                    val respString = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        val detail = if (respString.isNotBlank()) respString else response.message
                        throw SupabaseException("Falha ao consultar tabela '$tableName' (HTTP ${response.code}): $detail")
                    }
                    PostgrestSelectResult(respString, moshi)
                }
            } catch (e: SupabaseException) {
                throw e
            } catch (e: Exception) {
                throw SupabaseException("Erro de conexão ao consultar '$tableName': ${e.localizedMessage ?: e.message}")
            }
        }
    }

    suspend fun <T : Any> update(
        item: T,
        filterBuilder: (FilterBuilder.() -> Unit)? = null
    ): PostgrestInsertResult {
        val filter = FilterBuilder().apply { filterBuilder?.invoke(this) }
        return withContext(Dispatchers.IO) {
            val cleanUrl = baseUrl.trim().removeSuffix("/")
            val cleanKey = anonKey.trim()

            val queryParams = StringBuilder()
            filter.filters.forEach { (col, opValue) ->
                if (queryParams.isNotEmpty()) queryParams.append("&")
                queryParams.append(col).append("=").append(opValue)
            }

            val jsonBody = moshi.adapter(item.javaClass).toJson(item)
            val mediaType = "application/json; charset=utf-8".toMediaType()

            val url = if (queryParams.isNotEmpty()) {
                "$cleanUrl/rest/v1/$tableName?$queryParams"
            } else {
                "$cleanUrl/rest/v1/$tableName"
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", cleanKey)
                .addHeader("Authorization", "Bearer $cleanKey")
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .addHeader("Prefer", "return=representation")
                .patch(jsonBody.toRequestBody(mediaType))
                .build()

            try {
                okHttpClient.newCall(request).execute().use { response ->
                    val respString = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        throw SupabaseException("Falha ao atualizar '$tableName' (HTTP ${response.code}): $respString")
                    }
                    PostgrestInsertResult(respString, moshi)
                }
            } catch (e: SupabaseException) {
                throw e
            } catch (e: Exception) {
                throw SupabaseException("Erro ao atualizar '$tableName': ${e.localizedMessage ?: e.message}")
            }
        }
    }

    suspend fun delete(
        filterBuilder: (FilterBuilder.() -> Unit)? = null
    ): Boolean {
        val filter = FilterBuilder().apply { filterBuilder?.invoke(this) }
        return withContext(Dispatchers.IO) {
            val cleanUrl = baseUrl.trim().removeSuffix("/")
            val cleanKey = anonKey.trim()

            val queryParams = StringBuilder()
            filter.filters.forEach { (col, opValue) ->
                if (queryParams.isNotEmpty()) queryParams.append("&")
                queryParams.append(col).append("=").append(opValue)
            }

            val url = if (queryParams.isNotEmpty()) {
                "$cleanUrl/rest/v1/$tableName?$queryParams"
            } else {
                "$cleanUrl/rest/v1/$tableName"
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", cleanKey)
                .addHeader("Authorization", "Bearer $cleanKey")
                .delete()
                .build()

            try {
                okHttpClient.newCall(request).execute().use { it.isSuccessful }
            } catch (e: Exception) {
                false
            }
        }
    }

    suspend fun <T : Any> upsert(
        item: T,
        onConflict: String? = null
    ): PostgrestInsertResult {
        return withContext(Dispatchers.IO) {
            val cleanUrl = baseUrl.trim().removeSuffix("/")
            val cleanKey = anonKey.trim()

            if (!cleanUrl.startsWith("http") || cleanKey.isBlank()) {
                throw SupabaseException("Credenciais do Supabase não configuradas no BuildConfig.")
            }

            val jsonBody = moshi.adapter(item.javaClass).toJson(item)
            val mediaType = "application/json; charset=utf-8".toMediaType()

            val url = if (!onConflict.isNullOrBlank()) {
                "$cleanUrl/rest/v1/$tableName?on_conflict=$onConflict"
            } else {
                "$cleanUrl/rest/v1/$tableName"
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", cleanKey)
                .addHeader("Authorization", "Bearer $cleanKey")
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates,return=representation")
                .post(jsonBody.toRequestBody(mediaType))
                .build()

            try {
                okHttpClient.newCall(request).execute().use { response ->
                    val respString = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        val detail = if (respString.isNotBlank()) respString else response.message
                        throw SupabaseException("Falha ao fazer upsert em '$tableName' (HTTP ${response.code}): $detail")
                    }
                    PostgrestInsertResult(respString, moshi)
                }
            } catch (e: SupabaseException) {
                throw e
            } catch (e: Exception) {
                throw SupabaseException("Erro ao fazer upsert em '$tableName': ${e.localizedMessage ?: e.message}")
            }
        }
    }
}

class InsertOptions {
    var shouldSelect: Boolean = false
    fun select() {
        shouldSelect = true
    }
}

class FilterBuilder {
    val filters = mutableMapOf<String, String>()
    var orderParam: String? = null
    var limitParam: Int? = null

    fun eq(column: String, value: Any) {
        filters[column] = "eq.$value"
    }

    fun neq(column: String, value: Any) {
        filters[column] = "neq.$value"
    }

    fun gt(column: String, value: Any) {
        filters[column] = "gt.$value"
    }

    fun gte(column: String, value: Any) {
        filters[column] = "gte.$value"
    }

    fun lt(column: String, value: Any) {
        filters[column] = "lt.$value"
    }

    fun lte(column: String, value: Any) {
        filters[column] = "lte.$value"
    }

    fun order(column: String, ascending: Boolean = true) {
        orderParam = "$column.${if (ascending) "asc" else "desc"}"
    }

    fun limit(count: Int) {
        limitParam = count
    }
}

class PostgrestInsertResult(
    val rawJson: String,
    val moshi: Moshi
) {
    inline fun <reified T : Any> decodeSingle(): T {
        val clean = rawJson.trim()
        if (clean.startsWith("[") && clean.endsWith("]")) {
            val listType = Types.newParameterizedType(List::class.java, T::class.java)
            val list = moshi.adapter<List<T>>(listType).fromJson(clean)
            return list?.firstOrNull()
                ?: throw SupabaseException("O Supabase retornou uma lista vazia após inserção: $clean")
        }
        return moshi.adapter(T::class.java).fromJson(clean)
            ?: throw SupabaseException("Falha ao converter resposta do Supabase: $clean")
    }
}

class PostgrestSelectResult(
    val rawJson: String,
    val moshi: Moshi
) {
    inline fun <reified T : Any> decodeList(): List<T> {
        val clean = rawJson.trim()
        if (clean.isBlank() || clean == "[]") return emptyList()
        val listType = Types.newParameterizedType(List::class.java, T::class.java)
        return moshi.adapter<List<T>>(listType).fromJson(clean) ?: emptyList()
    }

    inline fun <reified T : Any> decodeSingleOrNull(): T? {
        val clean = rawJson.trim()
        if (clean.isBlank() || clean == "[]") return null
        val listType = Types.newParameterizedType(List::class.java, T::class.java)
        val list = moshi.adapter<List<T>>(listType).fromJson(clean)
        return list?.firstOrNull()
    }
}
