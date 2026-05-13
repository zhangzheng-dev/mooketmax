package com.mooket.app

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.mooket.app.data.SessionManager
import com.mooket.app.navigation.MooketNavHost
import com.mooket.app.ui.theme.MooketTheme

class MainActivity : ComponentActivity() {
    companion object {
        lateinit var appContext: Context
            private set
    }
    // 延迟 finish 到下一帧，避免与 Compose 导航动画/vsync 时序冲突导致白屏
    private val finishHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.WHITE
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
        enableEdgeToEdge()

        // 初始化 SessionManager（恢复登录态）
        SessionManager.init(this)
        appContext = applicationContext

        setContent {
            val navController = rememberNavController()
            val lastPopTime = remember { mutableLongStateOf(0L) }
            val debounceMs = 300L

            // 拦截系统 back press，防止快速连续触发导致白屏
            val backCallback = remember {
                object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        val now = System.currentTimeMillis()
                        if (now - lastPopTime.longValue > debounceMs) {
                            lastPopTime.longValue = now
                            if (!navController.popBackStack()) {
                                // 没有更多页面，延迟退出，等待 vsync 异常帧过去后强制移除任务
                                finishHandler.postDelayed({ finishAndRemoveTask() }, 100)
                            }
                        }
                    }
                }
            }
            onBackPressedDispatcher.addCallback(this, backCallback)

            MooketTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White
                ) {
                    MooketNavHost(
                        context = applicationContext,
                        navController = navController
                    )
                }
            }
        }
    }
}
