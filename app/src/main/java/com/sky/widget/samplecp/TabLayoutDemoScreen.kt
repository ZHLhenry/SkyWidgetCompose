package com.sky.widget.samplecp

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sky.widget.badge.SkyBadgeState
import com.sky.widget.iface.SkyIconType
import com.sky.widget.samplecp.ui.theme.SkyWidgetComposeTheme
import com.sky.widget.tabLayout.SkyTabItem
import com.sky.widget.tabLayout.SkyTabLayout
import com.sky.widget.tabLayout.SkyTabMode
import com.sky.widget.tabLayout.rememberSkyTabStyle
import com.sky.widget.viewpage.SkyViewPage
import com.sky.widget.viewpage.rememberSkyViewPageState

/**
 * Tab 布局（SkyTabLayout）示例页。
 *
 * 演示：
 * - 等分模式（Fixed）：图标 + 文字底部导航样式，带数字 / 圆点角标
 * - 滑动模式（Scrollable）：Tab 宽度自适应，超出可横向滑动，选中项自动居中
 * - 联动 SkyViewPage：Tab 与分页容器双向同步
 * - 自定义 Tab 布局：通过 itemContent 插槽实现胶囊选中样式
 */
@Composable
fun TabLayoutDemoScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部标题栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2196F3))
                .statusBarsPadding()
                .height(56.dp)
                .padding(start = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = Color.White
                )
            }
            Text(
                text = "Tab 布局示例",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FixedModeCard()
            IconFontModeCard()
            VerticalModeCard()
            ScrollableModeCard()
            ViewPagerLinkedCard()
            SingleTabCustomContentCard()
            CustomItemCard()
        }
    }
}

/**
 * 等分模式：仿截图的底部导航样式，首页带 999+ 角标，"我的" 带圆点。
 */
@Composable
private fun FixedModeCard() {
    DemoCard(title = "等分模式（Fixed，图标 + 文字 + 角标）") {
        val homePainter = rememberVectorPainter(Icons.Default.Home)
        val emailPainter = rememberVectorPainter(Icons.Default.Email)
        val starPainter = rememberVectorPainter(Icons.Default.Star)
        val menuPainter = rememberVectorPainter(Icons.Default.Menu)
        val personPainter = rememberVectorPainter(Icons.Default.Person)

        val badgeState = remember {
            SkyBadgeState(initialNumber = 1000, maxNumber = 999)
        }
        val tabs = remember(homePainter, emailPainter, starPainter, menuPainter, personPainter) {
            listOf(
                SkyTabItem(text = "首页", icon = SkyIconType.painter(homePainter), badgeState = badgeState),
                SkyTabItem(text = "项目", icon = SkyIconType.painter(emailPainter)),
                SkyTabItem(text = "广场", icon = SkyIconType.painter(starPainter)),
                SkyTabItem(text = "体系", icon = SkyIconType.painter(menuPainter)),
                SkyTabItem(text = "我的", icon = SkyIconType.painter(personPainter), badgeState = SkyBadgeState(initialNumber = -1))
            )
        }

        var selectedIndex by remember { mutableIntStateOf(0) }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SkyTabLayout(
                tabs = tabs,
                selectedIndex = selectedIndex,
                onTabSelected = { selectedIndex = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFAFAFA)),
                style = rememberSkyTabStyle(iconTint = true)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { badgeState.setBadgeNumber(badgeState.number + 1) }) {
                    Text("首页角标 +1")
                }
                OutlinedButton(onClick = { badgeState.setBadgeNumber(0) }) {
                    Text("清除角标")
                }
            }
        }
    }
}

/**
 * iconfont 图标：iconName + iconFontName 指定 testSky 字体，随选中态着色（iconTint = true）。
 * 字体已在 SampleApp 启动时通过 SkyIconFontsLib.initRegister 注册。
 */
