import { translateDepartmentToRole } from './search.js';

export function displayQueues(queues) {
    const savedPersonsContainer = document.getElementById('savedPersons');
    if (queues.length > 0) {
        savedPersonsContainer.innerHTML = `
            <h3>Queue</h3>
            <div class="saved-persons-list" id="queuesList">
                ${queues.map(queue => `
                    <div class="saved-person-item queue-item clickable" draggable="true" data-queue-id="${queue.id}" onclick="navigateToQueue('${queue.id}')">
                        <div class="drag-handle" onclick="event.stopPropagation()">⋮⋮</div>
                        <div class="queue-info">
                            ${queue.person ? `
                                <strong>${queue.person.name}</strong> - ${translateDepartmentToRole(queue.person.department)}
                            ` : 'Unknown item'}
                            <span class="edit-indicator">→</span>
                        </div>
                    </div>
                `).join('')}
            </div>
        `;
    } else {
        savedPersonsContainer.innerHTML = '<h3>No queues yet</h3>';
    }
}

export function showHomePage() {
    document.querySelector('.container').innerHTML = `
        <header>
            <h1>Film Queuer</h1>
            <p>Discover and queue films from your favorite actors and directors</p>
        </header>
        <main>
            <section class="search-section">
                <h2>Search for a Person</h2>
                <div class="search-container">
                    <input type="text" id="personSearch" placeholder="Enter actor or director name...">
                    <button id="searchButton">Search</button>
                </div>
                <div id="searchResults" class="search-results"></div>
            </section>
            
            <section class="saved-persons-section">
                <div id="savedPersons"></div>
            </section>
        </main>
    `;
}

export function showFilmManagementPage(queueId, personName, department) {
    document.querySelector('.container').innerHTML = `
        <header>
            <div class="header-content">
                <h1>${personName}'s Films</h1>
                <p class="queue-subtitle">${translateDepartmentToRole(department)} • Queue ID: ${queueId.substring(0, 8)}...</p>
            </div>
            <button class="back-button" onclick="navigateToHome()">← Back to Queue List</button>
        </header>
        <main class="two-column-layout">
            <div class="left-column">
                <div class="queue-films-section">
                    <div class="queue-header-sticky">
                        <div class="queue-title-row">
                            <h2>Queue</h2>
                            <div class="queue-stats" id="queueStats">
                                <span class="film-count">0 films</span>
                            </div>
                        </div>
                    </div>
                    <div id="queueFilms" class="queue-films-list">
                        <p>Loading queue films...</p>
                    </div>
                </div>
            </div>
            
            <div class="right-column">
                <div class="person-films-section">
                    <div class="filmography-header">
                        <div class="filmography-title">
                            <h2>Browse Filmography</h2>
                            <p>Select films to add to your queue:</p>
                        </div>
                        <div class="vote-filter-inline">
                            <label for="voteFilter">Min votes: <span id="votePercentage">10</span>%</label>
                            <input type="range" id="voteFilter" min="0" max="100" value="10" class="vote-slider-inline">
                            <p class="filter-info-inline" id="filterInfo">Loading...</p>
                        </div>
                    </div>
                    <div id="personFilms" class="person-films-list">
                        <p>Loading films...</p>
                    </div>
                </div>
            </div>
        </main>
    `;
}

export function displayFilteredFilms(filteredFilms, allFilms, threshold, queuedFilmIds) {
    const personFilmsContainer = document.getElementById('personFilms');
    const filterInfo = document.getElementById('filterInfo');
    
    filterInfo.textContent = `Showing ${filteredFilms.length} of ${allFilms.length} films (threshold: ${Math.round(threshold)} votes)`;
    
    if (filteredFilms.length > 0) {
        personFilmsContainer.innerHTML = `
            <div class="films-grid">
                ${filteredFilms.map(film => {
                    const isInQueue = queuedFilmIds.has(film.id);
                    return `
                        <div class="film-card ${isInQueue ? 'in-queue' : ''}" data-film-id="${film.id}">
                            <div class="film-poster">
                                ${film.posterPath ? 
                                    `<img src="${film.posterPath}" alt="${film.title}">` : 
                                    '<div class="no-poster">🎬</div>'
                                }
                                ${isInQueue ? '<div class="queue-indicator">✓</div>' : ''}
                            </div>
                            <div class="film-info">
                                <h4>${film.title}</h4>
                                ${film.originalTitle && film.originalTitle !== film.title ? 
                                    `<p class="original-title">(${film.originalTitle})</p>` : ''
                                }
                                ${film.releaseDate ? `<p class="release-date">${film.releaseDate.substring(0, 4)}</p>` : ''}
                                ${film.role ? `<p class="role">as ${film.role}</p>` : ''}
                                ${film.voteAverage > 0 ? `<p class="rating">★ ${film.voteAverage.toFixed(1)} (${film.voteCount} votes)</p>` : ''}
                                <button class="add-film-btn ${isInQueue ? 'in-queue' : ''}" onclick="addFilmToQueue('${film.id}', '${film.title.replace(/'/g, "\\\'")}')" ${isInQueue ? 'disabled' : ''}>
                                    ${isInQueue ? 'In Queue' : 'Add to Queue'}
                                </button>
                            </div>
                        </div>
                    `;
                }).join('')}
            </div>
        `;
    } else {
        personFilmsContainer.innerHTML = '<p>No films match the current vote threshold.</p>';
    }
}

export function displayQueueFilms(films) {
    const queueFilmsContainer = document.getElementById('queueFilms');
    
    if (films.length > 0) {
        queueFilmsContainer.innerHTML = `
            ${films.map(film => `
                <div class="queue-film-item" draggable="true" data-film-tmdb-id="${film.tmdbId}">
                    <div class="drag-handle">⋮⋮</div>
                    <div class="queue-film-info">
                        <h4>${film.title}</h4>
                        ${film.originalTitle && film.originalTitle !== film.title ? 
                            `<p class="original-title">(${film.originalTitle})</p>` : ''
                        }
                        ${film.releaseDate ? `<p class="release-date">${film.releaseDate.substring(0, 4)}</p>` : ''}
                    </div>
                    <button class="remove-film-btn" onclick="removeFilmFromQueue('${film.tmdbId}', '${film.title.replace(/'/g, "\\\'")}')">
                        <i data-feather="trash-2"></i>
                    </button>
                </div>
            `).join('')}
        `;
    } else {
        queueFilmsContainer.innerHTML = '<div class="empty-queue"><p>No films in your queue yet.</p><p class="empty-queue-subtitle">Browse the filmography and add some films!</p></div>';
    }
    
    // Initialize Feather icons
    if (typeof feather !== 'undefined') {
        feather.replace();
    }
}

export function updateQueueStats(filmCount) {
    const queueStats = document.getElementById('queueStats');
    if (queueStats) {
        const filmText = filmCount === 1 ? 'film' : 'films';
        queueStats.innerHTML = `<span class="film-count">${filmCount} ${filmText}</span>`;
    }
}