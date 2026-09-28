package com.example.data.supabase

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * SessionHolder mantém em memória a sessão do usuário autenticado no app.
 */
object SessionHolder {
    private val _currentUser = MutableStateFlow<Usuario?>(null)
    val currentUserState: StateFlow<Usuario?> = _currentUser.asStateFlow()

    private val _currentLoja = MutableStateFlow<Loja?>(null)
    val currentLojaState: StateFlow<Loja?> = _currentLoja.asStateFlow()

    var currentUser: Usuario?
        get() = _currentUser.value
        set(value) {
            _currentUser.value = value
        }

    var currentLoja: Loja?
        get() = _currentLoja.value
        set(value) {
            _currentLoja.value = value
        }

    fun setSession(usuario: Usuario, loja: Loja? = null) {
        _currentUser.value = usuario
        _currentLoja.value = loja
    }

    fun clear() {
        com.example.data.SingleSessionManager.terminateSession()
        _currentUser.value = null
        _currentLoja.value = null
    }

    fun clearSession() {
        clear()
    }

    val isLoggedIn: Boolean
        get() = _currentUser.value != null

    val isAdm: Boolean
        get() = _currentUser.value?.cargo.equals("adm", ignoreCase = true)

    val isMaster: Boolean
        get() = _currentUser.value?.cargo.equals("master", ignoreCase = true)

    val isOperador: Boolean
        get() = _currentUser.value?.cargo.equals("operador", ignoreCase = true)
}
