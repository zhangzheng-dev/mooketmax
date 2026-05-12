package com.mooket.app.ui.screens.merchant

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mooket.app.R
import com.mooket.app.data.model.EmployeeOffer
import com.mooket.app.data.model.MerchantDetail
import com.mooket.app.data.model.OfferSummary
import com.mooket.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * 价格排序枚举
 */
private enum class SortOrder {
    NONE,   // 默认顺序
    ASC,    // 升序
    DESC    // 降序
}

/**
 * 商家详情页
 * 设计来源：Figma - node-id: 2-3871 (上半部分) & 2-3960 (下半部分)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerchantScreen(
    merchantId: Long,
    category: String,
    onBackClick: () -> Unit,
    viewModel: MerchantViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }
    var originalText by remember { mutableStateOf("") }

    // 国家厂号筛选状态
    var showCountryFactoryFilter by remember { mutableStateOf(false) }
    var selectedCountry by remember { mutableStateOf<String?>(null) }
    var selectedFactories by remember { mutableStateOf<Set<String>>(emptySet()) }

    // 地区筛选状态
    var showRegionFilter by remember { mutableStateOf(false) }
    var selectedRegions by remember { mutableStateOf<Set<String>>(emptySet()) }

    // 产品筛选状态
    var showProductFilter by remember { mutableStateOf(false) }
    var selectedProducts by remember { mutableStateOf<Set<String>>(emptySet()) }

    // 货物类型筛选状态
    var showGoodsTypeFilter by remember { mutableStateOf(false) }
    var selectedGoodsTypes by remember { mutableStateOf<Set<String>>(emptySet()) }

    // 饲养方式筛选状态
    var showFeedingMethodFilter by remember { mutableStateOf(false) }
    var selectedFeedingMethods by remember { mutableStateOf<Set<String>>(emptySet()) }

    // 产品展开状态 - 记录哪些产品是展开的（使用索引）
    var expandedIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }

    // 价格排序状态 - NONE: 默认顺序, ASC: 升序, DESC: 降序
    var priceSortOrder by remember { mutableStateOf(SortOrder.NONE) }

    // 综合推荐排序状态 - true: 综合推荐排序, false: 价格排序
    var sortByRecommend by remember { mutableStateOf(true) }

    fun toggleOfferExpand(index: Int) {
        expandedIndices = if (expandedIndices.contains(index)) {
            expandedIndices - index
        } else {
            expandedIndices + index
        }
    }

    // 当merchantId变化时，重置展开状态（不默认展开）
    LaunchedEffect(merchantId) {
        expandedIndices = emptySet()
        viewModel.loadMerchantDetail(merchantId, category)
    }

    // 底部抽屉 - 原文详情
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 40.dp)
            ) {
                // 标题
                Text(
                    text = "原文内容",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 原文内容
                Text(
                    text = if (originalText.isBlank()) "抱歉，暂无原文！" else originalText,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    color = if (originalText.isBlank()) Color(0xFF9DA4A3) else Color(0xFF3C4947)
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (uiState.merchant != null) {
                        Row(
                            modifier = Modifier.height(30.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 商家Logo图标 - Figma: 24dp x 24dp
                            Image(
                                painter = painterResource(id = R.drawable.ic_merchant_logo),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            // 商家名称 - Figma: 20px, 行高30px, 颜色#171D1C
                            Text(
                                text = uiState.merchant!!.merchantShortName ?: uiState.merchant!!.merchantName,
                                fontSize = 20.sp,
                                lineHeight = 30.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF171D1C)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            // 标签和皇冠图标 - Figma: 整体73dp x 20dp, 圆角2.2dp
                            if (!uiState.merchant!!.merchantTags.isNullOrEmpty()) {
                                val firstTag = uiState.merchant!!.merchantTags.split("|").firstOrNull() ?: uiState.merchant!!.merchantTags
                                val isTrusted = firstTag.contains("知名商家")
                                Row(
                                    modifier = Modifier.height(20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 标签背景 - 宽度根据字体内容自适应
                                    Box(
                                        modifier = Modifier
                                            .wrapContentWidth()
                                            .height(20.dp)
                                            .background(Color(0xFF244C56), RoundedCornerShape(2.2.dp))
                                            .padding(horizontal = 7.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = firstTag,
                                            fontSize = 11.sp,
                                            lineHeight = 16.sp,
                                            color = Color(0xFFF2FFFD)
                                        )
                                    }
                                    // 皇冠图标 - Figma: 从43dp开始，宽30dp，高20dp
                                    if (isTrusted) {
                                        Image(
                                            painter = painterResource(id = R.drawable.ic_merchant_crown),
                                            contentDescription = "知名商家",
                                            modifier = Modifier
                                                .offset(x = (-12).dp)
                                                .size(width = 30.dp, height = 20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    Image(
                        painter = painterResource(id = R.drawable.ic_back_arrow),
                        contentDescription = "返回",
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onBackClick() }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Primary)
                }
            }
            uiState.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.error ?: "加载失败",
                        color = Error
                    )
                }
            }
            uiState.merchant != null -> {
                MerchantContent(
                    merchant = uiState.merchant!!,
                    currentProducts = uiState.currentProducts,
                    isLoadingMore = uiState.isLoadingMore,
                    hasMorePages = uiState.hasMorePages,
                    onLoadMore = { viewModel.loadMoreProducts() },
                    selectedTab = uiState.selectedTab,
                    onTabSelected = { viewModel.selectTab(it) },
                    onCopyPhone = { phone ->
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("phone", phone)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "已复制手机号", Toast.LENGTH_SHORT).show()
                    },
                    onCallClick = { phone ->
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:$phone")
                        }
                        context.startActivity(intent)
                    },
                    onViewOriginalText = { text ->
                        originalText = text
                        showBottomSheet = true
                    },
                    showCountryFactoryFilter = showCountryFactoryFilter,
                    selectedCountry = selectedCountry,
                    selectedFactories = selectedFactories,
                    onCountryFactoryClick = {
                        showRegionFilter = false
                        showProductFilter = false
                        showGoodsTypeFilter = false
                        showFeedingMethodFilter = false
                        showCountryFactoryFilter = true
                    },
                    onCountrySelected = { selectedCountry = it },
                    onFactoryToggle = { factory ->
                        selectedFactories = if (selectedFactories.contains(factory)) {
                            selectedFactories - factory
                        } else {
                            selectedFactories + factory
                        }
                    },
                    onFilterReset = {
                        selectedCountry = null
                        selectedFactories = emptySet()
                        showCountryFactoryFilter = false
                    },
                    onFilterConfirm = {
                        showCountryFactoryFilter = false
                    },
                    hasCountrySelection = selectedCountry != null || selectedFactories.isNotEmpty(),
                    showRegionFilter = showRegionFilter,
                    selectedRegions = selectedRegions,
                    onRegionClick = {
                        showCountryFactoryFilter = false
                        showProductFilter = false
                        showGoodsTypeFilter = false
                        showFeedingMethodFilter = false
                        showRegionFilter = true
                    },
                    onRegionToggle = { region ->
                        selectedRegions = if (region in selectedRegions) {
                            selectedRegions - region
                        } else {
                            selectedRegions + region
                        }
                    },
                    onRegionReset = {
                        selectedRegions = emptySet()
                        showRegionFilter = false
                    },
                    onRegionConfirm = { showRegionFilter = false },
                    showProductFilter = showProductFilter,
                    selectedProducts = selectedProducts,
                    onProductClick = {
                        showCountryFactoryFilter = false
                        showRegionFilter = false
                        showGoodsTypeFilter = false
                        showFeedingMethodFilter = false
                        showProductFilter = true
                    },
                    onProductToggle = { product ->
                        selectedProducts = if (product in selectedProducts) {
                            selectedProducts - product
                        } else {
                            selectedProducts + product
                        }
                    },
                    onProductReset = {
                        selectedProducts = emptySet()
                        showProductFilter = false
                    },
                    onProductConfirm = { showProductFilter = false },
                    showGoodsTypeFilter = showGoodsTypeFilter,
                    selectedGoodsTypes = selectedGoodsTypes,
                    onGoodsTypeClick = {
                        showCountryFactoryFilter = false
                        showRegionFilter = false
                        showProductFilter = false
                        showFeedingMethodFilter = false
                        showGoodsTypeFilter = true
                    },
                    onGoodsTypeToggle = { goodsType ->
                        selectedGoodsTypes = if (goodsType in selectedGoodsTypes) {
                            selectedGoodsTypes - goodsType
                        } else {
                            selectedGoodsTypes + goodsType
                        }
                    },
                    onGoodsTypeReset = {
                        selectedGoodsTypes = emptySet()
                        showGoodsTypeFilter = false
                    },
                    onGoodsTypeConfirm = { showGoodsTypeFilter = false },
                    showFeedingMethodFilter = showFeedingMethodFilter,
                    selectedFeedingMethods = selectedFeedingMethods,
                    onFeedingMethodClick = {
                        showCountryFactoryFilter = false
                        showRegionFilter = false
                        showProductFilter = false
                        showGoodsTypeFilter = false
                        showFeedingMethodFilter = true
                    },
                    onFeedingMethodToggle = { method ->
                        selectedFeedingMethods = if (method in selectedFeedingMethods) {
                            selectedFeedingMethods - method
                        } else {
                            selectedFeedingMethods + method
                        }
                    },
                    onFeedingMethodReset = {
                        selectedFeedingMethods = emptySet()
                        showFeedingMethodFilter = false
                    },
                    onFeedingMethodConfirm = { showFeedingMethodFilter = false },
                    expandedIndices = expandedIndices,
                    onToggleOfferExpand = { toggleOfferExpand(it) },
                    priceSortOrder = priceSortOrder,
                    onPriceSortToggle = {
                        sortByRecommend = false
                        priceSortOrder = when (priceSortOrder) {
                            SortOrder.NONE -> SortOrder.ASC
                            SortOrder.ASC -> SortOrder.DESC
                            SortOrder.DESC -> SortOrder.NONE
                        }
                    },
                    sortByRecommend = sortByRecommend,
                    onSortByRecommendToggle = {
                        sortByRecommend = !sortByRecommend
                        priceSortOrder = SortOrder.NONE
                    },
                    modifier = Modifier
                        .padding(paddingValues)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun MerchantContent(
    merchant: MerchantDetail,
    currentProducts: List<OfferSummary>,
    isLoadingMore: Boolean,
    hasMorePages: Boolean,
    onLoadMore: () -> Unit,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onCopyPhone: (String) -> Unit,
    onCallClick: (String) -> Unit,
    onViewOriginalText: (String) -> Unit,
    showCountryFactoryFilter: Boolean,
    selectedCountry: String?,
    selectedFactories: Set<String>,
    onCountryFactoryClick: () -> Unit,
    onCountrySelected: (String?) -> Unit,
    onFactoryToggle: (String) -> Unit,
    onFilterReset: () -> Unit,
    onFilterConfirm: () -> Unit,
    hasCountrySelection: Boolean,
    showRegionFilter: Boolean,
    selectedRegions: Set<String>,
    onRegionClick: () -> Unit,
    onRegionToggle: (String) -> Unit,
    onRegionReset: () -> Unit,
    onRegionConfirm: () -> Unit,
    showProductFilter: Boolean,
    selectedProducts: Set<String>,
    onProductClick: () -> Unit,
    onProductToggle: (String) -> Unit,
    onProductReset: () -> Unit,
    onProductConfirm: () -> Unit,
    showGoodsTypeFilter: Boolean,
    selectedGoodsTypes: Set<String>,
    onGoodsTypeClick: () -> Unit,
    onGoodsTypeToggle: (String) -> Unit,
    onGoodsTypeReset: () -> Unit,
    onGoodsTypeConfirm: () -> Unit,
    showFeedingMethodFilter: Boolean,
    selectedFeedingMethods: Set<String>,
    onFeedingMethodClick: () -> Unit,
    onFeedingMethodToggle: (String) -> Unit,
    onFeedingMethodReset: () -> Unit,
    onFeedingMethodConfirm: () -> Unit,
    expandedIndices: Set<Int>,
    onToggleOfferExpand: (Int) -> Unit,
    priceSortOrder: SortOrder,
    onPriceSortToggle: () -> Unit,
    sortByRecommend: Boolean,
    onSortByRecommendToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val sheetState = rememberModalBottomSheetState()
    var currentFilterType by remember { mutableStateOf("") }

    // filtered list
    val list by derivedStateOf {
        var result = currentProducts
        val hasCountryFilter = selectedCountry != null
        val hasFactoryFilter = selectedFactories.isNotEmpty()
        val hasRegionFilter = selectedRegions.isNotEmpty()
        val hasProductFilter = selectedProducts.isNotEmpty()
        val hasGoodsTypeFilter = selectedGoodsTypes.isNotEmpty()
        val hasFeedingFilter = selectedFeedingMethods.isNotEmpty()
        val hasAnyFilter = hasCountryFilter || hasFactoryFilter || hasRegionFilter || hasProductFilter || hasGoodsTypeFilter || hasFeedingFilter
        if (hasAnyFilter) {
            result = result.filter { offer ->
                if (hasCountryFilter || hasFactoryFilter) {
                    val offerFactoryKey = "${offer.country}${offer.factoryNo}"
                    val countryMatch = selectedCountry == null || offer.country == selectedCountry
                    val factoryMatch = selectedFactories.isEmpty() || selectedFactories.contains(offerFactoryKey)
                    if (!countryMatch || !factoryMatch) return@filter false
                }
                if (hasRegionFilter) {
                    val regionMatch = offer.employeeOffers?.any { employee ->
                        val locations = employee.goodsLocation?.split("/")?.map { it.trim() } ?: emptyList()
                        locations.any { it in selectedRegions }
                    } ?: false
                    if (!regionMatch) return@filter false
                }
                if (hasProductFilter && offer.productName !in selectedProducts) return@filter false
                if (hasGoodsTypeFilter) {
                    val goodsTypeMatch = offer.employeeOffers?.any { it.goodsType in selectedGoodsTypes } ?: false
                    if (!goodsTypeMatch) return@filter false
                }
                if (hasFeedingFilter) {
                    val feedingMatch = offer.employeeOffers?.any { it.feedingMethod in selectedFeedingMethods } ?: false
                    if (!feedingMatch) return@filter false
                }
                true
            }
        }
        if (sortByRecommend) {
            result = result.sortedByDescending { it.employeeOffers?.size ?: 0 }
        } else {
            result = when (priceSortOrder) {
                SortOrder.NONE -> result.sortedByDescending { it.publishTime }
                SortOrder.ASC -> result.sortedBy { offer ->
                    offer.employeeOffers?.mapNotNull { it.price }?.minOrNull() ?: offer.price ?: Double.MAX_VALUE
                }
                SortOrder.DESC -> result.sortedByDescending { offer ->
                    offer.employeeOffers?.mapNotNull { it.price }?.maxOrNull() ?: offer.price ?: Double.MIN_VALUE
                }
            }
        }
        result
    }

    // scroll to bottom load more
    LaunchedEffect(listState, list.size, hasMorePages, isLoadingMore) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItem >= 10 && lastVisibleItem >= totalItems - 3 && hasMorePages && !isLoadingMore
        }.collect { shouldLoadMore ->
            if (shouldLoadMore && list.isNotEmpty()) onLoadMore()
        }
    }

    // root Box: LazyColumn for scroll + filter overlays
    Box(modifier = modifier.fillMaxSize().background(Background)) {
        LazyColumn(state = listState) {
            // DataDashboard - item (not sticky)
            item(key = "merchant_dashboard") {
                DataDashboard(merchant = merchant)
            }

            // Light green interval - item (not sticky)
            item(key = "merchant_interval") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .background(Color(0xFFF4FBF8))
                )
            }

            // Combined Tab + Filter stickyHeader
            stickyHeader(key = "merchant_sticky") {
                Column {
                    TabSection(
                        selectedTab = selectedTab,
                        onTabSelected = onTabSelected,
                        priceSortOrder = priceSortOrder,
                        onPriceSortToggle = onPriceSortToggle,
                        sortByRecommend = sortByRecommend,
                        onSortByRecommendToggle = onSortByRecommendToggle
                    )
                    FilterSection(
                        onCountryFactoryClick = onCountryFactoryClick,
                        hasCountrySelection = hasCountrySelection,
                        onRegionClick = onRegionClick,
                        hasRegionSelection = selectedRegions.isNotEmpty(),
                        onProductClick = onProductClick,
                        hasProductSelection = selectedProducts.isNotEmpty(),
                        onGoodsTypeClick = onGoodsTypeClick,
                        hasGoodsTypeSelection = selectedGoodsTypes.isNotEmpty(),
                        onFeedingMethodClick = onFeedingMethodClick,
                        hasFeedingMethodSelection = selectedFeedingMethods.isNotEmpty(),
                        activeFilter = when {
                            showCountryFactoryFilter -> "countryFactory"
                            showRegionFilter -> "region"
                            showProductFilter -> "product"
                            showGoodsTypeFilter -> "goodsType"
                            showFeedingMethodFilter -> "feedingMethod"
                            else -> null
                        }
                    )
                }
            }

            // list content
            if (list.isEmpty() && !isLoadingMore) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "no data", color = TextHint)
                    }
                }
            } else {
                itemsIndexed(
                    items = list,
                    key = { index, offer -> offer.offerId ?: "${offer.productName}_${offer.country}_${offer.factoryNo}_${offer.publishTime}_$index".hashCode() }
                ) { index, offer ->
                    Column {
                        OfferCardNew(
                            offer = offer,
                            isExpanded = expandedIndices.contains(index),
                            onToggleExpand = { onToggleOfferExpand(index) },
                            onCopyPhone = { onCopyPhone(merchant.contactPhone) },
                            onCallClick = { onCallClick(merchant.contactPhone) },
                            onViewOriginalText = onViewOriginalText,
                            merchantPhone = merchant.contactPhone
                        )
                    }
                    if (index < list.lastIndex) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Divider(color = Border, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                // bottom loading state
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            isLoadingMore -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Primary, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(text = "loading...", fontSize = 14.sp, color = Primary)
                                }
                            }
                            hasMorePages -> {
                                Text(text = "load more", fontSize = 12.sp, color = Color(0xFF9DA4A3))
                            }
                            else -> {
                                Text(text = "no more", fontSize = 12.sp, color = Color(0xFF9DA4A3))
                            }
                        }
                    }
                }

                // bottom spacer
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        // filter overlay panels - ModalBottomSheet from bottom
        if (showCountryFactoryFilter || showRegionFilter || showProductFilter || showGoodsTypeFilter || showFeedingMethodFilter) {
            currentFilterType = when {
                showCountryFactoryFilter -> "countryFactory"
                showRegionFilter -> "region"
                showProductFilter -> "product"
                showGoodsTypeFilter -> "goodsType"
                showFeedingMethodFilter -> "feedingMethod"
                else -> ""
            }
        }
        if (showCountryFactoryFilter || showRegionFilter || showProductFilter || showGoodsTypeFilter || showFeedingMethodFilter) {
            ModalBottomSheet(
                onDismissRequest = {
                    if (showCountryFactoryFilter) onFilterConfirm()
                    else if (showRegionFilter) onRegionConfirm()
                    else if (showProductFilter) onProductConfirm()
                    else if (showGoodsTypeFilter) onGoodsTypeConfirm()
                    else if (showFeedingMethodFilter) onFeedingMethodConfirm()
                },
                sheetState = sheetState
            ) {
                when (currentFilterType) {
                    "countryFactory" -> CountryFactoryFilterPanel(
                        offers = currentProducts,
                        selectedCountry = selectedCountry,
                        selectedFactories = selectedFactories,
                        onCountrySelected = onCountrySelected,
                        onFactoryToggle = onFactoryToggle,
                        onReset = onFilterReset,
                        onConfirm = onFilterConfirm,
                        modifier = Modifier.fillMaxWidth()
                    )
                    "region" -> RegionFilterPanel(
                        offers = currentProducts,
                        selectedRegions = selectedRegions,
                        onRegionToggle = onRegionToggle,
                        onReset = onRegionReset,
                        onConfirm = onRegionConfirm,
                        modifier = Modifier.fillMaxWidth()
                    )
                    "product" -> ProductFilterPanel(
                        offers = currentProducts,
                        selectedProducts = selectedProducts,
                        onProductToggle = onProductToggle,
                        onReset = onProductReset,
                        onConfirm = onProductConfirm,
                        modifier = Modifier.fillMaxWidth()
                    )
                    "goodsType" -> GoodsTypeFilterPanel(
                        selectedGoodsTypes = selectedGoodsTypes,
                        onGoodsTypeToggle = onGoodsTypeToggle,
                        onReset = onGoodsTypeReset,
                        onConfirm = onGoodsTypeConfirm,
                        modifier = Modifier.fillMaxWidth()
                    )
                    "feedingMethod" -> FeedingMethodFilterPanel(
                        selectedFeedingMethods = selectedFeedingMethods,
                        onFeedingMethodToggle = onFeedingMethodToggle,
                        onReset = onFeedingMethodReset,
                        onConfirm = onFeedingMethodConfirm,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}


/**
 * 商家头部 - Figma node-id: 2:3873
 */
