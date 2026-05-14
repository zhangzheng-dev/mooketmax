package com.mooket.app.ui.screens.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIos
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

private val InventoryBg = Color(0xFFF5F5F5)
private val InventoryPanel = Color.White
private val InventoryPanelSoft = Color(0xFFFAFAFA)
private val InventoryMutedPanel = Color(0xFFF7F8FA)
private val InventoryText = Color(0xFF171C22)
private val InventorySubText = Color(0xFF5E6670)
private val InventoryMuted = Color(0xFF7A818B)
private val InventoryBorder = Color(0xFFE5E7EB)
private val InventoryAccent = Color(0xFFF6C914)
private val InventoryGreen = Color(0xFF006A61)
private val InventoryPositive = Color(0xFFEF3B45)
private val InventoryNegative = Color(0xFF0F9466)
private val PivotNameWidth = 128.dp
private val PivotColumnWidths = listOf(58.dp, 62.dp, 62.dp, 88.dp, 88.dp, 50.dp, 74.dp, 76.dp, 76.dp)

private enum class FooterIconType {
    STATUS,
    FILTER,
    ACTIVITY
}

@Composable
fun InventoryScreen(
    onBackClick: () -> Unit,
    viewModel: InventoryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(InventoryBg)
            .statusBarsPadding()
    ) {
        when {
            uiState.isCheckingPermission -> CenterState("检查权限中...")
            !uiState.hasPermission -> CenterState(uiState.error ?: "当前账号没有库存数据查看权限", onRetry = viewModel::checkPermissionAndLoad)
            uiState.isLoading -> CenterState("正在同步库存数据...")
            uiState.error != null -> CenterState(uiState.error ?: "加载失败", onRetry = viewModel::loadInventoryData)
            else -> InventoryContent(
                uiState = uiState,
                onBackClick = onBackClick,
                onRefresh = viewModel::loadInventoryData,
                onTabSelected = viewModel::selectTab,
                onPivotSearch = viewModel::setPivotSearchQuery,
                onDetailSearch = viewModel::setDetailSearchQuery,
                onToggleShowKg = viewModel::toggleShowKg,
                onDynamicGroupByToggle = viewModel::toggleDynamicGroupBy,
                onDetailSort = viewModel::selectDetailSort
            )
        }
    }
}

@Composable
private fun InventoryContent(
    uiState: InventoryUiState,
    onBackClick: () -> Unit,
    onRefresh: () -> Unit,
    onTabSelected: (InventoryTab) -> Unit,
    onPivotSearch: (String) -> Unit,
    onDetailSearch: (String) -> Unit,
    onToggleShowKg: () -> Unit,
    onDynamicGroupByToggle: (DynamicGroupBy) -> Unit,
    onDetailSort: (DetailSortKey) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        item {
            InventoryTopBar(onBackClick = onBackClick, onRefresh = onRefresh)
        }
        item {
            TopOverviewHeader(
                summary = uiState.analytics.summary
            )
        }
        item {
            TopModuleTabs(currentTab = uiState.currentTab, onChange = onTabSelected)
        }

        when (uiState.currentTab) {
            InventoryTab.PIVOT_STANDARD -> {
                item {
                    PivotControlSection(
                        query = uiState.pivotSearchQuery,
                        showKg = uiState.showKg,
                        summary = uiState.analytics.summary,
                        onQueryChange = onPivotSearch,
                        onToggleShowKg = onToggleShowKg
                    )
                }
                item {
                    PivotTableSection(
                        products = uiState.analytics.products,
                        showKg = uiState.showKg
                    )
                }
            }

            InventoryTab.DYNAMIC_INVENTORY -> {
                item {
                    DynamicStickySection(
                        summary = uiState.dynamicSummary,
                        groupBys = uiState.dynamicGroupBys,
                        onToggle = onDynamicGroupByToggle
                    )
                }
                item {
                    DynamicCardsGrid(cards = uiState.dynamicGroupCards)
                }
            }

            InventoryTab.INVENTORY_DETAIL -> {
                item {
                    DetailSearchAndSort(
                        query = uiState.detailSearchQuery,
                        sortKey = uiState.detailSortKey,
                        sortDirection = uiState.detailSortDirection,
                        onQueryChange = onDetailSearch,
                        onSort = onDetailSort
                    )
                }
                item {
                    val filtered = remember(uiState.analytics.detailRows, uiState.detailSearchQuery, uiState.detailSortKey, uiState.detailSortDirection) {
                        val q = uiState.detailSearchQuery.trim().lowercase()
                        val rows = if (q.isBlank()) {
                            uiState.analytics.detailRows
                        } else {
                            uiState.analytics.detailRows.filter {
                                listOf(it.containerId, it.contractId, it.factoryCode, it.productName, it.skuCode)
                                    .any { value -> value.lowercase().contains(q) }
                            }
                        }
                        sortDetailRows(rows, uiState.detailSortKey, uiState.detailSortDirection)
                    }
                    DetailTable(rows = filtered)
                }
            }
        }
    }
}

