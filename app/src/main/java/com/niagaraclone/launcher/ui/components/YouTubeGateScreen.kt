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
    var activeVideoId by remember { mutableStateOf<String?>(null) }
    var activeStartSeconds by remember { mutableStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Intercept back button: if viewing video, return to URL prompt; if at prompt, exit to Still home
    BackHandler {
        if (activeVideoId != null) {
            activeVideoId = null
            errorMessage = null
        } else {
            onDismissRequest()
        }
    }

    val attemptOpenVideo: (String) -> Unit = { rawUrl ->
        val cleanUrl = rawUrl.trim()
        val parsed = parseYouTubeUrl(cleanUrl)
        if (parsed != null) {
            errorMessage = null
            activeVideoId = parsed.first
            activeStartSeconds = parsed.second
            if (settings.youtubeHistoryEnabled) {
                onSaveHistory(
                    YouTubeHistoryItem(
                        id = parsed.first,
                        title = "Video (${parsed.first})",
                        url = cleanUrl
                    )
                )
            }
            focusManager.clearFocus()
        } else {
            errorMessage = "Please enter a valid YouTube video URL or ID"
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

                        // Single URL Field
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = {
                                urlInput = it
                                if (errorMessage != null) errorMessage = null
                            },
                            placeholder = {
                                Text(
                                    text = "https://youtube.com/watch?v=...",
                                    color = TextMuted,
                                    fontSize = 14.sp
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
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
                            imageVector = Icons.Outlined.OpenInNew,
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
                            // Video finished: return to intentional gate rather than letting YouTube autoplay!
                            activeVideoId = null
                        },
                        modifier = Modifier.fillMaxSize()
                    )
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
 * Dedicated Hardware-Accelerated WebView player with zero YouTube algorithmic recommendations.
 * Uses youtube-nocookie.com embedded iframe player API with JavaScript bridge to detect video completion.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun DedicatedYouTubeWebView(
    videoId: String,
    startSeconds: Int = 0,
    onVideoEnded: () -> Unit,
    modifier: Modifier = Modifier
) {
    val embedHtml = remember(videoId, startSeconds) {
        val startParam = if (startSeconds > 0) "&start=$startSeconds" else ""
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
            <style>
                * { margin: 0; padding: 0; box-sizing: border-box; background: #000; }
                html, body { width: 100%; height: 100%; overflow: hidden; }
                #player { width: 100%; height: 100%; border: none; }
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
                        playerVars: {
                            'autoplay': 1,
                            'rel': 0,
                            'modestbranding': 1,
                            'playsinline': 1,
                            'iv_load_policy': 3,
                            'fs': 1,
                            'enablejsapi': 1
                            ${if (startSeconds > 0) ", 'start': $startSeconds" else ""}
                        },
                        events: {
                            'onStateChange': onPlayerStateChange
                        }
                    });
                }

                function onPlayerStateChange(event) {
                    // YT.PlayerState.ENDED = 0
                    if (event.data === 0) {
                        if (window.AndroidBridge && window.AndroidBridge.onVideoEnded) {
                            window.AndroidBridge.onVideoEnded();
                        }
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
                    mediaPlaybackRequiresUserGesture = false
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                }
                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onVideoEnded() {
                        post { onVideoEnded() }
                    }
                }, "AndroidBridge")

                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                        // Prevent navigation outside the embed container to eliminate algorithmic rabbit holes!
                        return true
                    }
                }
                loadDataWithBaseURL("https://www.youtube-nocookie.com", embedHtml, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL("https://www.youtube-nocookie.com", embedHtml, "text/html", "UTF-8", null)
        },
        modifier = modifier
    )
}

/**
 * Robust YouTube video URL parser supporting:
 * - https://www.youtube.com/watch?v=VIDEO_ID
 * - https://youtu.be/VIDEO_ID
 * - https://www.youtube.com/shorts/VIDEO_ID
 * - https://m.youtube.com/watch?v=VIDEO_ID
 * - https://youtube.com/embed/VIDEO_ID
 * - Raw 11-character video ID
 *
 * Extracts (VideoID, StartTimestampInSeconds).
 */
fun parseYouTubeUrl(input: String): Pair<String, Int>? {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return null

    // If user entered raw 11-character video ID
    val rawIdRegex = Regex("^[a-zA-Z0-9_-]{11}\$")
    if (rawIdRegex.matches(trimmed)) {
        return Pair(trimmed, 0)
    }

    val uri = try {
        Uri.parse(trimmed)
    } catch (e: Exception) {
        return null
    }

    val host = uri.host?.lowercase() ?: ""
    var extractedId: String? = null
    var startSeconds = 0

    // Parse timestamp if present (t=120 or t=2m15s or start=120)
    val tParam = uri.getQueryParameter("t") ?: uri.getQueryParameter("start")
    if (!tParam.isNullOrEmpty()) {
        startSeconds = parseTimestampSeconds(tParam)
    }

    // youtu.be/VIDEO_ID
    if (host == "youtu.be") {
        val segment = uri.pathSegments.firstOrNull()
        if (!segment.isNullOrEmpty() && segment.length >= 11) {
            extractedId = segment.substring(0, 11)
        }
    }

    // youtube.com, m.youtube.com, www.youtube.com
    if (host.contains("youtube.com")) {
        // /watch?v=VIDEO_ID
        val vParam = uri.getQueryParameter("v")
        if (!vParam.isNullOrEmpty() && vParam.length >= 11) {
            extractedId = vParam.substring(0, 11)
        }

        if (extractedId == null) {
            val segments = uri.pathSegments
            val shortsIndex = segments.indexOf("shorts")
            if (shortsIndex != -1 && segments.size > shortsIndex + 1) {
                val id = segments[shortsIndex + 1]
                if (id.length >= 11) extractedId = id.substring(0, 11)
            }

            val embedIndex = segments.indexOf("embed")
            if (embedIndex != -1 && segments.size > embedIndex + 1) {
                val id = segments[embedIndex + 1]
                if (id.length >= 11) extractedId = id.substring(0, 11)
            }
        }
    }

    return extractedId?.let { Pair(it, startSeconds) }
}

private fun parseTimestampSeconds(param: String): Int {
    return try {
        if (param.all { it.isDigit() }) {
            param.toInt()
        } else {
            // e.g. 1h20m30s or 2m15s or 45s
            var total = 0
            val hMatch = Regex("(\\d+)h").find(param)
            if (hMatch != null) total += hMatch.groupValues[1].toInt() * 3600
            val mMatch = Regex("(\\d+)m").find(param)
            if (mMatch != null) total += mMatch.groupValues[1].toInt() * 60
            val sMatch = Regex("(\\d+)s").find(param)
            if (sMatch != null) total += sMatch.groupValues[1].toInt()
            total
        }
    } catch (_: Exception) {
        0
    }
}
