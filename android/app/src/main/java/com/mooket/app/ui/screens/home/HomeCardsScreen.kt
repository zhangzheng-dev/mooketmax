package com.mooket.app.ui.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mooket.app.R
import com.mooket.app.data.model.HomeCardItem
import com.mooket.app.ui.screens.home.cards.*
import com.mooket.app.ui.theme.*

/**
 * 首页卡片页面（瀑布流展示8种卡片）
 * 支持自选数据和历史搜索数据两个 Tab
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeCardsScreen(
    onBackClick: () -> Unit,
    onProductClick: (Int, String, String) -> Unit,
    onCountryClick: (String, String) -> Unit,
    onBrandClick: (String, String) -> Unit,
    onMerchantClick: (Long, String) -> Unit,
    onFactoryClick: (String, String, String) -> Unit,
    onCountryProductClick: (String, String, String) -> Unit,
    onCountryFactoryProductClick: (String, String, String, String) -> Unit,
    onBrandProductClick: (String, String, String) -> Unit,
    viewModel: HomeCardsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val categories = listOf("牛", "猪")
    var expanded by remember { mutableStateOf(false) }
    var isEditMode by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("数据卡片") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // 品类选择器
                    Box {
                        TextButton(onClick = { expanded = true }) {
                            Text(uiState.selectedCategory)
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category) },
                                    onClick = {
                                        viewModel.selectCategory(category)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                    // 编辑按钮
                    Text(
                        text = if (isEditMode) "完成" else "编辑",
                        fontSize = 14.sp,
                        color = if (isEditMode) Primary else Color(0xFF9DA4A3),
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .clickable { isEditMode = !isEditMode }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Background)
        ) {
            // Tab 栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 自选数据 Tab
                TabItem(
                    selected = uiState.selectedTab == 0,
                    icon = Icons.Default.Star,
                    text = "自选数据",
                    onClick = {
                        viewModel.selectTab(0)
                    },
                    iconTint = if (uiState.selectedTab == 0) Primary else Color(0xFF9DA4A3),
                    textColor = if (uiState.selectedTab == 0) Primary else Color(0xFF9DA4A3),
                    customIcon = CandleChartIcon
                )
                // 历史搜索数据 Tab
                TabItem(
                    selected = uiState.selectedTab == 1,
                    icon = Icons.Default.Schedule,
                    text = "历史搜索数据",
                    onClick = {
                        viewModel.selectTab(1)
                    },
                    iconTint = if (uiState.selectedTab == 1) Primary else Color(0xFF3C4947),
                    textColor = if (uiState.selectedTab == 1) Primary else Color(0xFF3C4947)
                )
            }

            // Tab 内容
            Box(modifier = Modifier.fillMaxSize()) {
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Primary)
                    }
                } else {
                    val cards = when (uiState.selectedTab) {
                        0 -> uiState.selfSelectCards
                        else -> uiState.recentSearchCards
                    }

                    if (cards.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (uiState.selectedTab == 0) "暂无自选数据" else "暂无历史搜索数据",
                                color = TextHint,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        LazyVerticalStaggeredGrid(
                            columns = StaggeredGridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalItemSpacing = 12.dp,
                            contentPadding = PaddingValues(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(cards, key = { "${it.cardType}_${it.rank}_${it.historyId}" }) { card ->
                                HomeCard(
                                    card = card,
                                    category = uiState.selectedCategory,
                                    onProductClick = onProductClick,
                                    onCountryClick = onCountryClick,
                                    onBrandClick = onBrandClick,
                                    onMerchantClick = onMerchantClick,
                                    onFactoryClick = onFactoryClick,
                                    onCountryProductClick = onCountryProductClick,
                                    onCountryFactoryProductClick = onCountryFactoryProductClick,
                                    onBrandProductClick = onBrandProductClick,
                                    isEditMode = isEditMode,
                                    onAddToSelfSelect = if (uiState.selectedTab == 1 && card.historyId != null) {
                                        { viewModel.moveToSelfSelect(card.historyId) }
                                    } else null,
                                    onDelete = if (card.historyId != null) {
                                        {
                                            if (uiState.selectedTab == 0) {
                                                viewModel.cancelSelfSelect(card.historyId)
                                            } else {
                                                viewModel.deleteSearchHistory(card.historyId)
                                            }
                                        }
                                    } else null
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabItem(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit,
    iconTint: Color = if (selected) Primary else TextHint,
    textColor: Color = if (selected) Primary else TextHint,
    customIcon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = customIcon ?: icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = iconTint
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                fontSize = 14.sp,
                color = textColor,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        // 选中指示条
        Box(
            modifier = Modifier
                .height(3.dp)
                .width(if (selected) 16.dp else 0.dp)
                .background(
                    color = if (selected) Primary else Color.Transparent,
                    shape = RoundedCornerShape(12.dp)
                )
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeCard(
    card: HomeCardItem,
    category: String,
    onProductClick: (Int, String, String) -> Unit,
    onCountryClick: (String, String) -> Unit,
    onBrandClick: (String, String) -> Unit,
    onMerchantClick: (Long, String) -> Unit,
    onFactoryClick: (String, String, String) -> Unit,
    onCountryProductClick: (String, String, String) -> Unit,
    onCountryFactoryProductClick: (String, String, String, String) -> Unit,
    onBrandProductClick: (String, String, String) -> Unit,
    isEditMode: Boolean = false,
    onAddToSelfSelect: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    Box {
        when (card.cardType) {
            "product" -> ProductCard(
                card = card,
                onClick = {
                    card.productId?.let { id ->
                        onProductClick(id, category, card.productName ?: "")
                    }
                }
            )
            "country" -> CountryCard(
                card = card,
                onClick = {
                    card.country?.let { country ->
                        onCountryClick(country, category)
                    }
                }
            )
            "brand" -> BrandCard(
                card = card,
                onClick = {
                    card.brandName?.let { name ->
                        onBrandClick(name, category)
                    }
                }
            )
            "merchant" -> MerchantCard(
                card = card,
                onClick = {
                    card.merchantId?.let { id ->
                        onMerchantClick(id, category)
                    }
                }
            )
            "factory" -> FactoryCard(
                card = card,
                onClick = {
                    card.country?.let { country ->
                        card.factoryNo?.let { factoryNo ->
                            onFactoryClick(country, factoryNo, category)
                        }
                    }
                }
            )
            "brandProduct" -> BrandProductCard(
                card = card,
                onClick = if (card.brandName == null || card.productName == null) null else ({
                    onBrandProductClick(card.brandName!!, card.productName!!, category)
                })
            )
            "factoryProduct" -> FactoryProductCard(
                card = card,
                onClick = if (card.country == null || card.factoryNo == null || card.productName == null) null else ({
                    onCountryFactoryProductClick(card.country!!, card.factoryNo!!, card.productName!!, category)
                })
            )
            "countryProduct" -> CountryProductCard(
                card = card,
                onClick = if (card.country == null || card.productName == null) null else ({
                    onCountryProductClick(card.country!!, card.productName!!, category)
                })
            )
            else -> {
                // Unknown card type, render empty box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(Color.White, RoundedCornerShape(8.dp))
                )
            }
        }

        // 编辑模式下的右上角操作按钮
        if (isEditMode) {
            Row(
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 添加到自选按钮
                if (onAddToSelfSelect != null) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(Color(0xFFEFF5F3), RoundedCornerShape(4.dp))
                            .clickable { onAddToSelfSelect() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_add_to_self_select),
                            contentDescription = "添加到自选",
                            tint = Primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                // 删除按钮
                if (onDelete != null) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(Color(0xFFE7F5F3), RoundedCornerShape(4.dp))
                            .clickable { onDelete() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_delete_history),
                            contentDescription = "删除",
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
