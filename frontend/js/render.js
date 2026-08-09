const DEPARTMENT_ROLES = {
    ACTING: 'Actor',
    DIRECTING: 'Director',
    WRITING: 'Writer',
    OTHER: 'Crew',
};

export function roleLabel(department) {
    return DEPARTMENT_ROLES[department] || department || '';
}

export function yearOf(dateStr) {
    return dateStr ? dateStr.substring(0, 4) : 'N/A';
}

export function runtimeLabel(minutes) {
    if (!minutes) return '—';
    const h = Math.floor(minutes / 60);
    const m = minutes % 60;
    return h > 0 ? `${h}h ${m}m` : `${m}m`;
}

function esc(str) {
    return String(str ?? '').replace(/'/g, "\\'");
}

export function queueDisplayName(queue) {
    if (queue.type === 'PERSON') return queue.person?.name || 'Unknown Person';
    if (queue.type === 'NAMED') return queue.name || 'Unnamed Queue';
    return 'Unknown Queue';
}

export function queueSubLabel(queue) {
    if (queue.type === 'PERSON') return queue.person ? roleLabel(queue.person.department) : '';
    if (queue.type === 'NAMED') return queue.description || 'Named queue';
    return '';
}

export function avatarHtml(queue, size = '') {
    const cls = `avatar ${size}`.trim();
    if (queue.type === 'PERSON' && queue.person?.imagePath) {
        return `<div class="${cls}"><img src="${queue.person.imagePath}" alt="${esc(queue.person.name)}"></div>`;
    }
    const initial = queueDisplayName(queue).charAt(0).toUpperCase() || '?';
    return `<div class="${cls}">${initial}</div>`;
}

/* ===== Top bar ===== */
export function renderTopbar(active) {
    return `
        <header class="topbar">
            <div class="brand" data-nav="home">
                <span class="brand-mark">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><rect x="4" y="4" width="16" height="16" rx="3"/><circle cx="12" cy="12" r="2.5"/></svg>
                </span>
                Film Queuer
            </div>
            <nav class="topbar-nav">
                <button class="nav-pill ${active === 'home' ? 'active' : ''}" data-nav="home">Home</button>
                <button class="nav-pill ${active === 'manage' ? 'active' : ''}" data-nav="manage">Manage</button>
            </nav>
        </header>
    `;
}

/* ===== Home ===== */
export function renderHomeShell() {
    return `
        <div class="page">
            <div class="page-header">
                <h1>Your Queues</h1>
                <span class="subtitle">Top priority queues, ranked</span>
            </div>
            <div id="queuePreviews" class="panel home-table-panel">
                <div class="loading-text">Loading your queues...</div>
            </div>
        </div>
    `;
}

export function renderQueuePreviews(previews) {
    if (previews.length === 0) {
        return `<div class="empty-state">No queues yet. <span class="link-action" data-nav="manage">Create your first queue</span></div>`;
    }
    return `
        <table class="queue-overview-table">
            <thead>
                <tr>
                    <th class="qo-rank">#</th>
                    <th class="qo-queue">Queue</th>
                    <th class="qo-count">Films</th>
                    <th class="qo-preview">Preview</th>
                </tr>
            </thead>
            <tbody>
                ${previews.map((preview, index) => queueOverviewRow(preview, index)).join('')}
            </tbody>
        </table>
    `;
}

function queueOverviewRow(preview, index) {
    const q = preview.queue;
    const films = preview.films;
    return `
        <tr data-nav-queue="${q.id}">
            <td class="qo-rank">${index + 1}</td>
            <td class="qo-queue">
                <div class="qo-queue-cell">
                    ${avatarHtml(q)}
                    <div>
                        <p class="queue-card-title">${esc(queueDisplayName(q))}</p>
                        <span class="queue-card-meta">${esc(queueSubLabel(q))}</span>
                    </div>
                </div>
            </td>
            <td class="qo-count"><span class="badge">${preview.totalFilms}</span></td>
            <td class="qo-preview">
                ${films.length > 0 ? `
                    <table class="mini-film-table">
                        <tbody>
                            <tr>
                                ${films.map((f, i) => `
                                    <td class="mft-cell ${i === 0 ? 'mft-cell-primary' : ''}">
                                        <div class="mft-film">
                                            <div class="mft-thumb">
                                                ${f.posterPath ? `<img src="${f.posterPath}" alt="${esc(f.title)}">` : '<span class="placeholder">🎬</span>'}
                                                <button class="mft-watched-btn" title="Mark as watched"
                                                    data-watched-queue="${q.id}" data-watched-film="${f.tmdbId}" data-watched-title="${esc(f.title)}">
                                                    <i data-feather="check"></i>
                                                </button>
                                            </div>
                                            <div class="mft-info">
                                                <span class="mft-title">${esc(f.title)}</span>
                                                <span class="mft-meta">${yearOf(f.releaseDate)} &middot; ${runtimeLabel(f.runtime)}</span>
                                            </div>
                                        </div>
                                    </td>
                                `).join('')}
                            </tr>
                        </tbody>
                    </table>
                ` : '<span class="queue-card-meta">No films added yet</span>'}
            </td>
        </tr>
    `;
}

/* ===== Manage ===== */
export function renderManageShell() {
    return `
        <div class="page">
            <div class="page-header">
                <h1>Manage Queues</h1>
                <span class="subtitle">Search people, create queues, and set priority order</span>
            </div>
            <div class="manage-layout">
                <div class="manage-side">
                    <div class="panel">
                        <p class="panel-title">Search person</p>
                        <div class="field-row">
                            <input type="text" id="personSearch" placeholder="Actor or director name...">
                            <button class="btn btn-primary" id="searchButton">Search</button>
                        </div>
                        <div id="searchResults" class="result-list"></div>
                    </div>
                    <div class="panel">
                        <p class="panel-title">Create named queue</p>
                        <div class="field-row">
                            <input type="text" id="namedQueueInput" placeholder="e.g. Weekend Watchlist" maxlength="100">
                            <button class="btn btn-primary" id="createQueueButton">Create</button>
                        </div>
                    </div>
                </div>
                <div class="panel">
                    <p class="panel-title">All queues &middot; drag to reorder</p>
                    <div id="queueRankList" class="rank-list"><div class="loading-text">Loading queues...</div></div>
                </div>
            </div>
        </div>
    `;
}

export function renderPersonResults(results) {
    if (results.length === 0) return '<p class="hint-text">No results found.</p>';
    return results.map((person) => `
        <div class="result-row">
            <div class="avatar">${person.profilePath ? `<img src="${person.profilePath}" alt="${esc(person.name)}">` : person.name.charAt(0).toUpperCase()}</div>
            <div class="result-info">
                <p class="result-name">${esc(person.name)}</p>
                <p class="result-sub">${person.department ? roleLabel(person.department) : ''}${person.knownFor?.length ? ' &middot; ' + person.knownFor.slice(0, 2).join(', ') : ''}</p>
            </div>
            <button class="btn btn-sm select-person-btn"
                data-id="${person.id}" data-name="${esc(person.name)}"
                data-department="${person.department || ''}" data-image="${person.profilePath || ''}">Add</button>
        </div>
    `).join('');
}

export function renderRankList(queues) {
    if (queues.length === 0) {
        return '<div class="empty-state">No queues yet. Search a person or create a named queue to get started.</div>';
    }
    return queues.map((q, i) => `
        <div class="rank-row" data-drag-id="${q.id}">
            <span class="rank-num">${i + 1}</span>
            <div class="queue-row" data-nav-queue="${q.id}">
                <span class="drag-handle">&#8942;&#8942;</span>
                ${avatarHtml(q)}
                <div class="queue-row-info">
                    <p class="queue-row-name">${esc(queueDisplayName(q))}</p>
                    <span class="queue-row-sub">${esc(queueSubLabel(q))}</span>
                </div>
                <span class="badge badge-accent">${q.filmCount} film${q.filmCount === 1 ? '' : 's'}</span>
                <span class="badge">${q.type === 'PERSON' ? 'Person' : 'Named'}</span>
            </div>
            <button class="btn btn-icon delete-queue-btn" title="Delete queue"
                data-delete-queue="${q.id}" data-queue-name="${esc(queueDisplayName(q))}">
                <i data-feather="trash-2"></i>
            </button>
        </div>
    `).join('');
}

/* ===== Queue detail ===== */
export function renderQueueDetailShell(queue) {
    const name = queueDisplayName(queue);
    const showFilmographyTab = queue.type === 'PERSON';
    return `
        <div class="page">
            <div class="detail-layout">
                <div class="detail-side">
                    <div class="panel">
                        <h2 class="detail-title">${esc(name)}</h2>
                        <p class="detail-stats" id="queueStats">Loading...</p>
                        <hr class="section-divider">
                        <div id="queueFilms" class="queue-film-list"><p class="loading-text">Loading queue films...</p></div>
                    </div>
                </div>
                <div class="panel">
                    <div class="tabs">
                        ${showFilmographyTab ? '<button class="tab active" data-tab="filmography">Filmography</button>' : ''}
                        <button class="tab ${showFilmographyTab ? '' : 'active'}" data-tab="search-movies">Search Movies</button>
                        <button class="tab" data-tab="search-tv">Search TV</button>
                    </div>
                    ${showFilmographyTab ? `
                    <div class="tab-panel active" data-panel="filmography">
                        <div class="filmography-controls">
                            <div id="departmentSelector"></div>
                            <div class="vote-filter">
                                <label for="voteFilter">Min votes <span id="votePercentage">10</span>%</label>
                                <input type="range" id="voteFilter" min="0" max="100" value="10">
                            </div>
                            <span class="filter-info" id="filterInfo"></span>
                        </div>
                        <div id="filmographyGrid" class="film-grid"><p class="loading-text">Loading films...</p></div>
                    </div>` : ''}
                    <div class="tab-panel ${showFilmographyTab ? '' : 'active'}" data-panel="search-movies">
                        <div class="field-row">
                            <input type="text" id="movieSearch" placeholder="Search for movies...">
                            <button class="btn btn-primary" id="movieSearchButton">Search</button>
                        </div>
                        <p class="search-info" id="movieSearchInfo"></p>
                        <div id="movieSearchGrid" class="film-grid"></div>
                    </div>
                    <div class="tab-panel" data-panel="search-tv">
                        <div class="field-row">
                            <input type="text" id="tvSearch" placeholder="Search for TV shows...">
                            <button class="btn btn-primary" id="tvSearchButton">Search</button>
                        </div>
                        <p class="search-info" id="tvSearchInfo"></p>
                        <div id="tvSearchGrid" class="film-grid"></div>
                    </div>
                </div>
            </div>
        </div>
    `;
}

export function renderDepartmentSelector(departments, current) {
    if (!departments || departments.length <= 1) return '';
    return `
        <select id="departmentSelect">
            ${departments.map((d) => `<option value="${d}" ${d === current ? 'selected' : ''}>${roleLabel(d)}</option>`).join('')}
        </select>
    `;
}

export function renderQueueFilms(films) {
    if (films.length === 0) {
        return `<div class="empty-queue">No films in this queue yet.<div class="empty-sub">Browse and add films from the right panel.</div></div>`;
    }
    return films.map((f) => `
        <div class="queue-film-row" data-drag-id="${f.tmdbId}">
            <div class="qf-poster">${f.posterPath ? `<img src="${f.posterPath}" alt="${esc(f.title)}">` : ''}</div>
            <div class="qf-info">
                <p class="qf-title">${esc(f.title)}</p>
                <span class="qf-sub">${yearOf(f.releaseDate)}${f.runtime ? ` &middot; ${f.runtime}m` : ''}</span>
            </div>
            <button class="btn btn-icon remove-film-btn" data-id="${f.tmdbId}" data-title="${esc(f.title)}" title="Remove">
                <i data-feather="trash-2"></i>
            </button>
        </div>
    `).join('');
}

export function renderFilmGrid(films, queuedFilmIds, options = {}) {
    if (films.length === 0) {
        return options.emptyMessage ? `<p class="muted-text">${options.emptyMessage}</p>` : '';
    }
    return films.map((film) => {
        const isQueued = queuedFilmIds.has(film.id);
        return `
            <div class="film-tile ${isQueued ? 'in-queue' : ''}">
                <div class="film-tile-poster">
                    ${film.posterPath ? `<img src="${film.posterPath}" alt="${esc(film.title)}">` : '<div class="placeholder">🎬</div>'}
                    ${film.voteAverage > 0 ? `<span class="rating-chip">★ ${film.voteAverage.toFixed(1)}</span>` : ''}
                    ${isQueued ? '<span class="queued-check">✓</span>' : ''}
                </div>
                <div class="film-tile-body">
                    <p class="film-tile-title">${esc(film.title)}</p>
                    <span class="film-tile-sub">${yearOf(film.releaseDate)}</span>
                    ${film.role ? `<span class="film-tile-role">as ${esc(film.role)}</span>` : ''}
                    <button class="add-tile-btn" data-add-film="${film.id}" data-title="${esc(film.title)}" data-tv="${film.tv ? '1' : '0'}" ${isQueued ? 'disabled' : ''}>
                        ${isQueued ? 'In queue' : 'Add to queue'}
                    </button>
                </div>
            </div>
        `;
    }).join('');
}
