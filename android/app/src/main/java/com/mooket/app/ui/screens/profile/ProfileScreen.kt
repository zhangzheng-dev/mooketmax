package com.mooket.app.ui.screens.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIos
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import android.widget.Toast
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.mooket.app.R
import com.mooket.app.data.SessionManager
import com.mooket.app.data.api.RetrofitClient
import com.mooket.app.data.repository.ProfileRepository
import com.mooket.app.ui.theme.*

/**
 * 个人中心页面
 */
@Composable
fun ProfileScreen(
    onBackClick: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToEditProfile: () -> Unit = {}
) {
    val repository = remember { ProfileRepository(RetrofitClient.apiService) }
    val viewModel: ProfileViewModel = viewModel { ProfileViewModel(repository) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // showUpdateDialog 由 LaunchedEffect + 点击"发现新版本"两种方式触发
    // 控制确认弹窗
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.hasUpdate) {
        if (uiState.hasUpdate && !uiState.isCheckingUpdate) {
            showUpdateDialog = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            ProfileHeader(
                title = "个人中心",
                onBackClick = onBackClick,
                onSettingsClick = { /* TODO: 设置页面 */ }
            )

            // User Info Card
            UserInfoCard(
                avatarUrl = uiState.profile?.avatarUrl,
                nickname = uiState.profile?.nickname ?: "未登录",
                phone = uiState.profile?.phone ?: "",
                mooketNo = uiState.profile?.mooketNo,
                realNameStatus = uiState.profile?.realNameStatus,
                realName = uiState.profile?.realName,
                onEditClick = onNavigateToEditProfile
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Menu Section
            MenuSection(
                onUserAgreementClick = { /* TODO */ },
                onPrivacyPolicyClick = { /* TODO */ },
                onLogoutAccountClick = { showCancelDialog = true },
                onAboutClick = { /* TODO: 关于牧集 */ }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // App Update Section
            AppUpdateSection(
                version = uiState.version,
                hasUpdate = uiState.hasUpdate,
                isCheckingUpdate = uiState.isCheckingUpdate,
                onCheckUpdate = { viewModel.checkUpdate() },
                onShowUpdateDialog = { showUpdateDialog = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Logout Button
            LogoutButton(
                isLoggingOut = uiState.isLoggingOut,
                onLogout = { showLogoutDialog = true }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }

        // 退出登录确认弹窗
        if (showLogoutDialog) {
            ConfirmDialog(
                title = "确认退出",
                message = "确定要退出当前账号吗？",
                confirmText = "退出",
                cancelText = "取消",
                onConfirm = {
                    showLogoutDialog = false
                    viewModel.logout {
                        onNavigateToLogin()
                    }
                },
                onDismiss = { showLogoutDialog = false }
            )
        }

        // 注销账号确认弹窗
        if (showCancelDialog) {
            ConfirmDialog(
                title = "确认注销",
                message = "注销后将清除所有历史数据，且无法恢复。确定要注销账号吗？",
                confirmText = "注销",
                cancelText = "取消",
                isDanger = true,
                onConfirm = {
                    showCancelDialog = false
                    viewModel.cancelAccount {
                        onNavigateToLogin()
                    }
                },
                onDismiss = { showCancelDialog = false }
            )
        }

        // 版本更新弹窗
        if (showUpdateDialog) {
            UpdateDialog(
                version = uiState.version,
                updateContent = uiState.updateMessage ?: "发现新版本，是否立即更新？",
                isDownloading = uiState.isDownloadingUpdate,
                downloadProgress = uiState.updateDownloadProgress,
                onConfirm = {
                    uiState.updateUrl?.let { url ->
                        viewModel.downloadApk(context, url, { progress ->
                            // 进度在 ViewModel 中更新
                        }) { apkFile ->
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                apkFile
                            )
                            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "application/vnd.android.package-archive")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(installIntent)
                        }
                    }
                },
                onDismiss = { showUpdateDialog = false }
            )
        }
    }
}

/**
 * 通用确认弹窗
 */
@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    cancelText: String,
    isDanger: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 取消按钮
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Background,
                            contentColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Border)
                    ) {
                        Text(text = cancelText, fontSize = 14.sp)
                    }

                    // 确认按钮
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDanger) Error else Primary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = confirmText, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

/**
 * 版本更新弹窗（带下载进度条）
 */
