package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.model.AuditItem
import com.example.data.model.AuditSession
import com.example.data.model.AuditType
import com.example.data.model.Product
import com.example.data.model.ProductCatalog
import com.example.data.model.ProductHistory
import com.example.data.model.Sector
import com.example.data.repository.ProductRepository
import com.example.notification.MondayReminderScheduler
import com.example.util.AuditPdfGenerator
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreenTab {
    INVENTORY,
    STOCK,
    MONITORING,
    AUDIT,
    MONDAY_AUDIT
}

enum class StockSortOption(val label: String) {
    EXPIRY_ASC("Validade: Próxima primeiro"),
    EXPIRY_DESC("Validade: Mais distante"),
    QTY_DESC("Estoque: Maior primeiro"),
    QTY_ASC("Estoque: Menor primeiro"),
    ONLY_ZERO("Estoque Zerado"),
    NEWEST("Cadastro: Mais recente"),
    OLDEST("Cadastro: Mais antigo")
}

enum class StatusFilter(val label: String, val chipEmoji: String) {
    ALL("Todos", "📦"),
    EXPIRING_NEXT_DAY("Vence Amanhã", "🟣"),
    EXPIRING_UNDER_15_DAYS("Vence ≤15d", "🟡"),
    EXPIRED_NOT_REMOVED("Vencidos sem Baixa", "⛔"),
    MARKDOWN_REQUESTED("Rebaixa Solicitada", "📤"),
    AWAITING_REVIEW("Aguarda Rebaixa", "⏳"),
    MARKDOWN_APPROVED("Rebaixa Aprovada", "🟢"),
    MUST_REMOVE_1_DAY("Retirar ≤1d", "🚨"),
    IN_MARKDOWN("Em Rebaixa", "🏷️"),
    MARKDOWN_REBAIXA("Rebaixa ≤15d", "🏷️"),
    CRITICAL_7_DAYS("Vence ≤7d", "⚠️"),
    EXPIRED("Vencidos", "⛔"),
    SAFE("No Prazo", "✅")
}

data class AuthUser(
    val username: String,
    val registrationNumber: String,
    val role: String = "operador",
    val lojaId: String? = null,
    val nomeLoja: String? = null,
    val supabaseId: String? = null,
    val ativo: Boolean = true,
    val loginTimeMillis: Long = System.currentTimeMillis()
)

data class DashboardMetrics(
    val totalCount: Int = 0,
    val totalStockQuantity: Int = 0, // Quantidade total de produtos em estoque físico/depósito
    val totalSalesQuantity: Int = 0, // Quantidade de produtos sendo apresentados em área de vendas
    val expiringUnder15DaysCount: Int = 0, // Vencimento inferior a 15 dias (< 15 dias)
    val expiringNextDayCount: Int = 0, // Vencimento no próximo dia (≤ 1 dia / amanhã)
    val expiredNotRemovedCount: Int = 0, // Venceu e não foi dada baixa/retirada de área
    val markdownRequestedCount: Int = 0, // Sendo solicitada a rebaixa
    val awaitingMarkdownCount: Int = 0, // Aguarda rebaixa (pendente de resposta/conferência)
    val markdownApprovedCount: Int = 0, // Teve a rebaixa aprovada/aceita
    val urgentRemovalCount: Int = 0, // Vencem em breve (≤1 dia / Roxo)
    val awaitingReviewCount: Int = 0, // Aguardam conferência / aceite (Amarelo)
    val inMarkdownCount: Int = 0, // Total em rebaixa aceita / ativa (Verde)
    val markdownNeededCount: Int = 0, // Faltando <= 15 dias
    val criticalCount: Int = 0, // Faltando <= 7 dias
    val expiredCount: Int = 0,
    val safeCount: Int = 0,
    val removedFromSalesCount: Int = 0
)

