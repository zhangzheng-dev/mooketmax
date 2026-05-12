package com.mooket.app.ui.screens.brand

import androidx.compose.animation.animateContentSize
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mooket.app.R
import com.mooket.app.data.model.BrandProductSummary
import com.mooket.app.ui.theme.*
import java.util.Locale

/**
 * 品牌详情页
 * 样式参考国家详情页，去除热门厂号/产品功能
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BrandDetailScreen(
    brandName: String,
    category: String,
    onBackClick: () -> Unit,
    onProductClick: (Int, String, String) -> Unit,
    onBrandProductClick: (String, String, String) -> Unit,
    onSearchDelete: (String) -> Unit,
    viewModel: BrandDetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(brandName, category) {
        viewModel.loadBrandDetail(brandName, category)
    }

    // 滚动到底部触发加载更多（canScrollForward 模式，CountryProductScreen 同款）
    val shouldLoadMore = !listState.canScrollForward
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
                                .padding(start = 9.dp, end = 13.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            SearchTag(
                                text = brandName,
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
            } else if (uiState.brandDetail != null) {
                val detail = uiState.brandDetail!!

                // 数据看板 - 非吸顶，随内容滚动
                item {
                    BrandDashboard(
                        brandName = detail.brandName,
                        factoryCount = detail.factoryCount,
                        productCount = detail.productCount,
                        todayOfferCount = detail.todayOfferCount,
                        yesterdayOfferCount = detail.yesterdayOfferCount,
                        todayInquiryCount = detail.todayInquiryCount,
                        yesterdayInquiryCount = detail.yesterdayInquiryCount,
                        selectedTab = uiState.selectedTab
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
                stickyHeader(key = "brand_tab") {
                    BrandTabSection(
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
                    key = { _, summary -> summary.productId ?: summary.hashCode() }
                ) { index, summary ->
                    BrandProductItem(
                        summary = summary,
                        isInquiryTab = uiState.selectedTab == 1,
                        onClick = { onBrandProductClick(brandName, summary.productName ?: "", category) }
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
 * 品牌数据看板
 */
@Composable
private fun BrandDashboard(
    brandName: String,
    factoryCount: Int,
    productCount: Int,
    todayOfferCount: Long,
    yesterdayOfferCount: Long,
    todayInquiryCount: Long,
    yesterdayInquiryCount: Long,
    selectedTab: Int
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
            // 左侧：品牌名称 + 工厂数/产品数
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = brandName,
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
                            text = "产品数",
                            fontSize = 10.sp,
                            color = Color(0xFF3C4947).copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$productCount",
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
                    text = formatCount(
                        if (selectedTab == 1) todayInquiryCount + yesterdayInquiryCount
                        else todayOfferCount + yesterdayOfferCount
                    ),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Primary
                )
            }
        }
    }
}

/**
 * Tab选择区域
 */
@Composable
private fun BrandTabSection(
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
 * 品牌产品汇总项（合并后：同一产品名合并，厂号横向滚动）
 */
@Composable
private fun BrandProductItem(
    summary: BrandProductSummary,
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
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = summary.productName ?: "",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )

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
                } else if (summary.priceMin != null) {
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

            // 第二行：厂号横向滚动 + 厂号数+报盘数（同一行，样式与 CountryDetailScreen 一致）
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
                    summary.factoryNos.split(",").filter { it.isNotBlank() }.forEach { factoryNo ->
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
                                    text = factoryNo.trim(),
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