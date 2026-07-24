package com.zenimeku.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = Color(0xFF121212))) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    HomeScreen()
                }
            }
        }
    }
}

data class AnimeItem(val title: String, val thumb: String, val ep: String)

@Composable
fun HomeScreen() {
    var animes by remember { mutableStateOf<List<AnimeItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val doc = Jsoup.connect("https://otakudesu.blog").get()
                val items = mutableListOf<AnimeItem>()
                doc.select(".detpost").forEach { el ->
                    val title = el.select(".jdlflm").text()
                    val thumb = el.select("img").attr("src")
                    val ep = el.select(".epz").text()
                    items.add(AnimeItem(title, thumb, ep))
                }
                animes = items
            } catch (e: Exception) {
                e.printStackTrace()
            }
            loading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        Text("Update Terbaru", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(bottom = 16.dp))
        
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(animes) { anime ->
                    Card(modifier = Modifier.fillMaxWidth().height(250.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))) {
                        Column {
                            AsyncImage(
                                model = anime.thumb,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth().weight(1f).background(Color.DarkGray)
                            )
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(anime.title, color = Color.White, maxLines = 2, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text(anime.ep, color = Color(0xFF6200EE), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