data class MonitoringMetrics(
    val urgentRemovalCount: Int = 0, // Precisa sair da gôndola hoje/amanhã
    val markdownPendingCount: Int = 0, // Faltando <= 15 dias sem pedido de rebaixa
    val dailyCheckCount: Int = 0, // Lembrete diário: rebaixas aguardando verificar se foi aceita
    val renewIn3DaysCount: Int = 0, // Lembrete a cada 3 dias: precisam de nova solicitação de rebaixa
    val removedCount: Int = 0, // Já retirados preventivamente
    val expiredCount: Int = 0 // Já vencidos
)

class ProductViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ProductRepository

    init {
        val database = AppDatabase.getDatabase(application, viewModelScope)
        repository = ProductRepository(
            database.productDao(),
            database.productHistoryDao(),
            database.sectorDao(),
            database.productCatalogDao(),
            database.auditDao()
        )
        MondayReminderScheduler.createNotificationChannel(application)
        MondayReminderScheduler.scheduleMondayReminder(application, 7, 0)
    }

    val allProductsRaw: StateFlow<List<Product>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSectors: StateFlow<List<Sector>> = repository.allSectors
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sectors: StateFlow<List<Sector>> = allSectors

    private val _currentTab = MutableStateFlow(AppScreenTab.INVENTORY)
    val currentTab = _currentTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Todos")
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _selectedSection = MutableStateFlow("Todas")
    val selectedSection = _selectedSection.asStateFlow()

    val allSections: StateFlow<List<String>> = allProductsRaw.map { products ->
        products.map { it.section.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedStatusFilter = MutableStateFlow(StatusFilter.ALL)
    val selectedStatusFilter = _selectedStatusFilter.asStateFlow()

    private val _selectedProduct = MutableStateFlow<Product?>(null)
    val selectedProduct = _selectedProduct.asStateFlow()

    private val _selectedProductHistory = MutableStateFlow<List<ProductHistory>>(emptyList())
    val selectedProductHistory = _selectedProductHistory.asStateFlow()

    private val _isAddEditProductOpen = MutableStateFlow(false)
    val isAddEditProductOpen = _isAddEditProductOpen.asStateFlow()

    private val _productToEdit = MutableStateFlow<Product?>(null)
    val productToEdit = _productToEdit.asStateFlow()

    private val _isAddSectorDialogOpen = MutableStateFlow(false)
    val isAddSectorDialogOpen = _isAddSectorDialogOpen.asStateFlow()

    private val _isMondayAuditOpen = MutableStateFlow(false)
    val isMondayAuditOpen = _isMondayAuditOpen.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage = _snackbarMessage.asStateFlow()

    private val _isSyncingRemote = MutableStateFlow(false)
    val isSyncingRemote = _isSyncingRemote.asStateFlow()

    // Auth State & User Session
    private val _currentUser = MutableStateFlow<AuthUser?>(AuthUser(username = "Admin", registrationNumber = "000000", role = "ADMIN"))
    val currentUser: StateFlow<AuthUser?> = _currentUser.asStateFlow()

    private val _isAuthenticating = MutableStateFlow(false)
    val isAuthenticating: StateFlow<Boolean> = _isAuthenticating.asStateFlow()

    private val _authErrorMessage = MutableStateFlow<String?>(null)
    val authErrorMessage: StateFlow<String?> = _authErrorMessage.asStateFlow()

    // Filtros e ordenação da aba Estoque (Requirement 7)
    private val _selectedStockSort = MutableStateFlow(StockSortOption.EXPIRY_ASC)
    val selectedStockSort: StateFlow<StockSortOption> = _selectedStockSort.asStateFlow()

    private val _selectedStockSector = MutableStateFlow("Todos")
    val selectedStockSector: StateFlow<String> = _selectedStockSector.asStateFlow()

    private val _selectedStockSection = MutableStateFlow("Todas")
    val selectedStockSection: StateFlow<String> = _selectedStockSection.asStateFlow()

    fun selectStockSort(option: StockSortOption) { _selectedStockSort.value = option }
    fun selectStockSector(sector: String) { _selectedStockSector.value = sector }
    fun selectStockSection(section: String) { _selectedStockSection.value = section }

    // Dashboard metrics
    val metrics: StateFlow<DashboardMetrics> = allProductsRaw.combine(_selectedCategory) { products, _ ->
        var total = 0
        var totalStock = 0
        var totalSales = 0
        var expiringUnder15 = 0
        var expiringNextDay = 0
        var expiredNotRemoved = 0
        var markdownRequested = 0
        var awaitingMarkdown = 0
        var markdownApproved = 0
        var urgentRemoval = 0
        var awaitingReview = 0
        var inMarkdown = 0
        var markdown = 0
        var critical = 0
        var expired = 0
        var safe = 0
        var removed = 0
        val now = System.currentTimeMillis()

        for (p in products) {
            total++
            val isAreaDeVendas = !p.isRemovedFromSales && !p.location.contains("estoque", ignoreCase = true) && !p.location.contains("depósito", ignoreCase = true) && !p.location.contains("deposito", ignoreCase = true)
            if (isAreaDeVendas) {
                totalSales += p.quantity
            } else {
                totalStock += p.quantity
            }

            if (p.isRemovedFromSales) {
                removed++
            }
            val days = p.getDaysRemaining(now)

            // Vencimento inferior a 15 dias (< 15 dias e não vencido ainda)
            if (!p.isRemovedFromSales && days in 0..14) {
                expiringUnder15++
            }
            // Vencimento no próximo dia (≤ 1 dia / amanhã)
            if (!p.isRemovedFromSales && days in 0..1) {
                expiringNextDay++
            }
            // Venceu e não foi dada baixa de retirada da área de vendas
            if (!p.isRemovedFromSales && days < 0) {
                expiredNotRemoved++
            }
            // Quantidade de produtos que está sendo solicitada a rebaixa
            if (p.markdownRequested) {
                markdownRequested++
            }
            // Quantidade de produtos que aguarda rebaixa
            if (p.markdownRequested && (p.markdownStatus == "PENDING_APPROVAL" || p.markdownStatus == "REQUESTED")) {
                awaitingMarkdown++
            }
            // Quantidade de produto que teve a rebaixa aprovada
            if (p.markdownStatus == "ACCEPTED") {
                markdownApproved++
            }

            // Contagem dos estados legados
            if (days in 0..1 && !p.isRemovedFromSales) {
                urgentRemoval++
            }
            if (!p.isRemovedFromSales && (p.isDueForDailyAcceptanceCheck() || (!p.markdownRequested && days in 0..15))) {
                awaitingReview++
            }
            if (!p.isRemovedFromSales && p.markdownStatus == "ACCEPTED") {
                inMarkdown++
            }

            when {
                days < 0 -> expired++
                days <= 1 -> {
                    critical++
                    markdown++
                }
                days <= 7 -> {
                    critical++
                    markdown++
                }
                days <= 15 -> markdown++
                days > 30 -> safe++
            }
        }
        DashboardMetrics(
            totalCount = total,
            totalStockQuantity = totalStock,
            totalSalesQuantity = totalSales,
            expiringUnder15DaysCount = expiringUnder15,
            expiringNextDayCount = expiringNextDay,
            expiredNotRemovedCount = expiredNotRemoved,
            markdownRequestedCount = markdownRequested,
            awaitingMarkdownCount = awaitingMarkdown,
            markdownApprovedCount = markdownApproved,
            urgentRemovalCount = urgentRemoval,
            awaitingReviewCount = awaitingReview,
            inMarkdownCount = inMarkdown,
            markdownNeededCount = markdown,
            criticalCount = critical,
            expiredCount = expired,
            safeCount = safe,
            removedFromSalesCount = removed
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardMetrics())

    // Monitoring metrics for dedicated second interface
    val monitoringMetrics: StateFlow<MonitoringMetrics> = allProductsRaw.combine(_searchQuery) { products, _ ->
        val now = System.currentTimeMillis()
        var urgent = 0
        var markdownPending = 0
        var dailyCheck = 0
        var renewIn3Days = 0
        var removed = 0
        var expired = 0

        for (p in products) {
            val days = p.getDaysRemaining(now)
            if (p.isRemovedFromSales) {
                removed++
            } else if (days < 0) {
                expired++
            } else if (days <= 1) {
                urgent++
            } else if (days <= 15) {
                if (!p.markdownRequested || p.markdownStatus == "NOT_REQUESTED") {
                    markdownPending++
                }
                if (p.isDueForDailyAcceptanceCheck()) {
                    dailyCheck++
                }
                if (p.isDueForNewMarkdownRequest(now) && p.markdownRequested) {
                    renewIn3Days++
                }
            }
        }
        MonitoringMetrics(
            urgentRemovalCount = urgent,
            markdownPendingCount = markdownPending,
            dailyCheckCount = dailyCheck,
            renewIn3DaysCount = renewIn3Days,
            removedCount = removed,
            expiredCount = expired
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonitoringMetrics())

    // Products filtered for General Grid
    val filteredProducts: StateFlow<List<Product>> = combine(
        allProductsRaw,
        _searchQuery,
        _selectedCategory,
        _selectedSection,
        _selectedStatusFilter
    ) { products, query, category, section, statusFilter ->
        val now = System.currentTimeMillis()
        products.filter { product ->
            // Category filter (Setor)
            val matchesCategory = category == "Todos" || product.category.equals(category, ignoreCase = true)

            // Section filter (Seção)
            val matchesSection = section == "Todas" || product.section.equals(section, ignoreCase = true)

            // Search filter (name, barcode, internalCode, batchCode, location, section)
            val matchesQuery = query.isBlank() ||
                    product.name.contains(query, ignoreCase = true) ||
                    product.barcode.contains(query, ignoreCase = true) ||
                    product.internalCode.contains(query, ignoreCase = true) ||
                    product.batchCode.contains(query, ignoreCase = true) ||
                    product.location.contains(query, ignoreCase = true) ||
                    product.section.contains(query, ignoreCase = true)

            // Status filter
            val days = product.getDaysRemaining(now)
            val matchesStatus = when (statusFilter) {
                StatusFilter.ALL -> true
                StatusFilter.EXPIRING_NEXT_DAY -> days in 0..1 && !product.isRemovedFromSales
                StatusFilter.EXPIRING_UNDER_15_DAYS -> days in 0..14 && !product.isRemovedFromSales
                StatusFilter.EXPIRED_NOT_REMOVED -> days < 0 && !product.isRemovedFromSales
                StatusFilter.MARKDOWN_REQUESTED -> product.markdownRequested
                StatusFilter.AWAITING_REVIEW -> product.markdownRequested && (product.markdownStatus == "PENDING_APPROVAL" || product.markdownStatus == "REQUESTED")
                StatusFilter.MARKDOWN_APPROVED -> product.markdownStatus == "ACCEPTED"
                StatusFilter.MUST_REMOVE_1_DAY -> days in 0..1 && !product.isRemovedFromSales
                StatusFilter.IN_MARKDOWN -> !product.isRemovedFromSales && product.markdownStatus == "ACCEPTED"
                StatusFilter.MARKDOWN_REBAIXA -> days in 0..15
                StatusFilter.CRITICAL_7_DAYS -> days in 0..7
                StatusFilter.EXPIRED -> days < 0
                StatusFilter.SAFE -> days > 30
            }

            matchesCategory && matchesSection && matchesQuery && matchesStatus
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Helper internal data class for Stock filters
    private data class StockFilters(
        val query: String,
        val sortOption: StockSortOption,
        val sector: String,
        val section: String,
        val lojaId: String
    )

    // Products filtered for the Stock tab (Requirement 7)
    val filteredStockProducts: StateFlow<List<Product>> = combine(
        allProductsRaw,
        combine(
            _searchQuery,
            _selectedStockSort,
            _selectedStockSector,
            _selectedStockSection,
            _currentUser
        ) { q, sort, sec, section, user ->
            StockFilters(q, sort, sec, section, user?.lojaId ?: "")
        }
    ) { products, filters ->
        val userLojaId = filters.lojaId.trim()

        // Multi-tenant: Isolamento estrito por loja_id
        val storeProducts = if (userLojaId.isNotBlank()) {
            products.filter { it.lojaId.isBlank() || it.lojaId.equals(userLojaId, ignoreCase = true) }
        } else {
            products
        }

        // Pesquisa global por Descrição, Código de Barras e PLU (Requirement 8)
        val queryFiltered = if (filters.query.isBlank()) {
            storeProducts
        } else {
            val q = filters.query.trim()
            storeProducts.filter { p ->
                p.name.contains(q, ignoreCase = true) ||
                p.barcode.contains(q, ignoreCase = true) ||
                p.internalCode.contains(q, ignoreCase = true)
            }
        }

        // Filtro por Setor
        val sectorFiltered = if (filters.sector == "Todos") {
            queryFiltered
        } else {
            queryFiltered.filter { it.category.equals(filters.sector, ignoreCase = true) }
        }

        // Filtro por Seção
        val sectionFiltered = if (filters.section == "Todas") {
            sectorFiltered
        } else {
            sectorFiltered.filter { it.section.equals(filters.section, ignoreCase = true) }
        }

        // Ordenação avançada (Requirement 7)
        when (filters.sortOption) {
            StockSortOption.EXPIRY_ASC -> sectionFiltered.sortedBy { it.expiryDate }
            StockSortOption.EXPIRY_DESC -> sectionFiltered.sortedByDescending { it.expiryDate }
            StockSortOption.QTY_DESC -> sectionFiltered.sortedByDescending { it.quantity }
            StockSortOption.QTY_ASC -> sectionFiltered.sortedBy { it.quantity }
            StockSortOption.ONLY_ZERO -> sectionFiltered.filter { it.quantity == 0 }
            StockSortOption.NEWEST -> sectionFiltered.sortedByDescending { it.createdAt }
            StockSortOption.OLDEST -> sectionFiltered.sortedBy { it.createdAt }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Products filtered for the Monitoring & Removal Interface
    val monitoringProducts: StateFlow<List<Product>> = combine(
        allProductsRaw,
        _searchQuery
    ) { products, query ->
        val now = System.currentTimeMillis()
        products.filter { product ->
            val days = product.getDaysRemaining(now)
            // Itens no radar de monitoramento: vencendo em até 15 dias, ou já vencidos, ou já retirados
            val isMonitoringItem = days <= 15 || product.isRemovedFromSales || days < 0
            val matchesQuery = query.isBlank() ||
                    product.name.contains(query, ignoreCase = true) ||
                    product.barcode.contains(query, ignoreCase = true) ||
                    product.internalCode.contains(query, ignoreCase = true) ||
                    product.location.contains(query, ignoreCase = true)

            isMonitoringItem && matchesQuery
        }.sortedWith(
            compareBy<Product> {
                // Prioridade 0: Faltando 1 dia ou hoje (não retirado) -> Urgência máxima!
                val days = it.getDaysRemaining(now)
                when {
                    !it.isRemovedFromSales && days in 0..1 -> 0
                    !it.isRemovedFromSales && days < 0 -> 1
                    !it.isRemovedFromSales && days in 2..7 -> 2
                    !it.isRemovedFromSales && days in 8..15 -> 3
                    else -> 4 // Já retirados
                }
            }.thenBy { it.expiryDate }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Tab Navigation ---
    fun selectTab(tab: AppScreenTab) {
        _currentTab.value = tab
    }

    // --- Search & Filters ---
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelected(category: String) {
        _selectedCategory.value = category
    }

    fun onSectionSelected(section: String) {
        _selectedSection.value = section
    }

    fun onStatusFilterSelected(filter: StatusFilter) {
        _selectedStatusFilter.value = filter
    }

    // --- Product Selection & Details ---
    fun selectProduct(product: Product) {
        _selectedProduct.value = product
        viewModelScope.launch {
            repository.getHistoryForProduct(product.id).collect { historyList ->
                _selectedProductHistory.value = historyList
            }
        }
    }

    fun clearSelectedProduct() {
        _selectedProduct.value = null
        _selectedProductHistory.value = emptyList()
    }

    // --- Product & Catalog Lookup (Auto-fill by barcode/PLU) ---
    suspend fun lookupProductByBarcode(barcode: String): Product? {
        return repository.getProductByBarcode(barcode)
    }

    suspend fun lookupCatalogByBarcode(barcode: String): ProductCatalog? {
        return repository.getCatalogItemByBarcode(barcode)
    }

    // --- Sectors Management ---
    fun openAddSectorDialog() {
        _isAddSectorDialogOpen.value = true
    }

    fun closeAddSectorDialog() {
        _isAddSectorDialogOpen.value = false
    }

    fun addCustomSector(sectorName: String) {
        viewModelScope.launch {
            val id = repository.addCustomSector(sectorName)
            if (id > 0) {
                _selectedCategory.value = sectorName.trim()
                _snackbarMessage.value = "Setor \"${sectorName.trim()}\" criado com sucesso!"
            } else {
                _snackbarMessage.value = "Nome de setor inválido ou já existente."
            }
            closeAddSectorDialog()
        }
    }

    fun deleteCustomSector(sector: Sector) {
        viewModelScope.launch {
            repository.deleteCustomSector(sector)
            if (_selectedCategory.value == sector.name) {
                _selectedCategory.value = "Todos"
            }
            _snackbarMessage.value = "Setor \"${sector.name}\" removido."
        }
    }

    private fun canOperateOnCategory(categoryOrSector: String?): Boolean {
        val user = com.example.data.supabase.SessionHolder.currentUser ?: return true
        return com.example.data.CargoManager.podeRealizarAcaoNoSetor(user, categoryOrSector)
    }

    // --- Removal from Sales Floor (Retirada 1 dia antes da área de venda) ---
    fun confirmRemovalFromSales(product: Product, reason: String = "Retirada obrigatória 1 dia antes do vencimento") {
        viewModelScope.launch {
            if (!canOperateOnCategory(product.category)) {
                val allowedSector = com.example.data.CargoManager.resolveUserSector(com.example.data.supabase.SessionHolder.currentUser)
                _snackbarMessage.value = "⛔ Operação bloqueada: Seu cargo permite alterações apenas no setor '$allowedSector'."
                return@launch
            }
            repository.confirmRemovalFromSales(product, reason)
            val updated = product.copy(
                isRemovedFromSales = true,
                removalConfirmedAt = System.currentTimeMillis()
            )
            if (_selectedProduct.value?.id == product.id) {
                _selectedProduct.value = updated
            }
            _snackbarMessage.value = "✅ \"${product.name}\" retirado da área de venda!"
        }
    }

    // --- Dialogs ---
    fun openAddProduct(prefilledBarcode: String = "") {
        _productToEdit.value = null
        _isAddEditProductOpen.value = true
    }

    fun openEditProduct(product: Product) {
        _productToEdit.value = product
        _isAddEditProductOpen.value = true
    }

    fun closeAddEditProduct() {
        _isAddEditProductOpen.value = false
        _productToEdit.value = null
    }

    fun openMondayAudit() {
        _isMondayAuditOpen.value = true
    }

    fun closeMondayAudit() {
        _isMondayAuditOpen.value = false
    }

    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
    }

    fun showSnackbar(message: String) {
        _snackbarMessage.value = message
    }

    // --- Save Product (Creates / Updates & Syncs Catalog) ---
    fun saveProduct(
        id: Long = 0,
        name: String,
        category: String,
        section: String = "",
        barcode: String,
        internalCode: String,
        quantity: Int,
        unit: String,
        expiryDate: Long,
        batchCode: String,
        location: String,
        regularPrice: Double,
        markdownDiscountPercent: Int,
        notes: String
    ) {
        viewModelScope.launch {
            if (!canOperateOnCategory(category)) {
                val allowedSector = com.example.data.CargoManager.resolveUserSector(com.example.data.supabase.SessionHolder.currentUser)
                _snackbarMessage.value = "⛔ Operação bloqueada: Seu cargo permite alterações apenas no setor '$allowedSector'."
                return@launch
            }
            if (id == 0L) {
                val newProduct = Product(
                    name = name,
                    category = category,
                    section = section,
                    barcode = barcode,
                    internalCode = internalCode,
                    quantity = quantity,
                    unit = unit,
                    expiryDate = expiryDate,
                    batchCode = batchCode,
                    location = location,
                    regularPrice = regularPrice,
                    markdownDiscountPercent = markdownDiscountPercent,
                    notes = notes
                )
                repository.insertProduct(newProduct)
                _snackbarMessage.value = "Produto \"$name\" cadastrado com sucesso!"
            } else {
                val existing = _productToEdit.value ?: _selectedProduct.value
                val existingSupabaseId = existing?.supabaseId
                val updatedProduct = Product(
                    id = id,
                    name = name,
                    category = category,
                    section = section.ifBlank { existing?.section ?: "" },
                    barcode = barcode,
                    internalCode = internalCode,
                    quantity = quantity,
                    unit = unit,
                    expiryDate = expiryDate,
                    batchCode = batchCode,
                    location = location,
                    regularPrice = regularPrice,
                    markdownDiscountPercent = markdownDiscountPercent,
                    notes = notes,
                    markdownRequested = existing?.markdownRequested ?: false,
                    markdownStatus = existing?.markdownStatus ?: "NONE",
                    markdownRequestedAt = existing?.markdownRequestedAt,
                    isRemovedFromSales = existing?.isRemovedFromSales ?: false,
                    removalConfirmedAt = existing?.removalConfirmedAt,
                    supabaseId = existingSupabaseId,
                    lastAuditedDate = existing?.lastAuditedDate ?: System.currentTimeMillis()
                )
                repository.updateProduct(updatedProduct, "Atualização de dados cadastrais/validade.")
                if (_selectedProduct.value?.id == id) {
                    _selectedProduct.value = updatedProduct
                }
                _snackbarMessage.value = "Produto \"$name\" atualizado com sucesso!"
            }
            closeAddEditProduct()
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            if (!canOperateOnCategory(product.category)) {
                val allowedSector = com.example.data.CargoManager.resolveUserSector(com.example.data.supabase.SessionHolder.currentUser)
                _snackbarMessage.value = "⛔ Operação bloqueada: Seu cargo permite alterações apenas no setor '$allowedSector'."
                return@launch
            }
            repository.deleteProduct(product)
            if (_selectedProduct.value?.id == product.id) {
                clearSelectedProduct()
            }
            _snackbarMessage.value = "Produto \"${product.name}\" excluído."
        }
    }

    fun requestPriceMarkdown(product: Product, discountPercent: Int = 30, customNotes: String = "") {
        confirmMarkdownRequested(product, isRequested = true)
    }

    fun confirmMarkdownRequested(product: Product, isRequested: Boolean) {
        viewModelScope.launch {
            if (!canOperateOnCategory(product.category)) {
                val allowedSector = com.example.data.CargoManager.resolveUserSector(com.example.data.supabase.SessionHolder.currentUser)
                _snackbarMessage.value = "⛔ Operação bloqueada: Seu cargo permite alterações apenas no setor '$allowedSector'."
                return@launch
            }
            repository.confirmMarkdownRequested(product, isRequested)
            val now = System.currentTimeMillis()
            val newCount = if (isRequested) product.markdownRequestCount + 1 else product.markdownRequestCount
            val updated = product.copy(
                markdownRequested = isRequested,
                markdownStatus = if (isRequested) "PENDING_APPROVAL" else "NOT_REQUESTED",
                markdownRequestedAt = if (isRequested) now else null,
                markdownRequestCount = newCount
            )
            if (_selectedProduct.value?.id == product.id) {
                _selectedProduct.value = updated
            }
            _snackbarMessage.value = if (isRequested) {
                "🏷️ Rebaixa de \"${product.name}\" confirmada como solicitada! Lembrete diário ativado."
            } else {
                "Rebaixa marcada como não solicitada para \"${product.name}\"."
            }
        }
    }

    fun confirmMarkdownAcceptance(product: Product, isAccepted: Boolean) {
        viewModelScope.launch {
            repository.confirmMarkdownAcceptance(product, isAccepted)
            val updated = product.copy(
                markdownStatus = if (isAccepted) "ACCEPTED" else "REJECTED",
                markdownCheckedAt = System.currentTimeMillis()
            )
            if (_selectedProduct.value?.id == product.id) {
                _selectedProduct.value = updated
            }
            _snackbarMessage.value = if (isAccepted) {
                "✅ Rebaixa de \"${product.name}\" confirmada como ACEITA pela gerência!"
            } else {
                "❌ Rebaixa de \"${product.name}\" NÃO aceita. Lembrete a cada 3 dias ativado."
            }
        }
    }

    fun requestRenewedMarkdown(product: Product) {
        viewModelScope.launch {
            repository.requestRenewedMarkdown(product)
            val updated = product.copy(
                markdownRequested = true,
                markdownStatus = "PENDING_APPROVAL",
                markdownRequestedAt = System.currentTimeMillis(),
                markdownRequestCount = product.markdownRequestCount + 1
            )
            if (_selectedProduct.value?.id == product.id) {
                _selectedProduct.value = updated
            }
            _snackbarMessage.value = "🔄 Nova rebaixa solicitada para \"${product.name}\"! Lembrete diário de aceitação reativado."
        }
    }

    fun adjustQuantity(product: Product, delta: Int) {
        viewModelScope.launch {
            repository.adjustQuantity(product, delta)
            val newQty = (product.quantity + delta).coerceAtLeast(0)
            val updated = product.copy(quantity = newQty)
            if (_selectedProduct.value?.id == product.id) {
                _selectedProduct.value = updated
            }
            _snackbarMessage.value = "Estoque de \"${product.name}\": $newQty ${product.unit}"
        }
    }

    fun recordMondayAuditItem(product: Product, verifiedQuantity: Int, notes: String) {
        viewModelScope.launch {
            repository.recordMondayAudit(product, verifiedQuantity, notes)
            val updated = product.copy(
                quantity = verifiedQuantity,
                lastAuditedDate = System.currentTimeMillis()
            )
            if (_selectedProduct.value?.id == product.id) {
                _selectedProduct.value = updated
            }
        }
    }

    fun triggerTestMondayNotification() {
        MondayReminderScheduler.showMondayNotification(getApplication())
        _snackbarMessage.value = "Notificação de teste de Segunda-feira enviada!"
    }

    fun triggerDailyMarkdownCheckNotification() {
        val count = monitoringMetrics.value.dailyCheckCount
        if (count > 0) {
            MondayReminderScheduler.showDailyMarkdownCheckNotification(getApplication(), count)
            _snackbarMessage.value = "Notificação de lembrete diário enviada ($count produto(s))!"
        } else {
            _snackbarMessage.value = "Nenhum produto aguardando verificação diária de rebaixa."
        }
    }

    fun triggerRenewMarkdownReminderNotification() {
        val count = monitoringMetrics.value.renewIn3DaysCount
        if (count > 0) {
            MondayReminderScheduler.showRenewMarkdownReminderNotification(getApplication(), count)
            _snackbarMessage.value = "Notificação de ciclo de 3 dias enviada ($count produto(s))!"
        } else {
            _snackbarMessage.value = "Nenhum produto pendente para novo ciclo de 3 dias."
        }
    }

    fun dismissCreatedStoreConfirmation() {}

    fun login(username: String, registrationNumber: String) {
        _currentUser.value = AuthUser(username = username, registrationNumber = registrationNumber)
    }

    fun logout() {
        _currentUser.value = null
    }
}
