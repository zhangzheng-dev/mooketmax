package com.mooket.app.ui.screens.countryproduct

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.ArrowBackIos
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mooket.app.R
import com.mooket.app.data.model.CountryProductDetail
import com.mooket.app.data.model.CountryProductFactory
import com.mooket.app.data.model.DailyPrice
import com.mooket.app.ui.theme.*
import java.util.Locale

/**
 * 国家+产品详情页
 * 设计来源：Figma - node-id: 158-1232
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CountryProductScreen(
    country: String,
    productName: String,
    category: String,
    onBackClick: () -> Unit,
    onFactoryClick: (String, String) -> Unit, // country, factoryNo
    onCountryDelete: (Int, String, String) -> Unit, // productId, productName, category -> go to ProductDetailScreen
    onProductDelete: (String, String) -> Unit, // country, category -> go to CountryDetailScreen
    viewModel: CountryProductViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(country, productName, category) {
        viewModel.loadCountryProduct(country, productName, category)
    }

    // Infinite scroll - detect when near bottom
    val shouldLoadMore = !listState.canScrollForward && uiState.factories.isNotEmpty()

    LaunchedEffect(shouldLoadMore, uiState.hasMorePages, uiState.isLoadingMore) {
        if (shouldLoadMore && uiState.hasMorePages && !uiState.isLoadingMore) {
            viewModel.loadMore()
        }
    }

    Scaffold(
        topBar = {
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
                                .padding(start = 7.dp, end = 13.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 国家标签 - 点击删除进入产品详情页
                                SearchTag(
                                    text = country,
                                    onClick = { uiState.detail?.productId?.let { onCountryDelete(it, productName, category) } }
                                )

                                // 产品标签 - 点击删除进入国家详情页
                                SearchTag(
                                    text = productName,
                                    onClick = { onProductDelete(country, category) }
                                )
                            }

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
            } else if (uiState.detail != null) {
                val detail = uiState.detail!!

                // 数据看板 - 非吸顶，随内容滚动
                item {
                    CountryProductDashboard(
                        detail = detail,
                        isInquiryTab = uiState.selectedTab == 1,
                        isTrendExpanded = uiState.isTrendExpanded,
                        onToggleTrend = { viewModel.toggleTrendExpanded() }
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
                stickyHeader(key = "country_product_tab") {
                    CountryProductTabSection(
                        selectedTab = uiState.selectedTab,
                        selectedSort = uiState.selectedSort,
                        onTabSelected = { viewModel.selectTab(it) },
                        onSortSelected = { viewModel.selectSort(it) }
                    )
                }

                // 厂号列表
                itemsIndexed(
                    items = uiState.factories,
                    key = { index, _ -> index }
                ) { index, factory ->
                    FactoryItem(
                        factory = factory,
                        country = country,
                        onClick = { onFactoryClick(country, factory.factoryNo ?: "") },
                        isInquiryTab = uiState.selectedTab == 1
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

                if (!uiState.hasMorePages && uiState.factories.isNotEmpty()) {
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
 * 国家+产品数据看板
 */
