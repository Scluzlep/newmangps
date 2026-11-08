package com.scluzlep.newmangps.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier

@Composable
fun MainSectionBar(selected: Int, onMap: () -> Unit, onFeatures: () -> Unit, onAccount: () -> Unit, modifier: Modifier = Modifier) {
    val language = LocalAppLanguage.current
    NavigationBar(modifier = modifier) {
        listOf(
            Triple(language.text("Map", "地图"), Icons.Default.Map, onMap),
            Triple(language.text("Function", "功能"), Icons.Default.GridView, onFeatures),
            Triple(language.text("My", "账户"), Icons.Default.AccountCircle, onAccount)
        ).forEachIndexed { index, (label, icon, action) ->
            NavigationBarItem(
                selected = selected == index,
                onClick = action,
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) }
            )
        }
    }
}
