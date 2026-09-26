package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ProductionQuantityLimits
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Product
import com.example.ui.theme.BlueExpressive
import com.example.ui.theme.BlueExpressiveContainer
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceContainer
import com.example.ui.theme.DarkSurfaceContainerHigh
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.ExpressiveChipShape
import com.example.ui.theme.RedExpressive
import com.example.ui.theme.RedExpressiveContainer
import com.example.ui.viewmodel.StockSortOption
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Aba "Estoque" (Requirement 7):
 * - Exibe todos os produtos da loja do usuário (inclusive produtos com estoque zerado).
 * - Filtros avançados de ordenação:
 *   - Validade (crescente/decrescente)
 *   - Quantidade em estoque (maior/menor, e apenas zerados)
 *   - Idade do cadastro (mais novo/mais antigo)
 *   - Filtro por Setor e Seção
 * - Sistema de pesquisa global padronizado (Requirement 8) por Descrição, Código de Barras e PLU.
 * - Ação para importação de planilhas CSV/Excel (Requirement 6).
 */
@Composable
fun StockScreen(
    nomeLoja: String,
    products: List<Product>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onScanClick: () -> Unit,
    selectedSort: StockSortOption,
    onSortChange: (StockSortOption) -> Unit,
    selectedSector: String,
    onSectorChange: (String) -> Unit,
    allSectors: List<String>,
    selectedSection: String,
    onSectionChange: (String) -> Unit,
    allSections: List<String>,
    onProductClick: (Product) -> Unit,
    onAddProductClick: () -> Unit,
    onImportSpreadsheetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalItems = products.size
    val totalUnits = products.sumOf { it.quantity }
    val zeroStockCount = products.count { it.quantity == 0 }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("stock_screen"),
        containerColor = DarkSurface,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddProductClick,
                containerColor = BlueExpressive,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("stock_fab_add_product")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Cadastrar Produto no Estoque")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header com Resumo de KPIs do Estoque
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkSurfaceContainer,
                border = BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Estoque Geral da Loja",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = DarkTextPrimary
                            )
                            Text(
                                text = "Container: $nomeLoja",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF60A5FA)
                            )
                        }

                        // Botão de Importar Planilha CSV
                        OutlinedButton(
                            onClick = onImportSpreadsheetClick,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = BlueExpressive),
                            border = BorderStroke(1.dp, BlueExpressive.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("stock_import_sheet_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.UploadFile,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Importar Planilha", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3 KPI Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Total Cadastrados
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainerHigh),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Itens Únicos", fontSize = 10.sp, color = DarkTextSecondary)
                                Text(
                                    text = "$totalItems",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkTextPrimary
                                )
                            }
                        }

                        // Saldo Total
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainerHigh),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Saldo Total", fontSize = 10.sp, color = DarkTextSecondary)
                                Text(
                                    text = "$totalUnits un",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldSafe
                                )
                            }
                        }

                        // Estoque Zerado
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = if (zeroStockCount > 0) RedExpressiveContainer.copy(alpha = 0.3f) else DarkSurfaceContainerHigh
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Zerados (0 un)", fontSize = 10.sp, color = if (zeroStockCount > 0) RedExpressive else DarkTextSecondary)
                                Text(
                                    text = "$zeroStockCount",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (zeroStockCount > 0) RedExpressive else DarkTextPrimary
                                )
                            }
                        }
                    }
                }
            }

            // BARRA DE PESQUISA GLOBAL PADRONIZADA (Requirement 8)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkSurface,
                border = BorderStroke(0.5.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("global_search_input"),
                        placeholder = {
                            Text(
                                "Buscar por Descrição, Código de Barras ou PLU...",
                                fontSize = 12.sp,
                                color = DarkTextSecondary
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Pesquisar",
                                tint = DarkTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(
                                        onClick = { onSearchChange("") },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Limpar busca",
                                            tint = DarkTextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                // Botão de bipagem com a câmera ou leitor
                                Surface(
                                    shape = CircleShape,
                                    color = BlueExpressiveContainer,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clickable { onScanClick() }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.QrCodeScanner,
                                            contentDescription = "Bipar código",
                                            tint = BlueExpressive,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkSurfaceContainer,
                            unfocusedContainerColor = DarkSurfaceContainer,
                            focusedBorderColor = BlueExpressive,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = DarkTextPrimary,
                            unfocusedTextColor = DarkTextPrimary
                        )
                    )
                }
            }

            // BARRA DE FILTROS E ORDENAÇÃO AVANÇADA (Requirement 7)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurfaceContainer)
                    .padding(vertical = 6.dp)
            ) {
                // Chips de Ordenação
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ordenar:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkTextSecondary
                    )

                    StockSortOption.entries.forEach { option ->
                        val isSelected = selectedSort == option
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSortChange(option) },
                            label = { Text(option.label, fontSize = 11.sp) },
                            shape = ExpressiveChipShape,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BlueExpressive,
                                selectedLabelColor = Color.White,
                                containerColor = DarkSurfaceContainerHigh,
                                labelColor = DarkTextSecondary
                            ),
                            border = BorderStroke(1.dp, if (isSelected) BlueExpressive else DarkBorder)
                        )
                    }
                }

                // Chips de Setor e Seção
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Setor:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkTextSecondary
                    )

                    val sectorsOptions = listOf("Todos") + allSectors
                    sectorsOptions.forEach { sec ->
                        val isSelected = (sec == "Todos" && selectedSector == "Todos") || selectedSector.equals(sec, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSectorChange(sec) },
                            label = { Text(sec, fontSize = 11.sp) },
                            shape = ExpressiveChipShape,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF3B82F6).copy(alpha = 0.25f),
                                selectedLabelColor = Color(0xFF93C5FD),
                                containerColor = DarkSurfaceContainerHigh,
                                labelColor = DarkTextSecondary
                            ),
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFF3B82F6) else DarkBorder)
                        )
                    }

                    if (allSections.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Seção:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkTextSecondary
                        )

                        val sectionsOptions = listOf("Todas") + allSections
                        sectionsOptions.forEach { s ->
                            val isSelected = (s == "Todas" && selectedSection == "Todas") || selectedSection.equals(s, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSectionChange(s) },
                                label = { Text(s, fontSize = 11.sp) },
                                shape = ExpressiveChipShape,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF8B5CF6).copy(alpha = 0.25f),
                                    selectedLabelColor = Color(0xFFC4B5FD),
                                    containerColor = DarkSurfaceContainerHigh,
                                    labelColor = DarkTextSecondary
                                ),
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFF8B5CF6) else DarkBorder)
                            )
                        }
                    }
                }
            }

            // LISTA DE PRODUTOS DO ESTOQUE
            if (products.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = DarkSurfaceContainerHigh,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Inventory2,
                                    contentDescription = null,
                                    tint = DarkTextSecondary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Nenhum produto encontrado no estoque",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = DarkTextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Cadastre manualmente ou importe uma planilha para alimentar o estoque desta loja.",
                            style = MaterialTheme.typography.bodySmall,
                            color = DarkTextSecondary,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = onImportSpreadsheetClick,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = BlueExpressive)
                            ) {
                                Text("Importar Planilha")
                            }
                            Button(
                                onClick = onAddProductClick,
                                colors = ButtonDefaults.buttonColors(containerColor = BlueExpressive)
                            ) {
                                Text("+ Cadastrar Item")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(top = 10.dp, bottom = 80.dp)
                ) {
                    items(products, key = { it.id }) { product ->
                        StockProductItemCard(
                            product = product,
                            onClick = { onProductClick(product) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Card informativo de cada produto no estoque.
 * Destaca visualmente itens com estoque zerado e datas de validade.
 */
@Composable
fun StockProductItemCard(
    product: Product,
    onClick: () -> Unit
) {
    val isZeroStock = product.quantity == 0
    val daysRemaining = product.getDaysRemaining()
    val sdf = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val formattedExpiry = remember(product.expiryDate) { sdf.format(Date(product.expiryDate)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("stock_item_${product.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isZeroStock) DarkSurfaceContainer.copy(alpha = 0.95f) else DarkSurfaceContainer
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (isZeroStock) RedExpressive.copy(alpha = 0.5f) else DarkBorder
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Linha Superior: Categoria / Setor + Tags de Estoque
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF3B82F6).copy(alpha = 0.15f),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Text(
                            text = product.category.ifBlank { "Geral" },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF93C5FD),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (product.section.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF8B5CF6).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = product.section,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC4B5FD),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Badge de Quantidade em Estoque
                if (isZeroStock) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = RedExpressiveContainer,
                        border = BorderStroke(1.dp, RedExpressive)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ProductionQuantityLimits,
                                contentDescription = null,
                                tint = RedExpressive,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "ESTOQUE ZERADO",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = RedExpressive
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "${product.quantity} ${product.unit}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = EmeraldSafe,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Descrição do Produto
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = DarkTextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Linha Inferior: PLU/Código de Barras + Validade
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Códigos
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (product.internalCode.isNotBlank()) {
                        Text(
                            text = "PLU: ${product.internalCode}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF60A5FA),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    if (product.barcode.isNotBlank()) {
                        Text(
                            text = "EAN: ${product.barcode}",
                            fontSize = 11.sp,
                            color = DarkTextSecondary
                        )
                    }
                }

                // Data de Validade
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = when {
                            daysRemaining < 0 -> RedExpressive
                            daysRemaining <= 1 -> Color(0xFFD946EF)
                            daysRemaining <= 15 -> Color(0xFFF59E0B)
                            else -> DarkTextSecondary
                        },
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$formattedExpiry (${if (daysRemaining < 0) "Vencido" else "${daysRemaining}d"})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            daysRemaining < 0 -> RedExpressive
                            daysRemaining <= 1 -> Color(0xFFD946EF)
                            daysRemaining <= 15 -> Color(0xFFF59E0B)
                            else -> DarkTextSecondary
                        }
                    )
                }
            }
        }
    }
}
