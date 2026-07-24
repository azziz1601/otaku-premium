package com.zenimeku.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import coil.compose.AsyncImage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.net.URLDecoder
import java.net.URLEncoder

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = Color(0xFF121212))) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavigation()
                }
            }
        }
    }
}

object Storage {
    private const val PREFS = "zenimeku_prefs"
    private const val HISTORY_KEY = "history"
    private val gson = Gson()

    fun saveHistory(context: Context, item: AnimeItem) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val json = prefs.getString(HISTORY_KEY, "[]")
        val type = object : TypeToken<MutableList<AnimeItem>>() {}.type
        val history: MutableList<AnimeItem> = gson.fromJson(json, type)
        history.removeAll { it.url == item.url }
        history.add(0, item)
        if (history.size > 20) history.removeLast()
        prefs.edit().putString(HISTORY_KEY, gson.toJson(history)).apply()
    }

    fun getHistory(context: Context): List<AnimeItem> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val json = prefs.getString(HISTORY_KEY, "[]")
        val type = object : TypeToken<List<AnimeItem>>() {}.type
        return gson.fromJson(json, type)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute == "home" || currentRoute == "schedule" || currentRoute == "history") {
                NavigationBar(containerColor = Color(0xFF1E1E1E)) {
                    NavigationBarItem(
                        selected = currentRoute == "home",
                        onClick = {
                            navController.navigate("home") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFFBB86FC), selectedTextColor = Color(0xFFBB86FC), unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray)
                    )
                    NavigationBarItem(
                        selected = currentRoute == "schedule",
                        onClick = {
                            navController.navigate("schedule") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.DateRange, contentDescription = "Jadwal") },
                        label = { Text("Jadwal") },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFFBB86FC), selectedTextColor = Color(0xFFBB86FC), unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray)
                    )
                    NavigationBarItem(
                        selected = currentRoute == "history",
                        onClick = {
                            navController.navigate("history") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.List, contentDescription = "Riwayat") },
                        label = { Text("Riwayat") },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFFBB86FC), selectedTextColor = Color(0xFFBB86FC), unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray)
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            NavHost(navController = navController, startDestination = "home") {
                composable("home") { HomeScreen(navController) }
                composable("schedule") { ScheduleScreen(navController) }
                composable("history") { HistoryScreen(navController) }
                composable("search/{query}") { backStackEntry ->
                    val query = backStackEntry.arguments?.getString("query") ?: ""
                    SearchScreen(navController, query)
                }
                composable("detail/{url}") { backStackEntry ->
                    val url = URLDecoder.decode(backStackEntry.arguments?.getString("url") ?: "", "UTF-8")
                    DetailScreen(navController, url)
                }
                composable("episode/{url}") { backStackEntry ->
                    val url = URLDecoder.decode(backStackEntry.arguments?.getString("url") ?: "", "UTF-8")
                    EpisodeScreen(navController, url)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    var animes by remember { mutableStateOf<List<AnimeItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        if (animes.isEmpty()) {
            animes = Scraper.fetchHome()
            loading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Zenimeku") },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E1E1E), titleContentColor = Color.White)
        )
        
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Cari Anime...") },
                singleLine = true
            )
            IconButton(onClick = {
                if (searchQuery.isNotEmpty()) {
                    navController.navigate("search/$searchQuery")
                }
            }) {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
            }
        }

        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyVerticalGrid(columns = GridCells.Fixed(2), contentPadding = PaddingValues(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(animes) { anime ->
                    AnimeCard(anime) {
                        val encoded = URLEncoder.encode(anime.url, "UTF-8")
                        navController.navigate("detail/$encoded")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(navController: NavController) {
    var schedule by remember { mutableStateOf<List<ScheduleDay>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        if (schedule.isEmpty()) {
            schedule = Scraper.fetchSchedule()
            loading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Jadwal Rilis") },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E1E1E), titleContentColor = Color.White)
        )
        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                items(schedule) { day ->
                    Text(day.day, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFBB86FC), modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                    day.animes.forEach { anime ->
                        Text(
                            text = "- ${anime.title}",
                            color = Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val encoded = URLEncoder.encode(anime.url, "UTF-8")
                                    navController.navigate("detail/$encoded")
                                }
                                .padding(vertical = 4.dp, horizontal = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(navController: NavController) {
    val context = LocalContext.current
    val history = remember { Storage.getHistory(context) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Riwayat Tontonan") },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E1E1E), titleContentColor = Color.White)
        )
        if (history.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Belum ada riwayat.") }
        } else {
            LazyColumn(contentPadding = PaddingValues(8.dp)) {
                items(history) { ep ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(4.dp).clickable {
                            val encoded = URLEncoder.encode(ep.url, "UTF-8")
                            navController.navigate("episode/$encoded")
                        },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Lanjutkan Menonton", color = Color(0xFFBB86FC), fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(ep.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(navController: NavController, query: String) {
    var animes by remember { mutableStateOf<List<AnimeItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(query) {
        animes = Scraper.fetchSearch(query)
        loading = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Hasil: $query") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White) }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E1E1E), titleContentColor = Color.White)
        )

        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (animes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Tidak ditemukan.") }
        } else {
            LazyVerticalGrid(columns = GridCells.Fixed(2), contentPadding = PaddingValues(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(animes) { anime ->
                    AnimeCard(anime) {
                        val encoded = URLEncoder.encode(anime.url, "UTF-8")
                        navController.navigate("detail/$encoded")
                    }
                }
            }
        }
    }
}

@Composable
fun AnimeCard(anime: AnimeItem, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().height(250.dp).clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))) {
        Column {
            AsyncImage(
                model = anime.thumb,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().weight(1f).background(Color.DarkGray)
            )
            Column(modifier = Modifier.padding(8.dp)) {
                Text(anime.title, color = Color.White, maxLines = 2, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                if (anime.ep.isNotEmpty()) {
                    Text(anime.ep, color = Color(0xFFBB86FC), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(navController: NavController, url: String) {
    var detail by remember { mutableStateOf<AnimeDetail?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(url) {
        detail = Scraper.fetchDetail(url)
        loading = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(detail?.title ?: "Loading...", maxLines = 1) },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White) }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E1E1E), titleContentColor = Color.White)
        )

        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (detail == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Gagal memuat detail.") }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        AsyncImage(model = detail!!.thumb, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.height(250.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Genres
                    if (detail!!.genres.isNotEmpty()) {
                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.Center) {
                            detail!!.genres.forEach { genre ->
                                Surface(
                                    color = Color(0xFF333333),
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Text(genre, color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                            }
                        }
                    }

                    // Info
                    if (detail!!.info.isNotEmpty()) {
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Informasi", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFBB86FC), modifier = Modifier.padding(bottom = 8.dp))
                                detail!!.info.forEach { (k, v) ->
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                        Text(k, color = Color.Gray, fontSize = 14.sp, modifier = Modifier.weight(0.3f))
                                        Text(v, color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(0.7f))
                                    }
                                }
                            }
                        }
                    }

                    Text("Sinopsis", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFBB86FC))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(detail!!.sinopsis, fontSize = 14.sp, color = Color(0xFFDDDDDD))
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Daftar Episode", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFBB86FC))
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                items(detail!!.episodes) { ep ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                            val encoded = URLEncoder.encode(ep.url, "UTF-8")
                            navController.navigate("episode/$encoded")
                        },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                    ) {
                        Text(ep.title, modifier = Modifier.padding(16.dp), color = Color.White)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeScreen(navController: NavController, url: String) {
    val context = LocalContext.current
    var episode by remember { mutableStateOf<EpisodeDetail?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(url) {
        episode = Scraper.fetchEpisode(url)
        loading = false
        if (episode != null) {
            Storage.saveHistory(context, AnimeItem(episode!!.title, "", url, ""))
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(episode?.title ?: "Loading...", maxLines = 1) },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White) }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E1E1E), titleContentColor = Color.White)
        )

        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (episode == null || episode!!.streamUrl.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Gagal memuat video.") }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    // Video Player
                    Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f/9f).background(Color.Black)) {
                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.mediaPlaybackRequiresUserGesture = false
                                    settings.useWideViewPort = true
                                    settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                    webChromeClient = WebChromeClient()
                                    webViewClient = WebViewClient()
                                    
                                    android.webkit.CookieManager.getInstance().setAcceptCookie(true)
                                    android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                                    settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36"
                                    
                                    val htmlData = """
                                        <html>
                                        <head>
                                            <meta name="viewport" content="width=device-width, initial-scale=1">
                                            <style>
                                                body { margin: 0; padding: 0; background-color: #000; overflow: hidden; }
                                                iframe { width: 100%; height: 100%; border: none; }
                                            </style>
                                        </head>
                                        <body>
                                            <iframe src="${episode!!.streamUrl}" allowfullscreen="true" webkitallowfullscreen="true" mozallowfullscreen="true"></iframe>
                                        </body>
                                        </html>
                                    """.trimIndent()
                                    loadDataWithBaseURL("https://otakudesu.blog/", htmlData, "text/html", "UTF-8", null)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Navigation Buttons
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Button(
                            onClick = { 
                                episode!!.prevUrl?.let { 
                                    navController.navigate("episode/${URLEncoder.encode(it, "UTF-8")}") { popUpTo("episode/$url") { inclusive = true } }
                                } 
                            },
                            enabled = episode!!.prevUrl != null,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBB86FC))
                        ) { Text("« Prev") }
                        
                        Button(
                            onClick = { 
                                episode!!.allUrl?.let { 
                                    navController.navigate("detail/${URLEncoder.encode(it, "UTF-8")}") { popUpTo("episode/$url") { inclusive = true } }
                                } 
                            },
                            enabled = episode!!.allUrl != null,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                        ) { Text("Semua Eps") }
                        
                        Button(
                            onClick = { 
                                episode!!.nextUrl?.let { 
                                    navController.navigate("episode/${URLEncoder.encode(it, "UTF-8")}") { popUpTo("episode/$url") { inclusive = true } }
                                } 
                            },
                            enabled = episode!!.nextUrl != null,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBB86FC))
                        ) { Text("Next »") }
                    }
                }
            }
        }
    }
}
