package com.zenimeku.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

data class AnimeItem(val title: String, val thumb: String, val url: String, val ep: String = "")
data class AnimeDetail(
    val title: String,
    val thumb: String,
    val sinopsis: String,
    val info: Map<String, String>,
    val genres: List<String>,
    val episodes: List<EpisodeItem>
)
data class EpisodeItem(val title: String, val url: String)
data class DownloadLink(val host: String, val url: String)
data class DownloadRes(val resolution: String, val links: List<DownloadLink>)
data class EpisodeDetail(
    val title: String,
    val streamUrl: String,
    val nextUrl: String?,
    val prevUrl: String?,
    val allUrl: String?,
    val downloads: List<DownloadRes>
)
data class ScheduleDay(val day: String, val animes: List<AnimeItem>)

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
            
            var title = doc.select(".infozingle p").firstOrNull { it.text().contains("Judul") }?.text()?.replace("Judul:", "")?.trim()
            if (title.isNullOrEmpty()) title = doc.select(".info h1").text().trim()
            if (title.isNullOrEmpty()) title = doc.select(".jdlrx h1").text().trim()
            
            val thumb = doc.select(".fotoanime img").attr("src")
            val sinopsis = doc.select(".sinopc p").joinToString("\n") { it.text().trim() }
            
            val info = mutableMapOf<String, String>()
            val genres = mutableListOf<String>()
            
            doc.select(".infozingle p").forEach { p ->
                val text = p.text().trim()
                if (text.contains(":")) {
                    val parts = text.split(":", limit = 2)
                    val key = parts[0].trim()
                    val value = parts.getOrNull(1)?.trim() ?: ""
                    if (key.equals("genre", ignoreCase = true)) {
                        p.select("a").forEach { genres.add(it.text().trim()) }
                    } else if (!key.equals("judul", ignoreCase = true)) {
                        info[key] = value
                    }
                }
            }
            
            val eps = mutableListOf<EpisodeItem>()
            doc.select(".episodelist ul li").forEach { el ->
                val epEl = el.select("a").firstOrNull()
                if (epEl != null) {
                    val href = epEl.attr("href")
                    if (href.contains("/episode/")) {
                        eps.add(EpisodeItem(epEl.text().trim(), href))
                    }
                }
            }
            AnimeDetail(title, thumb, sinopsis, info, genres, eps)
        } catch (e: Exception) { 
            e.printStackTrace()
            null
        }
    }

    suspend fun fetchEpisode(url: String): EpisodeDetail? = withContext(Dispatchers.IO) {
        try {
            val doc = Jsoup.connect(url).get()
            val title = doc.select(".venutama h1").text().trim()
            
            val streamUrl = doc.select(".responsive-embed-stream iframe").attr("src").ifEmpty {
                doc.select("#lightsVideo iframe").attr("src")
            }
            
            var next: String? = null
            var prev: String? = null
            var all: String? = null
            doc.select(".flir a").forEach { el ->
                val text = el.text().trim().lowercase()
                val href = el.attr("href")
                if (text.contains("next")) next = href
                if (text.contains("prev")) prev = href
                if (text.contains("all")) all = href
            }
            
            val downloads = mutableListOf<DownloadRes>()
            doc.select(".download ul li").forEach { el ->
                val res = el.select("strong").text().trim()
                val links = mutableListOf<DownloadLink>()
                el.select("a").forEach { a ->
                    links.add(DownloadLink(a.text().trim(), a.attr("href")))
                }
                if (res.isNotEmpty() && links.isNotEmpty()) {
                    downloads.add(DownloadRes(res, links))
                }
            }
            
            EpisodeDetail(title, streamUrl, next, prev, all, downloads)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun fetchSchedule(): List<ScheduleDay> = withContext(Dispatchers.IO) {
        val schedules = mutableListOf<ScheduleDay>()
        try {
            val doc = Jsoup.connect("$BASE_URL/jadwal-rilis/").get()
            doc.select(".kgjdwl321").forEach { el ->
                val day = el.select("h2").text().trim()
                val animes = mutableListOf<AnimeItem>()
                el.select("ul li a").forEach { a ->
                    animes.add(AnimeItem(a.text().trim(), "", a.attr("href"), ""))
                }
                if (day.isNotEmpty()) schedules.add(ScheduleDay(day, animes))
            }
        } catch (e: Exception) { e.printStackTrace() }
        schedules
    }
}
