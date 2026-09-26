package com.example.util

import android.util.Base64
import android.util.Log
import com.example.data.supabase.AppLog
import com.example.data.supabase.AtualizarLoja
import com.example.data.supabase.AtualizarUsuario
import com.example.data.supabase.AtualizarUsuarioCompleto
import com.example.data.supabase.Cargo
import com.example.data.supabase.Loja
import com.example.data.supabase.NovaLoja
import com.example.data.supabase.NovoProduto
import com.example.data.supabase.NovoUsuario
import com.example.data.supabase.Produto
import com.example.data.supabase.RemoteFeedback
import com.example.data.supabase.UserFeedback
import com.example.data.supabase.Usuario
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Utilitário de Criptografia e Descriptografia ponta-a-ponta (Client-Side Encryption).
 *
 * - Criptografa os dados antes de enviar ao banco de dados Supabase (AES-256-CBC).
 * - Ao inspecionar o banco no console web do Supabase, nomes, setores, códigos e detalhes
 *   aparecem como ciphertexts ("ENC:..."), impedindo a visualização desprotegida por terceiros.
 * - Descriptografa os dados de forma 100% transparente assim que são recebidos pelo app,
 *   garantindo que os usuários autenticados vejam todas as informações normais.
 * - Suporta dados legados/não criptografados: se o texto não contiver o prefixo "ENC:",
 *   retorna o valor original sem falhas.
 */
object CryptoUtils {
    private const val TAG = "CryptoUtils"
    const val PREFIX = "ENC:"
    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/CBC/PKCS5Padding"
    private const val IV_SIZE = 16

    // Chave simétrica AES-256 derivada deterministicamente via SHA-256
    private val secretKey: SecretKeySpec by lazy {
        val masterSecret = "SynkEnterprise_DataProtection_VaultKey_v2026_AES256"
        val md = MessageDigest.getInstance("SHA-256")
        val keyBytes = md.digest(masterSecret.toByteArray(Charsets.UTF_8))
        SecretKeySpec(keyBytes, ALGORITHM)
    }

    fun isEncrypted(text: String?): Boolean {
        if (text == null) return false
        return text.startsWith(PREFIX)
    }

    /**
     * Criptografa uma string usando AES-256-CBC com IV randômico para cada operação.
     * Retorna a string no formato "ENC:<Base64(IV + Ciphertext)>".
     */
    fun encrypt(plainText: String?): String {
        if (plainText == null) return ""
        if (plainText.isEmpty()) return ""
        if (plainText.startsWith(PREFIX)) return plainText // Já criptografado

        return try {
            val iv = ByteArray(IV_SIZE)
            SecureRandom().nextBytes(iv)
            val ivSpec = IvParameterSpec(iv)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            val combined = ByteArray(iv.size + encryptedBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)

            PREFIX + Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao criptografar: ${e.message}")
            plainText
        }
    }

