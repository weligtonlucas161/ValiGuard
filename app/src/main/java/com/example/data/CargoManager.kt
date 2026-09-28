package com.example.data

import android.content.Context
import android.util.Log
import com.example.data.supabase.Cargo
import com.example.data.supabase.Usuario
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/**
 * Gerenciador de Cargos e Regras de Acesso por Setor.
 * Permite ao Master criar cargos customizados divididos por setores,
 * onde o operador só pode realizar ações no seu respectivo setor.
 */
object CargoManager {
    private const val PREFS_NAME = "cargos_loja_prefs"
    private const val KEY_CARGOS_JSON = "cargos_list_json"

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }
    private val listType = Types.newParameterizedType(List::class.java, Cargo::class.java)
    private val adapter by lazy { moshi.adapter<List<Cargo>>(listType) }

    // Cargos padrão pré-configurados
    val CARGOS_PADRAO = listOf(
        Cargo(nome = "Master", setor = null, descricao = "Acesso total irrestrito a todos os setores e gestão da loja"),
        Cargo(nome = "Operador Geral", setor = null, descricao = "Acesso de operação em todos os setores da loja"),
        Cargo(nome = "Operador - Hortifruti", setor = "Hortifruti", descricao = "Operações restritas ao setor de Hortifruti"),
        Cargo(nome = "Operador - Frios e Laticínios", setor = "Frios e Laticínios", descricao = "Operações restritas ao setor de Frios e Laticínios"),
        Cargo(nome = "Operador - Carnes e Açougue", setor = "Açougue", descricao = "Operações restritas ao setor de Carnes e Açougue"),
        Cargo(nome = "Operador - Padaria", setor = "Padaria", descricao = "Operações restritas ao setor de Padaria"),
        Cargo(nome = "Operador - Mercearia", setor = "Mercearia", descricao = "Operações restritas ao setor de Mercearia"),
        Cargo(nome = "Operador - Bebidas", setor = "Bebidas", descricao = "Operações restritas ao setor de Bebidas"),
        Cargo(nome = "Conferente de Perecíveis", setor = null, descricao = "Conferência e auditoria de estoques perecíveis")
    )

    /**
     * Retorna a lista de cargos da loja (permite gerenciar, editar, apagar e criar livremente).
     */
    fun getCargos(context: Context, lojaId: String?): List<Cargo> {
        val prefsKey = if (!lojaId.isNullOrBlank()) "${PREFS_NAME}_$lojaId" else PREFS_NAME
        val prefs = context.getSharedPreferences(prefsKey, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_CARGOS_JSON, null)
        if (!json.isNullOrBlank()) {
            try {
                val list = adapter.fromJson(json)
                if (!list.isNullOrEmpty()) {
                    return list
                }
            } catch (e: Exception) {
                Log.w("CargoManager", "Erro ao carregar cargos: ${e.message}")
            }
        }

        // Se não havia cargos salvos ainda para a loja, inicializa com os padrões
        val defaults = CARGOS_PADRAO.map { it.copy(loja_id = lojaId) }
        try {
            prefs.edit().putString(KEY_CARGOS_JSON, adapter.toJson(defaults)).apply()
        } catch (e: Exception) {
            Log.e("CargoManager", "Erro ao salvar cargos padrão: ${e.message}")
        }
        return defaults
    }

    /**
     * Salva ou atualiza um cargo criado ou editado pelo Master.
     */
    fun salvarCargo(context: Context, cargo: Cargo, lojaId: String? = cargo.loja_id) {
        val prefsKey = if (!lojaId.isNullOrBlank()) "${PREFS_NAME}_$lojaId" else PREFS_NAME
        val prefs = context.getSharedPreferences(prefsKey, Context.MODE_PRIVATE)
        val atuais = getCargos(context, lojaId).toMutableList()
        val index = atuais.indexOfFirst { it.id == cargo.id || it.nome.equals(cargo.nome, ignoreCase = true) }
        if (index >= 0) {
            atuais[index] = cargo.copy(loja_id = lojaId)
        } else {
            atuais.add(cargo.copy(loja_id = lojaId))
        }

        try {
            val json = adapter.toJson(atuais)
            prefs.edit().putString(KEY_CARGOS_JSON, json).apply()
        } catch (e: Exception) {
            Log.e("CargoManager", "Erro ao salvar cargo: ${e.message}")
        }
    }

    /**
     * Exclui um cargo por ID ou Nome.
     */
    fun excluirCargo(context: Context, cargoId: String, lojaId: String? = null) {
        val prefsKey = if (!lojaId.isNullOrBlank()) "${PREFS_NAME}_$lojaId" else PREFS_NAME
        val prefs = context.getSharedPreferences(prefsKey, Context.MODE_PRIVATE)
        val atuais = getCargos(context, lojaId).toMutableList()
        atuais.removeAll { it.id == cargoId }
        try {
            val json = adapter.toJson(atuais)
            prefs.edit().putString(KEY_CARGOS_JSON, json).apply()
        } catch (e: Exception) {
            Log.e("CargoManager", "Erro ao excluir cargo: ${e.message}")
        }
    }

    /**
     * Restaura os cargos padrão da loja.
     */
    fun restaurarCargosPadrao(context: Context, lojaId: String?): List<Cargo> {
        val prefsKey = if (!lojaId.isNullOrBlank()) "${PREFS_NAME}_$lojaId" else PREFS_NAME
        val prefs = context.getSharedPreferences(prefsKey, Context.MODE_PRIVATE)
        val defaults = CARGOS_PADRAO.map { it.copy(loja_id = lojaId) }
        try {
            prefs.edit().putString(KEY_CARGOS_JSON, adapter.toJson(defaults)).apply()
        } catch (e: Exception) {
            Log.e("CargoManager", "Erro ao restaurar padrões: ${e.message}")
        }
        return defaults
    }

    /**
     * Identifica o setor ao qual o usuário está restrito (se houver).
     * Se retornar null, o usuário tem acesso irrestrito a todos os setores.
     */
    fun resolveUserSector(usuario: Usuario?): String? {
        if (usuario == null) return null
        val cargoLower = usuario.cargo.lowercase()

        // ADM e Master SEMPRE têm acesso a todos os setores
        if (cargoLower == "adm" || cargoLower == "master") return null

        // 1. Campo explícito 'setor' no usuário
        val setorCampo = usuario.setor?.trim()
        if (!setorCampo.isNullOrEmpty() && !setorCampo.equals("Todos", ignoreCase = true) && !setorCampo.equals("Geral", ignoreCase = true)) {
            return setorCampo
        }

        // 2. Extração pelo nome do cargo (ex: "Operador - Hortifruti", "Operador (Açougue)")
        val delimiters = listOf(" - ", " / ", " (", ":", " – ")
        for (delimiter in delimiters) {
            if (usuario.cargo.contains(delimiter)) {
                val partes = usuario.cargo.split(delimiter)
                if (partes.size > 1) {
                    val possivelSetor = partes[1].replace(")", "").trim()
                    if (possivelSetor.isNotEmpty() && !possivelSetor.equals("Geral", ignoreCase = true)) {
                        return possivelSetor
                    }
                }
            }
        }

        return null
    }

    /**
     * Valida se o usuário tem permissão para realizar ações no produto daquele setor.
     * Regra do Usuário: "onde somente operador daquele setor poderá realizar ações somente no seu setor."
     */
    fun podeRealizarAcaoNoSetor(usuario: Usuario?, produtoSetor: String?): Boolean {
        if (usuario == null) return false
        val restrictedSector = resolveUserSector(usuario) ?: return true // null = acesso total a qualquer setor
        if (produtoSetor.isNullOrBlank()) return false

        val cleanProdSetor = produtoSetor.trim()
        val cleanRestricted = restrictedSector.trim()

        return cleanProdSetor.equals(cleanRestricted, ignoreCase = true) ||
                cleanProdSetor.contains(cleanRestricted, ignoreCase = true) ||
                cleanRestricted.contains(cleanProdSetor, ignoreCase = true)
    }
}