@Composable
private fun InventoryTopBar(onBackClick: () -> Unit, onRefresh: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(InventoryBg)
            .height(54.dp)
            .padding(horizontal = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(34.dp)
                .clip(RoundedCornerShape(17.dp))
                .clickable { onBackClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBackIos,
                contentDescription = "返回",
                tint = InventoryText,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = "库存",
            color = InventoryText,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Center)
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(34.dp)
                .clip(RoundedCornerShape(17.dp))
                .clickable { onRefresh() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "刷新",
                tint = InventoryGreen,
                modifier = Modifier.size(21.dp)
            )
        }
    }
}

@Composable
private fun TopOverviewHeader(
    summary: InventoryPivotSummary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(InventoryBg)
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .background(InventoryPanel, RoundedCornerShape(4.dp))
            .border(1.dp, InventoryBorder, RoundedCornerShape(4.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OverviewMetricBlock(
            title = "预计总盈利",
            value = summary.totalFloatingPnL,
            unit = "万",
            subLabel = "浮盈亏总额",
            footerLabel = "${summary.watchedProducts}/${summary.totalItems} 单已盯市",
            footerIcon = FooterIconType.STATUS,
            modifier = Modifier.weight(3f)
        )
        OverviewDivider()
        OverviewMetricBlock(
            title = "每日资金燃烧",
            value = summary.totalDailyBurn,
            unit = "/天",
            subLabel = "利息+仓储/天",
            footerLabel = "按估值计算",
            footerIcon = FooterIconType.FILTER,
            emphasizeBurn = true,
            modifier = Modifier.weight(3f)
        )
        OverviewDivider()
        OverviewCashBlock(
            beforeValue = summary.totalNetCashBefore,
            afterValue = summary.totalRecoverableCash,
            modifier = Modifier.weight(4f)
        )
    }
}

@Composable
private fun OverviewMetricBlock(
    title: String,
    value: Double,
    unit: String,
    subLabel: String,
    footerLabel: String,
    footerIcon: FooterIconType,
    modifier: Modifier = Modifier,
    emphasizeBurn: Boolean = false
) {
    Column(modifier = modifier.heightIn(min = 82.dp)) {
        Text(title, color = InventoryText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Spacer(Modifier.height(3.dp))
        Text(
            text = if (emphasizeBurn) "-${inventoryFormatMoneyMagnitude(value)}$unit" else inventoryFormatMoneyWan(value, signed = true),
            color = if (emphasizeBurn) InventoryNegative else moneyColor(value),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(subLabel, color = InventoryMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.weight(1f))
        OverviewFooter(label = footerLabel, icon = footerIcon)
    }
}

@Composable
private fun OverviewCashBlock(beforeValue: Double, afterValue: Double, modifier: Modifier = Modifier) {
    Column(modifier = modifier.heightIn(min = 82.dp)) {
        Text("预计可回现金", color = InventoryText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        CashLine("交割前", beforeValue, emptyDashWhenZero = true)
        CashLine("交割后", afterValue)
        Spacer(Modifier.weight(1f))
        OverviewFooter(label = "基于当前库存", icon = FooterIconType.ACTIVITY)
    }
}

@Composable
private fun CashLine(label: String, value: Double, emptyDashWhenZero: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = InventoryMuted, fontSize = 10.sp)
        Spacer(Modifier.width(4.dp))
        Text(
            if (emptyDashWhenZero && kotlin.math.abs(value) < 0.0001) "-" else inventoryFormatMoneyWan(value, signed = true),
            color = moneyColor(value),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun OverviewFooter(label: String, icon: FooterIconType) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 0.dp, color = Color.Transparent)
            .padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = InventorySubText, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        TinyFooterIcon(type = icon, tint = InventorySubText)
    }
}

@Composable
private fun TinyFooterIcon(type: FooterIconType, tint: Color) {
    Canvas(modifier = Modifier.size(12.dp)) {
        val stroke = Stroke(width = 1.15.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun x(v: Float) = size.width * v / 12f
        fun y(v: Float) = size.height * v / 12f

        when (type) {
            FooterIconType.STATUS -> {
                val shield = Path().apply {
                    moveTo(x(6f), y(1.15f))
                    lineTo(x(2.4f), y(2.55f))
                    quadraticBezierTo(x(1.7f), y(2.82f), x(1.7f), y(3.65f))
                    lineTo(x(1.7f), y(7.25f))
                    quadraticBezierTo(x(1.7f), y(8.35f), x(2.58f), y(9.08f))
                    lineTo(x(5.1f), y(10.92f))
                    quadraticBezierTo(x(6f), y(11.55f), x(6.9f), y(10.92f))
                    lineTo(x(9.42f), y(9.08f))
                    quadraticBezierTo(x(10.3f), y(8.35f), x(10.3f), y(7.25f))
                    lineTo(x(10.3f), y(3.65f))
                    quadraticBezierTo(x(10.3f), y(2.82f), x(9.6f), y(2.55f))
                    close()
                }
                drawPath(shield, color = tint, style = stroke)
                drawLine(tint, Offset(x(4.5f), y(6f)), Offset(x(5.35f), y(6.85f)), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(x(5.35f), y(6.85f)), Offset(x(7.65f), y(4.55f)), stroke.width, StrokeCap.Round)
            }

            FooterIconType.FILTER -> {
                val radius = size.minDimension * 0.245f
                drawCircle(tint, radius, Offset(x(4f), y(8f)), style = stroke)
                drawCircle(tint, radius, Offset(x(6f), y(4f)), style = stroke)
                drawCircle(tint, radius, Offset(x(8f), y(8f)), style = stroke)
            }

            FooterIconType.ACTIVITY -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(x(1f), y(1f)),
                    size = Size(x(10f), y(10f)),
                    cornerRadius = CornerRadius(x(2f), y(2f)),
                    style = stroke
                )
                val path = Path().apply {
                    moveTo(x(3.6f), y(7.25f))
                    lineTo(x(4.85f), y(5.7f))
                    lineTo(x(6.45f), y(6.35f))
                    lineTo(x(8.4f), y(4.75f))
                }
                drawPath(path, color = tint, style = stroke)
            }
        }
    }
}

@Composable
private fun OverviewDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 6.dp)
            .width(1.dp)
            .height(24.dp)
            .background(InventoryBorder)
    )
}

