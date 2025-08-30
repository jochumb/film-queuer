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

export function displayQueuePreviews(queuePreviews) {
    const container = document.getElementById('queuePreviews');
    
    if (!container) return; // Not on home page
    
    if (queuePreviews.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <p>No queues yet! <span class="create-queue-link" onclick="navigateToManage()">Create your first queue</span></p>
            </div>
        `;
        return;
    }
    
    container.innerHTML = queuePreviews.map((queuePreview, index) => {
        const cardColorClass = `card-color-${(index % 6) + 1}`;
        return `
            <div class="queue-preview-card ${cardColorClass}" onclick="navigateToQueue('${queuePreview.queue.id}')">
                <div class="queue-preview-header">
                    <div class="person-info">
                        <div class="person-avatar ${queuePreview.queue.person?.imagePath ? 'has-image' : ''}">
                            ${queuePreview.queue.person?.imagePath ? 
                                `<img src="${queuePreview.queue.person.imagePath}" alt="${queuePreview.queue.person.name}">` : 
                                (queuePreview.queue.person ? queuePreview.queue.person.name.charAt(0).toUpperCase() : '?')
                            }
                        </div>
                        <div class="person-details">
                            <h4>${queuePreview.queue.person ? queuePreview.queue.person.name : 'Unknown'}</h4>
                            <span class="queue-preview-role">${queuePreview.queue.person ? translateDepartmentToRole(queuePreview.queue.person.department) : ''}</span>
                        </div>
                    </div>
                </div>
                <div class="queue-preview-films">
                    ${queuePreview.films.length > 0 ? 
                        queuePreview.films.map(film => {
                            const letterboxdUrl = `https://letterboxd.com/tmdb/${film.tmdbId}/`;
                            return `
                                <div class="preview-film-item">
                                    <div class="film-icon">🎬</div>
                                    <a href="${letterboxdUrl}" target="_blank" class="film-title-link" onclick="event.stopPropagation()">
                                        <span class="film-title">${film.title}</span>
                                        <span class="film-year">${film.releaseDate ? new Date(film.releaseDate).getFullYear() : 'N/A'}</span>
                                    </a>
                                </div>
                            `;
                        }).join('') : 
                        '<div class="no-films"><div class="empty-icon">📽️</div><span>No films added yet</span></div>'
                    }
                    ${queuePreview.totalFilms > 3 ? 
                        `<div class="more-films">+${queuePreview.totalFilms - 3} more films</div>` : 
                        ''
                    }
                </div>
            </div>
        `;
    }).join('');
}

export function showHomePage() {
    document.querySelector('.container').innerHTML = `
        <header>
            <h1>Film Queuer</h1>
            <p>Discover and queue films from your favorite actors and directors</p>
        </header>
        <main class="home-page">
            <section class="queue-previews-section">
                <div class="section-header">
                    <h3>Priority Queues</h3>
                    <span class="view-all-link" onclick="navigateToManage()">Manage Queues →</span>
                </div>
                <div id="queuePreviews" class="queue-previews-container">
                    <div class="loading-message">Loading your queues...</div>
                </div>
            </section>
        </main>
    `;
}

export function showManagePage() {
    document.querySelector('.container').innerHTML = `
        <header>
            <div class="header-content">
                <h1>Queue Management</h1>
                <p>Search for people and manage your film queues</p>
            </div>
            <button class="back-button" onclick="navigateToHome()">← Back to Home</button>
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
            <button class="back-button" onclick="navigateToManage()">← Back to Queue Management</button>
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