@Composable
private fun IconFontModeCard() {
    DemoCard(title = "iconfont 图标（iconName + iconFontName）") {
        val fontName = "testSky_iconfont"
        val badgeState = remember { SkyBadgeState(initialNumber = 3) }
        val tabs = remember(badgeState) {
            listOf(
                SkyTabItem(text = "首页", icon = SkyIconType.iconFont("testSkyshouye", fontName), badgeState = badgeState),
                SkyTabItem(text = "项目", icon = SkyIconType.iconFont("testSkyxiangmu", fontName)),
                SkyTabItem(text = "广场", icon = SkyIconType.iconFont("testSkyguangchang", fontName)),
                SkyTabItem(text = "体系", icon = SkyIconType.iconFont("testSkytixi", fontName)),
                SkyTabItem(text = "我的", icon = SkyIconType.iconFont("testSkywode", fontName))
            )
        }

        var selectedIndex by remember { mutableIntStateOf(0) }

        SkyTabLayout(
            tabs = tabs,
            selectedIndex = selectedIndex,
            onTabSelected = { selectedIndex = it },
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFAFAFA)),
            style = rememberSkyTabStyle(iconTint = true)
        )
    }
}

/**
 * 纵向模式：左侧纵向滑动 Tab（orientation = Vertical），高度受限时可上下滑动，选中项自动居中。
 */
@Composable
private fun VerticalModeCard() {
    DemoCard(title = "纵向模式（orientation = Vertical，侧边栏式导航）") {
        val titles = listOf(
            "推荐", "关注", "热点", "视频", "图文", "问答",
            "直播", "同城", "购物", "消息", "日程", "附近"
        )
        val tabs = remember { titles.map { SkyTabItem(text = it) } }
        var selectedIndex by remember { mutableIntStateOf(0) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            SkyTabLayout(
                tabs = tabs,
                selectedIndex = selectedIndex,
                onTabSelected = { selectedIndex = it },
                modifier = Modifier
                    .width(88.dp)
                    .fillMaxHeight()
                    .background(Color(0xFFFAFAFA)),
                mode = SkyTabMode.Scrollable,
                orientation = Orientation.Vertical,
                style = rememberSkyTabStyle(itemSpacing = 8.dp, edgePadding = 8.dp)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(start = 12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE3F2FD)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "当前选中：${titles[selectedIndex]}",
                    fontSize = 16.sp,
                    color = Color(0xFF555555)
                )
            }
        }
    }
}

/**
 * 滑动模式：多个 Tab 超出屏幕宽度，可横向滑动，选中项自动滚动居中。
 */
@Composable
private fun ScrollableModeCard() {
    DemoCard(title = "滑动模式（Scrollable，选中自动居中）") {
        val icons = listOf(
            Icons.Default.Home, Icons.Default.Email, Icons.Default.Star,
            Icons.Default.Menu, Icons.Default.Person, Icons.Default.Search,
            Icons.Default.Favorite, Icons.Default.Settings, Icons.Default.ShoppingCart,
            Icons.Default.Notifications, Icons.Default.DateRange, Icons.Default.Place
        ).map { rememberVectorPainter(it) }
        val titles = listOf(
            "推荐", "关注", "热点", "视频", "图文", "问答",
            "直播", "同城", "购物", "消息", "日程", "附近"
        )
        val tabs = remember(icons) {
            titles.mapIndexed { index, title ->
                SkyTabItem(
                    text = title,
                    icon = SkyIconType.painter(icons[index]),
                    badgeState = if (index == 2) SkyBadgeState(initialNumber = 5) else null
                )
            }
        }

        var selectedIndex by remember { mutableIntStateOf(0) }

        SkyTabLayout(
            tabs = tabs,
            selectedIndex = selectedIndex,
            onTabSelected = { selectedIndex = it },
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFAFAFA)),
            mode = SkyTabMode.Scrollable,
            style = rememberSkyTabStyle(iconTint = true, itemSpacing = 24.dp)
        )
    }
}

/**
 * 联动 SkyViewPage：点击 Tab 翻页，滑动页面 Tab 自动跟随。
 */
