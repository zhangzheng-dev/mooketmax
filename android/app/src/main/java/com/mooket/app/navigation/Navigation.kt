package com.mooket.app.navigation

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mooket.app.data.SessionManager
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import com.mooket.app.ui.screens.home.HomeScreen
import com.mooket.app.ui.screens.home.HomeCardsScreen
import com.mooket.app.ui.screens.merchant.MerchantScreen
import com.mooket.app.ui.screens.product.ProductDetailScreen
import com.mooket.app.ui.screens.product.ProductDetailViewModel
import com.mooket.app.ui.screens.search.SearchScreen
import com.mooket.app.ui.screens.search.SearchViewModel
import com.mooket.app.ui.screens.search.SearchViewModelFactory
import com.mooket.app.ui.screens.country.CountryDetailScreen
import com.mooket.app.ui.screens.factory.FactoryDetailScreen
import com.mooket.app.ui.screens.countryproduct.CountryProductScreen
import com.mooket.app.ui.screens.countryfactoryproduct.CountryFactoryProductScreen
import com.mooket.app.ui.screens.substitute.SubstituteProductScreen
import com.mooket.app.ui.screens.substitute.SubstituteProductViewModel
import com.mooket.app.ui.screens.datacomparison.DataComparisonScreen
import com.mooket.app.ui.screens.datacomparison.DataComparisonViewModel
import com.mooket.app.ui.screens.brand.BrandDetailScreen
import com.mooket.app.ui.screens.brand.BrandDetailViewModel
import com.mooket.app.ui.screens.brandproduct.BrandProductDetailScreen
import com.mooket.app.ui.screens.brandproduct.BrandProductDetailViewModel
import com.mooket.app.ui.screens.login.LoginScreen
import com.mooket.app.ui.screens.login.LoginViewModel
import com.mooket.app.ui.screens.login.PhoneInputScreen
import com.mooket.app.ui.screens.login.SmsVerifyScreen
import com.mooket.app.ui.screens.login.OneClickLoginScreen
import com.mooket.app.ui.screens.login.RegisterScreen
import com.mooket.app.ui.screens.profile.ProfileScreen
import com.mooket.app.ui.screens.profile.EditProfileScreen
import com.mooket.app.ui.screens.inventory.InventoryScreen

private fun routeEncode(value: String): String = Uri.encode(value)
private fun routeDecode(value: String): String = Uri.decode(value)

/**
 * 导航路由
 */
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object HomeCards : Screen("home/cards")
    object Login : Screen("login")
    object Search : Screen("search")
    object Merchant : Screen("merchant/{merchantId}/{category}") {
        fun createRoute(merchantId: Long, category: String) = "merchant/$merchantId/$category"
    }
    object Product : Screen("product/{productId}/{category}/{productName}") {
        fun createRoute(productId: Int, category: String, productName: String) =
            "product/$productId/${routeEncode(category)}/${routeEncode(productName)}"
    }
    object Country : Screen("country/{country}/{category}") {
        fun createRoute(country: String, category: String) =
            "country/${routeEncode(country)}/${routeEncode(category)}"
    }
    object Factory : Screen("factory/{country}/{factoryNo}/{category}") {
        fun createRoute(country: String, factoryNo: String, category: String) =
            "factory/${routeEncode(country)}/${routeEncode(factoryNo)}/${routeEncode(category)}"
    }
    object CountryProduct : Screen("country-product/{country}/{productName}/{category}") {
        fun createRoute(country: String, productName: String, category: String) =
            "country-product/${routeEncode(country)}/${routeEncode(productName)}/${routeEncode(category)}"
    }
    object CountryFactoryProduct : Screen("country-factory-product/{country}/{factoryNo}/{productName}/{category}") {
        fun createRoute(country: String, factoryNo: String, productName: String, category: String) =
            "country-factory-product/${routeEncode(country)}/${routeEncode(factoryNo)}/${routeEncode(productName)}/${routeEncode(category)}"
    }
    object SubstituteProduct : Screen("substitute-product/{country}/{factoryNo}/{productName}/{category}") {
        fun createRoute(country: String, factoryNo: String, productName: String, category: String) =
            "substitute-product/${routeEncode(country)}/${routeEncode(factoryNo)}/${routeEncode(productName)}/${routeEncode(category)}"
    }
    object DataComparison : Screen("data-comparison/{country}/{factoryNos}/{productName}/{category}/{excludeFactoryNo}") {
        fun createRoute(country: String, factoryNos: List<String>, productName: String, category: String, excludeFactoryNo: String? = null) =
            "data-comparison/${routeEncode(country)}/${factoryNos.joinToString(",") { routeEncode(it) }}/${routeEncode(productName)}/${routeEncode(category)}/${routeEncode(excludeFactoryNo ?: "")}"
    }
    object Brand : Screen("brand/{brandName}/{category}") {
        fun createRoute(brandName: String, category: String) =
            "brand/${routeEncode(brandName)}/${routeEncode(category)}"
    }
    object BrandProduct : Screen("brand-product/{brandName}/{productName}/{category}") {
        fun createRoute(brandName: String, productName: String, category: String) =
            "brand-product/${routeEncode(brandName)}/${routeEncode(productName)}/${routeEncode(category)}"
    }
    object Profile : Screen("profile")
    object EditProfile : Screen("profile/edit")
    object Inventory : Screen("inventory")
}

