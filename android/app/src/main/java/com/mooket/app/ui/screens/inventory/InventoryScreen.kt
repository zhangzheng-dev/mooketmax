package com.mooket.app.ui.screens.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIos
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mooket.app.data.model.InventoryItem
import com.mooket.app.data.model.PivotSummary
import com.mooket.app.ui.screens.inventory.components.DynamicInventoryCard
import com.mooket.app.ui.screens.inventory.components.PivotTable
import com.mooket.app.ui.theme.*

/**
 * 库存页面
 */
@Composable
fun InventoryScreen(
    onBackClick: () -> Unit,
    viewModel: InventoryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header
            InventoryHeader(
                onBackClick = onBackClick,
                onRefreshClick = { viewModel.loadInventoryData() }
            )

            // Tab栏
            InventoryTabBar(
                currentTab = uiState.currentTab,
                onTabSelected = { viewModel.selectTab(it) }
            )

            // 内容区
            when {
                uiState.isCheckingPermission -> {
                    LoadingContent(message = "检查权限中...")
                }
                !uiState.hasPermission -> {
                    ErrorContent(
                        message = uiState.error ?: "没有库存数据查看权限",
                        onRetry = { viewModel.checkPermissionAndLoad() }
                    )
                }
                uiState.isLoading -> {
                    LoadingContent(message = "加载库存数据...")
                }
                uiState.error != null -> {
                    ErrorContent(
                        message = uiState.error ?: "加载失败",
                        onRetry = { viewModel.loadInventoryData() }
                    )
                }
                else -> {
                    when (uiState.currentTab) {
                        InventoryTab.PIVOT_STANDARD -> {
                            PivotStandardContent(
                                pivotSummaries = uiState.pivotSummaries
                            )
                        }
                        InventoryTab.DYNAMIC_INVENTORY -> {
                            DynamicInventoryContent(
                                cards = uiState.dynamicInventoryCards
                            )
                        }
                        InventoryTab.INVENTORY_DETAIL -> {
                            InventoryDetailContent(
                                items = uiState.items
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InventoryHeader(
    onBackClick: () -> Unit,
    onRefreshClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onBackClick() }
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBackIos,
                contentDescription = "返回",
                tint = TextPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "库存",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = "刷新",
            tint = Primary,
            modifier = Modifier.size(24.dp).clickable { onRefreshClick() }
        )
    }
}

@Composable
private fun InventoryTabBar(
    currentTab: InventoryTab,
    onTabSelected: (InventoryTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        InventoryTab.entries.forEach { tab ->
            val isSelected = tab == currentTab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = if (isSelected) Primary else PrimaryLight.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (tab) {
                        InventoryTab.PIVOT_STANDARD -> "标准透视"
                        InventoryTab.DYNAMIC_INVENTORY -> "动态库存"
                        InventoryTab.INVENTORY_DETAIL -> "库存明细"
                    },
                    color = if (isSelected) Color.White else Primary,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun LoadingContent(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(color = Primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                color = TextHint,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = message,
                color = Error,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "点击重试",
                color = Primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { onRetry() }
            )
        }
    }
}

@Composable
private fun PivotStandardContent(
    pivotSummaries: List<PivotSummary>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "产品透视汇总",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        item {
            PivotTable(summaries = pivotSummaries)
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            // 统计信息
            if (pivotSummaries.isNotEmpty()) {
                val totalWeight = pivotSummaries.map { it.totalWeight }.sum()
                val totalItems = pivotSummaries.map { it.itemCount }.sum()
                val totalProfit = pivotSummaries.mapNotNull { it.totalProfit }.filter { it != 0.0 }.sum()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(label = "总重量", value = formatTotalWeight(totalWeight))
                    StatItem(label = "总条数", value = totalItems.toString())
                    StatItem(
                        label = "总盈利",
                        value = formatProfit(totalProfit),
                        valueColor = if (totalProfit >= 0) Primary else Error
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    valueColor: Color = TextPrimary
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = valueColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = TextHint,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun DynamicInventoryContent(
    cards: List<DynamicInventoryCardData>
) {
    if (cards.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "暂无动态库存数据",
                color = TextHint,
                fontSize = 14.sp
            )
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(cards) { card ->
                DynamicInventoryCard(data = card)
            }
        }
    }
}

@Composable
private fun InventoryDetailContent(
    items: List<InventoryItem>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "库存明细 (${items.size}条)",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        items(items.take(100)) { item ->
            InventoryDetailItem(item = item)
        }

        if (items.size > 100) {
            item {
                Text(
                    text = "...还有 ${items.size - 100} 条数据",
                    color = TextHint,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun InventoryDetailItem(item: InventoryItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.productName,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "箱号: ${item.containerId}",
                color = TextHint,
                fontSize = 11.sp
            )
        }
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = formatWeight(item.weightKg),
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${item.pieces}件",
                color = TextHint,
                fontSize = 11.sp
            )
        }
    }
}

private fun formatTotalWeight(weight: Double): String {
    return when {
        weight >= 1000 -> String.format("%.1fT", weight / 1000)
        else -> String.format("%.0fKG", weight)
    }
}

private fun formatProfit(profit: Double): String {
    val prefix = if (profit > 0) "+" else ""
    return when {
        profit >= 10000 -> String.format("%s%.1f万", prefix, profit / 10000)
        profit >= 1000 -> String.format("%s%.1fK", prefix, profit / 1000)
        else -> String.format("%s%.0f", prefix, profit)
    }
}

private fun formatWeight(weight: Double): String {
    return when {
        weight >= 1000 -> String.format("%.1fT", weight / 1000)
        else -> String.format("%.0fKG", weight)
    }
}
