package android.bignerdranch.reportsapp.reports.presentation.components

import android.bignerdranch.reportsapp.storage.downloadMediaFile
import android.util.Log
import android.view.View
import android.widget.ImageButton
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView

@Composable
fun FullScreenVideoDialog(
    videoUrl: String,
    onDismiss: () -> Unit
) {
    val TAG = "FullScreenVideoDialog"
    val context = LocalContext.current
    var showDownloadOption by remember { mutableStateOf(false) }

    Log.d(TAG, "Attempting to open dialog with URL: $videoUrl") // Логируем URL

    val exoPlayer = remember {
        Log.d(TAG, "Initializing ExoPlayer...")
        ExoPlayer.Builder(context).build().apply {
            Log.d(TAG, "Setting media item...")
            Log.d("VIDEO_URL", "Trying to play: $videoUrl")
            setMediaItem(MediaItem.fromUri(videoUrl))
            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    Log.e(TAG, "ExoPlayer error: ${error.message}")
                }
            })
            prepare()
            Log.d(TAG, "ExoPlayer prepared")
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            Log.d(TAG, "Releasing ExoPlayer")
            exoPlayer.release()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Log.d(TAG, "Dialog content compositing")
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Кнопка закрытия (слева)
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .zIndex(1f)
            ) {
                Icon(Icons.Default.Close, "Закрыть", tint = Color.White)
            }

            // Кнопка меню (три точки справа)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .zIndex(1f)
            ) {
                IconButton(
                    onClick = { showDownloadOption = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Меню",
                        tint = Color.White
                    )
                }
            }

            DropdownMenu(
                expanded = showDownloadOption,
                onDismissRequest = { showDownloadOption = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Скачать видео") },
                    onClick = {
                        downloadMediaFile(context, videoUrl)
                        showDownloadOption = false
                    }
                )
            }

            // Плеер
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}