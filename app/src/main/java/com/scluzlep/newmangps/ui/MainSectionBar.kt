package com.scluzlep.newmangps.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.scluzlep.newmangps.ui.theme.AppIcons
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarDisplayMode
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun MainSectionBar(
    selected: Int,
    onMap: () -> Unit,
    onFeatures: () -> Unit,
    onAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    val language = LocalAppLanguage.current
    NavigationBar(
        modifier = modifier,
        color = MiuixTheme.colorScheme.surface,
        showDivider = true,
        defaultWindowInsetsPadding = true,
        mode = NavigationBarDisplayMode.IconAndText
    ) {
        val items = listOf(
            Triple(
                language.text("Map", "地图"),
                if (selected == 0) AppIcons.Map else AppIcons.MapOutlined,
                onMap
            ),
            Triple(
                language.text("Function", "功能"),
                if (selected == 1) AppIcons.GridView else AppIcons.GridViewOutlined,
                onFeatures
            ),
            Triple(
                language.text("My", "账户"),
                if (selected == 2) Icons.Filled.AccountCircle else Icons.Outlined.AccountCircle,
                onAccount
            )
        )
        items.forEachIndexed { index, (label, icon, action) ->
            NavigationBarItem(
                selected = selected == index,
                onClick = action,
                icon = icon,
                label = label
            )
        }
    }
}
