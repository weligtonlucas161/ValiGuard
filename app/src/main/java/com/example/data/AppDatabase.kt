package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.AuditDao
import com.example.data.dao.ProductCatalogDao
import com.example.data.dao.ProductDao
import com.example.data.dao.ProductHistoryDao
import com.example.data.dao.SectorDao
import com.example.data.model.AuditItem
import com.example.data.model.AuditSession
import com.example.data.model.Product
import com.example.data.model.ProductCatalog
import com.example.data.model.ProductHistory
import com.example.data.model.Sector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

@Database(
    entities = [
        Product::class,
        ProductHistory::class,
        Sector::class,
        ProductCatalog::class,
        AuditSession::class,
        AuditItem::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun productHistoryDao(): ProductHistoryDao
    abstract fun sectorDao(): SectorDao
    abstract fun productCatalogDao(): ProductCatalogDao
    abstract fun auditDao(): AuditDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "supermarket_validade.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(
                        database.productDao(),
                        database.productHistoryDao(),
                        database.sectorDao(),
                        database.productCatalogDao()
                    )
                }
            }
        }

        private suspend fun populateInitialData(
            productDao: ProductDao,
            historyDao: ProductHistoryDao,
            sectorDao: SectorDao,
            catalogDao: ProductCatalogDao
        ) {
            val now = System.currentTimeMillis()

            // 1. Pre-populate default sectors
            val defaultSectors = listOf(
                "Laticínios",
                "Fiambreria",
                "Carnes",
                "Bebidas",
                "Padaria",
                "Mercearia",
                "Hortifrúti",
                "Congelados",
                "Bazar & Higiene"
            ).map { Sector(name = it, isCustom = false) }
            sectorDao.insertSectors(defaultSectors)

            fun futureDate(days: Int): Long {
                val c = Calendar.getInstance().apply {
                    timeInMillis = now
                    add(Calendar.DAY_OF_YEAR, days)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                }
                return c.timeInMillis
            }

            fun pastDate(days: Int): Long {
                val c = Calendar.getInstance().apply {
                    timeInMillis = now
                    add(Calendar.DAY_OF_YEAR, -days)
                    set(Calendar.HOUR_OF_DAY, 8)
                    set(Calendar.MINUTE, 0)
                }
                return c.timeInMillis
            }

            // 2. Initial Products (including <= 1 day for urgent removal, <= 15 days for markdown pulsation)
            val initialProducts = listOf(
                Product(
                    name = "Iogurte Natural Integral 170g",
                    category = "Laticínios",
                    section = "Iogurtes",
                    barcode = "7891000100123",
                    internalCode = "SKU-LAT-101",
                    quantity = 28,
                    unit = "un",
                    expiryDate = futureDate(8), // In 8 days -> ALERTA REBAIXA (≤15d) - Pulsando!
                    batchCode = "LOT-2024A",
                    location = "Geladeira 02 - Prateleira 3",
                    regularPrice = 4.29,
                    markdownDiscountPercent = 30,
                    notes = "Giro médio. Recomenda-se aplicar desconto de 30% na ponta de gôndola."
                ),
                Product(
                    name = "Presunto Cozido Fatiado 200g",
                    category = "Fiambreria",
                    section = "Frios & Embutidos",
                    barcode = "7891000200456",
                    internalCode = "SKU-FIA-204",
                    quantity = 15,
                    unit = "un",
                    expiryDate = futureDate(1), // In 1 day -> RETIRAR DA ÁREA DE VENDA AMANHÃ!
                    batchCode = "L-88301",
                    location = "Balcão Refrigerado 01",
                    regularPrice = 9.90,
                    markdownDiscountPercent = 40,
                    notes = "Atenção: Deve ser retirado 1 dia antes da área de venda conforme norma sanitária!"
                ),
                Product(
                    name = "Leite UHT Integral 1L",
                    category = "Laticínios",
                    section = "Leites Longa Vida",
                    barcode = "7891000300789",
                    internalCode = "SKU-LAT-309",
                    quantity = 120,
                    unit = "un",
                    expiryDate = futureDate(14), // In 14 days -> ALERTA REBAIXA (≤15d) - Pulsando!
                    batchCode = "L-4410",
                    location = "Palete Central - Corredor 3",
                    regularPrice = 5.49,
                    markdownDiscountPercent = 30
                ),
                Product(
                    name = "Pão de Forma Tradicional 500g",
                    category = "Padaria",
                    section = "Pães Industriais",
                    barcode = "7891000400112",
                    internalCode = "SKU-PAD-411",
                    quantity = 35,
                    unit = "pct",
                    expiryDate = futureDate(0), // Hoje -> Vence Hoje / RETIRAR DA ÁREA DE VENDA!
                    batchCode = "PAD-0902",
                    location = "Gôndola 04 - Padaria",
                    regularPrice = 7.80,
                    markdownDiscountPercent = 50,
                    isRemovedFromSales = false
                ),
                Product(
                    name = "Peito de Frango Resfriado 1kg",
                    category = "Carnes",
                    section = "Aves",
                    barcode = "7891000500223",
                    internalCode = "SKU-CAR-522",
                    quantity = 22,
                    unit = "kg",
                    expiryDate = futureDate(4), // In 4 days -> ALERTA REBAIXA (≤15d) - Pulsando!
                    batchCode = "AV-9011",
                    location = "Ilha Congelados / Carnes",
                    regularPrice = 18.90,
                    markdownDiscountPercent = 35
                ),
                Product(
                    name = "Refrigerante Cola 2L",
                    category = "Bebidas",
                    barcode = "7891000600334",
                    internalCode = "SKU-BEB-633",
                    quantity = 85,
                    unit = "un",
                    expiryDate = futureDate(65), // In 65 days -> Seguro
                    batchCode = "BEB-221",
                    location = "Corredor 5",
                    regularPrice = 8.50
                ),
                Product(
                    name = "Manteiga Extra com Sal 200g",
                    category = "Laticínios",
                    barcode = "7891000700445",
                    internalCode = "SKU-LAT-744",
                    quantity = 14,
                    unit = "un",
                    expiryDate = futureDate(12), // In 12 days -> ALERTA REBAIXA (≤15d) - Pulsando!
                    batchCode = "MT-03",
                    location = "Geladeira 01",
                    regularPrice = 11.90,
                    markdownDiscountPercent = 30
                ),
                Product(
                    name = "Queijo Mussarela Peça 4kg",
                    category = "Fiambreria",
                    barcode = "7891000800556",
                    internalCode = "SKU-FIA-855",
                    quantity = 4,
                    unit = "un",
                    expiryDate = pastDate(2), // Vencido há 2 dias -> Vermelho Crítico
                    batchCode = "MZ-990",
                    location = "Câmara Fria 02",
                    regularPrice = 145.00
                )
            )

            productDao.insertProducts(initialProducts)

            // 3. Pre-populate Product Catalog (for automatic barcode autofill!)
            val catalogItems = initialProducts.map { prod ->
                ProductCatalog(
                    barcode = prod.barcode,
                    name = prod.name,
                    internalCode = prod.internalCode,
                    category = prod.category,
                    unit = prod.unit,
                    regularPrice = prod.regularPrice,
                    defaultLocation = prod.location,
                    lastUpdated = now
                )
            }
            catalogDao.insertAll(catalogItems)

            // 4. Initial Audit History
            val initialHistories = listOf(
                ProductHistory(
                    productId = 1,
                    productName = "Iogurte Natural Integral 170g",
                    actionType = "ALERTA_VENCIMENTO",
                    title = "Alerta de Rebaixa Ativado",
                    details = "Produto atingiu 8 dias para vencimento (≤15 dias). Alerta de rebaixa de 30% exibido."
                ),
                ProductHistory(
                    productId = 2,
                    productName = "Presunto Cozido Fatiado 200g",
                    actionType = "RETIRADA_PENDENTE",
                    title = "Aviso de Retirada da Área de Venda",
                    details = "Falta apenas 1 dia para o vencimento. Deve ser retirado da gôndola antes do início do expediente."
                ),
                ProductHistory(
                    productId = 3,
                    productName = "Leite UHT Integral 1L",
                    actionType = "CONFERENCIA_SEGUNDA",
                    title = "Conferência de Segunda-feira",
                    details = "Estoque físico conferido: 120 unidades em gôndola. Status regular."
                )
            )
            historyDao.insertHistories(initialHistories)
        }
    }
}
