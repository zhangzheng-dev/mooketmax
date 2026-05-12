package com.mooket.app.ui.screens.product

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIos
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mooket.app.R
import com.mooket.app.data.model.PriceTrend
import com.mooket.app.data.model.ProductDetail
import com.mooket.app.data.model.ProductSummary
import com.mooket.app.data.model.TrendPoint
import com.mooket.app.ui.theme.*
import java.util.Locale

/**
 * 产品详情页（搜索结果/产品聚合页）
 * 设计来源：Figma - node-id: 158-172
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProductDetailScreen(
    productId: Int,
    category: String,
    productName: String,
    onBackClick: () -> Unit,
    onSearchDelete: (String) -> Unit, // category -> go back to Search
    onCountryFactoryProductClick: (String, String, String, String) -> Unit, // country, factoryNo, productName, category
    viewModel: ProductDetailViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(productId, category) {
        viewModel.loadProductDetail(productId, category)
    }

    Scaffold(
        topBar = {
            // 顶部导航栏
            TopAppBar(
                title = { },
                navigationIcon = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBackIos,
                            contentDescription = "返回",
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { onBackClick() }
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        // 产品搜索框
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFEFF5F3))
                                .border(
                                    1.dp,
                                    Color(0xFFBBCAC6).copy(alpha = 0.3f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(start = 9.dp, end = 13.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            // 产品标签 - 点击删除回到搜索页
                            SearchTag(
                                text = productName,
                                onClick = { onSearchDelete(category) }
                            )

                            // 搜索图标
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color(0xFFADB7B5),
                                modifier = Modifier
                                    .size(16.dp)
                                    .align(Alignment.CenterEnd)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .padding(paddingValues),
            state = listState
        ) {
            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillParentMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Primary)
                    }
                }
            } else if (uiState.product != null) {
                val product = uiState.product!!

                // 数据看板 - 非吸顶，随内容滚动
                item {
                    DataDashboard(
                        product = product,
                        isInquiryTab = uiState.selectedTab == 1
                    )
                }

                // 浅绿间隔 - 不吸顶，随内容滚动
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .background(Color(0xFFF4FBF8))
                    )
                }

                // Tab选择区域 - 吸顶
                stickyHeader(key = "product_tab") {
                    TabSection(
                        selectedTab = uiState.selectedTab,
                        selectedSort = uiState.selectedSort,
                        onTabSelected = { viewModel.selectTab(it) },
                        onSortSelected = { viewModel.selectSort(it) }
                    )
                }

                // 列表内容
                if (uiState.isListRefreshing) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Primary,
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }

                itemsIndexed(uiState.currentSummaries) { index, summary ->
                    LaunchedEffect(index) {
                        if (index >= uiState.currentSummaries.size - 3 && uiState.hasMorePages && !uiState.isLoadingMore) {
                            viewModel.loadMore()
                        }
                    }

                    ProductSummaryItem(
                        summary = summary,
                        isLast = index == uiState.currentSummaries.size - 1 && !uiState.hasMorePages,
                        isInquiryTab = uiState.selectedTab == 1,
                        onClick = {
                            val c = summary.country
                            val fn = summary.factoryNo
                            if (c != null && fn != null) {
                                onCountryFactoryProductClick(c, fn, productName, category)
                            }
                        }
                    )
                }

                if (uiState.isLoadingMore) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Primary,
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }

                if (!uiState.hasMorePages && !uiState.isLoadingMore) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "没有更多了～",
                                fontSize = 11.sp,
                                color = Color(0xFF9DA4A3)
                            )
                        }
                    }
                }
            } else if (uiState.error != null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillParentMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.error ?: "加载失败",
                            fontSize = 14.sp,
                            color = TextHint
                        )
                    }
                }
            }
        }
    }
}

/**
 * 数据看板
 */
