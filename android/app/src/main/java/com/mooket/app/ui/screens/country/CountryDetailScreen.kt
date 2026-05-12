package com.mooket.app.ui.screens.country

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.ArrowBackIos
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mooket.app.R
import com.mooket.app.data.model.CountryProductSummary
import com.mooket.app.data.model.HotFactory
import com.mooket.app.data.model.HotProduct
import com.mooket.app.ui.theme.*
import com.mooket.app.ui.util.CountryFlagUtil
import java.util.Locale

/**
 * 国家详情页
 * 设计来源：Figma - node-id: 1796-2741
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CountryDetailScreen(
    country: String,
    category: String,
    onBackClick: () -> Unit,
    onProductClick: (String, String, String) -> Unit, // country, productName, category -> Country+Product
    onFactoryClick: (String, String, String) -> Unit, // country, factoryNo, category -> Country+Factory
    onSearchDelete: (String) -> Unit, // category -> go back to Search
    viewModel: CountryDetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var isHotSectionExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(country, category) {
        viewModel.loadCountryDetail(country, category)
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
                                .padding(start = 9.dp, end = 13.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            SearchTag(
                                text = country,
                                onClick = { onSearchDelete(category) }
                            )

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
            } else if (uiState.countryDetail != null) {
                val detail = uiState.countryDetail!!

                // 数据看板 - 非吸顶，随内容滚动
                item {
                    CountryDashboard(
                        country = detail.country,
                        category = category,
                        factoryCount = detail.factoryCount,
                        merchantCount = detail.merchantCount,
                        offerCount = detail.offerCount,
                        selectedTab = uiState.selectedTab,
                        hotFactories = detail.hotFactories,
                        hotProducts = detail.hotProducts,
                        isExpanded = isHotSectionExpanded,
                        onToggleExpand = { isHotSectionExpanded = !isHotSectionExpanded },
                        onFactoryClick = onFactoryClick,
                        onProductClick = onProductClick
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
                stickyHeader(key = "country_tab") {
                    CountryTabSection(
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

                    CountryProductItem(
                        summary = summary,
                        isInquiryTab = uiState.selectedTab == 1,
                        onClick = { onProductClick(country, summary.productName ?: "", category) }
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
 * 国家数据看板
 */
@Composable
private fun CountryDashboard(
    country: String,
    category: String,
    factoryCount: Int,
    merchantCount: Int,
    offerCount: Long,
    selectedTab: Int,
    hotFactories: List<HotFactory>,
    hotProducts: List<HotProduct>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onFactoryClick: (String, String, String) -> Unit, // country, factoryNo, category
    onProductClick: (String, String, String) -> Unit  // country, productName, category
) {
    // Capture parameters to avoid receiver scope resolution issues in nested lambdas
    val capturedCountry = country
    val capturedCategory = category

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
            // 左侧：国家名称 + 工厂数/商家数
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 国旗emoji
                    val flagEmoji = CountryFlagUtil.getFlagEmoji(country)
                    Text(
                        text = flagEmoji,
                        fontSize = 24.sp
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = country,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "工厂数",
                            fontSize = 10.sp,
                            color = Color(0xFF3C4947).copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$factoryCount",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "商家数",
                            fontSize = 10.sp,
                            color = Color(0xFF3C4947).copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$merchantCount",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }
                }
            }

            // 垂直分割线
            Box(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .width(1.dp)
                    .height(60.dp)
                    .background(Color(0xFFDEE4E1).copy(alpha = 0.5f))
            )

            // 右侧：近2日报盘数/求购数
            Column(
                modifier = Modifier
                    .background(Color.White),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = if (selectedTab == 1) "近2日求购" else "近2日报盘",
                    fontSize = 10.sp,
                    color = Color(0xFF3C4947).copy(alpha = 0.5f)
                )
                Text(
                    text = formatCount(offerCount),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Primary
                )
            }
        }

        // 水平分割线
        Divider(
            modifier = Modifier.padding(top = 12.dp),
            color = Color(0xFFDEE4E1).copy(alpha = 0.5f),
            thickness = 0.5.dp
        )

        // 热门厂号/产品标签 - 可点击展开/收起
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .clickable { onToggleExpand() },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "热门厂号/产品",
                fontSize = 12.sp,
                color = TextPrimary
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isExpanded) "收起" else "展开",
                    fontSize = 12.sp,
                    color = TextPrimary
                )
                Icon(
                    painter = painterResource(id = R.drawable.ic_right),
                    contentDescription = null,
                    modifier = Modifier
                        .size(16.dp)
                        .rotate(if (isExpanded) -90f else 90f),
                    tint = TextPrimary
                )
            }
        }

        // 展开的热门厂号/产品内容 - 左右两列布局
        if (isExpanded) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 左侧：热门厂号
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    hotFactories.forEach { factory ->
                        HotFactoryItem(factory = factory, country = capturedCountry, category = capturedCategory, onClick = onFactoryClick)
                    }
                }

                // 中间分隔线
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(118.dp)
                        .background(Border)
                )

                // 右侧：热门产品
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    hotProducts.forEach { product ->
                        HotProductItem(product = product, country = capturedCountry, category = capturedCategory, onClick = onProductClick)
                    }
                }
            }
        }
    }
}