@Composable
private fun SummaryBoard(summary: InventoryPivotSummary, showKg: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(InventoryPanel)
            .border(1.dp, InventoryBorder, RoundedCornerShape(8.dp))
            .padding(vertical = 14.dp)
    ) {
        SummaryColumn(
            items = listOf(
                "总重量" to inventoryFormatWeight(summary.totalWeight, showKg),
                "占用资金" to inventoryFormatMoneyWan(summary.totalOccupiedCash)
            ),
            modifier = Modifier.weight(1f)
        )
        VerticalDivider()
        SummaryColumn(
            items = listOf(
                "总件数" to summary.totalPieces.toString(),
                "浮盈亏" to inventoryFormatMoneyWan(kotlin.math.abs(summary.totalFloatingPnL))
            ),
            valueColor = moneyColor(summary.totalFloatingPnL),
            modifier = Modifier.weight(1f)
        )
        VerticalDivider()
        SummaryColumn(
            items = listOf(
                "品类" to summary.totalProducts.toString(),
                "可用现金" to inventoryFormatMoneyWan(kotlin.math.abs(summary.totalRecoverableCash))
            ),
            valueColor = moneyColor(summary.totalRecoverableCash),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SummaryColumn(items: List<Pair<String, String>>, modifier: Modifier = Modifier, valueColor: Color = InventoryText) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        items.forEachIndexed { index, item ->
            Text(item.first, color = InventorySubText, fontSize = 11.sp, maxLines = 1)
            Spacer(Modifier.height(4.dp))
            Text(item.second, color = if (index == 1) valueColor else InventoryText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            if (index == 0) {
                Spacer(Modifier.height(10.dp))
                Divider(color = InventoryBorder, thickness = 0.7.dp, modifier = Modifier.padding(horizontal = 18.dp))
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun TopModuleTabs(currentTab: InventoryTab, onChange: (InventoryTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .background(InventoryPanel, RoundedCornerShape(8.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        InventoryTab.entries.forEach { tab ->
            val selected = currentTab == tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .background(if (selected) InventoryGreen else Color.Transparent, RoundedCornerShape(6.dp))
                    .clickable { onChange(tab) },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    TopTabIcon(tab = tab, selected = selected)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        tabLabel(tab),
                        color = if (selected) Color.White else InventorySubText,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun TopTabIcon(tab: InventoryTab, selected: Boolean) {
    val tint = if (selected) Color.White else InventorySubText
    when (tab) {
        InventoryTab.PIVOT_STANDARD -> ReceiptSearchTabIcon(tint)
        InventoryTab.DYNAMIC_INVENTORY -> SettingTabIcon(tint)
        InventoryTab.INVENTORY_DETAIL -> DocumentTextTabIcon(tint)
    }
}

@Composable
private fun ReceiptSearchTabIcon(tint: Color) {
    Canvas(modifier = Modifier.size(14.dp)) {
        val stroke = Stroke(width = 1.35.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun x(v: Float) = size.width * v / 14f
        fun y(v: Float) = size.height * v / 14f
        val receipt = Path().apply {
            moveTo(x(3f), y(1.7f))
            lineTo(x(10.3f), y(1.7f))
            lineTo(x(10.3f), y(6.2f))
            moveTo(x(3f), y(1.7f))
            lineTo(x(3f), y(12.2f))
            lineTo(x(4.5f), y(11.1f))
            lineTo(x(6f), y(12.2f))
        }
        drawPath(receipt, color = tint, style = stroke)
        drawLine(tint, Offset(x(5f), y(4f)), Offset(x(8.5f), y(4f)), stroke.width, StrokeCap.Round)
        drawLine(tint, Offset(x(5f), y(6.3f)), Offset(x(7f), y(6.3f)), stroke.width, StrokeCap.Round)
        drawCircle(tint, radius = x(2f), center = Offset(x(9.3f), y(9.2f)), style = stroke)
        drawLine(tint, Offset(x(10.8f), y(10.7f)), Offset(x(12.5f), y(12.4f)), stroke.width, StrokeCap.Round)
    }
}

@Composable
private fun SettingTabIcon(tint: Color) {
    Canvas(modifier = Modifier.size(14.dp)) {
        val strokeWidth = 1.35.dp.toPx()
        fun x(v: Float) = size.width * v / 14f
        fun y(v: Float) = size.height * v / 14f
        listOf(3f, 7f, 11f).forEach { yy ->
            drawLine(tint, Offset(x(1.7f), y(yy)), Offset(x(12.3f), y(yy)), strokeWidth, StrokeCap.Round)
        }
        drawCircle(tint, radius = x(1.1f), center = Offset(x(5f), y(3f)), style = Stroke(strokeWidth, cap = StrokeCap.Round))
        drawCircle(tint, radius = x(1.1f), center = Offset(x(9f), y(7f)), style = Stroke(strokeWidth, cap = StrokeCap.Round))
        drawCircle(tint, radius = x(1.1f), center = Offset(x(6.7f), y(11f)), style = Stroke(strokeWidth, cap = StrokeCap.Round))
    }
}

@Composable
private fun DocumentTextTabIcon(tint: Color) {
    Canvas(modifier = Modifier.size(14.dp)) {
        val stroke = Stroke(width = 1.35.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun x(v: Float) = size.width * v / 14f
        fun y(v: Float) = size.height * v / 14f
        drawRoundRect(
            color = tint,
            topLeft = Offset(x(3f), y(1.5f)),
            size = Size(x(8f), y(11f)),
            cornerRadius = CornerRadius(x(1.4f), y(1.4f)),
            style = stroke
        )
        drawLine(tint, Offset(x(8.5f), y(1.7f)), Offset(x(11f), y(4.2f)), stroke.width, StrokeCap.Round)
        drawLine(tint, Offset(x(5f), y(6f)), Offset(x(9f), y(6f)), stroke.width, StrokeCap.Round)
        drawLine(tint, Offset(x(5f), y(8.4f)), Offset(x(9f), y(8.4f)), stroke.width, StrokeCap.Round)
    }
}

@Composable
private fun PivotControlSection(
    query: String,
    showKg: Boolean,
    summary: InventoryPivotSummary,
    onQueryChange: (String) -> Unit,
    onToggleShowKg: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .fillMaxWidth()
            .background(InventoryPanel, RoundedCornerShape(8.dp))
            .border(1.dp, InventoryBorder, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("标准透视表", color = InventoryText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(8.dp))
            Text("Standard Pivot", color = InventoryMuted, fontSize = 11.sp)
            Spacer(Modifier.weight(1f))
            UnitSwitch(showKg = showKg, onToggleShowKg = onToggleShowKg)
        }
        Spacer(Modifier.height(10.dp))
        SearchBox(value = query, placeholder = "搜索品名、厂号、箱号", onChange = onQueryChange)
        Spacer(Modifier.height(10.dp))
        SummaryBoard(summary = summary, showKg = showKg)
    }
}

@Composable
private fun UnitSwitch(showKg: Boolean, onToggleShowKg: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("单位", color = InventoryMuted, fontSize = 11.sp)
        Spacer(Modifier.width(6.dp))
        Row(
            modifier = Modifier
                .border(1.dp, InventoryBorder, RoundedCornerShape(6.dp))
                .clip(RoundedCornerShape(6.dp))
        ) {
            UnitSwitchItem(
                text = "KG",
                selected = showKg,
                onClick = { if (!showKg) onToggleShowKg() }
            )
            UnitSwitchItem(
                text = "T",
                selected = !showKg,
                onClick = { if (showKg) onToggleShowKg() }
            )
        }
    }
}

@Composable
private fun UnitSwitchItem(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(28.dp)
            .width(38.dp)
            .background(if (selected) InventoryGreen else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else InventorySubText,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun PivotTableSection(products: List<InventoryPivotProduct>, showKg: Boolean) {
    val scrollState = rememberScrollState()
    var expandedProducts by remember(products) { mutableStateOf(setOf<String>()) }
    Column(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .fillMaxWidth()
            .background(InventoryPanel, RoundedCornerShape(8.dp))
            .border(1.dp, InventoryBorder, RoundedCornerShape(8.dp))
    ) {
        PivotTableHeader(scrollState = scrollState, showKg = showKg)
        if (products.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                Text("暂无库存数据", color = InventoryMuted)
            }
        } else {
            products.forEach { product ->
                val expanded = expandedProducts.contains(product.productName)
                PivotProductRow(
                    product = product,
                    showKg = showKg,
                    scrollState = scrollState,
                    expanded = expanded,
                    onToggle = {
                        expandedProducts = if (expanded) {
                            expandedProducts - product.productName
                        } else {
                            expandedProducts + product.productName
                        }
                    }
                )
                if (expanded) {
                    product.factories.forEach { factory ->
                        PivotFactoryRow(product = product, factory = factory, showKg = showKg, scrollState = scrollState)
                    }
                }
            }
        }
    }
}

@Composable
private fun PivotTableHeader(scrollState: androidx.compose.foundation.ScrollState, showKg: Boolean) {
    Row(modifier = Modifier.fillMaxWidth().background(InventoryPanelSoft)) {
        HeaderCell("品名", width = PivotNameWidth, textAlign = TextAlign.Start)
        Row(modifier = Modifier.horizontalScroll(scrollState, enabled = false)) {
            listOf("成本(￥/KG)", "未发货", "在途", "在库", "总重量(${if (showKg) "KG" else "T"})", "件数", "占用资金", "浮盈亏", "可用现金")
                .zip(PivotColumnWidths)
                .forEach { (label, width) ->
                    HeaderCell(label, width = width, textAlign = TextAlign.End)
            }
        }
    }
}

@Composable
private fun PivotProductRow(
    product: InventoryPivotProduct,
    showKg: Boolean,
    scrollState: androidx.compose.foundation.ScrollState,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    PivotRowShell(
        nameContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ToggleSquareIcon(expanded = expanded)
                Spacer(Modifier.width(6.dp))
                Text(
                    product.productName,
                    color = InventoryText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        values = listOf(
            inventoryFormatPrice(product.weightedAvgCost),
            inventoryFormatWeight(product.pendingWeight, showKg),
            inventoryFormatWeight(product.transitWeight, showKg),
            inventoryFormatWeight(product.inStockWeight, showKg),
            inventoryFormatWeight(product.totalWeight, showKg),
            product.totalPieces.toString(),
            inventoryFormatMoneyWan(product.occupiedCash),
            inventoryFormatMoneyWan(product.floatingPnL, signed = true),
            inventoryFormatMoneyWan(product.recoverableCash, signed = true)
        ),
        scrollState = scrollState,
        strong = true,
        onClick = onToggle
    )
}

@Composable
private fun PivotFactoryRow(product: InventoryPivotProduct, factory: InventoryFactoryDetail, showKg: Boolean, scrollState: androidx.compose.foundation.ScrollState) {
    PivotRowShell(
        nameContent = {
            Row(verticalAlignment = Alignment.Top) {
                FactoryTreeMarkerIcon()
                Spacer(Modifier.width(6.dp))
                Column {
                    Text(factory.factoryCode, color = InventoryText, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                    Text(listOfNotNull(factory.country, factory.coldStorage).joinToString("/").ifBlank { product.productName }, color = InventoryMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        },
        values = listOf(
            inventoryFormatPrice(factory.avgCost),
            inventoryFormatWeight(factory.pendingWeight, showKg),
            inventoryFormatWeight(factory.transitWeight, showKg),
            inventoryFormatWeight(factory.inStockWeight, showKg),
            inventoryFormatWeight(factory.weightKg, showKg),
            factory.pieces.toString(),
            inventoryFormatMoneyWan(factory.occupiedCash),
            inventoryFormatMoneyWan(factory.floatingPnL, signed = true),
            inventoryFormatMoneyWan(factory.recoverableCash, signed = true)
        ),
        scrollState = scrollState,
        strong = false
    )
}

@Composable
private fun PivotRowShell(
    nameContent: @Composable () -> Unit,
    values: List<String>,
    scrollState: androidx.compose.foundation.ScrollState,
    strong: Boolean,
    onClick: (() -> Unit)? = null
) {
    val rowModifier = Modifier
        .fillMaxWidth()
        .background(if (strong) Color.White else Color(0xFFEEEEEE))
        .border(width = 0.3.dp, color = InventoryBorder)
        .heightIn(min = 46.dp)
        .let { modifier -> if (onClick != null) modifier.clickable { onClick() } else modifier }

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(PivotNameWidth).padding(horizontal = 10.dp, vertical = 8.dp)) { nameContent() }
        Row(modifier = Modifier.horizontalScroll(scrollState)) {
            values.zip(PivotColumnWidths).forEachIndexed { index, (value, width) ->
                DataCell(value, width = width, color = if (index == 7 || index == 8) moneyColorFromText(value) else InventoryText)
            }
        }
    }
}

@Composable
private fun ToggleSquareIcon(expanded: Boolean) {
    Canvas(modifier = Modifier.size(14.dp)) {
        val strokeWidth = 1.1.dp.toPx()
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val borderColor = if (expanded) InventoryGreen else InventoryBorder
        drawRoundRect(
            color = borderColor,
            topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
            size = Size(size.width - 2.dp.toPx(), size.height - 2.dp.toPx()),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
            style = stroke
        )
        val cx = size.width / 2f
        val cy = size.height / 2f
        if (expanded) {
            drawLine(InventoryGreen, Offset(cx - 3.dp.toPx(), cy - 1.dp.toPx()), Offset(cx, cy + 2.dp.toPx()), strokeWidth, StrokeCap.Round)
            drawLine(InventoryGreen, Offset(cx, cy + 2.dp.toPx()), Offset(cx + 3.dp.toPx(), cy - 1.dp.toPx()), strokeWidth, StrokeCap.Round)
        } else {
            drawLine(InventoryMuted, Offset(cx - 1.dp.toPx(), cy - 3.dp.toPx()), Offset(cx + 2.dp.toPx(), cy), strokeWidth, StrokeCap.Round)
            drawLine(InventoryMuted, Offset(cx + 2.dp.toPx(), cy), Offset(cx - 1.dp.toPx(), cy + 3.dp.toPx()), strokeWidth, StrokeCap.Round)
        }
    }
}

@Composable
private fun FactoryTreeMarkerIcon() {
    Canvas(modifier = Modifier.size(12.dp)) {
        val strokeWidth = 0.8.dp.toPx()
        fun x(v: Float) = size.width * v / 12f
        fun y(v: Float) = size.height * v / 12f
        val path = Path().apply {
            moveTo(x(6f), y(3f))
            lineTo(x(6f), y(7f))
            quadraticBezierTo(x(6f), y(8f), x(7f), y(8f))
            lineTo(x(11f), y(8f))
        }
        drawPath(
            path = path,
            color = InventoryGreen,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
private fun DynamicStickySection(
    summary: DynamicInventorySummary,
    groupBys: Set<DynamicGroupBy>,
    onToggle: (DynamicGroupBy) -> Unit
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .fillMaxWidth()
            .background(InventoryPanel, RoundedCornerShape(8.dp))
            .border(1.dp, InventoryBorder, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DynamicGroupBy.entries.forEach { item ->
                FilterChip(text = item.label, selected = groupBys.contains(item), showBadge = true, onClick = { onToggle(item) })
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            MiniMetric("分组数", summary.groupCount.toString(), Modifier.weight(1f))
            MiniMetric("柜数", summary.containerCount.toString(), Modifier.weight(1f))
            MiniMetric("总重量", inventoryFormatWeight(summary.totalWeight), Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            MiniMetric("平均成本", inventoryFormatPrice(summary.averageCost), Modifier.weight(1f))
            MiniMetric("总盈利", inventoryFormatMoneyWan(summary.totalProfit, signed = true), Modifier.weight(1f), moneyColor(summary.totalProfit))
            MiniMetric("占用资金", inventoryFormatMoneyWan(summary.totalOccupiedCash), Modifier.weight(1f))
        }
    }
}

@Composable
private fun DynamicCardsGrid(cards: List<DynamicInventoryGroupCard>) {
    if (cards.isEmpty()) {
        Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
            Text("暂无动态库存数据", color = InventoryMuted)
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.heightIn(max = 4000.dp).padding(horizontal = 12.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        userScrollEnabled = false
    ) {
        items(cards) { card -> DynamicCard(card) }
    }
}

@Composable
private fun DynamicCard(card: DynamicInventoryGroupCard) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(InventoryPanel, RoundedCornerShape(8.dp))
            .border(1.dp, InventoryBorder, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        card.titleLines.forEach {
            Text("${it.first}:${it.second}", color = InventoryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(card.alertLabel, color = if (card.alertLabel.contains("预警")) InventoryPositive else InventoryGreen, fontSize = 10.sp, modifier = Modifier.background(Color(0xFFF2F7F7), RoundedCornerShape(3.dp)).padding(horizontal = 5.dp, vertical = 3.dp))
            Spacer(Modifier.weight(1f))
            Text("${card.containerCount}", color = InventoryText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("柜", color = InventoryText, fontSize = 11.sp)
        }
        Spacer(Modifier.height(8.dp))
        MetricLine("资金占用", "${String.format("%.1f", card.fundRatio * 100)}%", InventoryAccent)
        MetricLine("持仓成本", inventoryFormatPrice(card.averageCost), InventoryText)
        MetricLine("资金投入", inventoryFormatMoneyWan(card.occupiedCash), InventoryText)
        MetricLine("总盈利", inventoryFormatMoneyWan(card.profit, signed = true), moneyColor(card.profit))
        MetricLine("平均库龄", card.avgAge?.let { "${it}天" } ?: "--", InventoryAccent)
        MetricLine("ROI", "${if (card.roi >= 0) "+" else ""}${String.format("%.1f", card.roi)}%", moneyColor(card.roi))
        Spacer(Modifier.height(6.dp))
        Text(card.insight, color = InventoryMuted, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun DetailSearchAndSort(
    query: String,
    sortKey: DetailSortKey,
    sortDirection: SortDirection,
    onQueryChange: (String) -> Unit,
    onSort: (DetailSortKey) -> Unit
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .fillMaxWidth()
            .background(InventoryPanel, RoundedCornerShape(8.dp))
            .border(1.dp, InventoryBorder, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        SearchBox(value = query, placeholder = "搜索箱号、合同、厂号、品名、SKU", onChange = onQueryChange)
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DetailSortKey.entries.forEach { key ->
                val selected = key == sortKey
                FilterChip(
                    text = if (selected) "${key.label} ${if (sortDirection == SortDirection.ASC) "升序" else "降序"}" else key.label,
                    selected = selected,
                    onClick = { onSort(key) }
                )
            }
        }
    }
}

@Composable
private fun DetailTable(rows: List<ResolvedInventoryRow>) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .fillMaxWidth()
            .background(InventoryPanel, RoundedCornerShape(8.dp))
            .border(1.dp, InventoryBorder, RoundedCornerShape(8.dp))
    ) {
        Row(modifier = Modifier.fillMaxWidth().background(InventoryPanelSoft)) {
            HeaderCell("箱柜信息", 174.dp)
            Row(modifier = Modifier.horizontalScroll(scrollState, enabled = false)) {
                listOf("资金方/状态", "重量", "成本", "销售价", "总成本", "应收账款", "每日成本", "可回现金", "盈利", "生产日期").forEach {
                    HeaderCell(it, 96.dp)
                }
            }
        }
        if (rows.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                Text("暂无库存明细", color = InventoryMuted)
            }
        } else {
            rows.take(300).forEachIndexed { index, row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (index % 2 == 0) Color.White else Color(0xFFEEEEEE))
                        .border(0.3.dp, InventoryBorder)
                        .heightIn(min = 78.dp)
                ) {
                    Column(modifier = Modifier.width(174.dp).padding(10.dp)) {
                        Text(row.containerId.ifBlank { "--" }, color = InventoryText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Text(row.contractId.ifBlank { row.skuCode }, color = InventorySubText, fontSize = 10.sp, maxLines = 1)
                        Text("${row.country} | ${row.factoryCode}", color = InventoryText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Text(row.productName, color = InventorySubText, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Row(modifier = Modifier.horizontalScroll(scrollState)) {
                        DetailDataCell("${row.funder}\n${row.status}", color = statusColor(row.status))
                        DetailDataCell(inventoryFormatWeight(row.weightKg, true))
                        DetailDataCell("avg:${String.format("%.2f", row.costPerKg)}")
                        DetailDataCell(if (row.sellingPricePerKg > 0) "avg:${String.format("%.2f", row.sellingPricePerKg)}" else "--")
                        DetailDataCell(inventoryFormatMoneyWan(row.costPerKg * row.weightKg))
                        DetailDataCell(inventoryFormatMoneyWan(row.sellingPricePerKg * row.weightKg))
                        DetailDataCell("￥${String.format("%.2f", row.dailyCost)}/天", color = InventoryAccent)
                        DetailDataCell(inventoryFormatMoneyWan(row.recoverableCash, signed = true), color = moneyColor(row.recoverableCash))
                        DetailDataCell(inventoryFormatMoneyWan(row.profit, signed = true), color = moneyColor(row.profit))
                        DetailDataCell(row.productionDate)
                    }
                }
            }
        }
    }
}

@Composable
private fun CenterState(message: String, onRetry: (() -> Unit)? = null) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (onRetry == null) CircularProgressIndicator(color = InventoryAccent)
        Spacer(Modifier.height(14.dp))
        Text(message, color = InventorySubText, fontSize = 14.sp, textAlign = TextAlign.Center)
        if (onRetry != null) {
            Spacer(Modifier.height(12.dp))
            Text("重试", color = InventoryGreen, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { onRetry() })
        }
    }
}

@Composable
private fun SearchBox(value: String, placeholder: String, onChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .background(InventoryMutedPanel, RoundedCornerShape(6.dp))
            .border(1.dp, InventoryBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = InventoryMuted, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f)) {
            if (value.isBlank()) Text(placeholder, color = InventoryMuted, fontSize = 12.sp)
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = InventoryText, fontSize = 13.sp),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun MiniMetric(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = InventoryText) {
    Column(
        modifier = modifier
            .background(InventoryMutedPanel, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = InventorySubText, fontSize = 10.sp, maxLines = 1)
        Spacer(Modifier.height(3.dp))
        Text(value, color = valueColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun HeaderCell(text: String, width: Dp, textAlign: TextAlign = TextAlign.Start) {
    Box(
        modifier = Modifier.width(width).height(42.dp).padding(horizontal = 6.dp),
        contentAlignment = if (textAlign == TextAlign.End) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Text(
            text,
            color = InventorySubText,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = textAlign
        )
    }
}

@Composable
private fun DataCell(text: String, width: Dp, color: Color = InventoryText) {
    Box(modifier = Modifier.width(width).padding(horizontal = 6.dp, vertical = 10.dp), contentAlignment = Alignment.CenterEnd) {
        Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun DetailDataCell(text: String, color: Color = InventoryText) {
    Box(modifier = Modifier.width(96.dp).fillMaxHeight().padding(horizontal = 6.dp, vertical = 10.dp), contentAlignment = Alignment.CenterEnd) {
        Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.End, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun FilterChip(text: String, selected: Boolean, onClick: () -> Unit, showBadge: Boolean = false) {
    Box(
        modifier = Modifier
            .height(32.dp)
            .background(if (selected) Color(0x0D006A61) else Color(0xFFF3F4F4), RoundedCornerShape(6.dp))
            .border(1.dp, if (selected) InventoryGreen else Color(0xFFE2E7E6), RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (selected) InventoryGreen else InventorySubText, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal, maxLines = 1)
        if (selected && showBadge) {
            SelectedFilterBadge(modifier = Modifier.align(Alignment.TopEnd))
        }
    }
}

@Composable
private fun SelectedFilterBadge(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(width = 12.dp, height = 10.dp)) {
        fun x(v: Float) = size.width * v / 12f
        fun y(v: Float) = size.height * v / 10f
        val badge = Path().apply {
            moveTo(x(0f), y(0f))
            lineTo(x(10f), y(0f))
            quadraticBezierTo(x(12f), y(0f), x(12f), y(2f))
            lineTo(x(12f), y(10f))
            lineTo(x(4f), y(10f))
            quadraticBezierTo(x(0f), y(10f), x(0f), y(6f))
            close()
        }
        drawPath(badge, color = InventoryGreen)
        drawLine(Color.White, Offset(x(3f), y(5f)), Offset(x(5f), y(7f)), 1.15.dp.toPx(), StrokeCap.Round)
        drawLine(Color.White, Offset(x(5f), y(7f)), Offset(x(9f), y(3f)), 1.15.dp.toPx(), StrokeCap.Round)
    }
}

@Composable
private fun MetricLine(label: String, value: String, color: Color = InventoryText) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = InventorySubText, fontSize = 10.sp)
        Text(value, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun VerticalDivider() {
    Box(modifier = Modifier.width(1.dp).height(92.dp).background(InventoryBorder))
}

private fun tabLabel(tab: InventoryTab): String {
    return when (tab) {
        InventoryTab.PIVOT_STANDARD -> "标准透视"
        InventoryTab.DYNAMIC_INVENTORY -> "动态库存"
        InventoryTab.INVENTORY_DETAIL -> "库存明细"
    }
}

private fun moneyColor(value: Double): Color {
    return when {
        value > 0 -> InventoryPositive
        value < 0 -> InventoryNegative
        else -> InventoryText
    }
}

private fun moneyColorFromText(value: String): Color {
    return when {
        value.startsWith("+") -> InventoryPositive
        value.startsWith("-") -> InventoryNegative
        else -> InventoryText
    }
}

private fun statusColor(status: String): Color {
    return when {
        status.contains("在库") -> InventoryGreen
        status.contains("在途") -> Color(0xFF3163DC)
        status.contains("清关") -> InventoryAccent
        else -> InventorySubText
    }
}
