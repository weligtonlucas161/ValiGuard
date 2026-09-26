package com.example.data.repository

import com.example.data.dao.AuditDao
import com.example.data.dao.ProductCatalogDao
import com.example.data.dao.ProductDao
import com.example.data.dao.ProductHistoryDao
import com.example.data.dao.SectorDao
import com.example.data.model.AuditItem
import com.example.data.model.AuditSession
import com.example.data.model.AuditStatus
import com.example.data.model.AuditType
import com.example.data.model.Product
import com.example.data.model.ProductCatalog
import com.example.data.model.ProductHistory
import com.example.data.model.Sector
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.Flow

class ProductRepository(
    private val productDao: ProductDao,
    private val productHistoryDao: ProductHistoryDao,
    private val sectorDao: SectorDao,
    private val catalogDao: ProductCatalogDao,
    private val auditDao: AuditDao
) {
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()
    val allSectors: Flow<List<Sector>> = sectorDao.getAllSectors()

    fun getProductById(id: Long): Flow<Product?> = productDao.getProductById(id)

    fun getHistoryForProduct(productId: Long): Flow<List<ProductHistory>> =
        productHistoryDao.getHistoryForProduct(productId)

    fun getAllRecentHistory(): Flow<List<ProductHistory>> =
        productHistoryDao.getAllRecentHistory()

    // --- Product Catalog (Auto-fill by barcode) ---
    suspend fun getCatalogItemByBarcode(barcode: String): ProductCatalog? {
        if (barcode.isBlank()) return null
        return catalogDao.getByBarcode(barcode.trim())
    }

    suspend fun saveCatalogItem(
        barcode: String,
        name: String,
        internalCode: String,
        category: String,
        unit: String,
        regularPrice: Double,
        location: String
    ) {
        if (barcode.isBlank()) return
        catalogDao.insertOrUpdate(
            ProductCatalog(
                barcode = barcode.trim(),
                name = name.trim(),
                internalCode = internalCode.trim(),
                category = category.trim(),
                unit = unit.trim(),
                regularPrice = regularPrice,
                defaultLocation = location.trim(),
                lastUpdated = System.currentTimeMillis()
            )
        )
    }

    // --- Sectors Management ---
    suspend fun addCustomSector(name: String): Long {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return -1
        return sectorDao.insertSector(Sector(name = trimmed, isCustom = true))
    }

    suspend fun deleteCustomSector(sector: Sector) {
        if (sector.isCustom) {
            sectorDao.deleteSector(sector)
        }
    }

    // --- Products CRUD ---
    suspend fun insertProduct(product: Product): Long {
        val newId = productDao.insertProduct(product)

        // Save/update in product catalog for future barcode scanning
        if (product.barcode.isNotBlank()) {
            saveCatalogItem(
                barcode = product.barcode,
                name = product.name,
                internalCode = product.internalCode,
                category = product.category,
                unit = product.unit,
                regularPrice = product.regularPrice,
                location = product.location
            )
        }

        productHistoryDao.insertHistory(
            ProductHistory(
                productId = newId,
                productName = product.name,
                actionType = "CADASTRO",
                title = "Item Cadastrado no Setor",
                details = "Produto registrado: ${product.name} (Cód: ${product.internalCode.ifBlank { "N/A" }}), Lote: ${product.batchCode.ifBlank { "N/A" }}, Estoque: ${product.quantity} ${product.unit}, Validade: ${product.getFormattedExpiryDate()}.",
                quantityAtTime = product.quantity,
                expiryDateAtTime = product.expiryDate
            )
        )
        return newId
    }

    suspend fun updateProduct(product: Product, changeDescription: String? = null) {
        productDao.updateProduct(product)

        // Keep catalog updated
        if (product.barcode.isNotBlank()) {
            saveCatalogItem(
                barcode = product.barcode,
                name = product.name,
                internalCode = product.internalCode,
                category = product.category,
                unit = product.unit,
                regularPrice = product.regularPrice,
                location = product.location
            )
        }

        if (!changeDescription.isNullOrBlank()) {
            productHistoryDao.insertHistory(
                ProductHistory(
                    productId = product.id,
                    productName = product.name,
                    actionType = "EDICAO",
                    title = "Dados Atualizados",
                    details = changeDescription,
                    quantityAtTime = product.quantity,
                    expiryDateAtTime = product.expiryDate
                )
            )
        }
    }

    suspend fun deleteProduct(product: Product) {
        productDao.deleteProduct(product)
    }

    // --- Removal from Sales (Retirada 1 dia antes da área de venda) ---
    suspend fun confirmRemovalFromSales(product: Product, reason: String = "Retirada obrigatória 1 dia antes do vencimento") {
        val now = System.currentTimeMillis()
        val updated = product.copy(
            isRemovedFromSales = true,
            removalConfirmedAt = now,
            notes = if (product.notes.isNotBlank()) "${product.notes}\n[Retirado da Área de Venda em ${product.getFormattedExpiryDate()}]: $reason".trim()
            else "[Retirado da Área de Venda]: $reason"
        )
        productDao.updateProduct(updated)

        productHistoryDao.insertHistory(
            ProductHistory(
                productId = product.id,
                productName = product.name,
                actionType = "RETIRADA_AREA_VENDA",
                title = "Produto Retirado da Área de Venda",
                details = "Item retirado preventivamente da gôndola/ilha 1 dia antes do vencimento conforme norma do setor. Quantidade recolhida: ${product.quantity} ${product.unit}. Destino: Troca/Descarte/Padaria interna.",
                quantityAtTime = product.quantity,
                expiryDateAtTime = product.expiryDate
            )
        )
    }

    suspend fun confirmMarkdownRequested(product: Product, isRequested: Boolean) {
        val now = System.currentTimeMillis()
        val newCount = if (isRequested) product.markdownRequestCount + 1 else product.markdownRequestCount
        val updated = product.copy(
            markdownRequested = isRequested,
            markdownStatus = if (isRequested) "PENDING_APPROVAL" else "NOT_REQUESTED",
            markdownRequestedAt = if (isRequested) now else null,
            markdownRequestCount = newCount
        )
        productDao.updateProduct(updated)

        val action = if (isRequested) "REBAIXA_SOLICITADA" else "REBAIXA_CANCELADA"
        val title = if (isRequested) "Rebaixa de Preço Solicitada à Gerência (Tentativa #$newCount)" else "Rebaixa Marcada como Não Solicitada"
        val details = if (isRequested) {
            "Confirmação registrada no sistema: Solicitação de rebaixa encaminhada para aprovação da gerência. Lembrete diário ativado para verificar se a rebaixa foi aceita."
        } else {
            "Status atualizado: Operador registrou que a rebaixa de preço não foi solicitada para este produto."
        }

        productHistoryDao.insertHistory(
            ProductHistory(
                productId = product.id,
                productName = product.name,
                actionType = action,
                title = title,
                details = details,
                quantityAtTime = product.quantity,
                expiryDateAtTime = product.expiryDate
            )
        )
    }

    suspend fun confirmMarkdownAcceptance(product: Product, isAccepted: Boolean) {
        val now = System.currentTimeMillis()
        val statusStr = if (isAccepted) "ACCEPTED" else "REJECTED"
        val updated = product.copy(
            markdownStatus = statusStr,
            markdownCheckedAt = now
        )
        productDao.updateProduct(updated)

        val action = if (isAccepted) "REBAIXA_ACEITA" else "REBAIXA_RECUSADA"
        val title = if (isAccepted) "Rebaixa ACEITA pela Gerência" else "Rebaixa NÃO ACEITA pela Gerência (Recusada)"
        val details = if (isAccepted) {
            "Verificação diária confirmada: A gerência aprovou a rebaixa de preço! O item segue na área de venda com desconto aplicado para giro rápido."
        } else {
            "Verificação diária confirmada: A gerência não aceitou a rebaixa. O sistema emitirá um novo lembrete a cada 3 dias para solicitar uma nova rebaixa."
        }

        productHistoryDao.insertHistory(
            ProductHistory(
                productId = product.id,
                productName = product.name,
                actionType = action,
                title = title,
                details = details,
                quantityAtTime = product.quantity,
                expiryDateAtTime = product.expiryDate
            )
        )
    }

    suspend fun requestRenewedMarkdown(product: Product) {
        val now = System.currentTimeMillis()
        val newCount = product.markdownRequestCount + 1
        val updated = product.copy(
            markdownRequested = true,
            markdownStatus = "PENDING_APPROVAL",
            markdownRequestedAt = now,
            markdownRequestCount = newCount
        )
        productDao.updateProduct(updated)

        productHistoryDao.insertHistory(
            ProductHistory(
                productId = product.id,
                productName = product.name,
                actionType = "NOVA_REBAIXA_SOLICITADA",
                title = "Nova Rebaixa Solicitada (Lembrete 3 Dias - Tentativa #$newCount)",
                details = "Lembrete de 3 dias atendido: Nova proposta de rebaixa encaminhada à gerência para evitar perda por vencimento. Verificação diária reativada.",
                quantityAtTime = product.quantity,
                expiryDateAtTime = product.expiryDate
            )
        )
    }

    suspend fun requestMarkdown(product: Product, discountPercent: Int, customNotes: String) {
        confirmMarkdownRequested(product, isRequested = true)
    }

    suspend fun recordMondayAudit(product: Product, verifiedQuantity: Int, auditNotes: String) {
        val updated = product.copy(
            quantity = verifiedQuantity,
            lastAuditedDate = System.currentTimeMillis()
        )
        productDao.updateProduct(updated)

        val diff = verifiedQuantity - product.quantity
        val qtyDetail = if (diff == 0) "Estoque verificado e confirmado ($verifiedQuantity ${product.unit})"
        else "Ajuste na conferência: ${product.quantity} -> $verifiedQuantity ${product.unit} (${if (diff > 0) "+$diff" else "$diff"})"

        productHistoryDao.insertHistory(
            ProductHistory(
                productId = product.id,
                productName = product.name,
                actionType = "CONFERENCIA_SEGUNDA",
                title = "Conferência Manual de Segunda-feira",
                details = "Conferência manual antes do início do expediente. $qtyDetail. Local: ${product.location.ifBlank { "Setor" }}. ${auditNotes.trim()}".trim(),
                quantityAtTime = verifiedQuantity,
                expiryDateAtTime = product.expiryDate
            )
        )
    }

    suspend fun adjustQuantity(product: Product, delta: Int) {
        val newQuantity = (product.quantity + delta).coerceAtLeast(0)
        val updated = product.copy(quantity = newQuantity)
        productDao.updateProduct(updated)

        productHistoryDao.insertHistory(
            ProductHistory(
                productId = product.id,
                productName = product.name,
                actionType = "AJUSTE_ESTOQUE",
                title = if (delta > 0) "Entrada / Reposição de Estoque" else "Baixa / Saída de Estoque",
                details = "Quantidade alterada de ${product.quantity} para $newQuantity ${product.unit} (${if (delta > 0) "+$delta" else "$delta"}).",
                quantityAtTime = newQuantity,
                expiryDateAtTime = product.expiryDate
            )
        )
    }

    // ==========================================
    // SEÇÃO DE AUDITORIA (VALIDADE, PRESENÇA, ESTOQUE)
    // ==========================================

    fun getActiveSessionFlow(type: String): Flow<AuditSession?> = auditDao.getActiveSessionFlow(type)

    suspend fun getActiveSession(type: String): AuditSession? = auditDao.getActiveSession(type)

    fun getCompletedSessionsFlow(): Flow<List<AuditSession>> = auditDao.getCompletedSessionsFlow()

    fun getItemsForSessionFlow(sessionId: Long): Flow<List<AuditItem>> = auditDao.getItemsForSessionFlow(sessionId)

    suspend fun getItemsForSession(sessionId: Long): List<AuditItem> = auditDao.getItemsForSession(sessionId)

    suspend fun startOrResumeSession(type: String, operatorName: String): AuditSession {
        val existing = auditDao.getActiveSession(type)
        if (existing != null) return existing

        val newSession = AuditSession(
            auditType = type,
            status = AuditStatus.IN_PROGRESS.name,
            startedAt = System.currentTimeMillis(),
            operatorName = operatorName
        )
        val id = auditDao.insertSession(newSession)
        val created = newSession.copy(id = id)

        // Se for auditoria de PRESENÇA, pré-carregar produtos cadastrados como pendentes
        if (type == AuditType.PRESENCE.code) {
            val allProds = productDao.getAllProductsList()
            val initialItems = allProds.map { prod ->
                AuditItem(
                    auditSessionId = id,
                    productId = prod.id,
                    barcode = prod.barcode,
                    productName = prod.name,
                    internalCode = prod.internalCode,
                    sector = prod.location,
                    isPresent = false
                )
            }
            auditDao.insertItems(initialItems)
            auditDao.updateSession(created.copy(totalItems = initialItems.size, missingCount = initialItems.size))
        }

        return created
    }

    // Auditoria de Validade: registra conferência do produto e atualiza no banco
    suspend fun recordValidityAuditItem(
        sessionId: Long,
        barcode: String,
        productName: String,
        internalCode: String,
        sector: String,
        batch: String,
        expiryDate: Long,
        qtySales: Int,
        qtyStock: Int
    ) {
        val matchingProduct = productDao.getProductByBarcode(barcode.trim())
        val auditItem = AuditItem(
            auditSessionId = sessionId,
            productId = matchingProduct?.id,
            barcode = barcode.trim(),
            productName = productName.trim(),
            internalCode = internalCode.trim(),
            sector = sector.trim(),
            batch = batch.trim(),
            expiryDate = expiryDate,
            quantitySalesArea = qtySales,
            quantityStockArea = qtyStock,
            scannedAt = System.currentTimeMillis()
        )
        auditDao.insertItem(auditItem)

        // Se o produto já existe no cadastro, atualiza o lote, validade e estoque
        if (matchingProduct != null) {
            val totalQty = qtySales + qtyStock
            val updated = matchingProduct.copy(
                batchCode = batch.trim(),
                expiryDate = expiryDate,
                quantity = if (totalQty > 0) totalQty else matchingProduct.quantity,
                location = sector.ifBlank { matchingProduct.location },
                lastAuditedDate = System.currentTimeMillis()
            )
            productDao.updateProduct(updated)
            val formattedDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(expiryDate))
            productHistoryDao.insertHistory(
                ProductHistory(
                    productId = matchingProduct.id,
                    productName = matchingProduct.name,
                    actionType = "AUDITORIA_VALIDADE",
                    title = "Auditoria de Validade e Lote",
                    details = "Lote conferido: $batch, Validade: $formattedDate. Vendas: $qtySales un, Estoque: $qtyStock un.",
                    quantityAtTime = totalQty,
                    expiryDateAtTime = expiryDate
                )
            )
        }
    }

    // Auditoria de Presença: operador bipa produto por produto, sistema marca e retira da lista pendente
    suspend fun scanBarcodeForPresence(sessionId: Long, barcode: String): Boolean {
        val cleanBarcode = barcode.trim()
        val rowsUpdated = auditDao.markBarcodePresent(sessionId, cleanBarcode, System.currentTimeMillis())
        if (rowsUpdated > 0) {
            return true
        } else {
            // Se o produto não estava na lista inicial mas existe no cadastro ou foi bipado avulso
            val prod = productDao.getProductByBarcode(cleanBarcode)
            val newItem = AuditItem(
                auditSessionId = sessionId,
                productId = prod?.id,
                barcode = cleanBarcode,
                productName = prod?.name ?: "Produto Cód. $cleanBarcode",
                internalCode = prod?.internalCode ?: "",
                sector = prod?.location ?: "Geral",
                isPresent = true,
                scannedAt = System.currentTimeMillis()
            )
            auditDao.insertItem(newItem)
            return true
        }
    }

    suspend fun markItemPresentManually(itemId: Long) {
        auditDao.markItemPresentById(itemId, System.currentTimeMillis())
    }

    suspend fun unmarkItemPresent(itemId: Long) {
        auditDao.unmarkItemPresentById(itemId)
    }

    // Auditoria de Estoque: operador bipa produto e diz quantidade e local (Área de Vendas ou Depósito)
    suspend fun recordStockAuditItem(
        sessionId: Long,
        barcode: String,
        productName: String,
        sector: String,
        location: String,
        quantity: Int
    ) {
        val matchingProduct = productDao.getProductByBarcode(barcode.trim())
        val auditItem = AuditItem(
            auditSessionId = sessionId,
            productId = matchingProduct?.id,
            barcode = barcode.trim(),
            productName = productName.trim(),
            sector = sector.trim(),
            location = location,
            quantitySalesArea = if (location == "Área de Vendas") quantity else 0,
            quantityStockArea = if (location != "Área de Vendas") quantity else 0,
            scannedAt = System.currentTimeMillis()
        )
        auditDao.insertItem(auditItem)

        if (matchingProduct != null) {
            productHistoryDao.insertHistory(
                ProductHistory(
                    productId = matchingProduct.id,
                    productName = matchingProduct.name,
                    actionType = "AUDITORIA_ESTOQUE",
                    title = "Auditoria de Estoque Físico ($location)",
                    details = "Contagem registrada: $quantity unidades em $location.",
                    quantityAtTime = quantity,
                    expiryDateAtTime = matchingProduct.expiryDate
                )
            )
        }
    }

    suspend fun completeAuditSession(sessionId: Long, pdfPath: String): AuditSession? {
        val session = auditDao.getSessionById(sessionId) ?: return null
        val items = auditDao.getItemsForSession(sessionId)
        val presentCount = items.count { it.isPresent }
        val missingCount = items.size - presentCount

        val updated = session.copy(
            status = AuditStatus.COMPLETED.name,
            completedAt = System.currentTimeMillis(),
            totalItems = items.size,
            presentCount = presentCount,
            missingCount = missingCount,
            pdfFilePath = pdfPath
        )
        auditDao.updateSession(updated)
        return updated
    }

    suspend fun resetPresenceAuditSession(sessionId: Long) {
        // "essa lista é resetado toda vez após a finalização enquanto isso é salvo no banco de dados local até finalizar o processo"
        // Apaga os itens temporários da sessão finalizada para a próxima sessão começar limpa
        auditDao.clearItemsForSession(sessionId)
    }

    suspend fun getProductByBarcode(barcode: String): Product? {
        if (barcode.isBlank()) return null
        return productDao.getProductByBarcode(barcode.trim())
    }

    suspend fun updateProductRegularPrice(productId: Long, price: Double) {
        val product = productDao.getProductByIdSync(productId) ?: return
        val updated = product.copy(regularPrice = price)
        productDao.updateProduct(updated)
    }

    suspend fun deleteCompletedAuditSession(sessionId: Long, pdfFilePath: String?) {
        auditDao.clearItemsForSession(sessionId)
        auditDao.deleteSession(sessionId)
        if (!pdfFilePath.isNullOrBlank()) {
            try {
                val file = java.io.File(pdfFilePath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (_: Exception) {}
        }
    }
}