@Composable
private fun UpdateDialog(
    version: String,
    updateContent: String,
    isDownloading: Boolean = false,
    downloadProgress: Int = 0,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = { if (!isDownloading) onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isDownloading) "正在下载更新..." else "发现新版本 v$version",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (isDownloading) {
                    // 下载进度条
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LinearProgressIndicator(
                            progress = downloadProgress / 100f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = Primary,
                            trackColor = PrimaryLight
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$downloadProgress%",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                    }
                } else {
                    Text(
                        text = updateContent,
                        fontSize = 14.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!isDownloading) {
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Background,
                                contentColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Border)
                        ) {
                            Text(text = "稍后", fontSize = 14.sp)
                        }
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        enabled = !isDownloading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Primary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isDownloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Text(text = "立即更新", fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Profile Header
 */
@Composable
private fun ProfileHeader(
    title: String,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.ArrowBackIos,
            contentDescription = "返回",
            tint = TextPrimary,
            modifier = Modifier
                .size(24.dp)
                .clickable { onBackClick() }
        )
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = "设置",
            tint = TextPrimary,
            modifier = Modifier
                .size(24.dp)
                .clickable { onSettingsClick() }
        )
    }
}

/**
 * User Info Card
 */
@Composable
private fun UserInfoCard(
    avatarUrl: String?,
    nickname: String,
    phone: String,
    mooketNo: String?,
    realNameStatus: String?,
    realName: String?,
    onEditClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(Surface, RoundedCornerShape(12.dp))
            .border(1.dp, Border, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        // Logo + Edit Button Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_mooket_max_logo),
                contentDescription = "MooketMax Logo",
                modifier = Modifier.height(14.645.dp).width(90.dp)
            )
            Text(
                text = "编辑资料",
                fontSize = 12.sp,
                color = Primary,
                modifier = Modifier
                    .background(PrimaryLight, RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .clickable { onEditClick() }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Avatar + Name/Phone Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：昵称 + 手机号 + 牧集号
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = nickname,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    // Real name badge
                    if (realNameStatus == "approved" && !realName.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(Primary, RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "已实名",
                                fontSize = 10.sp,
                                color = Surface
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (phone.isNotEmpty()) "${phone.substring(0, 3)}****${phone.substring(phone.length - 4)}" else "未登录",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                if (!mooketNo.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "牧集号: $mooketNo",
                        fontSize = 12.sp,
                        color = TextHint
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 右侧：头像
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(PrimaryLight),
                contentAlignment = Alignment.Center
            ) {
                if (!avatarUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = "头像",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "头像",
                        tint = Primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

/**
 * Menu Section
 */
@Composable
private fun MenuSection(
    onUserAgreementClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onLogoutAccountClick: () -> Unit,
    onAboutClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface, RoundedCornerShape(12.dp))
            .border(1.dp, Border, RoundedCornerShape(12.dp))
    ) {
        MenuItem(
            title = "用户协议",
            onClick = onUserAgreementClick
        )
        MenuDivider()
        MenuItem(
            title = "隐私条款",
            onClick = onPrivacyPolicyClick
        )
        MenuDivider()
        MenuItem(
            title = "注销账号",
            onClick = onLogoutAccountClick,
            titleColor = Error
        )
        MenuDivider()
        MenuItem(
            title = "关于牧集",
            onClick = onAboutClick,
            showArrow = true
        )
    }
}

@Composable
private fun MenuItem(
    title: String,
    onClick: () -> Unit,
    titleColor: Color = TextPrimary,
    showArrow: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            color = titleColor
        )
        if (showArrow) {
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = TextHint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun MenuDivider() {
    Divider(
        color = Border,
        thickness = 0.5.dp,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

/**
 * App Update Section
 */
@Composable
private fun AppUpdateSection(
    version: String,
    hasUpdate: Boolean,
    isCheckingUpdate: Boolean,
    onCheckUpdate: () -> Unit,
    onShowUpdateDialog: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface, RoundedCornerShape(12.dp))
            .border(1.dp, Border, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "当前版本",
            fontSize = 14.sp,
            color = TextPrimary
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = version,
                fontSize = 14.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.width(12.dp))
            if (isCheckingUpdate) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = Primary
                )
            } else if (hasUpdate) {
                // 有更新本地状态，直接弹窗，不需要再调 API
                Text(
                    text = "发现新版本",
                    fontSize = 12.sp,
                    color = Primary,
                    modifier = Modifier
                        .background(PrimaryLight, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .clickable { onShowUpdateDialog() }
                )
            } else {
                Text(
                    text = "检查更新",
                    fontSize = 12.sp,
                    color = TextHint,
                    modifier = Modifier
                        .background(Color(0xFFF0F0F0), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .clickable { onCheckUpdate() }
                )
            }
        }
    }
}

/**
 * Logout Button
 */
@Composable
private fun LogoutButton(
    isLoggingOut: Boolean,
    onLogout: () -> Unit
) {
    Button(
        onClick = onLogout,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(48.dp),
        enabled = !isLoggingOut,
        colors = ButtonDefaults.buttonColors(
            containerColor = Surface,
            contentColor = Error
        ),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Error)
    ) {
        if (isLoggingOut) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = Error
            )
        } else {
            Text(
                text = "退出登录",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}