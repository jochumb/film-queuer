const API_BASE = 'http://localhost:8080/api';

// Department to role translation
const DEPARTMENT_ROLES = {
    'ACTING': 'Actor',
    'DIRECTING': 'Director', 
    'WRITING': 'Writer',
    'OTHER': 'Crew Member'
};

function translateDepartmentToRole(department) {
    return DEPARTMENT_ROLES[department] || department;
}

document.addEventListener('DOMContentLoaded', function() {
    console.log('Film Queuer app initialized');
    
    testApiConnection();
    setupPersonSearch();
    loadQueues();
});

async function testApiConnection() {
    try {
        const response = await fetch('http://localhost:8080/');
        const text = await response.text();
        console.log('Backend connection:', text);
    } catch (error) {
        console.error('Backend connection failed:', error);
    }
}

function setupPersonSearch() {
    const searchInput = document.getElementById('personSearch');
    const searchButton = document.getElementById('searchButton');
    const searchResults = document.getElementById('searchResults');

    searchButton.addEventListener('click', performSearch);
    searchInput.addEventListener('keypress', function(e) {
        if (e.key === 'Enter') {
            performSearch();
        }
    });

    async function performSearch() {
        const query = searchInput.value.trim();
        if (!query) return;

        searchButton.disabled = true;
        searchButton.textContent = 'Searching...';
        searchResults.innerHTML = '<p>Searching...</p>';

        try {
            const response = await fetch(`${API_BASE}/persons/search?q=${encodeURIComponent(query)}`);
            const data = await response.json();

            if (data.results && data.results.length > 0) {
                displaySearchResults(data.results);
            } else {
                searchResults.innerHTML = '<p>No results found.</p>';
            }
        } catch (error) {
            console.error('Search failed:', error);
            searchResults.innerHTML = '<p>Search failed. Please try again.</p>';
        } finally {
            searchButton.disabled = false;
            searchButton.textContent = 'Search';
        }
    }

    function displaySearchResults(results) {
        searchResults.innerHTML = results.map(person => `
            <div class="person-card">
                <div class="person-image ${!person.profilePath ? 'no-image' : ''}">
                    ${person.profilePath ? `<img src="${person.profilePath}" alt="${person.name}">` : ''}
                </div>
                <div class="person-info">
                    <h3>${person.name}</h3>
                    ${person.department ? `<div class="person-department">${person.department}</div>` : ''}
                    ${person.knownFor.length > 0 ? `<div class="person-known-for">Known for: ${person.knownFor.slice(0, 3).join(', ')}</div>` : ''}
                    <button class="select-person-btn" onclick="selectPerson(${person.id}, '${person.name.replace(/'/g, "\\'")}', '${person.department || ''}')">
                        Select
                    </button>
                </div>
            </div>
        `).join('');
    }
}

async function selectPerson(tmdbId, name, department) {
    try {
        const response = await fetch(`${API_BASE}/persons/select`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                tmdbId: tmdbId,
                name: name,
                department: department
            })
        });

        if (response.ok) {
            alert(`${name} has been saved successfully!`);
            loadQueues();
        } else {
            alert('Failed to save person. Please try again.');
        }
    } catch (error) {
        console.error('Error saving person:', error);
        alert('Failed to save person. Please try again.');
    }
}

async function loadQueues() {
    try {
        const response = await fetch(`${API_BASE}/queues`);
        const queues = await response.json();
        displayQueues(queues);
    } catch (error) {
        console.error('Error loading queues:', error);
    }
}

function displayQueues(queues) {
    const savedPersonsContainer = document.getElementById('savedPersons');
    if (queues.length > 0) {
        savedPersonsContainer.innerHTML = `
            <h3>Queue</h3>
            <div class="saved-persons-list">
                ${queues.map(queue => `
                    <div class="saved-person-item clickable" onclick="manageQueueFilms('${queue.id}', '${queue.person?.tmdbId || ''}', '${queue.person?.name || 'Unknown'}', '${queue.person?.department || ''}')">
                        ${queue.person ? `
                            <strong>${queue.person.name}</strong> - ${translateDepartmentToRole(queue.person.department)}
                        ` : 'Unknown item'}
                        <span class="edit-indicator">→</span>
                    </div>
                `).join('')}
            </div>
        `;
    } else {
        savedPersonsContainer.innerHTML = '<h3>No queues yet</h3>';
    }
}

function manageQueueFilms(queueId, personTmdbId, personName, department) {
    // Store current queue data
    sessionStorage.setItem('currentQueueId', queueId);
    sessionStorage.setItem('currentPersonTmdbId', personTmdbId);
    sessionStorage.setItem('currentPersonName', personName);
    sessionStorage.setItem('currentDepartment', department);
    
    // Navigate to film management page
    showFilmManagementPage();
}

