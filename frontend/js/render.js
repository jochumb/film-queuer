// Backend-served images (e.g. locally-stored queue thumbnails) come back as a path like
// "/images/queue/<file>.jpg" rather than a full URL - must match api.js's API_BASE origin.
const IMAGE_BASE_URL = 'http://localhost:8080';

function resolveImageUrl(path) {
    return path.startsWith('http') ? path : `${IMAGE_BASE_URL}${path}`;
}

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

export function esc(str) {
    return String(str ?? '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

export function ownershipBadges(film) {
    if (!film.owned && !film.watched) return '';
    return `
        <span class="flags-inline">
            ${film.owned ? '<span class="flag-badge flag-owned">Owned</span>' : ''}
            ${film.watched ? '<span class="flag-badge flag-watched">Watched</span>' : ''}
        </span>
    `;
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
    if (queue.type === 'NAMED' && queue.imagePath) {
        return `<div class="${cls}"><img src="${resolveImageUrl(queue.imagePath)}" alt="${esc(queueDisplayName(queue))}"></div>`;
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
                <button class="nav-pill ${active === 'collection' ? 'active' : ''}" data-nav="collection">Collection</button>
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

export function renderQueuePreviews(previews, randomPicks) {
    const picks = (randomPicks || []).filter((item) => item.film);
    if (previews.length === 0 && picks.length === 0) {
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
                ${randomPicks ? randomPicksRow(picks) : ''}
                ${previews.map((preview, index) => queueOverviewRow(preview, index)).join('')}
            </tbody>
        </table>
    `;
}

function miniFilmCell(f, index, watchedQueueId) {
    return `
        <td class="mft-cell ${index === 0 ? 'mft-cell-primary' : ''}">
            <div class="mft-film">
                <div class="mft-thumb">
                    ${f.posterPath ? `<img src="${f.posterPath}" alt="${esc(f.title)}">` : '<span class="placeholder">🎬</span>'}
                    ${watchedQueueId ? `
                        <button class="mft-watched-btn" title="Mark as watched"
                            data-watched-queue="${watchedQueueId}" data-watched-film="${f.tmdbId}" data-watched-title="${esc(f.title)}">
                            <i data-feather="check"></i>
                        </button>
                    ` : ''}
                </div>
                <div class="mft-info">
                    <span class="mft-title">${esc(f.title)}</span>
                    <span class="mft-meta">${yearOf(f.releaseDate)} &middot; ${runtimeLabel(f.runtime)}</span>
                    ${ownershipBadges(f)}
                </div>
            </div>
        </td>
    `;
}

function miniFilmTable(films, watchedQueueId) {
    return `
        <table class="mini-film-table">
            <tbody>
                <tr>${films.map((f, i) => miniFilmCell(f, i, watchedQueueId)).join('')}</tr>
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
                ${films.length > 0 ? miniFilmTable(films, q.id) : '<span class="queue-card-meta">No films added yet</span>'}
            </td>
        </tr>
    `;
}

function randomPicksRow(picks) {
    const films = picks.map((item) => item.film);
    return `
        <tr class="random-picks-row">
            <td class="qo-rank">
                <button class="qo-shuffle-btn" type="button" data-shuffle-picks title="Shuffle picks">
                    <i data-feather="shuffle"></i>
                </button>
            </td>
            <td class="qo-queue">
                <div class="qo-queue-cell">
                    <div class="avatar">🎲</div>
                    <div>
                        <p class="queue-card-title">Tonight's Picks</p>
                        <span class="queue-card-meta">Random from your collection</span>
                    </div>
                </div>
            </td>
            <td class="qo-count"><span class="badge">${films.length}</span></td>
            <td class="qo-preview">
                ${films.length > 0 ? miniFilmTable(films, null) : '<span class="queue-card-meta">No matches — try shuffling</span>'}
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
    const editableAvatar = queue.type === 'NAMED'
        ? `<button type="button" class="avatar-edit-btn" data-edit-queue-image title="Set thumbnail image">${avatarHtml(queue, 'avatar-lg')}</button>`
        : '';
    return `
        <div class="page">
            <div class="detail-layout">
                <div class="detail-side">
                    <div class="panel">
                        <div class="detail-title-row">
                            ${editableAvatar}
                            <h2 class="detail-title">${esc(name)}</h2>
                        </div>
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
                ${ownershipBadges(f)}
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
                    ${ownershipBadges(film)}
                    <button class="add-tile-btn" data-add-film="${film.id}" data-title="${esc(film.title)}" data-tv="${film.tv ? '1' : '0'}" ${isQueued ? 'disabled' : ''}>
                        ${isQueued ? 'In queue' : 'Add to queue'}
                    </button>
                </div>
            </div>
        `;
    }).join('');
}

/* ===== Collection ===== */
export function renderCollectionShell() {
    return `
        <div class="page">
            <div class="page-header">
                <h1>Collection</h1>
                <span class="subtitle" id="collectionSubtitle">Films from your Letterboxd exports</span>
            </div>
            <div class="panel collection-panel">
                <div class="collection-controls">
                    <div class="collection-search">
                        <input type="text" id="collectionSearchInput" placeholder="Search title..." autocomplete="off">
                    </div>
                    <div class="collection-filters">
                        <label class="filter-toggle">
                            <input type="checkbox" id="ownedToggle" checked>
                            Owned
                        </label>
                        <label class="filter-toggle">
                            <input type="checkbox" id="watchedToggle">
                            Watched
                        </label>
                        <label class="filter-toggle">
                            <input type="checkbox" id="unmatchedOnlyToggle">
                            Unmatched only
                        </label>
                    </div>
                    <div class="collection-sort">
                        <label for="sortSelect">Sort</label>
                        <select id="sortSelect">
                            <option value="director:asc" selected>Director (A–Z)</option>
                            <option value="director:desc">Director (Z–A)</option>
                            <option value="title:asc">Title (A–Z)</option>
                            <option value="title:desc">Title (Z–A)</option>
                            <option value="year:asc">Year (oldest)</option>
                            <option value="year:desc">Year (newest)</option>
                            <option value="added:desc">Recently added</option>
                            <option value="added:asc">Oldest added</option>
                        </select>
                    </div>
                    <label class="filter-toggle filter-toggle-subtle">
                        <input type="checkbox" id="showRemovedToggle">
                        Show removed
                    </label>
                    <div class="pager" id="collectionPagerTop"></div>
                </div>
                <div class="collection-table-wrap">
                    <table class="collection-table">
                        <thead>
                            <tr>
                                <th class="ct-poster"></th>
                                <th>Title</th>
                                <th class="ct-year">Year</th>
                                <th>Director</th>
                                <th class="ct-actions"></th>
                            </tr>
                        </thead>
                        <tbody id="collectionBody">
                            <tr><td colspan="5" class="loading-text">Loading...</td></tr>
                        </tbody>
                    </table>
                </div>
                <div class="collection-controls collection-controls-bottom">
                    <div class="pager" id="collectionPagerBottom"></div>
                </div>
            </div>
        </div>
    `;
}

export function renderCollectionRows(items) {
    if (items.length === 0) {
        return '<tr><td colspan="5" class="muted-text">No items found.</td></tr>';
    }
    return items.map((item) => {
        const matched = item.filmTmdbId != null;
        const displayTitle = matched && item.film ? item.film.title : item.title;
        const displayYear = matched && item.film && item.film.releaseDate ? yearOf(item.film.releaseDate) : (item.year ?? 'N/A');
        const poster = matched && item.film ? item.film.posterPath : null;
        const directors = matched && item.film ? (item.film.directors || []) : [];
        const directorHtml = directors.length > 0
            ? directors.map((d) => `<span class="director-name" data-edit-sort-name data-director-id="${d.tmdbId}" data-director-name="${esc(d.name)}" data-director-sort-name="${esc(d.sortName)}" title="Click to fix sort order">${esc(d.name)}</span>`).join(', ')
            : '<span class="muted-text">—</span>';
        const titleHtml = matched && item.film
            ? `<span class="editable-title" data-edit-sort-title data-title-id="${item.film.tmdbId}" data-title-name="${esc(displayTitle)}" data-title-sort="${esc(item.film.sortTitle || displayTitle)}" title="Click to fix sort order">${esc(displayTitle)}</span>`
            : esc(displayTitle);
        const unmatchedIcon = matched ? '' : '<span class="unmatched-icon" title="Unmatched — use Fix match to link this film">⚠</span> ';
        return `
            <tr data-item-id="${item.id}">
                <td class="ct-poster">
                    <div class="ct-thumb">
                        ${poster ? `<img src="${poster}" alt="${esc(displayTitle)}">` : '<span class="placeholder">🎬</span>'}
                    </div>
                </td>
                <td class="ct-title">${unmatchedIcon}${titleHtml}</td>
                <td class="ct-year">${displayYear}</td>
                <td class="ct-director">${directorHtml}</td>
                <td class="ct-actions">
                    <button class="btn btn-sm add-to-queue-btn" data-tmdb-id="${item.filmTmdbId ?? ''}" data-title="${esc(displayTitle)}" ${matched ? '' : 'disabled'}>
                        Add to queue
                    </button>
                    <div class="row-menu">
                        <button class="btn btn-sm row-menu-toggle" type="button" data-row-menu-toggle aria-label="More actions" title="More actions">⋮</button>
                        <div class="row-menu-dropdown">
                            <button class="row-menu-item fix-match-btn" data-id="${item.id}" data-title="${esc(item.title)}" data-year="${item.year ?? ''}">Fix match</button>
                            ${item.removed
                                ? `<button class="row-menu-item restore-item-btn" data-id="${item.id}" data-title="${esc(displayTitle)}">Restore</button>`
                                : `<button class="row-menu-item remove-item-btn" data-id="${item.id}" data-title="${esc(displayTitle)}">Remove</button>`}
                        </div>
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

export function renderPager(offset, limit, total) {
    const from = total === 0 ? 0 : offset + 1;
    const to = Math.min(offset + limit, total);
    const totalPages = Math.max(1, Math.ceil(total / limit));
    const currentPage = Math.floor(offset / limit) + 1;
    return `
        <span class="pager-info">${from}–${to} of ${total}</span>
        <button class="btn btn-sm pager-prev" ${offset <= 0 ? 'disabled' : ''}>Prev</button>
        <span class="pager-page-jump">
            Page <input type="number" class="pager-page-input" min="1" max="${totalPages}" value="${currentPage}" data-total-pages="${totalPages}">
            of ${totalPages}
        </span>
        <button class="btn btn-sm pager-next" ${offset + limit >= total ? 'disabled' : ''}>Next</button>
    `;
}

export function renderLinkModal(item) {
    return `
        <div class="modal-overlay">
            <div class="modal-dialog link-modal-dialog">
                <div class="modal-header"><h3>Fix match</h3></div>
                <div class="modal-body">
                    <p class="link-modal-source">${esc(item.title)}${item.year ? ` (${item.year})` : ''}</p>
                    <div class="link-mode-tabs">
                        <button class="link-mode-tab active" data-link-mode="movie" type="button">Movies</button>
                        <button class="link-mode-tab" data-link-mode="tv" type="button">TV Shows</button>
                    </div>
                    <div class="field-row">
                        <input type="text" id="linkSearchInput" value="${esc(item.title)}">
                        <input type="number" id="linkYearInput" class="link-year-input" placeholder="Year" value="${item.year ?? ''}">
                        <button class="btn btn-primary" id="linkSearchButton">Search</button>
                    </div>
                    <div id="linkSearchResults" class="film-grid link-search-results"></div>
                </div>
                <div class="modal-footer">
                    <button class="btn" id="linkModalClose">Close</button>
                </div>
            </div>
        </div>
    `;
}

export function renderEditSortNameModal(director) {
    return `
        <div class="modal-overlay">
            <div class="modal-dialog">
                <div class="modal-header"><h3>Fix sort name</h3></div>
                <div class="modal-body">
                    <p class="link-modal-source">${esc(director.name)}</p>
                    <div class="field-row">
                        <input type="text" id="sortNameInput" value="${esc(director.sortName)}">
                    </div>
                </div>
                <div class="modal-footer">
                    <button class="btn" id="sortNameModalClose">Cancel</button>
                    <button class="btn btn-primary" id="sortNameModalSave">Save</button>
                </div>
            </div>
        </div>
    `;
}

export function renderEditSortTitleModal(film) {
    return `
        <div class="modal-overlay">
            <div class="modal-dialog">
                <div class="modal-header"><h3>Fix sort title</h3></div>
                <div class="modal-body">
                    <p class="link-modal-source">${esc(film.title)}</p>
                    <div class="field-row">
                        <input type="text" id="sortTitleInput" value="${esc(film.sortTitle)}">
                    </div>
                </div>
                <div class="modal-footer">
                    <button class="btn" id="sortTitleModalClose">Cancel</button>
                    <button class="btn btn-primary" id="sortTitleModalSave">Save</button>
                </div>
            </div>
        </div>
    `;
}

export function renderEditQueueImageModal(queue) {
    return `
        <div class="modal-overlay">
            <div class="modal-dialog">
                <div class="modal-header"><h3>Set thumbnail image</h3></div>
                <div class="modal-body">
                    <p class="link-modal-source">${esc(queueDisplayName(queue))}</p>
                    <div class="field-row">
                        <input type="text" id="queueImageInput" placeholder="https://..." value="${esc(queue.imagePath || '')}">
                    </div>
                </div>
                <div class="modal-footer">
                    <button class="btn" id="queueImageModalClose">Cancel</button>
                    <button class="btn btn-primary" id="queueImageModalSave">Save</button>
                </div>
            </div>
        </div>
    `;
}

export function renderLinkSearchResults(films) {
    if (films.length === 0) {
        return '<p class="muted-text">No results.</p>';
    }
    return films.map((film) => `
        <div class="film-tile link-result-tile" data-select-film="${film.id}" data-select-title="${esc(film.title)}" data-select-tv="${film.tv ? '1' : '0'}">
            <div class="film-tile-poster">
                ${film.posterPath ? `<img src="${film.posterPath}" alt="${esc(film.title)}">` : '<div class="placeholder">🎬</div>'}
            </div>
            <div class="film-tile-body">
                <p class="film-tile-title">${esc(film.title)}</p>
                <span class="film-tile-sub">${yearOf(film.releaseDate)}</span>
            </div>
        </div>
    `).join('');
}

export function renderQueuePickerModal(queues) {
    return `
        <div class="modal-overlay">
            <div class="modal-dialog">
                <div class="modal-header"><h3>Add to queue</h3></div>
                <div class="modal-body">
                    ${queues.length === 0 ? '<p class="muted-text">No queues yet.</p>' : `
                    <div class="rank-list queue-picker-list">
                        ${queues.map((q) => `
                            <div class="queue-row queue-picker-row" data-pick-queue="${q.id}">
                                ${avatarHtml(q)}
                                <div class="queue-row-info">
                                    <p class="queue-row-name">${esc(queueDisplayName(q))}</p>
                                    <span class="queue-row-sub">${esc(queueSubLabel(q))}</span>
                                </div>
                            </div>
                        `).join('')}
                    </div>
                    `}
                </div>
                <div class="modal-footer">
                    <button class="btn" id="queuePickerClose">Cancel</button>
                </div>
            </div>
        </div>
    `;
}
