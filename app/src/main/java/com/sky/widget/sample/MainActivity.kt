package com.sky.widget.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sky.widget.sample.ui.theme.SkyWidgetComposeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SkyWidgetComposeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AppRoot()
                }
            }
        }
    }
}

/**
 * 应用根：管理页面跳转（首页 / 各示例页）
 */
@Composable
fun AppRoot() {
    var currentPage by remember { mutableStateOf<Screen>(Screen.Home) }

    // 非首页时拦截返回按钮：示例详情页先返回刷新菜单，菜单再返回首页
    BackHandler(enabled = currentPage != Screen.Home) {
        currentPage = if (currentPage == Screen.Refresh) Screen.Home else Screen.Refresh
    }

    when (currentPage) {
        Screen.Home -> HomeScreen(onNavigate = { currentPage = it })
        Screen.Refresh -> RefreshDemoMenuScreen(
            onBack = { currentPage = Screen.Home },
            onNavigate = { currentPage = it }
        )
        Screen.RefreshBasic -> RefreshSampleScreen(onBack = { currentPage = Screen.Refresh })
        Screen.RefreshOnly -> OnlyRefreshDemoScreen(onBack = { currentPage = Screen.Refresh })
        Screen.LoadMoreOnly -> LoadMoreOnlyDemoScreen(onBack = { currentPage = Screen.Refresh })
        Screen.NoMoreHidden -> NoMoreHiddenDemoScreen(onBack = { currentPage = Screen.Refresh })
        Screen.Stickiness -> StickinessDemoScreen(onBack = { currentPage = Screen.Refresh })
        Screen.Horizontal -> HorizontalRefreshDemoScreen(onBack = { currentPage = Screen.Refresh })
        Screen.CustomIndicator -> CustomIndicatorDemoScreen(onBack = { currentPage = Screen.Refresh })
        Screen.AutoTrigger -> AutoTriggerDemoScreen(onBack = { currentPage = Screen.Refresh })
    }
}

sealed interface Screen {
    data object Home : Screen
    data object Refresh : Screen
    data object RefreshBasic : Screen
    data object RefreshOnly : Screen
    data object LoadMoreOnly : Screen
    data object NoMoreHidden : Screen
    data object Stickiness : Screen
    data object Horizontal : Screen
    data object CustomIndicator : Screen
    data object AutoTrigger : Screen
}

/**
 * 首页：组件网格列表（3列等宽网格）
 */
@Composable
fun HomeScreen(onNavigate: (Screen) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部标题栏（沉浸式蓝色状态栏）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2196F3))
                .statusBarsPadding()
                .height(56.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "组件",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }

        val components = listOf(
            ComponentItem("角标", "\uD83D\uDCC5"),
            ComponentItem("图标字体", "T"),
            ComponentItem("比例图片", "\uD83D\uDDBC"),
            ComponentItem("圆形图片", "\uD83C\uDFAF"),
            ComponentItem("底部导航", "\uD83D\uDCC5"),
            ComponentItem("文字滚动", "\uD83D\uDCDD"),
            ComponentItem("签名板", "\u270F\uFE0F"),
            ComponentItem("二维码", "\uD83D\uDCF6"),
            ComponentItem("刷新示例", "\uD83D\uDCC5"),
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(1.dp),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            items(components, key = { it.name }) { item ->
                ComponentGridItem(
                    item = item,
                    onClick = {
                        when (item.name) {
                            "刷新示例" -> onNavigate(Screen.Refresh)
                        }
                    }
                )
            }
        }
    }
}

/**
 * 单个组件网格项：正方形卡片 + 图标 + 文字标签
 */
@Composable
fun ComponentGridItem(item: ComponentItem, onClick: (() -> Unit)? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(Color.White)
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 图标区域
        Text(
            text = item.icon,
            fontSize = 28.sp,
            color = Color(0xFF2196F3),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(4.dp))

        // 标签文字
        Text(
            text = item.name,
            fontSize = 12.sp,
            color = Color(0xFF333333),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

data class ComponentItem(val name: String, val icon: String)

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun AppRootPreview() {
    SkyWidgetComposeTheme {
        AppRoot()
    }
}
