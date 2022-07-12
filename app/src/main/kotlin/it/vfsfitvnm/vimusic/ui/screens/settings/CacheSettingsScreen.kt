package it.vfsfitvnm.vimusic.ui.screens.settings

import android.text.format.Formatter
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.Coil
import coil.annotation.ExperimentalCoilApi
import it.vfsfitvnm.route.RouteHandler
import it.vfsfitvnm.vimusic.LocalPlayerServiceBinder
import it.vfsfitvnm.vimusic.R
import it.vfsfitvnm.vimusic.enums.ExoPlayerDiskCacheMaxSize
import it.vfsfitvnm.vimusic.ui.components.TopAppBar
import it.vfsfitvnm.vimusic.ui.components.themed.TextCard
import it.vfsfitvnm.vimusic.ui.screens.*
import it.vfsfitvnm.vimusic.ui.styling.LocalColorPalette
import it.vfsfitvnm.vimusic.ui.styling.LocalTypography
import it.vfsfitvnm.vimusic.utils.LocalPreferences
import it.vfsfitvnm.vimusic.utils.semiBold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoilApi::class)
@ExperimentalAnimationApi
@Composable
fun CacheSettingsScreen() {
    val albumRoute = rememberAlbumRoute()
    val artistRoute = rememberArtistRoute()

    val scrollState = rememberScrollState()

    RouteHandler(listenToGlobalEmitter = true) {
        albumRoute { browseId ->
            AlbumScreen(
                browseId = browseId ?: error("browseId cannot be null")
            )
        }

        artistRoute { browseId ->
            ArtistScreen(
                browseId = browseId ?: error("browseId cannot be null")
            )
        }

        host {
            val context = LocalContext.current
            val colorPalette = LocalColorPalette.current
            val typography = LocalTypography.current
            val preferences = LocalPreferences.current
            val binder = LocalPlayerServiceBinder.current

            val coroutineScope = rememberCoroutineScope()

            Column(
                modifier = Modifier
                    .background(colorPalette.background)
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(bottom = 72.dp)
            ) {
                TopAppBar(
                    modifier = Modifier
                        .height(52.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.chevron_back),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(colorPalette.text),
                        modifier = Modifier
                            .clickable(onClick = pop)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .size(24.dp)
                    )

                    BasicText(
                        text = "Cache",
                        style = typography.m.semiBold
                    )

                    Spacer(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .size(24.dp)
                    )
                }

                Coil.imageLoader(context).diskCache?.let { diskCache ->
                    var diskCacheSize by remember(diskCache) {
                        mutableStateOf(diskCache.size)
                    }

                    SettingsEntryGroupText(title = "IMAGE CACHE")

                    EnumValueSelectorSettingsEntry(
                        title = "Max size",
                        selectedValue = preferences.coilDiskCacheMaxSize,
                        onValueSelected = {
                            preferences.coilDiskCacheMaxSize = it
                        }
                    )

                    DisabledSettingsEntry(
                        title = "Space used",
                        text = "${
                            Formatter.formatShortFileSize(
                                context,
                                diskCacheSize
                            )
                        } (${
                            diskCacheSize * 100 / preferences.coilDiskCacheMaxSize.bytes.coerceAtLeast(
                                1
                            )
                        }%)",
                    )

                    SettingsEntry(
                        title = "Clear space",
                        text = "Wipe every cached image",
                        onClick = {
                            coroutineScope.launch(Dispatchers.IO) {
                                diskCache.clear()
                                diskCacheSize = diskCache.size
                            }
                        }
                    )
                }

                binder?.cache?.let { cache ->
                    val diskCacheSize by remember {
                        derivedStateOf {
                            cache.cacheSpace
                        }
                    }

                    SettingsEntryGroupText(title = "SONG CACHE")

                    EnumValueSelectorSettingsEntry(
                        title = "Max size",
                        selectedValue = preferences.exoPlayerDiskCacheMaxSize,
                        onValueSelected = {
                            preferences.exoPlayerDiskCacheMaxSize = it
                        }
                    )

                    DisabledSettingsEntry(
                        title = "Space used",
                        text = buildString {
                            append(Formatter.formatShortFileSize(context, diskCacheSize))

                            when (val size = preferences.exoPlayerDiskCacheMaxSize) {
                                ExoPlayerDiskCacheMaxSize.Unlimited -> {}
                                else -> append("(${diskCacheSize * 100 / size.bytes}%)")
                            }
                        }
                    )
                }

                TextCard(icon = R.drawable.alert_circle) {
                    Title(text = "Cache strategy")
                    Text(text = "The cache follows the LRU (Least Recently Used) strategy: when it runs out of space, the resources that haven't been accessed for the longest time are cleared to accommodate the new resource.")
                }
            }
        }
    }
}