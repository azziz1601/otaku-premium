document.addEventListener('DOMContentLoaded', async () => {
  try {
    const cfgRes = await fetch('https://raw.githubusercontent.com/azziz1601/otaku-premium/main/public/config.json?t=' + Date.now());
    const cfg = await cfgRes.json();
    window.API_URL = cfg.api_url;
  } catch(e) {
    window.API_URL = 'https://40aa299a111d7a.lhr.life';
  }

  const loader = document.getElementById('loader');
  const contentArea = document.getElementById('content-area');
  const searchInput = document.getElementById('search-input');
  const searchBtn = document.getElementById('search-btn');
  const genresContainer = document.getElementById('genres-container');
  const liveSearchResults = document.getElementById('live-search-results');

  // --- LOCAL STORAGE HELPERS ---
  function getHistory() { return JSON.parse(localStorage.getItem('anime_history') || '[]'); }
  function saveHistory(ep) {
    let h = getHistory();
    h = h.filter(x => x.url !== ep.url);
    h.unshift(ep);
    if(h.length > 20) h.pop();
    localStorage.setItem('anime_history', JSON.stringify(h));
  }
  function getBookmarks() { return JSON.parse(localStorage.getItem('anime_bookmarks') || '[]'); }
  function toggleBookmark(anime) {
    let b = getBookmarks();
    const exists = b.find(x => x.url === anime.url);
    if(exists) { b = b.filter(x => x.url !== anime.url); } 
    else { b.push(anime); }
    localStorage.setItem('anime_bookmarks', JSON.stringify(b));
  }
  function isBookmarked(url) { return !!getBookmarks().find(x => x.url === url); }

  // --- SKELETON LOADER ---
  function getSkeletonGrid() {
    return '<div class="grid">' + Array(12).fill().map(() => `
      <div class="card" style="box-shadow:none;background:transparent;pointer-events:none">
        <div class="skeleton-card skeleton"></div>
        <div class="info">
          <div class="skeleton-text skeleton"></div>
          <div class="skeleton-text skeleton" style="width:50%"></div>
        </div>
      </div>
    `).join('') + '</div>';
  }

  // --- LOAD GENRES ---
  async function loadGenres() {
    try {
      const res = await fetch(`${window.API_URL}/api/genres`);
      const json = await res.json();
      if (json.success) {
        genresContainer.innerHTML = json.data.map(g => 
          `<a href="#genre?url=${encodeURIComponent(g.url)}&title=${encodeURIComponent(g.title)}" class="genre-tag">${g.title}</a>`
        ).join('');
      }
    } catch (e) { console.error(e); }
  }

  function createCardHTML(anime) {
    const ratingText = isNaN(parseFloat(anime.rating)) ? `Rilis: ${anime.rating}` : `Rating: ${anime.rating}`;
    return `
      <a href="#detail?url=${encodeURIComponent(anime.detailUrl)}" class="card">
        <img src="${anime.thumbnail || 'https://via.placeholder.com/200x300?text=No+Image'}" alt="${anime.title}">
        <div class="info">
          <h3>${anime.title}</h3>
          ${anime.episode ? `<div style="color:var(--accent);font-size:0.85rem;margin-bottom:0.2rem">${anime.episode}</div>` : ''}
          <div class="rating">${ratingText}</div>
          ${anime.date ? `<div style="font-size:0.8rem;opacity:0.7;margin-top:0.2rem">${anime.date}</div>` : ''}
        </div>
      </a>
    `;
  }

  // --- LOAD HOME ---
  async function loadHome(page = 1) {
    loader.style.display = 'none';
    if(page === 1) { window.scrollTo(0,0); contentArea.innerHTML = '<h2 style="text-align:center;margin:2rem 0">Update Terbaru</h2>' + getSkeletonGrid(); }
    try {
      const res = await fetch(`${window.API_URL}/api/anime?page=${page}`);
      const json = await res.json();
      if (json.success) {
        let html = json.data.map(createCardHTML).join('');
        if(page === 1) {
          contentArea.innerHTML = `<h2 style="text-align:center;margin:2rem 0">Update Terbaru</h2><div class="grid" id="home-grid">${html}</div>`;
        } else {
          document.getElementById('home-grid').innerHTML += html;
        }
        const existingBtn = document.getElementById('load-more-btn');
        if(existingBtn) existingBtn.remove();
        contentArea.innerHTML += `<div style="text-align:center"><button id="load-more-btn" class="action-btn">Muat Lebih Banyak</button></div>`;
        document.getElementById('load-more-btn').addEventListener('click', () => loadHome(page + 1));
      }
    } catch (e) {
      if(page === 1) contentArea.innerHTML = `<p style="text-align:center">Error: ${e.message}</p>`;
    }
  }

  // --- LOAD SEARCH ---
  async function loadSearch(query, page = 1) {
    loader.style.display = 'none';
    if(page === 1) { window.scrollTo(0,0); contentArea.innerHTML = `<h2 style="text-align:center;margin:2rem 0">Hasil Pencarian: ${query}</h2>` + getSkeletonGrid(); }
    try {
      const res = await fetch(`${window.API_URL}/api/search?q=${encodeURIComponent(query)}&page=${page}`); // Note: Backend may not support search pagination out of the box, but we'll try
      const json = await res.json();
      if (json.success) {
        let html = json.data.map(createCardHTML).join('');
        if (json.data.length === 0 && page === 1) {
          contentArea.innerHTML = `<h2 style="text-align:center;margin:2rem 0">Hasil Pencarian: ${query}</h2><p style="text-align:center">Tidak ada hasil.</p>`;
        } else {
          if(page === 1) {
            contentArea.innerHTML = `<h2 style="text-align:center;margin:2rem 0">Hasil Pencarian: ${query}</h2><div class="grid" id="search-grid">${html}</div>`;
          } else {
            document.getElementById('search-grid').innerHTML += html;
          }
          if (json.data.length >= 10) { // arbitrary guess for pagination
            const existingBtn = document.getElementById('load-more-btn');
            if(existingBtn) existingBtn.remove();
            contentArea.innerHTML += `<div style="text-align:center"><button id="load-more-btn" class="action-btn">Muat Lebih Banyak</button></div>`;
            document.getElementById('load-more-btn').addEventListener('click', () => loadSearch(query, page + 1));
          }
        }
      }
    } catch (e) { if(page === 1) contentArea.innerHTML = `<p style="text-align:center">Error: ${e.message}</p>`; }
  }

  // --- LOAD DETAIL ---
  async function loadDetail(url) {
    loader.style.display = 'none';
    window.scrollTo(0, 0);
    contentArea.innerHTML = `<div style="text-align:center;margin:4rem 0">${getSkeletonGrid().split('</div>')[0]}</div>`; // fake loader
    
    try {
      const res = await fetch(`${window.API_URL}/api/detail?url=${encodeURIComponent(url)}`);
      const json = await res.json();
      if (json.success) {
        const d = json.data;
        const epsHTML = d.episodes.map(ep => {
          let label = ep.episode;
          const seasonMatch = label.match(/Season\s+\d+/i);
          const episodeMatch = label.match(/Episode\s+[\d\.]+/i);
          const endMatch = label.match(/\(End\)/i);
          let shortLabel = [];
          if (seasonMatch) shortLabel.push(seasonMatch[0]);
          if (episodeMatch) shortLabel.push(episodeMatch[0]);
          if (endMatch) shortLabel.push(endMatch[0]);
          if (shortLabel.length > 0) label = shortLabel.join(' ');
          return `<a href="#episode?url=${encodeURIComponent(ep.url)}" class="ep-link">${label}</a>`;
        }).join('');
        
        contentArea.innerHTML = `
          <div class="detail-container">
            <div class="detail-header" style="display:flex;flex-direction:column;align-items:center;text-align:center;margin-bottom:2rem">
              ${d.thumbnail ? `<img src="${d.thumbnail}" alt="${d.title}" style="max-width:300px;border-radius:12px;margin-bottom:1rem;box-shadow:0 8px 16px rgba(0,0,0,0.5)">` : ''}
              <h2 style="font-size:1.8rem;margin-bottom:0.5rem">${d.title}</h2>
              <div>
                ${d.genre.map(g => `<span class="genre-tag" style="display:inline-block;margin:0.2rem">${g}</span>`).join('')}
              </div>
              <button id="bookmark-btn" class="action-btn" style="margin-top:1rem;width:100%;max-width:300px" 
                data-url="${url}" data-title="${d.title}" data-thumb="${d.thumbnail}">
                ${isBookmarked(url) ? 'Hapus Bookmark ❌' : 'Tambah Bookmark ❤️'}
              </button>
            </div>
            
            ${d.info && Object.keys(d.info).length > 0 ? `
            <div class="detail-info" style="margin-bottom:2rem;background:var(--card-bg);padding:1.5rem;border-radius:12px;box-shadow:0 4px 12px rgba(0,0,0,0.3)">
              <h3 style="margin-bottom:1rem;border-bottom:2px solid var(--accent);display:inline-block;padding-bottom:0.3rem">Informasi</h3>
              <div style="display:grid;grid-template-columns:repeat(auto-fill, minmax(200px, 1fr));gap:1rem;font-size:0.9rem">
                ${Object.entries(d.info).map(([k, v]) => `
                  <div style="background:rgba(255,255,255,0.02);padding:0.8rem;border-radius:8px;border-left:3px solid var(--accent)">
                    <strong style="color:var(--accent-hover);display:block;margin-bottom:0.2rem;font-size:0.8rem;text-transform:uppercase;letter-spacing:1px">${k}</strong>
                    <span style="opacity:0.9">${v}</span>
                  </div>
                `).join('')}
              </div>
            </div>
            ` : ''}

            <div class="episodes-section" style="margin-bottom:2rem;background:var(--card-bg);padding:1.5rem;border-radius:12px;box-shadow:0 4px 12px rgba(0,0,0,0.3)">
              <h3 style="margin-bottom:1rem;border-bottom:2px solid var(--accent);display:inline-block;padding-bottom:0.3rem">Daftar Episode</h3>
              <div class="episodes-grid" style="display:flex;flex-direction:column;gap:0.8rem">
                ${epsHTML}
              </div>
            </div>

            <div class="detail-content" style="background:var(--card-bg);padding:1.5rem;border-radius:12px;box-shadow:0 4px 12px rgba(0,0,0,0.3)">
              <h3 style="margin-bottom:1rem;border-bottom:2px solid var(--accent);display:inline-block;padding-bottom:0.3rem">Sinopsis</h3>
              <p style="white-space:pre-wrap;line-height:1.6;opacity:0.9">${d.sinopsis || 'Tidak ada sinopsis.'}</p>
            </div>
          </div>
        `;

        document.getElementById('bookmark-btn').addEventListener('click', (e) => {
          const btn = e.target;
          const anime = { url: btn.dataset.url, title: btn.dataset.title, thumbnail: btn.dataset.thumb };
          toggleBookmark(anime);
          btn.innerText = isBookmarked(anime.url) ? 'Hapus Bookmark ❌' : 'Tambah Bookmark ❤️';
        });

      }
    } catch (e) {
      contentArea.innerHTML = `<p style="text-align:center">Error: ${e.message}</p>`;
    }
  }

  // --- LOAD EPISODE ---
  async function loadEpisode(url) {
    loader.style.display = 'none';
    window.scrollTo(0, 0);
    contentArea.innerHTML = `<div style="text-align:center;margin:4rem 0">${getSkeletonGrid().split('</div>')[0]}</div>`;
    try {
      const res = await fetch(`${window.API_URL}/api/episode?url=${encodeURIComponent(url)}`);
      const json = await res.json();
      if (json.success) {
        const d = json.data;
        // Save History
        saveHistory({ url: url, title: d.title });
        
        let downloadsHTML = '';
        if(d.downloads && d.downloads.length > 0) {
          downloadsHTML = `<div class="downloads-section" style="margin-top:2rem;background:rgba(255,255,255,0.05);padding:1.5rem;border-radius:12px">` +
            `<h3 style="margin-bottom:1rem;border-bottom:2px solid var(--accent);display:inline-block;padding-bottom:0.3rem">Link Download (Alternatif)</h3>` +
            d.downloads.map(dl => `
              <div style="margin-bottom:1rem;display:flex;flex-wrap:wrap;align-items:center;gap:0.5rem">
                <strong style="color:var(--accent-hover);display:inline-block;width:50px;flex-shrink:0">${dl.resolution}</strong>
                ${dl.links.map(l => `<a href="${l.url}" target="_blank" rel="noopener noreferrer" style="color:#fff;background:rgba(255,255,255,0.1);padding:0.3rem 0.6rem;border-radius:4px;text-decoration:none;font-size:0.85rem;white-space:nowrap">${l.host}</a>`).join('')}
              </div>
            `).join('') +
          `</div>`;
        }

        contentArea.innerHTML = `
          <div class="episode-container">
            <h2 class="episode-title">${d.title}</h2>
            <div class="video-wrapper">
              <iframe src="${d.stream}" allowfullscreen></iframe>
            </div>
            <div class="nav-buttons">
              <a href="${d.prev ? `#episode?url=${encodeURIComponent(d.prev)}` : '#'}" class="nav-btn ${!d.prev ? 'disabled' : ''}">« Sebelumnya</a>
              <a href="${d.all ? `#detail?url=${encodeURIComponent(d.all)}` : '#'}" class="nav-btn ${!d.all ? 'disabled' : ''}">Semua Episode</a>
              <a href="${d.next ? `#episode?url=${encodeURIComponent(d.next)}` : '#'}" class="nav-btn ${!d.next ? 'disabled' : ''}">Selanjutnya »</a>
            </div>
            ${downloadsHTML}
          </div>
        `;
      }
    } catch (e) {
      contentArea.innerHTML = `<p style="text-align:center">Error: ${e.message}</p>`;
    }
  }

  // --- LOAD GENRE DETAIL ---
  async function loadGenreDetail(url, title, page = 1) {
    loader.style.display = 'none';
    if(page === 1) { window.scrollTo(0,0); contentArea.innerHTML = `<h2 style="text-align:center;margin:2rem 0">Genre: ${title}</h2>` + getSkeletonGrid(); }
    try {
      const res = await fetch(`${window.API_URL}/api/genre-detail?url=${encodeURIComponent(url)}&page=${page}`);
      const json = await res.json();
      if (json.success) {
        let html = json.data.map(createCardHTML).join('');
        if (json.data.length === 0 && page === 1) {
          contentArea.innerHTML = `<h2 style="text-align:center;margin:2rem 0">Genre: ${title}</h2><p style="text-align:center">Tidak ada hasil.</p>`;
        } else {
          if (page === 1) {
            contentArea.innerHTML = `<h2 style="text-align:center;margin:2rem 0">Genre: ${title}</h2><div class="grid" id="genre-grid">${html}</div>`;
            window.genreLastItems = json.data.map(i => i.title).join();
          } else {
            const currentItems = json.data.map(i => i.title).join();
            if (currentItems === window.genreLastItems || json.data.length === 0) {
              const btn = document.getElementById('load-more-btn');
              if (btn) { btn.innerText = "Habis"; btn.disabled = true; btn.style.opacity = "0.5"; }
              return;
            }
            window.genreLastItems = currentItems;
            document.getElementById('genre-grid').insertAdjacentHTML('beforeend', html);
          }
          
          let existingBtn = document.getElementById('load-more-btn');
          if (existingBtn) existingBtn.remove();
          
          if (json.data.length >= 10) { 
            const btnHtml = `<div style="text-align:center" id="load-more-container"><button id="load-more-btn" class="action-btn">Muat Lebih Banyak</button></div>`;
            contentArea.insertAdjacentHTML('beforeend', btnHtml);
            document.getElementById('load-more-btn').addEventListener('click', () => loadGenreDetail(url, title, page + 1));
          }
        }
      }
    } catch (e) { if(page === 1) contentArea.innerHTML = `<p style="text-align:center">Error: ${e.message}</p>`; }
  }

  // --- LOAD WATCHLIST ---
  function loadWatchlist() {
    loader.style.display = 'none';
    window.scrollTo(0,0);
    const h = getHistory();
    const b = getBookmarks();
    
    let html = `<h2 style="text-align:center;margin:2rem 0">Riwayat & Bookmark</h2>`;
    
    html += `<div style="max-width:1000px;margin:0 auto;padding:0 1rem">`;
    
    html += `<h3 style="margin-bottom:1rem;border-bottom:2px solid var(--accent);display:inline-block;padding-bottom:0.3rem">Terakhir Ditonton</h3>`;
    if(h.length === 0) html += `<p style="margin-bottom:2rem;opacity:0.7">Belum ada riwayat tontonan.</p>`;
    else html += `<div class="grid" style="margin-bottom:2rem">${h.map(ep => `
      <a href="#episode?url=${encodeURIComponent(ep.url)}" class="card" style="height:auto">
        <div class="info">
          <div style="color:var(--accent);font-size:0.85rem;margin-bottom:0.5rem">Lanjutkan Menonton</div>
          <h3 style="white-space:normal;overflow:visible">${ep.title}</h3>
        </div>
      </a>
    `).join('')}</div>`;

    html += `<h3 style="margin-bottom:1rem;border-bottom:2px solid var(--accent);display:inline-block;padding-bottom:0.3rem">Bookmark Saya</h3>`;
    if(b.length === 0) html += `<p style="margin-bottom:2rem;opacity:0.7">Belum ada bookmark.</p>`;
    else html += `<div class="grid">${b.map(a => `
      <a href="#detail?url=${encodeURIComponent(a.url)}" class="card">
        <img src="${a.thumbnail || 'https://via.placeholder.com/200x300?text=No+Image'}" alt="${a.title}">
        <div class="info">
          <h3>${a.title}</h3>
        </div>
      </a>
    `).join('')}</div>`;

    html += `</div>`;
    contentArea.innerHTML = html;
  }

  // --- LOAD SCHEDULE ---
  async function loadSchedule() {
    loader.style.display = 'none';
    window.scrollTo(0, 0);
    contentArea.innerHTML = `<h2 style="text-align:center;margin:2rem 0">Jadwal Rilis Anime On-Going</h2><div style="text-align:center">${getSkeletonGrid().split('</div>')[0]}</div>`;
    try {
      const res = await fetch(`${window.API_URL}/api/schedule`);
      const json = await res.json();
      if (json.success) {
        const scheduleHTML = json.data.map(dayInfo => `
          <div class="schedule-day">
            <h2>${dayInfo.day}</h2>
            <ul class="schedule-list">
              ${dayInfo.animes.map(a => `
                <li><a href="#detail?url=${encodeURIComponent(a.detailUrl)}">${a.title}</a></li>
              `).join('')}
            </ul>
          </div>
        `).join('');
        contentArea.innerHTML = `<h2 style="text-align:center;margin:2rem 0">Jadwal Rilis Anime On-Going</h2><div class="schedule-container">${scheduleHTML}</div>`;
      }
    } catch (e) {
      contentArea.innerHTML = `<p style="text-align:center">Error: ${e.message}</p>`;
    }
  }

  // --- LIVE SEARCH LOGIC ---
  let searchTimeout;
  searchInput.addEventListener('input', (e) => {
    const q = e.target.value.trim();
    clearTimeout(searchTimeout);
    if (!q) { liveSearchResults.style.display = 'none'; return; }
    searchTimeout = setTimeout(async () => {
      liveSearchResults.style.display = 'flex';
      liveSearchResults.innerHTML = '<div style="padding:1rem;text-align:center">Mencari...</div>';
      try {
        const res = await fetch(`${window.API_URL}/api/search?q=${encodeURIComponent(q)}`);
        const json = await res.json();
        if (json.success && json.data.length > 0) {
          liveSearchResults.innerHTML = json.data.slice(0,5).map(a => `
            <a href="#detail?url=${encodeURIComponent(a.detailUrl)}" class="live-search-item" onclick="document.getElementById('live-search-results').style.display='none'">
              <img src="${a.thumbnail || 'https://via.placeholder.com/40x60'}" />
              <div style="flex:1;overflow:hidden">
                <div style="font-weight:bold;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">${a.title}</div>
                <div style="font-size:0.8rem;color:var(--accent)">${a.rating || ''}</div>
              </div>
            </a>
          `).join('');
          liveSearchResults.innerHTML += `<a href="#search?q=${encodeURIComponent(q)}" class="action-btn secondary" style="width:100%;margin:0.5rem 0 0 0" onclick="document.getElementById('live-search-results').style.display='none'">Lihat Semua Hasil</a>`;
        } else {
          liveSearchResults.innerHTML = '<div style="padding:1rem;text-align:center">Tidak ditemukan.</div>';
        }
      } catch(err) { liveSearchResults.style.display = 'none'; }
    }, 500);
  });
  document.addEventListener('click', (e) => {
    if(!e.target.closest('.search-bar')) liveSearchResults.style.display = 'none';
  });


  // --- ROUTER ---
  function handleRoute() {
    const hash = window.location.hash;
    if (hash.startsWith('#search?q=')) {
      const q = decodeURIComponent(hash.split('=')[1]);
      loadSearch(q);
    } else if (hash.startsWith('#detail?url=')) {
      const url = decodeURIComponent(hash.substring(12));
      loadDetail(url);
    } else if (hash.startsWith('#episode?url=')) {
      const url = decodeURIComponent(hash.substring(13));
      loadEpisode(url);
    } else if (hash.startsWith('#genre?url=')) {
      const params = new URLSearchParams(hash.substring(7));
      loadGenreDetail(params.get('url'), params.get('title'));
    } else if (hash === '#schedule') {
      loadSchedule();
    } else if (hash === '#watchlist') {
      loadWatchlist();
    } else {
      loadHome();
    }
  }

  // Event Listeners
  window.addEventListener('hashchange', handleRoute);
  
  searchBtn.addEventListener('click', () => {
    if(searchInput.value.trim()) {
      window.location.hash = `#search?q=${encodeURIComponent(searchInput.value.trim())}`;
      liveSearchResults.style.display = 'none';
    }
  });

  searchInput.addEventListener('keypress', (e) => {
    if(e.key === 'Enter' && searchInput.value.trim()) {
      window.location.hash = `#search?q=${encodeURIComponent(searchInput.value.trim())}`;
      liveSearchResults.style.display = 'none';
    }
  });

  // Init
  loader.style.display = 'none';
  loadGenres();
  handleRoute();

  // PWA Service Worker Registration
  if ('serviceWorker' in navigator) {
    navigator.serviceWorker.register('/sw.js').then(reg => {
      console.log('PWA ServiceWorker registered', reg.scope);
    }).catch(err => {
      console.log('PWA ServiceWorker failed', err);
    });
  }
});