@Composable
private fun MerchantHeader(merchant: MerchantDetail) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧图标
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(PrimaryLight, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 内部容器
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 商家图标
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(PrimaryLight, RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 商家名称
                Text(
                    text = merchant.merchantShortName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.width(8.dp))

                // 标签
                if (merchant.merchantTags.isNotEmpty()) {
                    val firstTag = merchant.merchantTags.split("|").firstOrNull() ?: merchant.merchantTags
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF244C56), RoundedCornerShape(2.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = firstTag,
                            fontSize = 11.sp,
                            color = Color(0xFFF2FFFD)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // 右侧搜索/筛选按钮
        Box(
            modifier = Modifier
                .background(Color(0xFFEFF5F3), RoundedCornerShape(4.dp))
                .border(0.5.dp, Color(0x4DBBCA66), RoundedCornerShape(4.dp))
                .padding(horizontal = 7.dp, vertical = 7.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "筛选",
                tint = TextHint,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * 数据看板 - Figma node-id: 2:3892
 */
@Composable
private fun DataDashboard(merchant: MerchantDetail) {
    Column(
        modifier = Modifier
            .border(width = 1.dp, color = Color(0xFFDEE4E1))
            .width(375.dp)
            .height(44.dp)
            .background(Color.White)
            .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            StatItem(title = "近2日报盘", value = merchant.todayOfferCount.toString())
            StatItem(title = "产品数", value = merchant.todayProductCount.toString())
            StatItem(title = "工厂数", value = merchant.todayFactoryCount.toString())
        }
    }
}

@Composable
private fun StatItem(title: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 10.sp,
            color = Color(0xFF3C4947).copy(alpha = 0.5f)
        )
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
    }
}

/**
 * Tab选择区域 - Figma node-id: 2:3905
 */
@Composable
private fun TabSection(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    priceSortOrder: SortOrder,
    onPriceSortToggle: () -> Unit,
    sortByRecommend: Boolean,
    onSortByRecommendToggle: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧 Tab - 报盘、求购
                Row(
                    horizontalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    // 报盘
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        TabText(
                            text = "报盘",
                            fontWeight = if (selectedTab == 0) FontWeight.SemiBold else FontWeight.Normal,
                            isSelected = selectedTab == 0,
                            onClick = { onTabSelected(0) }
                        )
                        if (selectedTab == 0) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .width(18.dp)
                                    .height(3.dp)
                                    .background(Primary, RoundedCornerShape(1.dp))
                            )
                        }
                    }
                    // 求购
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        TabText(
                            text = "求购",
                            fontWeight = if (selectedTab == 1) FontWeight.SemiBold else FontWeight.Normal,
                            isSelected = selectedTab == 1,
                            onClick = { onTabSelected(1) }
                        )
                        if (selectedTab == 1) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Box(
                                modifier = Modifier
                                    .width(18.dp)
                                    .height(2.dp)
                                    .background(Primary, RoundedCornerShape(1.dp))
                            )
                        }
                    }
                }

                // 右侧 Tab - 综合推荐、价格
                Row(
                    horizontalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    // 综合推荐 - 点击按报盘数排序
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        TabText(
                            text = "综合推荐",
                            fontWeight = if (sortByRecommend) FontWeight.SemiBold else FontWeight.Normal,
                            isSelected = sortByRecommend,
                            onClick = { onSortByRecommendToggle() }
                        )
                    }
                    // 价格 - 可点击排序（整个区域可点击）
                    Column(
                        modifier = Modifier.clickable { onPriceSortToggle() },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "价格",
                                fontSize = 14.sp,
                                fontWeight = if (!sortByRecommend && priceSortOrder != SortOrder.NONE) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (!sortByRecommend && priceSortOrder != SortOrder.NONE) TextPrimary else Color(0xFF3C4947)
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
                                            if (priceSortOrder == SortOrder.ASC) Primary else Color.Transparent,
                                            RoundedCornerShape(1.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowUp,
                                        contentDescription = "升序",
                                        tint = if (priceSortOrder == SortOrder.ASC) Color.White else Color(0xFF8B8B8B),
                                        modifier = Modifier.size(12.dp, 8.dp)
                                    )
                                }
                                // 下箭头 - 降序时背景变绿
                                Box(
                                    modifier = Modifier
                                        .size(12.dp, 8.dp)
                                        .background(
                                            if (priceSortOrder == SortOrder.DESC) Primary else Color.Transparent,
                                            RoundedCornerShape(1.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "降序",
                                        tint = if (priceSortOrder == SortOrder.DESC) Color.White else Color(0xFF8B8B8B),
                                        modifier = Modifier.size(12.dp, 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabText(
    text: String,
    fontWeight: FontWeight,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = fontWeight,
        color = if (isSelected) TextPrimary else Color(0xFF3C4947),
        modifier = Modifier.clickable { onClick() }
    )
}

/**
 * 筛选区域 - Figma node-id: 2:3928
 */
@Composable
private fun FilterSection(
    onCountryFactoryClick: () -> Unit,
    hasCountrySelection: Boolean = false,
    onRegionClick: () -> Unit = {},
    hasRegionSelection: Boolean = false,
    onProductClick: () -> Unit = {},
    hasProductSelection: Boolean = false,
    onGoodsTypeClick: () -> Unit = {},
    hasGoodsTypeSelection: Boolean = false,
    onFeedingMethodClick: () -> Unit = {},
    hasFeedingMethodSelection: Boolean = false,
    // 标识哪个筛选面板正在显示，用于背景色连接
    activeFilter: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            FilterChip(
                text = "国家厂号",
                isSelected = hasCountrySelection,
                isActive = activeFilter == "countryFactory",
                onClick = onCountryFactoryClick
            )
            FilterChip(
                text = "地区",
                isSelected = hasRegionSelection,
                isActive = activeFilter == "region",
                onClick = onRegionClick
            )
            FilterChip(
                text = "产品",
                isSelected = hasProductSelection,
                isActive = activeFilter == "product",
                onClick = onProductClick
            )
            FilterChip(
                text = "货物类型",
                isSelected = hasGoodsTypeSelection,
                isActive = activeFilter == "goodsType",
                onClick = onGoodsTypeClick
            )
            FilterChip(
                text = "饲养方式",
                isSelected = hasFeedingMethodSelection,
                isActive = activeFilter == "feedingMethod",
                onClick = onFeedingMethodClick
            )
        }
        // 当有活动筛选面板时，不显示分隔线，或者用背景色覆盖
        if (activeFilter == null) {
            Divider(color = Color(0xFFDEE4E1), thickness = 1.dp)
        } else {
            // 用背景色覆盖分隔线位置，实现视觉连接
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFFF3F6F5))
            )
        }
    }
}

