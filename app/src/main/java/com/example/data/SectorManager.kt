package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.data.supabase.SessionHolder
import org.json.JSONArray
import org.json.JSONObject

/**
 * Setor com seus respectivos subsetores (seções).
 * Permite que o usuário Master altere setores e crie novos subsetores.
 */
data class SetorComSubsetores(
    val nome: String,
    val subsetores: List<String>
)

object SectorManager {

    private const val PREFS_NAME = "setores_prefs"
    private const val KEY_SETORES_JSON = "key_setores_json"

    // Setores padrão com o setor de Frios detalhado conforme especificado pelo usuário
    val SETORES_PADRAO = listOf(
        SetorComSubsetores(
            nome = "Frios",
            subsetores = listOf(
                "Iogurtes",
                "Manteiga",
                "Requeijões",
                "Margarina",
                "Queijos",
                "Presuntos",
                "Salgados",
                "Congelados"
            )
        ),
        SetorComSubsetores(
            nome = "Laticínios",
            subsetores = listOf(
                "Leites",
                "Iogurtes & Bebidas Lácteas",
                "Sobremesas",
                "Queijos Frescos"
            )
        ),
        SetorComSubsetores(
            nome = "Mercearia",
            subsetores = listOf(
                "Grãos & Farináceos",
                "Massas & Molhos",
                "Enlatados & Conservas",
                "Óleos, Azeites & Vinagres",
                "Biscoitos & Snacks",
                "Matinais & Cafés"
            )
        ),
        SetorComSubsetores(
            nome = "Hortifrúti",
            subsetores = listOf(
                "Frutas",
                "Legumes",
                "Verduras & Folhagens",
                "Ovos",
                "Temperos & Ervas"
            )
        ),
        SetorComSubsetores(
            nome = "Padaria & Confeitaria",
            subsetores = listOf(
                "Pães Franceses & Especiais",
                "Bolos & Tortas",
                "Salgados Assados & Fritos",
                "Doces & Sobremesas"
            )
        ),
        SetorComSubsetores(
            nome = "Açougue & Carnes",
            subsetores = listOf(
                "Bovinos",
                "Aves & Frangos",
                "Suínos",
                "Embutidos & Linguiças",
                "Peixes & Frutos do Mar"
            )
        ),
        SetorComSubsetores(
            nome = "Bebidas",
            subsetores = listOf(
                "Refrigerantes",
                "Sucos & Néctares",
                "Cervejas & Chopp",
                "Vinhos & Destilados",
                "Águas Minerais & Energéticos"
            )
        ),
        SetorComSubsetores(
            nome = "Higiene & Limpeza",
            subsetores = listOf(
                "Higiene Pessoal",
                "Lavanderia & Sabão",
                "Limpeza Geral & Desinfetantes",
                "Papéis & Descartáveis"
            )
        )
    )

    private var setoresEmMemoria: MutableList<SetorComSubsetores>? = null

    private fun getPrefs(context: Context): SharedPreferences {
        val lojaId = SessionHolder.currentUser?.loja_id ?: "default"
        return context.getSharedPreferences("${PREFS_NAME}_$lojaId", Context.MODE_PRIVATE)
    }