@Composable
private fun ViewPagerLinkedCard() {
    DemoCard(title = "联动 SkyViewPage（滑动 / 点击双向同步）") {
        val pageColors = listOf(
            Color(0xFFFFE0B2), Color(0xFFC8E6C9), Color(0xFFBBDEFB), Color(0xFFF8BBD0)
        )
        val tabPainters: List<Painter> = listOf(
            Icons.Default.Home, Icons.Default.Email, Icons.Default.Star, Icons.Default.Person
        ).map { rememberVectorPainter(it) }
        val tabs = remember(tabPainters) {
            listOf("首页", "消息", "收藏", "我的").mapIndexed { index, title ->
                SkyTabItem(text = title, icon = SkyIconType.painter(tabPainters[index]))
            }
        }
        val pagerState = rememberSkyViewPageState()

        Column {
            SkyViewPage(
                pageCount = tabs.size,
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(pageColors[index]),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = tabs[index].text, fontSize = 20.sp, color = Color(0xFF555555))
                }
            }
            SkyTabLayout(
                tabs = tabs,
                pagerState = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFAFAFA)),
                style = rememberSkyTabStyle(iconTint = true)
            )
        }
    }
}

/**
 * 单个 Tab 自定义：SkyTabItem.customContent 实现底部导航中间凸起的圆形发布按钮。
 */
@Composable
private fun SingleTabCustomContentCard() {
    DemoCard(title = "单个 Tab 自定义（SkyTabItem.customContent）") {
        val homePainter = rememberVectorPainter(Icons.Default.Home)
        val starPainter = rememberVectorPainter(Icons.Default.Star)
        val emailPainter = rememberVectorPainter(Icons.Default.Email)
        val personPainter = rememberVectorPainter(Icons.Default.Person)

        val tabs = remember(homePainter, starPainter, emailPainter, personPainter) {
            listOf(
                SkyTabItem(text = "首页", icon = SkyIconType.painter(homePainter)),
                SkyTabItem(text = "收藏", icon = SkyIconType.painter(starPainter)),
                // 中间 Tab 完全自定义：凸起的圆形 + 按钮，不受默认「图标 + 文字」样式约束
                SkyTabItem(
                    text = "发布",
                    customContent = { selected ->
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .shadow(if (selected) 6.dp else 3.dp, CircleShape)
                                .background(
                                    if (selected) Color(0xFFFF5722) else Color(0xFFFF8A65),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "发布",
                                tint = Color.White
                            )
                        }
                    }
                ),
                SkyTabItem(text = "消息", icon = SkyIconType.painter(emailPainter)),
                SkyTabItem(text = "我的", icon = SkyIconType.painter(personPainter))
            )
        }

        var selectedIndex by remember { mutableIntStateOf(0) }

        SkyTabLayout(
            tabs = tabs,
            selectedIndex = selectedIndex,
            onTabSelected = { selectedIndex = it },
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFAFAFA)),
            style = rememberSkyTabStyle(iconTint = true)
        )
    }
}

/**
 * 自定义 Tab 布局：itemContent 插槽实现纯文字 + 胶囊选中背景样式。
 */
@Composable
private fun CustomItemCard() {
    DemoCard(title = "自定义 Tab 样式（itemContent 插槽）") {
        val tabs = remember {
            listOf("全部", "进行中", "已完成", "已关闭").map { SkyTabItem(text = it) }
        }
        var selectedIndex by remember { mutableIntStateOf(0) }

        SkyTabLayout(
            tabs = tabs,
            selectedIndex = selectedIndex,
            onTabSelected = { selectedIndex = it },
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFAFAFA)),
            mode = SkyTabMode.Scrollable,
            style = rememberSkyTabStyle(itemSpacing = 12.dp, edgePadding = 8.dp)
        ) { _, tab, selected ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) Color(0xFFFF5722) else Color(0xFFE0E0E0))
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tab.text,
                    color = if (selected) Color.White else Color(0xFF666666),
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
fun TabLayoutDemoScreenPreview() {
    SkyWidgetComposeTheme {
        TabLayoutDemoScreen(onBack = {})
    }
}
