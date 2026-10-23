package com.niagaraclone.launcher.ui.components

import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.niagaraclone.launcher.R
import com.niagaraclone.launcher.data.model.LauncherSettings
import com.niagaraclone.launcher.data.model.YouTubeHistoryItem
import com.niagaraclone.launcher.ui.theme.TextMuted
import com.niagaraclone.launcher.ui.theme.TextPrimary
import com.niagaraclone.launcher.ui.theme.TextSecondary

/**
 * Built-in Intentional YouTube Link Gate for Still — Minimal Launcher.
 * Enforces deliberate, single-video viewing without algorithmic feeds, recommendations, or rabbit holes.
 */
@Composable
fun YouTubeGateScreen(
    settings: LauncherSettings,
    historyList: List<YouTubeHistoryItem> = emptyList(),
    initialVideoId: String? = null,
    initialStartSeconds: Int = 0,
    onSaveHistory: (YouTubeHistoryItem) -> Unit = {},
    onRemoveHistoryItem: (String) -> Unit = {},
    onClearHistory: () -> Unit = {},
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val themeAccent = Color(settings.activeAccentHex)
    val themeAccentLight = Color(settings.activeAccentLightHex)

    var urlInput by remember { mutableStateOf("") }
    var activeVideoId by remember { mutableStateOf<String?>(initialVideoId) }
    var activeStartSeconds by remember { mutableStateOf(initialStartSeconds) }
    var hasPlaybackError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(initialVideoId, initialStartSeconds) {
        if (!initialVideoId.isNullOrBlank()) {
            activeVideoId = initialVideoId
            activeStartSeconds = initialStartSeconds
            hasPlaybackError = false
            errorMessage = null
        }
    }

    // Intercept back button: if viewing video, return to URL prompt; if at prompt, exit to Still home
    BackHandler {
        if (activeVideoId != null) {
            activeVideoId = null
            hasPlaybackError = false
            errorMessage = null
        } else {
            onDismissRequest()
        }
    }

    val attemptOpenVideo: (String) -> Unit = { rawUrl ->
        val cleanUrl = rawUrl.trim()
        val parsed = parseYouTubeInput(cleanUrl)
        when (parsed) {
            is YouTubeParseResult.DirectVideo -> {
                errorMessage = null
                hasPlaybackError = false
                activeVideoId = parsed.videoId
                activeStartSeconds = parsed.startSeconds
                if (settings.youtubeHistoryEnabled) {
                    onSaveHistory(
                        YouTubeHistoryItem(
                            id = parsed.videoId,
                            title = "Video (${parsed.videoId})",
                            url = cleanUrl
                        )
                    )
                }
                focusManager.clearFocus()
            }
            is YouTubeParseResult.SearchQuery -> {
                errorMessage = null
                try {
                    val searchIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(parsed.query))
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(searchIntent)
                } catch (_: Exception) {
                    errorMessage = "Could not open search"
                }
            }
            null -> {
                errorMessage = "Please enter a valid YouTube link, ID, or video title"
            }
        }
    }

    val pasteFromClipboard = {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = clipboard.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0).text?.toString()?.trim() ?: ""
                urlInput = text
                if (text.isNotEmpty()) {
                    attemptOpenVideo(text)
                }
            } else {
                errorMessage = "Clipboard is empty"
            }
        } catch (e: Exception) {
            errorMessage = "Could not read clipboard"
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (activeVideoId == null) {
            // ==========================================
            // MODE 1: INTENTIONAL LINK ENTRY GATE
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                // Top App Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismissRequest) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Still",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "Intentional YouTube",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.size(48.dp)) // balance balance
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        Spacer(modifier = Modifier.height(20.dp))

                        // Calm Icon
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0x1AFFFFFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PlayArrow,
                                contentDescription = null,
                                tint = themeAccentLight,
                                modifier = Modifier.size(34.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "What do you want to watch?",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            ),
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Choose your video deliberately. Still opens only your selected link with zero recommendations or algorithmic rabbit holes.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            ),
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        // Single URL / Search Field
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = {
                                urlInput = it
                                if (errorMessage != null) errorMessage = null
                            },
                            placeholder = {
                                Text(
                                    text = "Paste link, ChatGPT text, or video title…",
                                    color = TextMuted,
                                    fontSize = 13.sp
                                )
                            },
                            singleLine = false,
                            maxLines = 3,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Go
                            ),
                            keyboardActions = KeyboardActions(
                                onGo = { attemptOpenVideo(urlInput) }
                            ),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = themeAccentLight,
                                unfocusedBorderColor = Color(0x33FFFFFF),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = Color(0x14FFFFFF),
                                unfocusedContainerColor = Color(0x0AFFFFFF)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = errorMessage!!,
                                color = Color(0xFFFF6B6B),
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Action Buttons: Paste Link & Open Video
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = pasteFromClipboard,
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentPaste,
                                    contentDescription = null,
                                    tint = themeAccentLight,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Paste Link",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }

                            Button(
                                onClick = { attemptOpenVideo(urlInput) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = themeAccent),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Open Video",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        TextButton(
                            onClick = onDismissRequest,
                            modifier = Modifier.height(40.dp)
                        ) {
                            Text(
                                text = "Cancel",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Optional Local History Section
                    if (settings.youtubeHistoryEnabled && historyList.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            HorizontalDivider(color = Color(0x22FFFFFF))
                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Recent Videos (Local History)",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    ),
                                    color = TextSecondary
                                )
                                TextButton(onClick = onClearHistory) {
                                    Text(
                                        text = "Clear All",
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        items(historyList) { item ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0x0EFFFFFF),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { attemptOpenVideo(item.url.ifEmpty { "https://youtu.be/${item.id}" }) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.VideoLibrary,
                                        contentDescription = null,
                                        tint = themeAccentLight,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { onRemoveHistoryItem(item.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = TextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        } else {
            // ==========================================
            // MODE 2: DEDICATED DISTRACTION-FREE VIEWER
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Top App Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            activeVideoId = null
                            errorMessage = null
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to URL Prompt",
                            tint = Color.White
                        )
                    }

                    Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                        Text(
                            text = "Intentional Viewer",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Distraction-Free Playback",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = TextMuted
                        )
                    }

                    // Open external intent button
                    IconButton(
                        onClick = {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://www.youtube.com/watch?v=$activeVideoId")
                            ).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                            contentDescription = "Open in External App",
                            tint = themeAccentLight,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Return to Still home button
                    IconButton(onClick = onDismissRequest) {
                        Icon(
                            imageVector = Icons.Outlined.Home,
                            contentDescription = "Return to Still",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Dedicated Embedded Player Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color.Black)
                ) {
                    DedicatedYouTubeWebView(
                        videoId = activeVideoId!!,
                        startSeconds = activeStartSeconds,
                        onVideoEnded = {
                            activeVideoId = null
                        },
                        onVideoError = {
                            hasPlaybackError = true
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    if (hasPlaybackError) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xF0000000))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Playback Restricted by Creator",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "The owner disabled third-party embedding for this video.",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        val intent = Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("https://www.youtube.com/watch?v=$activeVideoId")
                                        ).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = Color.Black
                                    ),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text("Open in External Player", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Bottom mindful session summary & actions
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0x1AFFFFFF),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Mindful Viewing Session Active",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                ),
                                color = themeAccentLight
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Autoplay and algorithmic recommendations are strictly blocked. When this video ends, you will return to Still.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                activeVideoId = null
                                urlInput = ""
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Enter New Link",
                                color = TextPrimary,
                                fontSize = 12.sp
                            )
                        }

                        Button(
                            onClick = onDismissRequest,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = themeAccent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Return to Still",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Result of parsing YouTube input: either a direct video or a search query.
 */
sealed class YouTubeParseResult {
    data class DirectVideo(val videoId: String, val startSeconds: Int = 0) : YouTubeParseResult()
    data class SearchQuery(val query: String) : YouTubeParseResult()
}

/**
 * Dedicated Hardware-Accelerated WebView player with zero YouTube algorithmic recommendations.
 * Uses official YouTube embedded player with Origin and base URL configuration.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun DedicatedYouTubeWebView(
    videoId: String,
    startSeconds: Int = 0,
    onVideoEnded: () -> Unit,
    onVideoError: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val embedHtml = remember(videoId, startSeconds) {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <style>
                * { margin: 0; padding: 0; box-sizing: border-box; background: #000; }
                html, body { width: 100%; height: 100%; overflow: hidden; background: #000; }
                #player { width: 100%; height: 100%; position: absolute; top: 0; left: 0; border: none; }
            </style>
        </head>
        <body>
            <div id="player"></div>
            <script>
                var tag = document.createElement('script');
                tag.src = "https://www.youtube.com/iframe_api";
                var firstScriptTag = document.getElementsByTagName('script')[0];
                firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

                var player;
                function onYouTubeIframeAPIReady() {
                    player = new YT.Player('player', {
                        videoId: '$videoId',
                        width: '100%',
                        height: '100%',
                        playerVars: {
                            'autoplay': 1,
                            'playsinline': 1,
                            'rel': 0,
                            'modestbranding': 1,
                            'iv_load_policy': 3,
                            'fs': 1,
                            'enablejsapi': 1,
                            'origin': 'https://www.youtube.com'
                            ${if (startSeconds > 0) ", 'start': $startSeconds" else ""}
                        },
                        events: {
                            'onStateChange': onPlayerStateChange,
                            'onError': onPlayerError,
                            'onReady': onPlayerReady
                        }
                    });
                }

                function onPlayerReady(event) {
                    try { event.target.playVideo(); } catch(e) {}
                }

                function onPlayerStateChange(event) {
                    // YT.PlayerState.ENDED = 0
                    if (event.data === 0) {
                        if (window.AndroidBridge && window.AndroidBridge.onVideoEnded) {
                            window.AndroidBridge.onVideoEnded();
                        }
                    }
                }

                function onPlayerError(event) {
                    if (window.AndroidBridge && window.AndroidBridge.onVideoError) {
                        window.AndroidBridge.onVideoError(event.data);
                    }
                }
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(0xFF000000.toInt())
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                    userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                }
                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onVideoEnded() {
                        post { onVideoEnded() }
                    }

                    @JavascriptInterface
                    fun onVideoError(errorCode: Int) {
                        post { onVideoError(errorCode) }
                    }
                }, "AndroidBridge")

                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                        val url = request?.url?.toString() ?: ""
                        if (request?.isForMainFrame == true) {
                            if (!url.contains("youtube.com/embed") && !url.contains("youtube-nocookie.com/embed")) {
                                return true
                            }
                        }
                        return false
                    }

                    @Deprecated("Deprecated in Java")
                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                        return false
                    }
                }
                tag = "$videoId:$startSeconds"
                loadDataWithBaseURL("https://www.youtube.com", embedHtml, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            val expectedTag = "$videoId:$startSeconds"
            if (webView.tag != expectedTag) {
                webView.tag = expectedTag
                webView.loadDataWithBaseURL("https://www.youtube.com", embedHtml, "text/html", "UTF-8", null)
            }
        },
        modifier = modifier
    )
}

/**
 * Robust YouTube video URL parser and extractor.
 * Supports ALL sources including:
 * - ChatGPT markdown links: [Title](https://...)
 * - Backtick code blocks: `https://...`
 * - HTML embeds: <iframe src="https://...">
 * - Redirect wrappers: Google, ChatGPT, Reddit, Facebook, Attribution
 * - Shorts, Live streams, Music, Kids, Embeds, Schemeless
 * - Trailing punctuation: ., ,, ), !, ?, *, _, quotes
 * - Direct 11-char ID
 * - Timestamps in query (t=, start=, time_continue=) or fragment (#t=)
 */
fun parseYouTubeInput(input: String?): YouTubeParseResult? {
    if (input.isNullOrBlank()) return null
    var text = input.trim()

    // 1. Raw 11-character video ID directly
    val rawIdRegex = Regex("^[a-zA-Z0-9_-]{11}\$")
    if (rawIdRegex.matches(text)) {
        return YouTubeParseResult.DirectVideo(text, 0)
    }

    // 2. Decode redirects (Google, ChatGPT, Reddit, Facebook, Attribution)
    text = unwrapRedirects(text)

    // 3. Extract URL from Markdown link [Title](https://...)
    val markdownLinkMatch = Regex("\\[([^\\]]+)\\]\\((https?://[^\\s)]+)\\)").find(text)
    if (markdownLinkMatch != null) {
        text = markdownLinkMatch.groupValues[2]
    }

    // 4. Extract iframe embed src="https://..."
    val iframeMatch = Regex("<iframe[^>]+src=[\"'](https?://[^\"']+)[\"']", RegexOption.IGNORE_CASE).find(text)
    if (iframeMatch != null) {
        text = iframeMatch.groupValues[1]
    }

    // 5. Strip surrounding markdown/quotes/brackets/backticks from edges
    text = text.trim('`', '"', '\'', '<', '>', '(', ')', '[', ']', '{', '}', '*', '_', '~')

    // 6. Check if it's a YouTube search results URL
    val searchMatch = Regex("(?:youtube\\.com/results\\?.*search_query=)([^&\\s\"'>#]+)").find(text)
    if (searchMatch != null) {
        val rawQuery = searchMatch.groupValues[1]
        val decoded = try { java.net.URLDecoder.decode(rawQuery, "UTF-8") } catch (_: Exception) { rawQuery }
        return YouTubeParseResult.SearchQuery(decoded)
    }

    // 7. Extract Start Timestamp (if any)
    val timeMatch = Regex("[?&#](?:t|start|time_continue)=([0-9hms]+)").find(text)
    val startSeconds = if (timeMatch != null) parseTimestampSeconds(timeMatch.groupValues[1]) else 0

    // 8. Universal Video ID Extraction with lookahead to ignore trailing punctuation
    val videoIdPatterns = listOf(
        Regex("[?&]v=([a-zA-Z0-9_-]{11})(?=[^a-zA-Z0-9_-]|\$)"),
        Regex("youtu\\.be/([a-zA-Z0-9_-]{11})(?=[^a-zA-Z0-9_-]|\$)"),
        Regex("/(?:shorts|live|embed|v|e|w|watch)/([a-zA-Z0-9_-]{11})(?=[^a-zA-Z0-9_-]|\$)")
    )

    for (pattern in videoIdPatterns) {
        val match = pattern.find(text)
        if (match != null) {
            val candidate = match.groupValues[1]
            if (rawIdRegex.matches(candidate)) {
                return YouTubeParseResult.DirectVideo(candidate, startSeconds)
            }
        }
    }

    // 9. Android Uri fallback
    try {
        val norm = if (!text.startsWith("http://", ignoreCase = true) && !text.startsWith("https://", ignoreCase = true)) "https://$text" else text
        val uri = Uri.parse(norm)
        if (uri != null) {
            val vParam = uri.getQueryParameter("v")
            if (vParam != null && vParam.length >= 11) {
                val candidate = vParam.substring(0, 11)
                if (rawIdRegex.matches(candidate)) {
                    return YouTubeParseResult.DirectVideo(candidate, startSeconds)
                }
            }
            val segments = uri.pathSegments
            val targetIndices = listOf("shorts", "live", "embed", "v", "e", "watch", "w")
            for (target in targetIndices) {
                val idx = segments.indexOf(target)
                if (idx != -1 && segments.size > idx + 1) {
                    val seg = segments[idx + 1]
                    if (seg.length >= 11) {
                        val candidate = seg.substring(0, 11)
                        if (rawIdRegex.matches(candidate)) {
                            return YouTubeParseResult.DirectVideo(candidate, startSeconds)
                        }
                    }
                }
            }
        }
    } catch (_: Throwable) {}

    // 10. If user typed plain text (e.g. video title from ChatGPT like "Huberman Lab Sleep"):
    if (text.length in 2..120 && !text.contains("http://") && !text.contains("https://")) {
        return YouTubeParseResult.SearchQuery(text)
    }

    return null
}

fun parseYouTubeUrl(input: String?): Pair<String, Int>? {
    val result = parseYouTubeInput(input)
    return when (result) {
        is YouTubeParseResult.DirectVideo -> Pair(result.videoId, result.startSeconds)
        else -> null
    }
}

private fun unwrapRedirects(input: String): String {
    var text = input
    // Google redirect: q=https://...
    val googleMatch = Regex("[?&]q=(https?%3A%2F%2F[^&]+|https?://[^&]+)").find(text)
    if (googleMatch != null) {
        val rawTarget = googleMatch.groupValues[1]
        text = try { java.net.URLDecoder.decode(rawTarget, "UTF-8") } catch (_: Exception) { rawTarget }
    }
    // Generic redirect: url=https://... or u=https://... or redirect=https://...
    val genericMatch = Regex("[?&](?:url|u|redirect|target)=(https?%3A%2F%2F[^&]+|https?://[^&]+|%2Fwatch[^&]+)").find(text)
    if (genericMatch != null) {
        val rawTarget = genericMatch.groupValues[1]
        val decoded = try { java.net.URLDecoder.decode(rawTarget, "UTF-8") } catch (_: Exception) { rawTarget }
        text = if (decoded.startsWith("/")) "https://www.youtube.com$decoded" else decoded
    }
    return text
}

fun parseTimestampSeconds(param: String): Int {
    val clean = param.trim().lowercase().removePrefix("t=").removePrefix("start=")
    if (clean.isEmpty()) return 0
    return try {
        val pureSecondsMatch = Regex("^(\\d+)s?\$").find(clean)
        if (pureSecondsMatch != null) {
            pureSecondsMatch.groupValues[1].toInt()
        } else {
            var total = 0
            val hMatch = Regex("(\\d+)h").find(clean)
            if (hMatch != null) total += hMatch.groupValues[1].toInt() * 3600
            val mMatch = Regex("(\\d+)m").find(clean)
            if (mMatch != null) total += mMatch.groupValues[1].toInt() * 60
            val sMatch = Regex("(\\d+)s").find(clean)
            if (sMatch != null) total += sMatch.groupValues[1].toInt()
            total
        }
    } catch (_: Exception) {
        0
    }
}