@Composable
private fun FilterChip(
    text: String,
    isSelected: Boolean = false,
    isActive: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val borderColor = if (isSelected) Color(0xFF006A61) else Color.Transparent
    val textColor = if (isSelected) Color(0xFF006A61) else Color(0xFF3C4947)
    val iconColor = if (isSelected) Color(0xFF006A61) else Color(0xFF3C4947)
    // 活动时高度增加1dp，用于覆盖下方的分隔线，实现背景色连接
    val chipHeight = if (isActive) 28.dp else 27.dp

    Box(
        modifier = Modifier
            .height(chipHeight)
            .background(Color(0xFFF3F6F5), RoundedCornerShape(2.dp))
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(2.dp)
            )
            .clickable { onClick?.invoke() }
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = text,
                fontSize = 12.sp,
                lineHeight = 15.sp,
                color = textColor
            )
            // 下拉展开箭头图标 - Figma: 6dp宽, 12dp高
            Box(
                modifier = Modifier.size(width = 10.dp, height = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color(0xFF8B8B8B),
                    modifier = Modifier.size(10.dp, 16.dp)
                )
            }
        }
    }
}

/**
 * 解析重量字符串，提取数字和单位
 * 例如: "1000吨" -> ("1000", "吨"), "1000 MT" -> ("1000", "MT"), "1000" -> ("1000", "")
 * 重量值四舍五入保留一位小数，如果是整数则不显示小数部分
 */
