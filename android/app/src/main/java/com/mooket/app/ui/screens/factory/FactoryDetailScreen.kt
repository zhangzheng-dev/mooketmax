package com.mooket.app.ui.screens.factory

import androidx.compose.animation.animateContentSize
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mooket.app.R
import com.mooket.app.data.model.FactoryProduct
import com.mooket.app.ui.theme.*
import com.mooket.app.ui.util.CountryFlagUtil

/**
 * 厂号详情页
 * 设计来源：Figma - node-id: 158-341 (搜索结果/国家+厂号)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FactoryDetailScreen(
    country: String,
    factoryNo: String,
    category: String,
    onBackClick: () -> Unit,
    onProductClick: (country: String, factoryNo: String, productId: Int, productName: String) -> Unit,
    onSearchDelete: (String) -> Unit, // category -> go back to Search
    viewModel: FactoryDetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(country, factoryNo, category) {
        viewModel.loadFactoryDetail(country, factoryNo, category)
    }

    // 加载更多检测 - currentProducts.size 作为 key 确保数据加载后重启检查
    LaunchedEffect(listState, uiState.currentProducts.size) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index to listState.layoutInfo.totalItemsCount }
            .distinctUntilChanged()
            .debounce(150)
            .collect { (lastIndex, totalCount) ->
                if (lastIndex != null && totalCount > 0 && uiState.hasMorePages && !uiState.isLoadingMore) {
                    val atBottom = lastIndex >= totalCount - 1
                    if (atBottom) {
                        viewModel.loadMore()
                    }
                }
            }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        state = listState
    ) {
        // 顶部搜索栏 - 非吸顶，随内容滚动
        item {
            FactoryDetailHeader(
                country = uiState.factoryDetail?.country ?: country,
                factoryNo = uiState.factoryDetail?.factoryNo ?: factoryNo,
                onBackClick = onBackClick,
                onSearchDelete = onSearchDelete,
                category = category
            )
        }

        if (uiState.isLoading && uiState.factoryDetail == null) {
            item {
                Box(
                    modifier = Modifier
                        .fillParentMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Primary)
                }
            }
        } else if (uiState.error != null && uiState.factoryDetail == null) {
            item {
                Box(
                    modifier = Modifier
                        .fillParentMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = uiState.error ?: "加载失败", color = TextHint)
                }
            }
        } else {
            uiState.factoryDetail?.let { detail ->
                // 看板区域 - 非吸顶
                item {
                    FactoryDashboard(
                        country = detail.country,
                        factoryNo = detail.factoryNo,
                        productCount = detail.productCount,
                        inquiryCount = detail.inquiryCount,
                        recentOfferCount = detail.recentOfferCount
                    )
                }

                // 浅绿间隔 - 不吸顶，随内容滚动
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .background(Background)
                    )
                }

                // Tab选择区域 - 吸顶
                stickyHeader(key = "factory_tab") {
                    FactoryTabBar(
                        selectedTab = uiState.selectedTab,
                        selectedSort = uiState.selectedSort,
                        onTabSelect = { viewModel.selectTab(it) },
                        onSortSelect = { viewModel.selectSort(it) }
                    )
                }

                // 列表刷新指示器
                if (uiState.isListRefreshing) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Primary, modifier = Modifier.size(24.dp))
                        }
                    }
                }

                // 产品列表
                items(uiState.currentProducts, key = { it.productId }) { product ->
                    FactoryProductItem(
                        product = product,
                        isInquiryTab = uiState.selectedTab == 1,
                        onClick = { onProductClick(country, factoryNo, product.productId, product.productName) }
                    )
                }

                // 加载更多
                if (uiState.isLoadingMore) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Primary, modifier = Modifier.size(24.dp))
                        }
                    }
                }

                // 没有更多了
                if (!uiState.hasMorePages && uiState.currentProducts.isNotEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "没有更多了～", color = TextHint, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 顶部搜索栏 - 添加 statusBarsPadding() 避免被通知栏遮挡
 */