@Composable
fun MooketNavHost(
    navController: NavHostController = rememberNavController(),
    context: Context
) {
    // 防抖：防止快速连续 popBackStack/navigate 导致白屏（已禁用，保留变量避免编译错误）
    val navDebounceMs = 0L
    var lastNavTime by remember { mutableLongStateOf(0L) }
    fun safePopBackStack(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastNavTime > navDebounceMs) {
            lastNavTime = now
            return navController.popBackStack()
        }
        return false
    }
    fun safeNavigate(route: String) {
        val now = System.currentTimeMillis()
        if (now - lastNavTime > navDebounceMs) {
            lastNavTime = now
            navController.navigate(route)
        }
    }
    fun safeNavigateWithPopUp(route: String, popUpToRoute: String, inclusive: Boolean) {
        val now = System.currentTimeMillis()
        if (now - lastNavTime > navDebounceMs) {
            lastNavTime = now
            navController.navigate(route) {
                popUpTo(popUpToRoute) { this.inclusive = inclusive }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (SessionManager.isLoggedIn()) Screen.Home.route else Screen.Login.route,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(durationMillis = 100, easing = LinearEasing)
            )
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(durationMillis = 100, easing = LinearEasing)
            )
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = tween(durationMillis = 100, easing = LinearEasing)
            )
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(durationMillis = 100, easing = LinearEasing)
            )
        }
    ) {
        // 登录页
        composable(Screen.Login.route) {
            val viewModel: LoginViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsState()

            when (uiState.screen) {
                LoginScreen.PhoneInput -> {
                    PhoneInputScreen(
                        onSendCode = { phone -> viewModel.sendCode(phone) },
                        onOneClickLogin = { viewModel.oneClickLogin() },
                        isLoading = uiState.isLoading,
                        error = uiState.error,
                        onClearError = { viewModel.clearError() }
                    )
                }
                LoginScreen.SmsVerify -> {
                    SmsVerifyScreen(
                        phone = uiState.phone,
                        countdown = uiState.countdown,
                        isLoading = uiState.isLoading,
                        error = uiState.error,
                        onVerify = { code -> viewModel.loginWithCode(code) },
                        onResend = { viewModel.sendCode(uiState.phone) },
                        onBack = { viewModel.goBack() },
                        onClearError = { viewModel.clearError() }
                    )
                }
                LoginScreen.OneClick -> {
                    OneClickLoginScreen(
                        phone = uiState.phone,
                        isLoading = uiState.isLoading,
                        onOneClickLogin = { viewModel.oneClickLogin() },
                        onOtherLogin = { viewModel.goBack() },
                        onBack = { viewModel.goBack() }
                    )
                }
                LoginScreen.Register -> {
                    RegisterScreen(
                        nickname = uiState.nickname,
                        selectedTags = uiState.selectedIdentityTags,
                        isLoading = uiState.isLoading,
                        error = uiState.error,
                        onNicknameChange = { viewModel.updateNickname(it) },
                        onTagToggle = { viewModel.toggleIdentityTag(it) },
                        onConfirm = { viewModel.register(uiState.nickname, uiState.selectedIdentityTags.toList()) },
                        onBack = { viewModel.goBack() },
                        onClearError = { viewModel.clearError() }
                    )
                }
                LoginScreen.Home -> {
                    // 登录成功，跳转首页
                    HomeScreen(
                        onSearchClick = { category ->
                            safeNavigate(Screen.Search.route + "/${routeEncode(category)}")
                        },
                        onProductClick = { productId, cat, productName ->
                            safeNavigate(Screen.Product.createRoute(productId, cat, productName))
                        },
                        onCountryClick = { country, cat ->
                            safeNavigate(Screen.Country.createRoute(country, cat))
                        },
                        onBrandClick = { brandName, cat ->
                            safeNavigate(Screen.Brand.createRoute(brandName, cat))
                        },
                        onMerchantClick = { merchantId, cat ->
                            safeNavigate(Screen.Merchant.createRoute(merchantId, cat))
                        },
                        onFactoryClick = { country, factoryNo, cat ->
                            safeNavigate(Screen.Factory.createRoute(country, factoryNo, cat))
                        },
                        onCountryProductClick = { country, productName, cat ->
                            safeNavigate(Screen.CountryProduct.createRoute(country, productName, cat))
                        },
                        onCountryFactoryProductClick = { country, factoryNo, productName, cat ->
                            safeNavigate(Screen.CountryFactoryProduct.createRoute(country, factoryNo, productName, cat))
                        },
                        onBrandProductClick = { brandName, productName, cat ->
                            safeNavigate(Screen.BrandProduct.createRoute(brandName, productName, cat))
                        },
                        onHomeCardsClick = {
                            safeNavigate(Screen.HomeCards.route)
                        },
                        onProfileClick = {
                            safeNavigate(Screen.Profile.route)
                        },
                        onInventoryClick = {
                            safeNavigate(Screen.Inventory.route)
                        }
                    )
                }
            }
        }

        // 首页
        composable(Screen.Home.route) {
            HomeScreen(
                onSearchClick = { category ->
                    safeNavigate(Screen.Search.route + "/${routeEncode(category)}")
                },
                onProductClick = { productId, cat, productName ->
                    safeNavigate(Screen.Product.createRoute(productId, cat, productName))
                },
                onCountryClick = { country, cat ->
                    safeNavigate(Screen.Country.createRoute(country, cat))
                },
                onBrandClick = { brandName, cat ->
                    safeNavigate(Screen.Brand.createRoute(brandName, cat))
                },
                onMerchantClick = { merchantId, cat ->
                    safeNavigate(Screen.Merchant.createRoute(merchantId, cat))
                },
                onFactoryClick = { country, factoryNo, cat ->
                    safeNavigate(Screen.Factory.createRoute(country, factoryNo, cat))
                },
                onCountryProductClick = { country, productName, cat ->
                    safeNavigate(Screen.CountryProduct.createRoute(country, productName, cat))
                },
                onCountryFactoryProductClick = { country, factoryNo, productName, cat ->
                    safeNavigate(Screen.CountryFactoryProduct.createRoute(country, factoryNo, productName, cat))
                },
                onBrandProductClick = { brandName, productName, cat ->
                    safeNavigate(Screen.BrandProduct.createRoute(brandName, productName, cat))
                },
                onHomeCardsClick = {
                    safeNavigate(Screen.HomeCards.route)
                },
                onProfileClick = {
                    safeNavigate(Screen.Profile.route)
                },
                onInventoryClick = {
                    safeNavigate(Screen.Inventory.route)
                }
            )
        }

        // 首页卡片页
        composable(Screen.HomeCards.route) {
            HomeCardsScreen(
                onBackClick = {
                    safePopBackStack()
                },
                onProductClick = { productId, cat, productName ->
                    safeNavigate(Screen.Product.createRoute(productId, cat, productName))
                },
                onCountryClick = { country, cat ->
                    safeNavigate(Screen.Country.createRoute(country, cat))
                },
                onBrandClick = { brandName, cat ->
                    safeNavigate(Screen.Brand.createRoute(brandName, cat))
                },
                onMerchantClick = { merchantId, cat ->
                    safeNavigate(Screen.Merchant.createRoute(merchantId, cat))
                },
                onFactoryClick = { country, factoryNo, cat ->
                    safeNavigate(Screen.Factory.createRoute(country, factoryNo, cat))
                },
                onCountryProductClick = { country, productName, cat ->
                    safeNavigate(Screen.CountryProduct.createRoute(country, productName, cat))
                },
                onCountryFactoryProductClick = { country, factoryNo, productName, cat ->
                    safeNavigate(Screen.CountryFactoryProduct.createRoute(country, factoryNo, productName, cat))
                },
                onBrandProductClick = { brandName, productName, cat ->
                    safeNavigate(Screen.BrandProduct.createRoute(brandName, productName, cat))
                }
            )
        }

        // 搜索页
        composable(
            route = Screen.Search.route + "/{category}",
            arguments = listOf(navArgument("category") { type = NavType.StringType })
        ) { backStackEntry ->
            val category = backStackEntry.arguments?.getString("category")?.let(::routeDecode) ?: "牛"
            val viewModel: SearchViewModel = viewModel(
                factory = SearchViewModelFactory(context)
            )
            SearchScreen(
                viewModel = viewModel,
                category = category,
                onMerchantClick = { merchantId, cat ->
                    safeNavigate(Screen.Merchant.createRoute(merchantId, cat))
                },
                onProductClick = { productId, cat, productName ->
                    safeNavigate(Screen.Product.createRoute(productId, cat, productName))
                },
                onCountryClick = { country, cat ->
                    safeNavigate(Screen.Country.createRoute(country, cat))
                },
                onBrandClick = { brandName, cat ->
                    safeNavigate(Screen.Brand.createRoute(brandName, cat))
                },
                onFactoryClick = { country, factoryNo, cat ->
                    safeNavigate(Screen.Factory.createRoute(country, factoryNo, cat))
                },
                onCountryProductClick = { country, productName ->
                    safeNavigate(Screen.CountryProduct.createRoute(country, productName, category))
                },
                onCountryFactoryProductClick = { country, factoryNo, productName ->
                    safeNavigate(Screen.CountryFactoryProduct.createRoute(country, factoryNo, productName, category))
                },
                onBrandProductClick = { brandName, productName, cat ->
                    safeNavigate(Screen.BrandProduct.createRoute(brandName, productName, cat))
                },
                onBackClick = {
                    safePopBackStack()
                }
            )
        }

        // 商家详情页
        composable(
            route = Screen.Merchant.route,
            arguments = listOf(
                navArgument("merchantId") { type = NavType.LongType },
                navArgument("category") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val merchantId = backStackEntry.arguments?.getLong("merchantId") ?: 1L
            val category = backStackEntry.arguments?.getString("category")?.let(::routeDecode) ?: "牛"
            MerchantScreen(
                merchantId = merchantId,
                category = category,
                onBackClick = {
                    safePopBackStack()
                }
            )
        }

        // 产品详情页
        composable(
            route = Screen.Product.route,
            arguments = listOf(
                navArgument("productId") { type = NavType.IntType },
                navArgument("category") { type = NavType.StringType },
                navArgument("productName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getInt("productId") ?: 0
            val category = backStackEntry.arguments?.getString("category")?.let(::routeDecode) ?: "牛"
            val productName = backStackEntry.arguments?.getString("productName")?.let(::routeDecode) ?: ""
            val viewModel: ProductDetailViewModel = viewModel()
            ProductDetailScreen(
                productId = productId,
                category = category,
                productName = productName,
                onBackClick = {
                    safePopBackStack()
                },
                onSearchDelete = { cat ->
                    safeNavigateWithPopUp(Screen.Search.route + "/${routeEncode(cat)}", Screen.Search.route, true)
                },
                onCountryFactoryProductClick = { country, factoryNo, productName, cat ->
                    safeNavigate(Screen.CountryFactoryProduct.createRoute(country, factoryNo, productName, cat))
                },
                viewModel = viewModel
            )
        }

        // 国家详情页
        composable(
            route = Screen.Country.route,
            arguments = listOf(
                navArgument("country") { type = NavType.StringType },
                navArgument("category") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val country = backStackEntry.arguments?.getString("country")?.let(::routeDecode) ?: ""
            val category = backStackEntry.arguments?.getString("category")?.let(::routeDecode) ?: "牛"
            CountryDetailScreen(
                country = country,
                category = category,
                onBackClick = {
                    safePopBackStack()
                },
                onProductClick = { country, productName, cat ->
                    safeNavigate(Screen.CountryProduct.createRoute(country, productName, cat))
                },
                onFactoryClick = { country, factoryNo, cat ->
                    safeNavigate(Screen.Factory.createRoute(country, factoryNo, cat))
                },
                onSearchDelete = { cat ->
                    safeNavigateWithPopUp(Screen.Search.route + "/${routeEncode(cat)}", Screen.Search.route, true)
                }
            )
        }

        // 厂号详情页
        composable(
            route = Screen.Factory.route,
            arguments = listOf(
                navArgument("country") { type = NavType.StringType },
                navArgument("factoryNo") { type = NavType.StringType },
                navArgument("category") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val country = backStackEntry.arguments?.getString("country")?.let(::routeDecode) ?: ""
            val factoryNo = backStackEntry.arguments?.getString("factoryNo")?.let(::routeDecode) ?: ""
            val category = backStackEntry.arguments?.getString("category")?.let(::routeDecode) ?: "牛"
            FactoryDetailScreen(
                country = country,
                factoryNo = factoryNo,
                category = category,
                onBackClick = {
                    safePopBackStack()
                },
                onProductClick = { country, factoryNo, productId, productName ->
                    safeNavigate(Screen.CountryFactoryProduct.createRoute(country, factoryNo, productName, category))
                },
                onSearchDelete = { cat ->
                    safeNavigateWithPopUp(Screen.Search.route + "/${routeEncode(cat)}", Screen.Search.route, true)
                }
            )
        }

        // 国家+产品详情页
        composable(
            route = Screen.CountryProduct.route,
            arguments = listOf(
                navArgument("country") { type = NavType.StringType },
                navArgument("productName") { type = NavType.StringType },
                navArgument("category") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val country = backStackEntry.arguments?.getString("country")?.let(::routeDecode) ?: ""
            val productName = backStackEntry.arguments?.getString("productName")?.let(::routeDecode) ?: ""
            val category = backStackEntry.arguments?.getString("category")?.let(::routeDecode) ?: "牛"
            CountryProductScreen(
                country = country,
                productName = productName,
                category = category,
                onBackClick = {
                    safePopBackStack()
                },
                onFactoryClick = { c, factoryNo ->
                    safeNavigate(Screen.Factory.createRoute(c, factoryNo, category))
                },
                onCountryDelete = { productId, productName, cat ->
                    safeNavigate(Screen.Product.createRoute(productId, cat, productName))
                },
                onProductDelete = { country, cat ->
                    safeNavigate(Screen.Country.createRoute(country, cat))
                }
            )
        }

        // 国家+厂号+产品详情页
        composable(
            route = Screen.CountryFactoryProduct.route,
            arguments = listOf(
                navArgument("country") { type = NavType.StringType },
                navArgument("factoryNo") { type = NavType.StringType },
                navArgument("productName") { type = NavType.StringType },
                navArgument("category") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val country = backStackEntry.arguments?.getString("country")?.let(::routeDecode) ?: ""
            val factoryNo = backStackEntry.arguments?.getString("factoryNo")?.let(::routeDecode) ?: ""
            val productName = backStackEntry.arguments?.getString("productName")?.let(::routeDecode) ?: ""
            val category = backStackEntry.arguments?.getString("category")?.let(::routeDecode) ?: "牛"
            CountryFactoryProductScreen(
                country = country,
                factoryNo = factoryNo,
                productName = productName,
                category = category,
                onBackClick = {
                    safePopBackStack()
                },
                onFactoryClick = { c, factoryNo ->
                    safeNavigate(Screen.Factory.createRoute(c, factoryNo, category))
                },
                onCountryFactoryDelete = { productId, productName, cat ->
                    safeNavigate(Screen.Product.createRoute(productId, cat, productName))
                },
                onProductDelete = { country, factoryNo, cat ->
                    safeNavigate(Screen.Factory.createRoute(country, factoryNo, cat))
                },
                onSubstituteProductClick = { c, fn, pn, cat ->
                    safeNavigate(Screen.SubstituteProduct.createRoute(c, fn, pn, cat))
                }
            )
        }

        // 平替产品页
        composable(
            route = Screen.SubstituteProduct.route,
            arguments = listOf(
                navArgument("country") { type = NavType.StringType },
                navArgument("factoryNo") { type = NavType.StringType },
                navArgument("productName") { type = NavType.StringType },
                navArgument("category") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val country = backStackEntry.arguments?.getString("country")?.let(::routeDecode) ?: ""
            val factoryNo = backStackEntry.arguments?.getString("factoryNo")?.let(::routeDecode) ?: ""
            val productName = backStackEntry.arguments?.getString("productName")?.let(::routeDecode) ?: ""
            val category = backStackEntry.arguments?.getString("category")?.let(::routeDecode) ?: "牛"
            val viewModel: SubstituteProductViewModel = viewModel()
            SubstituteProductScreen(
                country = country,
                factoryNo = factoryNo,
                productName = productName,
                category = category,
                onBackClick = {
                    safePopBackStack()
                },
                onFactoryClick = { c, fn ->
                    safeNavigate(Screen.Factory.createRoute(c, fn, category))
                },
                onDataComparisonClick = { c, factoryNos, pn, cat, excludeFn ->
                    safeNavigate(Screen.DataComparison.createRoute(c, factoryNos, pn, cat, excludeFn))
                },
                viewModel = viewModel
            )
        }

        // 数据对比页
        composable(
            route = Screen.DataComparison.route,
            arguments = listOf(
                navArgument("country") { type = NavType.StringType },
                navArgument("factoryNos") { type = NavType.StringType },
                navArgument("productName") { type = NavType.StringType },
                navArgument("category") { type = NavType.StringType },
                navArgument("excludeFactoryNo") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val country = backStackEntry.arguments?.getString("country")?.let(::routeDecode) ?: ""
            val factoryNosStr = backStackEntry.arguments?.getString("factoryNos") ?: ""
            val factoryNos = factoryNosStr.split(",").filter { it.isNotEmpty() }.map(::routeDecode)
            val productName = backStackEntry.arguments?.getString("productName")?.let(::routeDecode) ?: ""
            val category = backStackEntry.arguments?.getString("category")?.let(::routeDecode) ?: "牛"
            val excludeFactoryNo = backStackEntry.arguments?.getString("excludeFactoryNo")?.let(::routeDecode)?.takeIf { it.isNotEmpty() }
            val viewModel: DataComparisonViewModel = viewModel()
            DataComparisonScreen(
                country = country,
                factoryNos = factoryNos,
                productName = productName,
                category = category,
                excludeFactoryNo = excludeFactoryNo,
                onBackClick = {
                    safePopBackStack()
                },
                viewModel = viewModel
            )
        }

        // 品牌详情页
        composable(
            route = Screen.Brand.route,
            arguments = listOf(
                navArgument("brandName") { type = NavType.StringType },
                navArgument("category") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val brandName = backStackEntry.arguments?.getString("brandName")?.let(::routeDecode) ?: ""
            val category = backStackEntry.arguments?.getString("category")?.let(::routeDecode) ?: "牛"
            val viewModel: BrandDetailViewModel = viewModel()
            BrandDetailScreen(
                brandName = brandName,
                category = category,
                onBackClick = {
                    safePopBackStack()
                },
                onProductClick = { productId, cat, productName ->
                    safeNavigate(Screen.Product.createRoute(productId, cat, productName))
                },
                onBrandProductClick = { bn, pn, cat ->
                    safeNavigate(Screen.BrandProduct.createRoute(bn, pn, cat))
                },
                onSearchDelete = { cat ->
                    safeNavigateWithPopUp(Screen.Search.route + "/${routeEncode(cat)}", Screen.Search.route, true)
                },
                viewModel = viewModel
            )
        }

        // 品牌+产品详情页
        composable(
            route = Screen.BrandProduct.route,
            arguments = listOf(
                navArgument("brandName") { type = NavType.StringType },
                navArgument("productName") { type = NavType.StringType },
                navArgument("category") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val brandName = backStackEntry.arguments?.getString("brandName")?.let(::routeDecode) ?: ""
            val productName = backStackEntry.arguments?.getString("productName")?.let(::routeDecode) ?: ""
            val category = backStackEntry.arguments?.getString("category")?.let(::routeDecode) ?: "牛"
            val viewModel: BrandProductDetailViewModel = viewModel()
            BrandProductDetailScreen(
                brandName = brandName,
                productName = productName,
                category = category,
                onBackClick = {
                    safePopBackStack()
                },
                onNavigateToProduct = { productId, prodName, cat ->
                    safeNavigate(Screen.Product.createRoute(productId, cat, prodName))
                },
                onNavigateToBrand = { bName, cat ->
                    safeNavigate(Screen.Brand.createRoute(bName, cat))
                },
                onCountryFactoryProductClick = { country, factoryNo, productName, cat ->
                    safeNavigate(Screen.CountryFactoryProduct.createRoute(country, factoryNo, productName, cat))
                },
                viewModel = viewModel
            )
        }

        // 个人中心页
        composable(Screen.Profile.route) {
            ProfileScreen(
                onBackClick = {
                    safePopBackStack()
                },
                onNavigateToLogin = {
                    safeNavigateWithPopUp(Screen.Login.route, Screen.Home.route, true)
                },
                onNavigateToEditProfile = {
                    safeNavigate(Screen.EditProfile.route)
                }
            )
        }

        // 编辑资料页
        composable(Screen.EditProfile.route) {
            EditProfileScreen(
                onBackClick = {
                    safePopBackStack()
                },
                onSaveSuccess = {
                    // 先返回上一页（EditProfile出栈），再跳转个人中心（新实例）
                    // 这样避免在Login流程上叠加，导致点返回键回不到首页
                    safePopBackStack()
                    safeNavigate(Screen.Profile.route)
                }
            )
        }

        // 库存页
        composable(Screen.Inventory.route) {
            InventoryScreen(
                onBackClick = {
                    safePopBackStack()
                }
            )
        }
    }
}
