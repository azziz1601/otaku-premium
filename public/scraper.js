const BASE_URL = 'https://otakudesu.blog';

const htmlCache = new Map();
const CACHE_TTL = 10 * 60 * 1000; 

async function fetchHTML(url) {
  if (htmlCache.has(url)) {
    const cached = htmlCache.get(url);
    if (Date.now() - cached.timestamp < CACHE_TTL) {
      return cached.data;
    }
    htmlCache.delete(url);
  }

  const response = await Capacitor.Plugins.CapacitorHttp.request({
    url,
    method: 'GET',
    headers: { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)' }
  });
  
  htmlCache.set(url, { data: response.data, timestamp: Date.now() });
  return response.data;
}

function parseHTML(html) {
  const parser = new DOMParser();
  return parser.parseFromString(html, 'text/html');
}

window.scraper = {
  fetchAnimeList: async (page = 1) => {
    const html = await fetchHTML(`${BASE_URL}/page/${page}`);
    const doc = parseHTML(html);
    const animeItems = [];
    doc.querySelectorAll('.detpost').forEach(elem => {
      const title = elem.querySelector('.jdlflm')?.textContent?.trim() || '';
      const detailUrl = elem.querySelector('a')?.getAttribute('href') || '';
      const thumbnail = elem.querySelector('img')?.getAttribute('src') || '';
      const ratingOrDay = elem.querySelector('.epztipe')?.textContent?.trim() || '';
      const episode = elem.querySelector('.epz')?.textContent?.trim() || '';
      const date = elem.querySelector('.newnime')?.textContent?.trim() || '';
      animeItems.push({ title, thumbnail, rating: ratingOrDay, episode, date, detailUrl });
    });
    return animeItems;
  },
  fetchSearch: async (query) => {
    const html = await fetchHTML(`${BASE_URL}/?s=${encodeURIComponent(query)}&post_type=anime`);
    const doc = parseHTML(html);
    const animeItems = [];
    doc.querySelectorAll('.chivsrc li').forEach(elem => {
      animeItems.push({
        title: elem.querySelector('h2 a')?.textContent?.trim() || '',
        detailUrl: elem.querySelector('h2 a')?.getAttribute('href') || '',
        thumbnail: elem.querySelector('img')?.getAttribute('src') || '',
        rating: (elem.querySelector('.set')?.textContent || '').replace('Rating : ', '').trim() || 'N/A'
      });
    });
    return animeItems;
  },
  fetchGenreList: async () => {
    const html = await fetchHTML(`${BASE_URL}/genre-list/`);
    const doc = parseHTML(html);
    const genres = [];
    doc.querySelectorAll('a[href^="/genres/"]').forEach(elem => {
      genres.push({
        title: elem.textContent?.trim(),
        url: elem.getAttribute('href')
      });
    });
    const uniqueGenres = [];
    const map = new Map();
    for (const item of genres) {
      if(!map.has(item.title) && item.title) {
          map.set(item.title, true);
          uniqueGenres.push(item);
      }
    }
    return uniqueGenres;
  },
  fetchAnimeDetail: async (detailUrl) => {
    const url = detailUrl.startsWith('http') ? detailUrl : `${BASE_URL}${detailUrl}`;
    const html = await fetchHTML(url);
    const doc = parseHTML(html);
    
    let titleText = doc.querySelector('.infozingle p')?.textContent || '';
    let title = titleText.replace('Judul:', '').trim() || doc.querySelector('.info h1')?.textContent?.trim();
    const thumbnail = doc.querySelector('.fotoanime img')?.getAttribute('src') || '';
    const sinopsis = Array.from(doc.querySelectorAll('.sinopc p')).map(p => p.textContent?.trim()).join('\n');
    
    const infoObj = {};
    doc.querySelectorAll('.infozingle p').forEach(elem => {
      const text = elem.textContent?.trim() || '';
      if (text.includes(':')) {
        const [key, ...val] = text.split(':');
        const cleanKey = key.trim().toLowerCase();
        if (cleanKey !== 'genre' && cleanKey !== 'judul') {
          infoObj[key.trim()] = val.join(':').trim();
        }
      }
    });

    const genre = [];
    doc.querySelectorAll('.infozingle p').forEach(p => {
       if (p.textContent.includes('Genre')) {
           p.querySelectorAll('a').forEach(a => genre.push(a.textContent.trim()));
       }
    });
    
    const episodes = [];
    doc.querySelectorAll('.episodelist ul li').forEach(elem => {
      const epEl = elem.querySelector('a');
      if (epEl) {
        const href = epEl.getAttribute('href');
        if (epEl.textContent && href && href.includes('/episode/')) {
          episodes.push({
            episode: epEl.textContent.trim(),
            url: href
          });
        }
      }
    });
    return { title, thumbnail, sinopsis, genre, episodes, info: infoObj };
  },
  fetchEpisodeDetail: async (epUrl) => {
    const url = epUrl.startsWith('http') ? epUrl : `${BASE_URL}${epUrl}`;
    const html = await fetchHTML(url);
    const doc = parseHTML(html);
    
    const title = doc.querySelector('.venutama h1')?.textContent?.trim();
    const stream = doc.querySelector('.responsive-embed-stream iframe')?.getAttribute('src') || doc.querySelector('#lightsVideo iframe')?.getAttribute('src');
    
    let next = null, prev = null, all = null;
    doc.querySelectorAll('.flir a').forEach(elem => {
      const text = elem.textContent?.trim().toLowerCase() || '';
      const href = elem.getAttribute('href');
      if (text.includes('next')) next = href;
      if (text.includes('prev')) prev = href;
      if (text.includes('all')) all = href;
    });

    const downloads = [];
    doc.querySelectorAll('.download ul li').forEach(elem => {
      const res = elem.querySelector('strong')?.textContent?.trim();
      const links = [];
      elem.querySelectorAll('a').forEach(aEl => {
        links.push({
          host: aEl.textContent?.trim(),
          url: aEl.getAttribute('href')
        });
      });
      if(res && links.length > 0) {
        downloads.push({ resolution: res, links });
      }
    });

    return { title, stream, next, prev, all, downloads };
  },
  fetchSchedule: async () => {
    const html = await fetchHTML(`${BASE_URL}/jadwal-rilis/`);
    const doc = parseHTML(html);
    const schedules = [];
    doc.querySelectorAll('.kgjdwl321').forEach(elem => {
      const day = elem.querySelector('h2')?.textContent?.trim();
      const animes = [];
      elem.querySelectorAll('ul li a').forEach(aEl => {
        animes.push({
          title: aEl.textContent?.trim(),
          detailUrl: aEl.getAttribute('href')
        });
      });
      if (day) schedules.push({ day, animes });
    });
    return schedules;
  },
  fetchAnimeByGenre: async (genreUrl, page = 1) => {
    const url = genreUrl.startsWith('http') ? `${genreUrl}page/${page}/` : `${BASE_URL}${genreUrl}page/${page}/`;
    const html = await fetchHTML(url);
    const doc = parseHTML(html);
    const animeItems = [];
    doc.querySelectorAll('.col-anime').forEach(elem => {
      animeItems.push({
        title: elem.querySelector('.col-anime-title a')?.textContent?.trim() || '',
        detailUrl: elem.querySelector('.col-anime-title a')?.getAttribute('href') || '',
        thumbnail: elem.querySelector('.col-anime-cover img')?.getAttribute('src') || '',
        rating: elem.querySelector('.col-anime-rating')?.textContent?.trim() || '',
        episode: elem.querySelector('.col-anime-eps')?.textContent?.trim() || ''
      });
    });
    return animeItems;
  }
};