    fun getSetores(context: Context): List<SetorComSubsetores> {
        setoresEmMemoria?.let { return it }

        val prefs = getPrefs(context)
        val jsonStr = prefs.getString(KEY_SETORES_JSON, null)
        if (jsonStr.isNullOrBlank()) {
            val defaults = SETORES_PADRAO.toMutableList()
            setoresEmMemoria = defaults
            salvarSetores(context, defaults)
            return defaults
        }

        try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<SetorComSubsetores>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val nome = obj.getString("nome")
                val subArray = obj.getJSONArray("subsetores")
                val subsetores = mutableListOf<String>()
                for (j in 0 until subArray.length()) {
                    subsetores.add(subArray.getString(j))
                }
                list.add(SetorComSubsetores(nome, subsetores))
            }
            setoresEmMemoria = list
            return list
        } catch (e: Exception) {
            val defaults = SETORES_PADRAO.toMutableList()
            setoresEmMemoria = defaults
            return defaults
        }
    }

    private fun salvarSetores(context: Context, list: List<SetorComSubsetores>) {
        try {
            val array = JSONArray()
            list.forEach { setor ->
                val obj = JSONObject()
                obj.put("nome", setor.nome)
                val subArray = JSONArray()
                setor.subsetores.forEach { sub -> subArray.put(sub) }
                obj.put("subsetores", subArray)
                array.put(obj)
            }
            getPrefs(context).edit().putString(KEY_SETORES_JSON, array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun adicionarOuAtualizarSetor(context: Context, nomeAntigo: String?, novoNome: String) {
        val list = getSetores(context).toMutableList()
        val index = if (nomeAntigo != null) list.indexOfFirst { it.nome.equals(nomeAntigo, ignoreCase = true) } else -1

        if (index >= 0) {
            val existente = list[index]
            list[index] = existente.copy(nome = novoNome.trim())
        } else {
            list.add(SetorComSubsetores(novoNome.trim(), emptyList()))
        }

        setoresEmMemoria = list
        salvarSetores(context, list)
    }

    fun adicionarSubsetor(context: Context, setorNome: String, novoSubsetor: String) {
        val list = getSetores(context).toMutableList()
        val index = list.indexOfFirst { it.nome.equals(setorNome, ignoreCase = true) }
        val cleanSub = novoSubsetor.trim()
        if (cleanSub.isEmpty()) return

        if (index >= 0) {
            val setor = list[index]
            if (!setor.subsetores.any { it.equals(cleanSub, ignoreCase = true) }) {
                val updatedSubs = setor.subsetores.toMutableList().apply { add(cleanSub) }
                list[index] = setor.copy(subsetores = updatedSubs)
                setoresEmMemoria = list
                salvarSetores(context, list)
            }
        }
    }

    fun removerSubsetor(context: Context, setorNome: String, subsetor: String) {
        val list = getSetores(context).toMutableList()
        val index = list.indexOfFirst { it.nome.equals(setorNome, ignoreCase = true) }
        if (index >= 0) {
            val setor = list[index]
            val updatedSubs = setor.subsetores.filterNot { it.equals(subsetor, ignoreCase = true) }
            list[index] = setor.copy(subsetores = updatedSubs)
            setoresEmMemoria = list
            salvarSetores(context, list)
        }
    }

    fun excluirSetor(context: Context, setorNome: String) {
        val list = getSetores(context).toMutableList()
        val removeu = list.removeAll { it.nome.equals(setorNome, ignoreCase = true) }
        if (removeu) {
            setoresEmMemoria = list
            salvarSetores(context, list)
        }
    }

    fun editarSubsetor(context: Context, setorNome: String, subsetorAntigo: String, novoSubsetorNome: String) {
        val list = getSetores(context).toMutableList()
        val index = list.indexOfFirst { it.nome.equals(setorNome, ignoreCase = true) }
        val cleanNovo = novoSubsetorNome.trim()
        if (cleanNovo.isEmpty() || index < 0) return

        val setor = list[index]
        val updatedSubs = setor.subsetores.map { sub ->
            if (sub.equals(subsetorAntigo, ignoreCase = true)) cleanNovo else sub
        }
        list[index] = setor.copy(subsetores = updatedSubs)
        setoresEmMemoria = list
        salvarSetores(context, list)
    }

    fun resetarSetoresPadrao(context: Context) {
        val defaults = SETORES_PADRAO.toMutableList()
        setoresEmMemoria = defaults
        salvarSetores(context, defaults)
    }

    fun getSubsetoresDoSetor(context: Context, setorNome: String): List<String> {
        val setor = getSetores(context).find { it.nome.equals(setorNome, ignoreCase = true) }
        return setor?.subsetores ?: emptyList()
    }
}