private fun parseWeight(weight: String?): Pair<String, String> {
    if (weight.isNullOrBlank()) {
        return "" to ""
    }
    // 匹配数字（包括小数）和后面的单位
    val regex = Regex("^([\\d.]+)(.*)$")
    val match = regex.find(weight.trim())
    return if (match != null) {
        val (value, unit) = match.destructured
        // 四舍五入保留一位小数，整数不显示小数部分
        val numValue = value.toDoubleOrNull()
        val roundedValue = if (numValue != null) {
            val rounded = Math.round(numValue * 10.0) / 10.0
            if (rounded == rounded.toLong().toDouble()) {
                rounded.toLong().toString()
            } else {
                String.format("%.1f", rounded)
            }
        } else {
            value
        }
        roundedValue to unit.trim()
    } else {
        weight to ""
    }
}

/**
 * 报盘卡片 - Figma node-id: 2:3961
 */
@Composable
private fun OfferCardNew(
    offer: OfferSummary,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onCopyPhone: () -> Unit,
    onCallClick: () -> Unit,
    onViewOriginalText: (String) -> Unit,
    merchantPhone: String
) {
    // 从员工报价中计算价格范围
    val priceRange = remember(offer.employeeOffers) {
        val prices = offer.employeeOffers?.mapNotNull { it.price } ?: emptyList()
        if (prices.isNotEmpty()) {
            val minPrice = prices.minOrNull()
            val maxPrice = prices.maxOrNull()
            if (minPrice != null && maxPrice != null && minPrice != maxPrice) {
                "¥ $minPrice - $maxPrice" to "/kg"
            } else if (minPrice != null) {
                "¥ $minPrice" to "/kg"
            } else null to null
        } else {
            if (offer.price != null && offer.priceMax != null) {
                "¥ ${offer.price} - ${offer.priceMax}" to "/kg"
            } else if (offer.price != null) {
                "¥ ${offer.price}" to "/kg"
            } else null to null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // 主卡片
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFBFFFE))
                .padding(vertical = 12.dp)
        ) {
            // 产品名称和价格
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (offer.factoryNo.isNullOrBlank()) {
                        "${offer.productName ?: ""} ${offer.country ?: ""}厂号不限"
                    } else {
                        "${offer.productName ?: ""} ${offer.country ?: ""}${offer.factoryNo}"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // 价格和展开图标
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // 价格 - 值16sp SemiBold #006a61, 单位10sp Regular #171d1c
                    if (priceRange.first != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = priceRange.first!!,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Primary
                            )
                            Text(
                                text = priceRange.second ?: "/kg",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Normal,
                                color = TextPrimary
                            )
                        }
                    }
                    // 展开图标 - 点击展开/收起员工卡片
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onToggleExpand() },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_right),
                            contentDescription = null,
                            colorFilter = if (!isExpanded) ColorFilter.tint(Color(0xFFBFCAC8)) else null,
                            modifier = Modifier
                                .size(12.dp, 16.dp)
                                .graphicsLayer { rotationZ = if (isExpanded) 270f else 90f }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 标签行 - 合并显示所有不重复的地点、类型、标签
            val employeeOfferLocations = offer.employeeOffers?.mapNotNull { extractCity(it.goodsLocation) }?.filter { it.isNotEmpty() }?.distinct() ?: emptyList()
            val employeeOfferTypes = offer.employeeOffers?.mapNotNull { it.goodsType }?.filter { it.isNotEmpty() }?.distinct() ?: emptyList()
            val employeeOfferFeedings = offer.employeeOffers?.mapNotNull { it.feedingMethod }?.filter { it.isNotEmpty() }?.distinct() ?: emptyList()
            val employeeOfferTags = offer.employeeOffers?.mapNotNull { it.tags?.split(",")?.filter { t -> t.isNotBlank() } }?.flatten()?.distinct()?.take(4) ?: emptyList()

            // 回退到 OfferSummary 本身的字段（如果没有 employeeOffers 数据）
            val allLocations = if (employeeOfferLocations.isNotEmpty()) employeeOfferLocations else listOfNotNull(offer.goodsLocation?.let { extractCity(it) }).filter { it.isNotEmpty() }
            val allTypes = if (employeeOfferTypes.isNotEmpty()) employeeOfferTypes else listOfNotNull(offer.goodsType).filter { it.isNotEmpty() }
            val allFeedings = if (employeeOfferFeedings.isNotEmpty()) employeeOfferFeedings else listOfNotNull(offer.feedingType).filter { it.isNotEmpty() }
            val allTags = if (employeeOfferTags.isNotEmpty()) employeeOfferTags else offer.tags?.split(",")?.filter { it.isNotBlank() }?.take(4) ?: emptyList()

            val hasAnyTag = allLocations.isNotEmpty() || allTypes.isNotEmpty() || allFeedings.isNotEmpty() || allTags.isNotEmpty()

            if (hasAnyTag) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (allLocations.isNotEmpty()) {
                        OfferTag(
                            text = allLocations.joinToString("/"),
                            backgroundColor = Color(0xFFF2F8F7),
                            textColor = Primary,
                            hasIcon = true
                        )
                    }
                    if (allTypes.isNotEmpty()) {
                        OfferTag(
                            text = allTypes.joinToString("/"),
                            backgroundColor = Color(0xFFF3F6F5),
                            textColor = Color(0xFF3C4947),
                            hasIcon = false
                        )
                    }
                    if (allFeedings.isNotEmpty()) {
                        OfferTag(
                            text = allFeedings.joinToString("/"),
                            backgroundColor = Color(0xFFF3F6F5),
                            textColor = Color(0xFF3C4947),
                            hasIcon = false
                        )
                    }
                    allTags.forEach { tag ->
                        val (bgColor, txtColor) = when {
                            tag.contains("大日期") || tag.contains("日期") -> Color(0xFFF2F3FF) to Color(0xFF3163DC)
                            tag.contains("可开票") || tag.contains("票") -> Color(0xFFFFF5E4) to Color(0xFFA07D17)
                            tag.contains("整柜") || tag.contains("柜") -> Color(0xFFFFF0ED) to Color(0xFFD54941)
                            tag.contains("一口价") || tag.contains("价") -> Color(0xFFF3F6F5) to Color(0xFF3C4947)
                            else -> Color(0xFFF3F6F5) to Color(0xFF3C4947)
                        }
                        OfferTag(text = tag.trim(), backgroundColor = bgColor, textColor = txtColor, hasIcon = false)
                    }
                }
            }
        }

        // 员工报价列表 - 仅在展开时显示
        if (isExpanded) {
            offer.employeeOffers?.forEach { employeeOffer ->
                EmployeeOfferCard(
                    offer = employeeOffer,
                    onCopyPhone = onCopyPhone,
                    onCallClick = onCallClick,
                    onViewOriginalText = onViewOriginalText,
                    merchantPhone = merchantPhone,
                    fallbackGoodsType = offer.goodsType,
                    fallbackFeedingType = offer.feedingType
                )
            }
        }
    }
}

