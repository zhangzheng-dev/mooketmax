package com.mooket.app.ui.screens.brandproduct

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mooket.app.R
import com.mooket.app.data.model.BrandDetail
import com.mooket.app.data.model.BrandProductSummary
import com.mooket.app.ui.theme.*
import java.util.Locale

/**
 * 品牌+产品详情页（搜索结果页）
 * 样式与 ProductDetailScreen 完全一致
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BrandProductDetailScreen(
    brandName: String,
    productName: String,
    category: String,
    onBackClick: () -> Unit,
    onNavigateToProduct: (productId: Int, productName: String, category: String) -> Unit,
    onNavigateToBrand: (brandName: String, category: String) -> Unit,
    onCountryFactoryProductClick: (String, String, String, String) -> Unit,
    viewModel: BrandProductDetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    // 删除标签导航状态：null=无删除，"brand"=删品牌跳产品页，"product"=删产品跳品牌页
    var pendingDeleteType by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(brandName, productName, category) {
        viewModel.loadBrandProductDetail(brandName, productName, category)
    }

    // 处理标签删除导航
    LaunchedEffect(pendingDeleteType) {
        when (pendingDeleteType) {
            "brand" -> {
                // 删除品牌标签 → 跳转产品详情页
                val productId = uiState.brandDetail?.summaries?.firstOrNull()?.productId ?: 0
                val prodName = uiState.brandDetail?.summaries?.firstOrNull()?.productName ?: productName
                if (productId > 0) {
                    onNavigateToProduct(productId, prodName, category)
                }
            }
            "product" -> {
                // 删除产品标签 → 跳转品牌详情页
                onNavigateToBrand(brandName, category)
            }
        }
        pendingDeleteType = null
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

                        // 品牌+产品搜索框
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                SearchTag(
                                    text = brandName,
                                    onClick = { pendingDeleteType = "brand" }
                                )
                                SearchTag(
                                    text = productName,
                                    onClick = { pendingDeleteType = "product" }
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
            } else if (uiState.brandDetail != null) {
                val detail = uiState.brandDetail!!

                // 数据看板 - 非吸顶，随内容滚动
                item {
                    BrandProductDashboard(
                        brandName = brandName,
                        productName = productName,
                        offerCount = if (uiState.selectedTab == 1) detail.todayInquiryCount + detail.yesterdayInquiryCount else detail.todayOfferCount + detail.yesterdayOfferCount,
                        priceMin = detail.priceMin,
                        priceMax = detail.priceMax,
                        merchantCount = detail.merchantCount ?: 0,
                        factoryCount = detail.factoryCount,
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
                stickyHeader(key = "brand_product_tab") {
                    BrandProductTabSection(
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

                itemsIndexed(
                    items = uiState.currentSummaries,
                    key = { index, summary -> summary.productId * 10000 + (uiState.currentPage - 1) * 10 + index }
                ) { index, summary ->
                    LaunchedEffect(index) {
                        if (index >= uiState.currentSummaries.size - 3 && uiState.hasMorePages && !uiState.isLoadingMore) {
                            viewModel.loadMore()
                        }
                    }

                    BrandProductSummaryItem(
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
 * 数据看板 - 与 ProductDetailScreen 的 DataDashboard 完全一致
 */
@Composable
private fun BrandProductDashboard(
    brandName: String,
    productName: String,
    offerCount: Long,
    priceMin: Double?,
    priceMax: Double?,
    merchantCount: Int,
    factoryCount: Int,
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
            // 左侧：品牌名 + 产品名 + 近2日报盘数
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = brandName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Text(
                        text = productName,
                        fontSize = 16.sp,
                        color = Color(0xFF6C7A77)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isInquiryTab) "近2日求购" else "近2日报盘",
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

            // 右侧：价格区间 + 商家数 + 工厂数
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

                    val priceText = if (priceMin != null && priceMin > 0 && priceMax != null && priceMax >= priceMin) {
                        "¥${formatPrice(priceMin)} - ${formatPrice(priceMax)}"
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

                Divider(
                    modifier = Modifier.width(100.dp),
                    color = Color(0xFFEFF5F3)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 商家数和工厂数
                Row {
                    Column {
                        Text(
                            text = "商家数",
                            fontSize = 10.sp,
                            color = Color(0xFF3C4947).copy(alpha = 0.5f)
                        )
                        Text(
                            text = "$merchantCount",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    Column {
                        Text(
                            text = "工厂数",
                            fontSize = 10.sp,
                            color = Color(0xFF3C4947).copy(alpha = 0.5f)
                        )
                        Text(
                            text = "$factoryCount",
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
 * Tab选择区域 - 与 ProductDetailScreen 的 TabSection 完全一致
 */
@Composable
private fun BrandProductTabSection(
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
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(0.dp)
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
 * 品牌+产品汇总项 - 与 ProductDetailScreen 的 ProductSummaryItem 完全一致
 */
@Composable
private fun BrandProductSummaryItem(
    summary: BrandProductSummary,
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
                // 第一行：国家厂号 + 价格/kg
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
                        if (summary.priceMin != null && summary.priceMin > 0) {
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
    summary.merchantNames
                            ?.filter { it.isNotBlank() }
                            ?.forEach { fullName ->
                                val parts = fullName.split("|")
                                val displayName = parts.firstOrNull()?.takeIf { it.isNotBlank() }
                                    ?: parts.getOrElse(1) { fullName }
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
                                            text = displayName,
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
                            text = "${summary.merchantCount ?: 0}",
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
                            text = "${summary.offerCount ?: 0}",
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

            Icon(
                painter = painterResource(id = R.drawable.ic_right),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Color(0xFF9DA4A3)
            )
        }
    }
}

/**
 * 搜索标签
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