/**
 * 热门厂号项
 */
@Composable
private fun HotFactoryItem(
    factory: HotFactory,
    country: String,
    category: String,
    onClick: (String, String, String) -> Unit
) {
    val (bgColor, rankColor) = when (factory.rank) {
        1 -> Color(0xFFFFF9F0) to Color(0xFF906134)  // 奶油色背景，棕色排名
        2 -> Color(0xFFF5F8FF) to Color(0xFF4B5462)  // 浅蓝色背景，灰色排名
        3 -> Color(0xFFFFF9F8) to Color(0xFF80521E)  // 浅粉色背景，棕色排名
        else -> Color(0xFFF3F6F5) to Color(0xFF9DA4A3)
    }
    val capturedCountry = country
    val capturedCategory = category

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(2.dp))
            .clickable { onClick(capturedCountry, factory.factoryNo, capturedCategory) }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 排名标签
        Box(
            modifier = Modifier.size(18.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(15.dp)
                    .background(rankColor, RoundedCornerShape(2.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${factory.rank}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        // 厂号名称
        Text(
            text = factory.factoryNo,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )

        // 报盘数量
        Text(
            text = formatCount(factory.offerCount.toLong()),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )

        // 右侧箭头
        Icon(
            painter = painterResource(id = R.drawable.ic_right),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = Color(0xFF9DA4A3)
        )
    }
}

/**
 * 热门产品项
 */
@Composable
private fun HotProductItem(
    product: HotProduct,
    country: String,
    category: String,
    onClick: (String, String, String) -> Unit
) {
    val (bgColor, rankColor) = when (product.rank) {
        1 -> Color(0xFFFFF9F0) to Color(0xFF906134)  // 奶油色背景，棕色排名
        2 -> Color(0xFFF5F8FF) to Color(0xFF4B5462)  // 浅蓝色背景，灰色排名
        3 -> Color(0xFFFFF9F8) to Color(0xFF80521E)  // 浅粉色背景，棕色排名
        else -> Color(0xFFF3F6F5) to Color(0xFF9DA4A3)
    }
    val capturedCountry = country
    val capturedCategory = category

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(2.dp))
            .clickable { onClick(capturedCountry, product.productName, capturedCategory) }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 排名标签
        Box(
            modifier = Modifier.size(18.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(15.dp)
                    .background(rankColor, RoundedCornerShape(2.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${product.rank}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        // 产品名称
        Text(
            text = product.productName,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )

        // 报盘数量
        Text(
            text = formatCount(product.offerCount.toLong()),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )

        // 右侧箭头
        Icon(
            painter = painterResource(id = R.drawable.ic_right),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = Color(0xFF9DA4A3)
        )
    }
}

/**
 * Tab选择区域
 */
@Composable
private fun CountryTabSection(
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

        Divider(color = Border)
    }
}

/**
 * 国家产品汇总项
 */
@Composable
private fun CountryProductItem(
    summary: CountryProductSummary,
    isInquiryTab: Boolean,
    onClick: () -> Unit
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
        // 左侧内容区
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // 第一行：产品名称 + 价格
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = summary.productName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )

                // 价格（主价格16sp Primary色，单位10sp TextPrimary色，底部对齐）
                if (summary.priceMin != null && summary.priceMin > 0 && summary.priceMax != null && summary.priceMax > 0) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "¥ ${formatPrice(summary.priceMin)} - ${formatPrice(summary.priceMax)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Primary
                        )
                        Text(
                            text = "/kg",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Normal,
                            color = TextPrimary
                        )
                    }
                } else if (summary.priceMin != null && summary.priceMin > 0) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "¥ ${formatPrice(summary.priceMin)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Primary
                        )
                        Text(
                            text = "/kg",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Normal,
                            color = TextPrimary
                        )
                    }
                } else {
                    Text(
                        text = "暂无报价",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 第二行：厂号 + 统计
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    summary.factoryNos.take(10).forEach { factoryNo ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_factory),
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = Color(0xFF244C56).copy(alpha = 0.4f)
                            )
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFF3F6F5), RoundedCornerShape(2.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = factoryNo,
                                    fontSize = 11.sp,
                                    color = Color(0xFF3C4947)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${summary.factoryCount}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "厂号",
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

private fun formatCount(count: Long): String {
    return when {
        count >= 10000 -> String.format(Locale.US, "%.1fk", count / 1000.0)
        count >= 1000 -> String.format(Locale.US, "%.1fk", count / 1000.0)
        else -> count.toString()
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