@Composable
private fun CountryProductDashboard(
    detail: CountryProductDetail,
    isInquiryTab: Boolean = false,
    isTrendExpanded: Boolean = false,
    onToggleTrend: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(16.dp)
    ) {
        // 标题：国家 + 产品名
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = detail.country,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .size(4.dp)
                    .background(Color(0xFF9DA4A3), RoundedCornerShape(50))
            )
            Text(
                text = detail.productName,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 价格区间和走势
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 左侧：价格区间
            Column {
                Text(
                    text = if (isInquiryTab) "近2日求购价格区间（RMB）" else "近2日报盘价格区间（RMB）",
                    fontSize = 10.sp,
                    color = Color(0xFF3C4947).copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    if (detail.priceMin != null && detail.priceMin > 0 && detail.priceMax != null && detail.priceMax > 0) {
                        Text(
                            text = "¥${formatPrice(detail.priceMin)}-${formatPrice(detail.priceMax)}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Primary
                        )
                        Text(
                            text = "/kg",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = TextPrimary,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    } else {
                        Text(
                            text = "协商报价",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextHint
                        )
                    }

                    // 涨跌指示
                    if (detail.priceChange != null && detail.priceChangeRate != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        PriceChangeIndicator(
                            change = detail.priceChange,
                            changeRate = detail.priceChangeRate
                        )
                    }
                }
            }

            // 右侧：7日报价走势
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "7日报价走势",
                    fontSize = 10.sp,
                    color = Color(0xFF9DA4A3),
                    modifier = Modifier.align(Alignment.End)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 迷你走势图
                if (detail.priceHistory7Days.isNotEmpty()) {
                    SparklineChart(
                        data = detail.priceHistory7Days,
                        modifier = Modifier
                            .width(82.dp)
                            .height(28.dp)
                    )
                } else {
                    Text(
                        text = "暂无走势数据",
                        fontSize = 10.sp,
                        color = TextHint,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }

        // 分割线
        Divider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = Border
        )

        // 统计行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                StatItem(label = "报盘数", value = detail.offerCount.toString())
                StatItem(label = "求购数", value = detail.inquiryCount.toString())
                StatItem(label = "商家数", value = detail.merchantCount.toString())
            }

            // 展开数据按钮
            Row(
                modifier = Modifier
                    .background(Color(0xFF171D1C).copy(alpha = 0.05f), RoundedCornerShape(2.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .clickable { onToggleTrend() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isTrendExpanded) "收起数据" else "展开数据",
                    fontSize = 10.sp,
                    color = TextPrimary
                )
                Icon(
                    imageVector = if (isTrendExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier
                        .size(12.dp),
                    tint = TextPrimary
                )
            }
        }

        // 趋势图展开区域
        if (isTrendExpanded) {
            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Border)
            Spacer(modifier = Modifier.height(12.dp))

            // 趋势图标题
            Text(
                text = "近30日价格趋势",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 趋势图（使用 priceHistory30Days 数据）
            if (detail.priceHistory30Days.isNotEmpty()) {
                CountryTrendChart(
                    data = detail.priceHistory30Days,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无趋势数据",
                        fontSize = 12.sp,
                        color = TextHint
                    )
                }
            }
        }
    }
}

/**
 * 统计项
 */
@Composable
private fun StatItem(label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color(0xFF3C4947).copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
    }
}

/**
 * 涨跌指示器 - 带折线走势的小图标
 */