    /**
     * Descriptografa uma string gerada pelo encrypt().
     * Se o valor não possuir o prefixo "ENC:", retorna o texto original intacto.
     */
    fun decrypt(cipherText: String?): String {
        if (cipherText == null) return ""
        if (!cipherText.startsWith(PREFIX)) return cipherText // Texto plano já legível

        return try {
            val base64Payload = cipherText.removePrefix(PREFIX)
            val combined = Base64.decode(base64Payload, Base64.NO_WRAP)
            if (combined.size < IV_SIZE) return cipherText

            val iv = ByteArray(IV_SIZE)
            System.arraycopy(combined, 0, iv, 0, IV_SIZE)
            val encryptedBytes = ByteArray(combined.size - IV_SIZE)
            System.arraycopy(combined, IV_SIZE, encryptedBytes, 0, encryptedBytes.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao descriptografar: ${e.message}")
            cipherText
        }
    }
}

// === Extensões para Produto ===
fun Produto.decrypted(): Produto = this.copy(
    nome = CryptoUtils.decrypt(this.nome),
    setor = CryptoUtils.decrypt(this.setor),
    codigo_barras = CryptoUtils.decrypt(this.codigo_barras),
    plu = this.plu?.let { CryptoUtils.decrypt(it) },
    secao = this.secao?.let { CryptoUtils.decrypt(it) },
    lote = this.lote?.let { CryptoUtils.decrypt(it) },
    localizacao = this.localizacao?.let { CryptoUtils.decrypt(it) },
    observacoes = this.observacoes?.let { CryptoUtils.decrypt(it) }
)

fun Produto.encrypted(): Produto = this.copy(
    nome = CryptoUtils.encrypt(this.nome),
    setor = CryptoUtils.encrypt(this.setor),
    codigo_barras = CryptoUtils.encrypt(this.codigo_barras),
    plu = this.plu?.let { CryptoUtils.encrypt(it) },
    secao = this.secao?.let { CryptoUtils.encrypt(it) },
    lote = this.lote?.let { CryptoUtils.encrypt(it) },
    localizacao = this.localizacao?.let { CryptoUtils.encrypt(it) },
    observacoes = this.observacoes?.let { CryptoUtils.encrypt(it) }
)

fun NovoProduto.encrypted(): NovoProduto = this.copy(
    nome = CryptoUtils.encrypt(this.nome),
    setor = CryptoUtils.encrypt(this.setor),
    codigo_barras = CryptoUtils.encrypt(this.codigo_barras),
    plu = this.plu?.let { CryptoUtils.encrypt(it) },
    secao = this.secao?.let { CryptoUtils.encrypt(it) },
    lote = this.lote?.let { CryptoUtils.encrypt(it) },
    localizacao = this.localizacao?.let { CryptoUtils.encrypt(it) },
    observacoes = this.observacoes?.let { CryptoUtils.encrypt(it) }
)

// === Extensões para Usuário ===
fun Usuario.decrypted(): Usuario {
    val decNome = CryptoUtils.decrypt(this.nome)
    val resolvedSetor = this.setor?.let { CryptoUtils.decrypt(it) }
        ?: com.example.data.CargoManager.resolveUserSector(this)
    return this.copy(
        nome = decNome,
        setor = resolvedSetor
    )
}

fun Usuario.encrypted(): Usuario = this.copy(
    nome = CryptoUtils.encrypt(this.nome),
    setor = this.setor?.let { CryptoUtils.encrypt(it) }
)

fun NovoUsuario.encrypted(): NovoUsuario = this.copy(
    nome = CryptoUtils.encrypt(this.nome),
    setor = this.setor?.let { CryptoUtils.encrypt(it) }
)

fun AtualizarUsuario.encrypted(): AtualizarUsuario = this.copy(
    nome = CryptoUtils.encrypt(this.nome),
    setor = this.setor?.let { CryptoUtils.encrypt(it) }
)

fun AtualizarUsuarioCompleto.encrypted(): AtualizarUsuarioCompleto = this.copy(
    nome = CryptoUtils.encrypt(this.nome),
    setor = this.setor?.let { CryptoUtils.encrypt(it) }
)

// === Extensões para Loja ===
fun Loja.decrypted(): Loja = this.copy(
    nome_loja = CryptoUtils.decrypt(this.nome_loja)
)

fun Loja.encrypted(): Loja = this.copy(
    nome_loja = CryptoUtils.encrypt(this.nome_loja)
)

fun NovaLoja.encrypted(): NovaLoja = this.copy(
    nome_loja = CryptoUtils.encrypt(this.nome_loja)
)

fun AtualizarLoja.encrypted(): AtualizarLoja = this.copy(
    nome_loja = CryptoUtils.encrypt(this.nome_loja)
)

// === Extensões para AppLog ===
fun AppLog.decrypted(): AppLog = this.copy(
    detalhes = CryptoUtils.decrypt(this.detalhes),
    usuario_nome = CryptoUtils.decrypt(this.usuario_nome)
)

fun AppLog.encrypted(): AppLog = this.copy(
    detalhes = CryptoUtils.encrypt(this.detalhes),
    usuario_nome = CryptoUtils.encrypt(this.usuario_nome)
)

// === Extensões para Feedbacks ===
fun RemoteFeedback.decrypted(): RemoteFeedback = this.copy(
    mensagem = CryptoUtils.decrypt(this.mensagem)
)

fun RemoteFeedback.encrypted(): RemoteFeedback = this.copy(
    mensagem = CryptoUtils.encrypt(this.mensagem)
)

fun UserFeedback.decrypted(): UserFeedback = this.copy(
    mensagem = CryptoUtils.decrypt(this.mensagem),
    usuario_nome = CryptoUtils.decrypt(this.usuario_nome),
    resposta_adm = this.resposta_adm?.let { CryptoUtils.decrypt(it) }
)

fun UserFeedback.encrypted(): UserFeedback = this.copy(
    mensagem = CryptoUtils.encrypt(this.mensagem),
    usuario_nome = CryptoUtils.encrypt(this.usuario_nome),
    resposta_adm = this.resposta_adm?.let { CryptoUtils.encrypt(it) }
)

// === Extensões para Cargo ===
fun Cargo.decrypted(): Cargo = this.copy(
    nome = CryptoUtils.decrypt(this.nome),
    setor = this.setor?.let { CryptoUtils.decrypt(it) },
    descricao = CryptoUtils.decrypt(this.descricao)
)

fun Cargo.encrypted(): Cargo = this.copy(
    nome = CryptoUtils.encrypt(this.nome),
    setor = this.setor?.let { CryptoUtils.encrypt(it) },
    descricao = CryptoUtils.encrypt(this.descricao)
)