@Composable
private fun DataDashboard(
    product: ProductDetail,
    isInquiryTab: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 左侧：产品名称 + 报盘数
            Column {
                Text(
                    text = product.productName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isInquiryTab) "近2日求购" else "近2日报盘",
                    fontSize = 10.sp,
                    color = Color(0xFF3C4947).copy(alpha = 0.5f)
                )

                Text(
                    text = formatCount(product.offerCount),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Primary
                )
            }

            // 右侧：价格区间 + 商家数/工厂数（横向排列）
            Column(
                modifier = Modifier
                    .background(Color.White)
                    .padding(start = 24.dp, end = 16.dp)
            ) {
                // 价格区间
                Column {
                    Text(
                        text = "价格区间（RMB）",
                        fontSize = 10.sp,
                        color = Color(0xFF3C4947).copy(alpha = 0.5f)
                    )

                    val priceText = if (product.priceMin != null && product.priceMin > 0 && product.priceMax != null && product.priceMax >= product.priceMin) {
                        "¥${formatPrice(product.priceMin)} - ${formatPrice(product.priceMax)}"
                    } else {
                        "暂无报价"
                    }

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = priceText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        if (priceText != "暂无报价") {
                            Text(
                                text = "/kg",
                                fontSize = 12.sp,
                                color = TextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 分割线（短一点，只在商家数区域上方）
                Divider(
                    modifier = Modifier.width(100.dp),
                    color = Color(0xFFEFF5F3)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 商家数和工厂数（横向排列，左对齐）
                Row {
                    // 商家数
                    Column {
                        Text(
                            text = "商家数",
                            fontSize = 10.sp,
                            color = Color(0xFF3C4947).copy(alpha = 0.5f)
                        )
                        Text(
                            text = "${product.merchantCount}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    // 工厂数
                    Column {
                        Text(
                            text = "工厂数",
                            fontSize = 10.sp,
                            color = Color(0xFF3C4947).copy(alpha = 0.5f)
                        )
                        Text(
                            text = "${product.factoryCount}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }

                    }
            }
        }
    }
}

/**
 * 30日价格趋势图
 */
@Composable
private fun PriceTrendChart(
    trend: List<TrendPoint>,
    modifier: Modifier = Modifier
) {
    val primaryColor = Primary
    val lineColor = Primary.copy(alpha = 0.3f)

    Column(modifier = modifier) {
        // 图表
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (trend.isEmpty()) return@Canvas

            val validPoints = trend.filter { it.avgPrice != null }
            if (validPoints.isEmpty()) return@Canvas

            val prices = validPoints.mapNotNull { it.avgPrice }
            if (prices.isEmpty()) return@Canvas

            val minPrice = prices.minOrNull() ?: return@Canvas
            val maxPrice = prices.maxOrNull() ?: return@Canvas
            val priceRange = if (maxPrice - minPrice == 0.0) 1.0 else maxPrice - minPrice

            val width = size.width
            val height = size.height
            val stepX = width / (trend.size - 1).coerceAtLeast(1)

            // 绘制背景网格线
            for (i in 0..4) {
                val y = height * i / 4
                drawLine(
                    color = lineColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // 绘制数据线
            val path = Path()
            var firstPoint = true

            trend.forEachIndexed { index: Int, point: TrendPoint ->
                if (point.avgPrice != null) {
                    val x = index * stepX
                    val y = height - ((point.avgPrice - minPrice) / priceRange * height).toFloat()

                    if (firstPoint) {
                        path.moveTo(x, y)
                        firstPoint = false
                    } else {
                        path.lineTo(x, y)
                    }
                }
            }

            drawPath(
                path = path,
                color = primaryColor,
                style = Stroke(width = 2.dp.toPx())
            )

            // 绘制数据点
            trend.forEachIndexed { index: Int, point: TrendPoint ->
                if (point.avgPrice != null) {
                    val x = index * stepX
                    val y = height - ((point.avgPrice - minPrice) / priceRange * height).toFloat()

                    drawCircle(
                        color = primaryColor,
                        radius = 3.dp.toPx(),
                        center = Offset(x, y)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // X轴日期标签
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val datesToShow = if (trend.size > 7) {
                listOf(0, trend.size / 4, trend.size / 2, trend.size * 3 / 4, trend.size - 1)
            } else {
                trend.indices.toList()
            }

            datesToShow.forEach { index ->
                if (index < trend.size) {
                    Text(
                        text = trend[index].date,
                        fontSize = 10.sp,
                        color = TextHint
                    )
                }
            }
        }
    }
}

/**
 * Tab选择区域
 */
@Composable
private fun TabSection(
    selectedTab: Int,
    selectedSort: String,
    onTabSelected: (Int) -> Unit,
    onSortSelected: (String) -> Unit
) {
    Column(modifier = Modifier.background(Color.White)) {
        // Tab内容
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：报盘/求购 Tab
            Row(
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 报盘 Tab
                Column(
                    modifier = Modifier.clickable { onTabSelected(0) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "报盘",
                        fontSize = 14.sp,
                        fontWeight = if (selectedTab == 0) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selectedTab == 0) TextPrimary else Color(0xFF3C4947)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .width(18.dp)
                            .height(3.dp)
                            .background(if (selectedTab == 0) Primary else Color.Transparent)
                    )
                }

                // 求购 Tab
                Column(
                    modifier = Modifier.clickable { onTabSelected(1) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "求购",
                        fontSize = 14.sp,
                        fontWeight = if (selectedTab == 1) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selectedTab == 1) TextPrimary else Color(0xFF3C4947)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .width(18.dp)
                            .height(2.dp)
                            .background(if (selectedTab == 1) Primary else Color.Transparent)
                    )
                }
            }

            // 右侧：排序
            Row(
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 综合推荐
                Text(
                    text = "综合推荐",
                    fontSize = 14.sp,
                    fontWeight = if (selectedSort == "comprehensive") FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selectedSort == "comprehensive") TextPrimary else Color(0xFF3C4947),
                    modifier = Modifier.clickable { onSortSelected("comprehensive") }
                )

                // 价格 - 可点击排序（整个区域可点击）
                Column(
                    modifier = Modifier.clickable { onSortSelected("price") },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "价格",
                            fontSize = 14.sp,
                            fontWeight = if (selectedSort == "price_asc" || selectedSort == "price_desc") FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selectedSort == "price_asc" || selectedSort == "price_desc") TextPrimary else Color(0xFF3C4947)
                        )
                        // 排序箭头 - 升序时上箭头背景变绿，降序时下箭头背景变绿
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            // 上箭头 - 升序时背景变绿
                            Box(
                                modifier = Modifier
                                    .size(12.dp, 8.dp)
                                    .background(
                                        if (selectedSort == "price_asc") Primary else Color.Transparent,
                                        RoundedCornerShape(1.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = "升序",
                                    tint = if (selectedSort == "price_asc") Color.White else Color(0xFF8B8B8B),
                                    modifier = Modifier.size(12.dp, 8.dp)
                                )
                            }
                            // 下箭头 - 降序时背景变绿
                            Box(
                                modifier = Modifier
                                    .size(12.dp, 8.dp)
                                    .background(
                                        if (selectedSort == "price_desc") Primary else Color.Transparent,
                                        RoundedCornerShape(1.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "降序",
                                    tint = if (selectedSort == "price_desc") Color.White else Color(0xFF8B8B8B),
                                    modifier = Modifier.size(12.dp, 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 底部分割线
        Divider(color = Border)
    }
}

/**
 * 产品汇总项
 */
@Composable
private fun ProductSummaryItem(
    summary: ProductSummary,
    isLast: Boolean,
    isInquiryTab: Boolean = false,
    onClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .clickable { onClick() }
            .border(
                width = if (isLast) 0.dp else 0.5.dp,
                color = Border,
                shape = RoundedCornerShape(0.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // 左侧内容
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // 第一行：国家厂号
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = summary.countryFactory ?: "",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Row(
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val priceText = if (summary.priceMin != null && summary.priceMin > 0 && summary.priceMax != null && summary.priceMax >= summary.priceMin) {
                            "¥ ${formatPrice(summary.priceMin)} - ${formatPrice(summary.priceMax)}"
                        } else {
                            "暂无报价"
                        }
                        Text(
                            text = priceText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Primary
                        )
                        if (summary.priceMin != null) {
                            Text(
                                text = "/kg",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Normal,
                                color = TextPrimary,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 第二行：商家名称 + 商家数/报盘数
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 商家名称列表（横向滚动）
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        summary.merchantNames?.forEach { name ->
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFF3F6F5), RoundedCornerShape(2.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_merchant_logo),
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = name,
                                        fontSize = 11.sp,
                                        color = Color(0xFF3C4947)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // 商家数和报盘数
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${summary.merchantCount}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "商家",
                            fontSize = 11.sp,
                            color = Color(0xFF3C4947)
                        )

                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(4.dp)
                                .background(Color(0xFF9DA4A3), CircleShape)
                        )

                        Text(
                            text = "${summary.offerCount}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (isInquiryTab) "求购" else "报盘",
                            fontSize = 11.sp,
                            color = Color(0xFF3C4947)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 右侧箭头（垂直居中于两行之间）
            Box(
                modifier = Modifier.height(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_right),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color(0xFF9DA4A3)
                )
            }
        }
    }
}

/**
 * 格式化数量（支持 12.4k 格式）
 */
private fun formatCount(count: Long): String {
    return when {
        count >= 10000 -> String.format(Locale.US, "%.1fk", count / 1000.0)
        count >= 1000 -> String.format(Locale.US, "%.1fk", count / 1000.0)
        else -> count.toString()
    }
}

/**
 * 格式化价格
 */
private fun formatPrice(price: Double): String {
    return if (price == price.toLong().toDouble()) {
        price.toLong().toString()
    } else {
        String.format(Locale.US, "%.1f", price)
    }
}

/**
 * 搜索标签 - 带删除功能的可点击标签
 */
@Composable
private fun SearchTag(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(2.dp))
            .background(Primary)
            .clickable { onClick() }
            .padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = text,
                fontSize = 12.sp,
                color = Color.White
            )
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "删除",
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(12.dp)
            )
        }
    }
}