@Composable
private fun PriceChangeIndicator(change: Double, changeRate: Double) {
    val isPositive = change >= 0
    val color = if (isPositive) Color(0xFFA53321) else Color(0xFF006A61) // 涨红跌绿

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 折线走势图标
        Icon(
            painter = painterResource(
                id = if (isPositive) R.drawable.ic_price_trend_up else R.drawable.ic_price_trend_down
            ),
            contentDescription = null,
            modifier = Modifier.size(10.dp, 6.dp),
            tint = color
        )

        Spacer(modifier = Modifier.width(2.dp))

        Text(
            text = "${if (isPositive) "+" else ""}${formatPrice(change)}  ${formatPrice(changeRate)}%",
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

/**
 * 迷你折线图
 */
@Composable
private fun SparklineChart(
    data: List<DailyPrice>,
    modifier: Modifier = Modifier
) {
    val primaryColor = Primary
    val lineColor = Primary.copy(alpha = 0.3f)

    Canvas(modifier = modifier) {
        if (data.isEmpty()) return@Canvas

        val prices = data.mapNotNull { it.avgPrice }
        if (prices.isEmpty()) return@Canvas

        val minPrice = prices.minOrNull() ?: return@Canvas
        val maxPrice = prices.maxOrNull() ?: return@Canvas
        val priceRange = if (maxPrice - minPrice == 0.0) 1.0 else maxPrice - minPrice

        val width = size.width
        val height = size.height
        val stepX = width / (data.size - 1).coerceAtLeast(1)

        // 绘制背景线
        val bgPath = Path().apply {
            data.forEachIndexed { index, _ ->
                val x = index * stepX
                val y = height / 2
                if (index == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
        drawPath(
            path = bgPath,
            color = lineColor,
            style = Stroke(width = 1.dp.toPx())
        )

        // 绘制数据线
        val path = Path().apply {
            data.forEachIndexed { index, item ->
                val x = index * stepX
                val y = if (item.avgPrice != null) {
                    height - ((item.avgPrice - minPrice) / priceRange * height).toFloat()
                } else {
                    height / 2
                }
                if (index == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
        drawPath(
            path = path,
            color = primaryColor,
            style = Stroke(width = 1.5.dp.toPx())
        )

        // 绘制最后一个点的圆点
        if (data.isNotEmpty()) {
            val lastPrice = data.last().avgPrice
            if (lastPrice != null) {
                val x = (data.size - 1) * stepX
                val y = height - ((lastPrice - minPrice) / priceRange * height).toFloat()
                drawCircle(
                    color = primaryColor,
                    radius = 3.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }
    }
}

/**
 * Tab选择区域
 */
@Composable
private fun CountryProductTabSection(
    selectedTab: Int,
    selectedSort: String,
    onTabSelected: (Int) -> Unit,
    onSortSelected: (String) -> Unit
) {
    Column(modifier = Modifier.background(Color.White)) {
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
                Text(
                    text = "综合推荐",
                    fontSize = 14.sp,
                    fontWeight = if (selectedSort == "comprehensive") FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selectedSort == "comprehensive") TextPrimary else Color(0xFF3C4947),
                    modifier = Modifier.clickable { onSortSelected("comprehensive") }
                )

// 价格排序（整个区域可点击，cycling：综合→升序→降序→综合）
                Column(
                    modifier = Modifier.clickable {
                        when (selectedSort) {
                            "comprehensive" -> onSortSelected("price_asc")
                            "price_asc" -> onSortSelected("price_desc")
                            "price_desc" -> onSortSelected("price_asc")
                            else -> onSortSelected("price_asc")
                        }
                    },
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
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
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

        Divider(color = Border)
    }
}

/**
 * 厂号项
 */
@Composable
private fun FactoryItem(
    factory: CountryProductFactory,
    country: String,
    onClick: () -> Unit,
    isInquiryTab: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .border(
                width = 0.5.dp,
                color = Border,
                shape = RoundedCornerShape(0.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧内容
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // 第一行：厂号 + 价格
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = factory.countryFactory ?: factory.factoryNo ?: "未知厂号",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Row(verticalAlignment = Alignment.Bottom) {
                    val hasValidPrice = factory.priceMin != null && factory.priceMax != null && factory.priceMin > 0 && factory.priceMax > 0
                    if (hasValidPrice) {
                        Text(
                            text = "¥ ${formatPrice(factory.priceMin!!)} - ${formatPrice(factory.priceMax!!)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Primary
                        )
                        Text(
                            text = "/kg ",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Normal,
                            color = TextPrimary
                        )
                    } else {
                        Text(
                            text = "协商报价",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextHint
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 第二行：商家名称标签 + 统计
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 商家名称标签 - 支持横向滚动
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    factory.merchantNames?.take(10)?.forEach { name ->
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFF3F6F5), RoundedCornerShape(2.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                androidx.compose.foundation.Image(
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

                // 统计
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${factory.merchantCount}",
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
                            .background(Color(0xFF9DA4A3), RoundedCornerShape(50))
                    )

                    Text(
                        text = "${factory.offerCount}",
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

        // 右侧箭头
        Icon(
            painter = painterResource(id = R.drawable.ic_right),
            contentDescription = null,
            modifier = Modifier
                .padding(start = 8.dp)
                .size(16.dp),
            tint = Color(0xFF9DA4A3)
        )
    }
}

private fun formatPrice(price: Double): String {
    return if (price == price.toLong().toDouble()) {
        price.toLong().toString()
    } else {
        String.format(Locale.US, "%.1f", price)
    }
}

/**
 * 国家+产品趋势图
 */
@Composable
private fun CountryTrendChart(
    data: List<DailyPrice>,
    modifier: Modifier = Modifier
) {
    val primaryColor = Primary
    val lineColor = Primary.copy(alpha = 0.3f)
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    Column(modifier = modifier) {
        // 图表 - 使用固定高度而不是 weight
        Box(modifier = Modifier.fillMaxWidth().height(120.dp)) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(data) {
                        detectTapGestures { offset ->
                            val stepX = size.width.toFloat() / (data.size - 1).coerceAtLeast(1)
                            val index = ((offset.x / stepX) + 0.5f).toInt().coerceIn(0, data.size - 1)
                            selectedIndex = if (selectedIndex == index) null else index
                        }
                    }
            ) {
                if (data.isEmpty()) return@Canvas

                val prices = data.mapNotNull { it.avgPrice }
                if (prices.isEmpty()) return@Canvas

                val minPrice = prices.minOrNull() ?: return@Canvas
                val maxPrice = prices.maxOrNull() ?: return@Canvas
                val priceRange = if (maxPrice - minPrice == 0.0) 1.0 else maxPrice - minPrice

                val width = size.width
                val height = size.height
                val stepX = width / (data.size - 1).coerceAtLeast(1)

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

                data.forEachIndexed { index, item ->
                    if (item.avgPrice != null) {
                        val x = index * stepX
                        val y = if (item.avgPrice != null) {
                            height - ((item.avgPrice - minPrice) / priceRange * height).toFloat()
                        } else {
                            height / 2
                        }

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
                data.forEachIndexed { index, item ->
                    if (item.avgPrice != null) {
                        val x = index * stepX
                        val y = height - ((item.avgPrice - minPrice) / priceRange * height).toFloat()

                        // 选中的点放大
                        val radius = if (selectedIndex == index) 6.dp.toPx() else 3.dp.toPx()

                        drawCircle(
                            color = primaryColor,
                            radius = radius,
                            center = Offset(x, y)
                        )
                    }
                }
            }

            // Tooltip - 显示在点击位置上方
            selectedIndex?.let { index ->
                val item = data[index]
                if (item.avgPrice != null) {
                    val prices = data.mapNotNull { it.avgPrice }
                    val minPrice = prices.minOrNull() ?: 0.0
                    val maxPrice = prices.maxOrNull() ?: 1.0
                    val priceRange = if (maxPrice - minPrice == 0.0) 1.0 else maxPrice - minPrice
                    val chartHeight = 120.dp
                    val chartWidth = 300.dp // 估算值
                    val stepX = chartWidth.value / (data.size - 1).coerceAtLeast(1)
                    val x = index * stepX
                    val y = chartHeight.value - ((item.avgPrice - minPrice) / priceRange * chartHeight.value).toFloat()

                    // tooltip 放在图表下方
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .offset(y = (-36).dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .background(Color.White, RoundedCornerShape(4.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .shadow(2.dp, RoundedCornerShape(4.dp)),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${item.date} ",
                                fontSize = 11.sp,
                                color = TextHint
                            )
                            Text(
                                text = "${String.format("%.1f", item.avgPrice)}${item.priceUnit ?: "元/kg"}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Primary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // X轴日期标签
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val datesToShow = if (data.size > 7) {
                listOf(0, data.size / 4, data.size / 2, data.size * 3 / 4, data.size - 1)
            } else {
                data.indices.toList()
            }

            datesToShow.forEach { index ->
                if (index < data.size) {
                    Text(
                        text = data[index].date,
                        fontSize = 10.sp,
                        color = TextHint
                    )
                }
            }
        }
    }
}

/**
 * 搜索标签 - 带删除功能的可点击标签
 * 设计来源：Figma - node-id: 158-182
 */
@Composable
fun SearchTag(
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