@Composable
private fun OfferTag(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    hasIcon: Boolean
) {
    Box(
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(1.dp))
            .padding(horizontal = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (hasIcon) {
                Image(
                    painter = painterResource(id = R.drawable.ic_location),
                    contentDescription = null,
                    modifier = Modifier.size(10.dp)
                )
            }
            Text(
                text = text,
                fontSize = 10.sp,
                color = textColor
            )
        }
    }
}

/**
 * 员工报价卡片 - Figma node-id: 2:3985 & 2:4042
 */
@Composable
private fun EmployeeOfferCard(
    offer: EmployeeOffer,
    onCopyPhone: () -> Unit,
    onCallClick: () -> Unit,
    onViewOriginalText: (String) -> Unit,
    merchantPhone: String,
    fallbackGoodsType: String? = null,
    fallbackFeedingType: String? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Color(0xFFFBFFFE),
                    RoundedCornerShape(4.dp)
                )
                .border(0.5.dp, Primary.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                .padding(12.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 用户信息和价格
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 解析重量数据
                    val (weightValue, weightUnit) = remember(offer.weight) { parseWeight(offer.weight) }
                    val hasWeight = weightValue.isNotEmpty()

                    if (hasWeight) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // 头像
                                Image(
                                    painter = painterResource(id = R.drawable.ic_avatar),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )

                                Text(
                                    text = offer.userNickname ?: "",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = merchantPhone,
                                fontSize = 12.sp,
                                color = TextHint,
                                modifier = Modifier.padding(start = 26.dp)
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 头像
                            Image(
                                painter = painterResource(id = R.drawable.ic_avatar),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )

                            Text(
                                text = "${offer.userNickname ?: ""} | $merchantPhone",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 重量 - Figma: 值16sp SemiBold, 单位10sp Regular
                        if (weightValue.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = weightValue,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                if (weightUnit.isNotEmpty()) {
                                    Text(
                                        text = weightUnit,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                        // 价格 - Figma: 值16sp SemiBold #006a61, 单位10sp Regular #171d1c
                        if (offer.price != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "¥${offer.price}",
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
                        }
                    }
                }

                // 时间、地区、标签
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val timeText = formatTime(offer.publishTime)
                    if (timeText.isNotEmpty()) {
                        Text(
                            text = timeText,
                            fontSize = 11.sp,
                            color = Color(0xFF3C4947)
                        )
                    }

                    if (offer.goodsLocation?.isNotEmpty() == true) {
                        OfferTag(
                            text = extractCity(offer.goodsLocation),
                            backgroundColor = Color(0xFFF2F8F7),
                            textColor = Primary,
                            hasIcon = true
                        )
                    }
                    val displayGoodsType = offer.goodsType ?: fallbackGoodsType
                    val displayFeedingType = offer.feedingMethod ?: fallbackFeedingType
                    if (displayGoodsType?.isNotEmpty() == true) {
                        OfferTag(
                            text = displayGoodsType,
                            backgroundColor = Color(0xFFF3F6F5),
                            textColor = Color(0xFF3C4947),
                            hasIcon = false
                        )
                    }
                    if (displayFeedingType?.isNotEmpty() == true) {
                        OfferTag(
                            text = displayFeedingType,
                            backgroundColor = Color(0xFFF3F6F5),
                            textColor = Color(0xFF3C4947),
                            hasIcon = false
                        )
                    }
                    offer.tags?.split(",")?.filter { it.isNotBlank() }?.take(4)?.forEach { tag ->
                        val (bgColor, txtColor) = when {
                            tag.contains("大日期") || tag.contains("日期") -> Color(0xFFF2F3FF) to Color(0xFF3163DC)
                            tag.contains("可开票") || tag.contains("票") -> Color(0xFFFFF5E4) to Color(0xFFA07D17)
                            tag.contains("整柜") || tag.contains("柜") -> Color(0xFFFFF0ED) to Color(0xFFD54941)
                            tag.contains("一口价") || tag.contains("价") -> Color(0xFFF3F6F5) to Color(0xFF3C4947)
                            else -> Color(0xFFF3F6F5) to Color(0xFF3C4947)
                        }
                        OfferTag(text = tag.trim(), backgroundColor = bgColor, textColor = txtColor, hasIcon = false)
                    }
                }

                Divider(
                    color = Primary.copy(alpha = 0.05f),
                    thickness = 0.5.dp
                )

                // 操作按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ActionButton(
                        icon = Icons.Default.Description,
                        text = "查看原文",
                        textColor = Color(0xFF3C4947),
                        onClick = { onViewOriginalText(offer.offerOriginalText ?: "") },
                        iconPainter = painterResource(id = R.drawable.ic_book)
                    )

                    VerticalDivider()

                    ActionButton(
                        icon = Icons.Default.ContentCopy,
                        text = "添加微信",
                        textColor = Color(0xFF3C4947),
                        onClick = onCopyPhone,
                        iconPainter = painterResource(id = R.drawable.ic_add_square)
                    )

                    VerticalDivider()

                    ActionButton(
                        icon = Icons.Default.Call,
                        text = "拨打电话",
                        textColor = Primary,
                        onClick = onCallClick
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    text: String,
    textColor: Color,
    onClick: () -> Unit,
    iconPainter: androidx.compose.ui.graphics.painter.Painter? = null
) {
    Row(
        modifier = Modifier.clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (iconPainter != null) {
            Image(
                painter = iconPainter,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = text,
            fontSize = 12.sp,
            color = textColor
        )
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .width(0.5.dp)
            .height(13.dp)
            .background(Color(0xFF3C4947))
    )
}

/**
 * 国家厂号筛选面板 - Figma node-id: 5-403
 */
@Composable
private fun CountryFactoryFilterPanel(
    offers: List<OfferSummary>,
    selectedCountry: String?,
    selectedFactories: Set<String>,
    onCountrySelected: (String?) -> Unit,
    onFactoryToggle: (String) -> Unit,
    onReset: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 从列表数据中动态提取国家和厂号
    val allFactories = offers.map { "${it.country}${it.factoryNo}" }.distinct()
    val countries = offers.mapNotNull { it.country }.distinct()

    // 根据选中的国家筛选厂号
    val filteredFactories = if (selectedCountry == null) {
        allFactories
    } else {
        allFactories.filter { it.startsWith(selectedCountry) }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFF3F6F5), RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
    ) {
        // 顶部边框 - Figma: border-top #DEE4E1
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFFDEE4E1))
        )

        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 国家筛选 chips - 支持水平滚动
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 全部
                CountryChip(
                    text = "全部",
                    isSelected = selectedCountry == null,
                    onClick = { onCountrySelected(null) }
                )
                // 其他国家
                countries.forEach { country ->
                    CountryChip(
                        text = country,
                        isSelected = selectedCountry == country,
                        onClick = { onCountrySelected(country) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 厂号列表 - 两列布局，最多显示4行，剩余可滑动
            val displayFactories = filteredFactories
            val maxVisibleRows = 4
            // 每行高度约44dp (20sp text + 24dp spacing)
            val rowHeight = 44.dp

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(rowHeight * maxVisibleRows)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    displayFactories.chunked(2).forEach { rowFactories ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            rowFactories.forEach { factory ->
                                val isSelected = selectedFactories.contains(factory)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // 选中状态显示done icon
                                    if (isSelected) {
                                        Image(
                                            painter = painterResource(id = R.drawable.ic_done),
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Text(
                                        text = factory,
                                        fontSize = 14.sp,
                                        color = if (isSelected) Color(0xFF006A61) else Color(0xFF171D1C),
                                        lineHeight = 20.sp,
                                        modifier = Modifier.clickable { onFactoryToggle(factory) }
                                    )
                                }
                            }
                            // 如果只有一项，补齐空白
                            if (rowFactories.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 重置和确定按钮
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 重置按钮
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .border(1.dp, Color(0xFF006A61), RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp))
                        .clickable { onReset() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "重置",
                        fontSize = 15.sp,
                        color = Color(0xFF006A61)
                    )
                }

                // 确定按钮
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .background(Color(0xFF006A61), RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                        .clickable { onConfirm() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "确定",
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * 地区筛选面板 - Figma node-id: 5-1150
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RegionFilterPanel(
    offers: List<OfferSummary>,
    selectedRegions: Set<String>,
    onRegionToggle: (String) -> Unit,
    onReset: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 从列表数据中动态提取地区（分割"/"分隔的多个城市）
    val allLocations = offers.flatMap { (it.goodsLocation ?: "").split("/") }
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
        .sorted()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFF3F6F5), RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
    ) {
        // 顶部边框
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFFDEE4E1))
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // 地区列表 - FlowRow流式布局，宽度自适应
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                allLocations.forEach { location ->
                    val isSelected = location in selectedRegions
                    Row(
                        modifier = Modifier
                            .height(32.dp)
                            .wrapContentWidth()
                            .background(Color.White, RoundedCornerShape(2.dp))
                            .border(
                                1.dp,
                                if (isSelected) Color(0xFF006A61) else Color.Transparent,
                                RoundedCornerShape(2.dp)
                            )
                            .clickable { onRegionToggle(location) }
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (isSelected) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_done),
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                        }
                        Text(
                            text = location,
                            fontSize = 12.sp,
                            color = if (isSelected) Color(0xFF006A61) else Color(0xFF171D1C),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 重置和确定按钮
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 重置按钮
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .border(1.dp, Color(0xFF006A61), RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp))
                        .clickable { onReset() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "重置",
                        fontSize = 15.sp,
                        color = Color(0xFF006A61)
                    )
                }

                // 确定按钮
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .background(Color(0xFF006A61), RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                        .clickable { onConfirm() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "确定",
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * 产品筛选面板 - 样式与地区筛选一致
 */
@Composable
private fun ProductFilterPanel(
    offers: List<OfferSummary>,
    selectedProducts: Set<String>,
    onProductToggle: (String) -> Unit,
    onReset: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 从列表数据中提取产品，并按盘数排序
    val productCounts = offers.groupingBy { it.productName }.eachCount()
        .filterKeys { it != null }
    val sortedProducts = productCounts.entries.sortedByDescending { it.value }.mapNotNull { it.key }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFF3F6F5), RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
    ) {
        // 顶部边框
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFFDEE4E1))
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // 产品列表 - 5列网格布局，最多显示4行
            val rowCount = 4
            val itemsPerRow = 5
            val itemHeight = 32.dp
            val visibleHeight = itemHeight * rowCount + 8.dp * (rowCount - 1)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(visibleHeight)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    sortedProducts.chunked(itemsPerRow).take(rowCount).forEach { rowProducts ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(itemHeight),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            rowProducts.forEach { product ->
                                val isSelected = product in selectedProducts
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .background(Color.White, RoundedCornerShape(2.dp))
                                        .border(
                                            1.dp,
                                            if (isSelected) Color(0xFF006A61) else Color.Transparent,
                                            RoundedCornerShape(2.dp)
                                        )
                                        .clickable { onProductToggle(product) }
                                        .padding(horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    if (isSelected) {
                                        Image(
                                            painter = painterResource(id = R.drawable.ic_done),
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                    }
                                    Text(
                                        text = product,
                                        fontSize = 12.sp,
                                        color = if (isSelected) Color(0xFF006A61) else Color(0xFF171D1C),
                                        maxLines = 1
                                    )
                                }
                            }
                            // 补齐空白
                            repeat(itemsPerRow - rowProducts.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 重置和确定按钮
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 重置按钮
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .border(1.dp, Color(0xFF006A61), RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp))
                        .clickable { onReset() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "重置",
                        fontSize = 15.sp,
                        color = Color(0xFF006A61)
                    )
                }

                // 确定按钮
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .background(Color(0xFF006A61), RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                        .clickable { onConfirm() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "确定",
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * 货物类型筛选面板 - 样式与地区筛选一致
 * 枚举值：现货、半期货、期货
 */
@Composable
private fun GoodsTypeFilterPanel(
    selectedGoodsTypes: Set<String>,
    onGoodsTypeToggle: (String) -> Unit,
    onReset: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 固定货物类型枚举值
    val goodsTypes = listOf("现货", "半期货", "期货")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFF3F6F5), RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
    ) {
        // 顶部边框
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFFDEE4E1))
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // 货物类型列表 - 3列布局居中显示
            val itemsPerRow = 3
            val itemHeight = 36.dp

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                goodsTypes.chunked(itemsPerRow).forEach { rowTypes ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(itemHeight),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowTypes.forEach { goodsType ->
                            val isSelected = goodsType in selectedGoodsTypes
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(Color.White, RoundedCornerShape(2.dp))
                                    .border(
                                        1.dp,
                                        if (isSelected) Color(0xFF006A61) else Color.Transparent,
                                        RoundedCornerShape(2.dp)
                                    )
                                    .clickable { onGoodsTypeToggle(goodsType) }
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (isSelected) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_done),
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = goodsType,
                                    fontSize = 14.sp,
                                    color = if (isSelected) Color(0xFF006A61) else Color(0xFF171D1C)
                                )
                            }
                        }
                        // 补齐空白
                        repeat(itemsPerRow - rowTypes.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 重置和确定按钮
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 重置按钮
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .border(1.dp, Color(0xFF006A61), RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp))
                        .clickable { onReset() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "重置",
                        fontSize = 15.sp,
                        color = Color(0xFF006A61)
                    )
                }

                // 确定按钮
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .background(Color(0xFF006A61), RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                        .clickable { onConfirm() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "确定",
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * 饲养方式筛选面板 - 样式与地区筛选一致
 * 枚举值：草饲、谷饲
 */
@Composable
private fun FeedingMethodFilterPanel(
    selectedFeedingMethods: Set<String>,
    onFeedingMethodToggle: (String) -> Unit,
    onReset: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 固定饲养方式枚举值
    val feedingMethods = listOf("草饲", "谷饲")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFF3F6F5), RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
    ) {
        // 顶部边框
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFFDEE4E1))
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // 饲养方式列表 - 2列布局居中显示
            val itemsPerRow = 2
            val itemHeight = 36.dp

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                feedingMethods.chunked(itemsPerRow).forEach { rowMethods ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(itemHeight),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowMethods.forEach { method ->
                            val isSelected = method in selectedFeedingMethods
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(Color.White, RoundedCornerShape(2.dp))
                                    .border(
                                        1.dp,
                                        if (isSelected) Color(0xFF006A61) else Color.Transparent,
                                        RoundedCornerShape(2.dp)
                                    )
                                    .clickable { onFeedingMethodToggle(method) }
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (isSelected) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_done),
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = method,
                                    fontSize = 14.sp,
                                    color = if (isSelected) Color(0xFF006A61) else Color(0xFF171D1C)
                                )
                            }
                        }
                        // 补齐空白
                        repeat(itemsPerRow - rowMethods.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 重置和确定按钮
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 重置按钮
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .border(1.dp, Color(0xFF006A61), RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp))
                        .clickable { onReset() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "重置",
                        fontSize = 15.sp,
                        color = Color(0xFF006A61)
                    )
                }

                // 确定按钮
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .background(Color(0xFF006A61), RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                        .clickable { onConfirm() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "确定",
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun CountryChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(27.dp)
            .background(
                if (isSelected) Color.White else Color.White,
                RoundedCornerShape(2.dp)
            )
            .border(
                1.dp,
                if (isSelected) Color(0xFF006A61) else Color.Transparent,
                RoundedCornerShape(2.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            lineHeight = 15.sp,
            color = Color(0xFF3C4947)
        )
    }
}

/**
 * 提取城市名称（去掉省份前缀）
 * 格式可能是：河北省/北京、河北省北京、河北省\北京 等
 */
private fun extractCity(location: String?): String {
    if (location.isNullOrBlank()) return ""
    return try {
        when {
            location.contains("/") -> location.substringAfter("/").trim()
            location.contains("\\") -> location.substringAfter("\\").trim()
            location.contains("省") -> location.substringAfter("省").trim()
            else -> location.trim()
        }
    } catch (e: Exception) {
        location
    }
}

private fun formatTime(timeString: String?): String {
    if (timeString.isNullOrEmpty()) return ""
    return try {
        // 尝试两种格式
        val date = try {
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse(timeString)
        } catch (e: Exception) {
            try {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(timeString)
            } catch (e: Exception) {
                null
            }
        } ?: return timeString.takeLast(5) // 解析失败则只显示时分

        val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

        val dateCalendar = Calendar.getInstance().apply { time = date }

        val prefix = when {
            dateCalendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            dateCalendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> "今天"
            dateCalendar.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
            dateCalendar.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR) -> "昨天"
            else -> SimpleDateFormat("MM-dd", Locale.getDefault()).format(date)
        }

        "$prefix ${outputFormat.format(date)}"
    } catch (e: Exception) {
        timeString.takeLast(5)
    }
}