@Composable
private fun FactoryDetailHeader(
    country: String,
    factoryNo: String,
    onBackClick: () -> Unit,
    onSearchDelete: (String) -> Unit,
    category: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .statusBarsPadding() // 添加这个避免被通知栏遮挡
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 返回按钮
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = "返回",
            modifier = Modifier
                .size(24.dp)
                .rotate(180f)
                .clickable { onBackClick() },
            tint = TextPrimary
        )

        Spacer(modifier = Modifier.width(4.dp))

        // 搜索标签框
        Box(
            modifier = Modifier
                .weight(1f)
                .height(42.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFEFF5F3))
                .border(
                    width = 1.dp,
                    color = Color(0xFFBBCAC6).copy(alpha = 0.3f),
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(start = 7.dp, end = 13.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            // 左侧国家+厂号标签 - 点击删除回到搜索页
            SearchTag(
                text = "$country$factoryNo",
                onClick = { onSearchDelete(category) }
            )

            // 右侧搜索图标
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
}

/**
 * 数据看板
 */
@Composable
private fun FactoryDashboard(
    country: String,
    factoryNo: String,
    productCount: Int,
    inquiryCount: Int,
    recentOfferCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：国旗 + 厂号 + 统计
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 国旗emoji
                    Text(
                        text = CountryFlagUtil.getFlagEmoji(country),
                        fontSize = 24.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$country$factoryNo",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 统计数据
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    StatItem(label = "产品数", value = productCount.toString())
                    StatItem(label = "求购数", value = inquiryCount.toString())
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

            // 右侧：近2日报盘数
            Column(
                modifier = Modifier,
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "近2日报盘",
                    fontSize = 10.sp,
                    color = TextHint
                )
                Text(
                    text = recentOfferCount.toString(),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Primary
                )
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, fontSize = 10.sp, color = TextHint)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    }
}

/**
 * Tab栏 - 价格排序样式跟国家详情页一样
 */
@Composable
private fun FactoryTabBar(
    selectedTab: Int,
    selectedSort: String,
    onTabSelect: (Int) -> Unit,
    onSortSelect: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：报盘/求购 Tab（选中项下方有绿色下划线）
            Row(
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.clickable { onTabSelect(0) },
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
                            .height(if (selectedTab == 0) 3.dp else 2.dp)
                            .background(if (selectedTab == 0) Primary else Color.Transparent)
                    )
                }

                Column(
                    modifier = Modifier.clickable { onTabSelect(1) },
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
                            .height(if (selectedTab == 1) 3.dp else 2.dp)
                            .background(if (selectedTab == 1) Primary else Color.Transparent)
                    )
                }
            }

            // 右侧：综合推荐/价格 - 价格排序样式跟国家详情页一样（带背景的箭头）
            Row(
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "综合推荐",
                    fontSize = 14.sp,
                    fontWeight = if (selectedSort == "comprehensive") FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selectedSort == "comprehensive") TextPrimary else Color(0xFF3C4947),
                    modifier = Modifier.clickable { onSortSelect("comprehensive") }
                )

                // 价格排序（整个区域可点击）
                Column(
                    modifier = Modifier.clickable { onSortSelect("price") },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "价格",
                            fontSize = 14.sp,
                            fontWeight = if (selectedSort.startsWith("price")) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selectedSort.startsWith("price")) TextPrimary else Color(0xFF3C4947)
                        )
                        // 双箭头 - 升序时上箭头背景变绿，降序时下箭头背景变绿
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

        Divider(color = Border, thickness = 0.5.dp)
    }
}

/**
 * 产品项 - 右侧箭头使用 ic_right
 */
@Composable
private fun FactoryProductItem(
    product: FactoryProduct,
    isInquiryTab: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
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
                    text = product.productName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )

                // 价格
                if (product.priceMin != null && product.priceMin > 0 && product.priceMax != null && product.priceMax > 0) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "¥ ${formatPrice(product.priceMin)} - ${formatPrice(product.priceMax)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Primary
                        )
                        Text(
                            text = "/kg",
                            fontSize = 10.sp,
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

            // 第二行：商家标签（横向滚动）+ 统计
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 商家标签 - 支持横向滚动
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    product.merchantNames.take(10).forEach { merchantName ->
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
                                    text = merchantName,
                                    fontSize = 11.sp,
                                    color = Color(0xFF3C4947)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 商家数/报盘数
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${product.merchantCount}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(text = "商家", fontSize = 11.sp, color = Color(0xFF3C4947))

                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .background(Color(0xFF9DA4A3), RoundedCornerShape(2.dp))
                    )

                    Text(
                        text = "${product.offerCount}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(text = if (isInquiryTab) "求购" else "报盘", fontSize = 11.sp, color = Color(0xFF3C4947))
                }
            }
        }

        // 右侧箭头 - 使用 ic_right 图标，跟国家详情页样式一样
        Icon(
            painter = painterResource(id = R.drawable.ic_right),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = Color(0xFF9DA4A3)
        )
    }

    // 分割线
    Divider(color = Border, thickness = 0.5.dp)
}

private fun formatPrice(price: Double?): String {
    return price?.let { String.format("%.1f", it) } ?: "-"
}

// 扩展 isAtBottom 属性
private val androidx.compose.foundation.lazy.LazyListState.isAtBottom: Boolean
    get() = layoutInfo.visibleItemsInfo.lastOrNull()?.index == layoutInfo.totalItemsCount - 1

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