function showFilmManagementPage() {
    const queueId = sessionStorage.getItem('currentQueueId');
    const personTmdbId = sessionStorage.getItem('currentPersonTmdbId');
    const personName = sessionStorage.getItem('currentPersonName');
    const department = sessionStorage.getItem('currentDepartment');
    
    document.querySelector('.container').innerHTML = `
        <header>
            <h1>${personName}'s Films</h1>
            <p class="queue-subtitle">${translateDepartmentToRole(department)} • Queue ID: ${queueId.substring(0, 8)}...</p>
        </header>
        <main>
            <div class="film-management-section">
                <button class="back-button" onclick="showMainPage()">← Back to Queue List</button>
                
                <div class="person-films-section">
                    <h2>Films by ${personName}</h2>
                    <p>Select films to add to the queue:</p>
                    <div class="vote-filter-section">
                        <label for="voteFilter">Minimum vote threshold: <span id="votePercentage">10</span>% of average</label>
                        <input type="range" id="voteFilter" min="0" max="100" value="10" class="vote-slider">
                        <p class="filter-info" id="filterInfo">Loading...</p>
                    </div>
                    <div id="personFilms" class="person-films-list">
                        <p>Loading films...</p>
                    </div>
                </div>
                
                <div class="queue-films-section">
                    <h2>Films in Queue</h2>
                    <div id="queueFilms" class="queue-films-list">
                        <p>Loading queue films...</p>
                    </div>
                </div>
            </div>
        </main>
    `;
    
    // Load person's films from TMDB and current queue films
    loadPersonFilms(personTmdbId, department);
    loadQueueFilms(queueId);
}

let allFilms = [];
let averageVoteCount = 0;

async function loadPersonFilms(personTmdbId, department) {
    const personFilmsContainer = document.getElementById('personFilms');
    
    try {
        personFilmsContainer.innerHTML = '<p>Loading films...</p>';
        
        const response = await fetch(`${API_BASE}/persons/${personTmdbId}/filmography?department=${encodeURIComponent(department)}`);
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const data = await response.json();
        
        if (data.films && data.films.length > 0) {
            // Store all films globally for filtering
            allFilms = data.films;
            
            // Calculate average vote count (excluding 0 vote films)
            const filmsWithVotes = allFilms.filter(film => film.voteCount > 0);
            averageVoteCount = filmsWithVotes.length > 0 
                ? filmsWithVotes.reduce((sum, film) => sum + film.voteCount, 0) / filmsWithVotes.length 
                : 0;
            
            // Setup slider event listener
            setupVoteFilter();
            
            // Display films with initial filter
            displayFilteredFilms(10);
        } else {
            personFilmsContainer.innerHTML = '<p>No films found for this person in their department.</p>';
        }
    } catch (error) {
        console.error('Error loading person films:', error);
        personFilmsContainer.innerHTML = '<p>Failed to load films. Please try again.</p>';
    }
}

function setupVoteFilter() {
    const slider = document.getElementById('voteFilter');
    const percentage = document.getElementById('votePercentage');
    
    slider.addEventListener('input', function() {
        const value = parseInt(this.value);
        percentage.textContent = value;
        displayFilteredFilms(value);
    });
}

function displayFilteredFilms(thresholdPercentage) {
    const personFilmsContainer = document.getElementById('personFilms');
    const filterInfo = document.getElementById('filterInfo');
    
    const threshold = averageVoteCount * (thresholdPercentage / 100);
    
    // Filter films: exclude 0 vote films and films below threshold
    const filteredFilms = allFilms.filter(film => 
        film.voteCount > 0 && film.voteCount >= threshold
    );
    
    // Update filter info
    filterInfo.textContent = `Showing ${filteredFilms.length} of ${allFilms.length} films (threshold: ${Math.round(threshold)} votes)`;
    
    if (filteredFilms.length > 0) {
        personFilmsContainer.innerHTML = `
            <div class="films-grid">
                ${filteredFilms.map(film => `
                    <div class="film-card" data-film-id="${film.id}">
                        <div class="film-poster">
                            ${film.posterPath ? 
                                `<img src="${film.posterPath}" alt="${film.title}">` : 
                                '<div class="no-poster">🎬</div>'
                            }
                        </div>
                        <div class="film-info">
                            <h4>${film.title}</h4>
                            ${film.originalTitle && film.originalTitle !== film.title ? 
                                `<p class="original-title">(${film.originalTitle})</p>` : ''
                            }
                            ${film.releaseDate ? `<p class="release-date">${film.releaseDate.substring(0, 4)}</p>` : ''}
                            ${film.role ? `<p class="role">as ${film.role}</p>` : ''}
                            ${film.voteAverage > 0 ? `<p class="rating">★ ${film.voteAverage.toFixed(1)} (${film.voteCount} votes)</p>` : ''}
                            <button class="add-film-btn" onclick="addFilmToQueue('${film.id}', '${film.title.replace(/'/g, "\\\'")}')">
                                Add to Queue
                            </button>
                        </div>
                    </div>
                `).join('')}
            </div>
        `;
    } else {
        personFilmsContainer.innerHTML = '<p>No films match the current vote threshold.</p>';
    }
}

function loadQueueFilms(queueId) {
    const queueFilmsContainer = document.getElementById('queueFilms');
    
    // TODO: Load actual films from queue API
    queueFilmsContainer.innerHTML = '<p>Queue films functionality coming soon</p>';
}

function addFilmToQueue(filmId, filmTitle) {
    // TODO: Implement adding film to queue
    alert(`Adding "${filmTitle}" to queue (Film ID: ${filmId})`);
    console.log('Add film to queue:', { filmId, filmTitle });
}

function showMainPage() {
    location.reload();
}