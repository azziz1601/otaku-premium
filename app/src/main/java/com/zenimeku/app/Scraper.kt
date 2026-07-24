package com.zenimeku.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

data class AnimeItem(val title: String, val thumb: String, val url: String, val ep: String = "")
data class AnimeDetail(val title: String, val thumb: String, val sinopsis: String, val episodes: List<EpisodeItem>)
data class EpisodeItem(val title: String, val url: String)
data class EpisodeDetail(val title: String, val streamUrl: String)

object Scraper {
    private const val BASE_URL = "https://otakudesu.blog"

    suspend fun fetchHome(): List<AnimeItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<AnimeItem>()
        try {
            val doc = Jsoup.connect(BASE_URL).get()
            doc.select(".detpost").forEach { el ->
                val title = el.select(".jdlflm").text()
                val thumb = el.select("img").attr("src")
                val ep = el.select(".epz").text()
                val url = el.select(".thumb a").attr("href")
                items.add(AnimeItem(title, thumb, url, ep))
            }
        } catch (e: Exception) { e.printStackTrace() }
        items
    }

    suspend fun fetchSearch(query: String): List<AnimeItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<AnimeItem>()
        try {
            val doc = Jsoup.connect("$BASE_URL/?s=$query&post_type=anime").get()
            doc.select(".chivsrc li").forEach { el ->
                val title = el.select("h2 a").text()
                val url = el.select("h2 a").attr("href")
                val thumb = el.select("img").attr("src")
                items.add(AnimeItem(title, thumb, url, ""))
            }
        } catch (e: Exception) { e.printStackTrace() }
        items
    }

    suspend fun fetchDetail(url: String): AnimeDetail? = withContext(Dispatchers.IO) {
        try {
            val doc = Jsoup.connect(url).get()
            val title = doc.select(".jdlrx h1").text()
            val thumb = doc.select(".fotoanime img").attr("src")
            val sinopsis = doc.select(".sinopc").text()
            
            val eps = mutableListOf<EpisodeItem>()
            doc.select(".episodelist").forEach { epList ->
                if (epList.text().contains("Episode", ignoreCase = true) || epList.text().contains("Batch", ignoreCase = true)) {
                    epList.select("ul li").forEach { el ->
                        val epTitle = el.select("a").text()
                        val epUrl = el.select("a").attr("href")
                        eps.add(EpisodeItem(epTitle, epUrl))
                    }
                }
            }
            AnimeDetail(title, thumb, sinopsis, eps)
        } catch (e: Exception) { 
            e.printStackTrace()
            null
        }
    }

    suspend fun fetchEpisode(url: String): EpisodeDetail? = withContext(Dispatchers.IO) {
        try {
            val doc = Jsoup.connect(url).get()
            val title = doc.select(".venutama h1").text()
            
            // Otakudesu iframe extraction logic
            val iframe = doc.select(".responsive-embed iframe").attr("src")
            val streamUrl = if (iframe.isNotEmpty()) iframe else doc.select("#lightsVideo iframe").attr("src")
            
            EpisodeDetail(title, streamUrl)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
