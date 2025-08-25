import { api } from './api.js';
import { displayQueues, showFilmManagementPage, displayFilteredFilms, displayQueueFilms, updateQueueStats } from './ui.js';
import { setupQueueDragAndDrop } from './dragdrop.js';
import { navigateToHome } from './navigation.js';

let allFilms = [];
let averageVoteCount = 0;
let queuedFilmIds = new Set();

export async function loadQueues() {
    try {
        const queues = await api.getQueues();
        displayQueues(queues);
    } catch (error) {
        console.error('Error loading queues:', error);
    }
}

export async function showQueuePage(queueId) {
    try {
        // Fetch queue details
        const queue = await api.getQueue(queueId);
        
        // Store current queue data
        sessionStorage.setItem('currentQueueId', queueId);
        sessionStorage.setItem('currentPersonTmdbId', queue.person?.tmdbId || '');
        sessionStorage.setItem('currentPersonName', queue.person?.name || 'Unknown');
        sessionStorage.setItem('currentDepartment', queue.person?.department || '');
        
        // Show film management page
        showFilmManagementPageInternal();
    } catch (error) {
        console.error('Error loading queue:', error);
        alert('Queue not found or error loading queue');
        navigateToHome();
    }
}

function showFilmManagementPageInternal() {
    const queueId = sessionStorage.getItem('currentQueueId');
    const personTmdbId = sessionStorage.getItem('currentPersonTmdbId');
    const personName = sessionStorage.getItem('currentPersonName');
    const department = sessionStorage.getItem('currentDepartment');
    
    showFilmManagementPage(queueId, personName, department);
    
    // Load person's films from TMDB and current queue films
    loadPersonFilms(personTmdbId, department);
    loadQueueFilms(queueId);
}

async function loadPersonFilms(personTmdbId, department) {
    const personFilmsContainer = document.getElementById('personFilms');
    
    try {
        personFilmsContainer.innerHTML = '<p>Loading films...</p>';
        
        const data = await api.getPersonFilmography(personTmdbId, department);
        
        if (data.films && data.films.length > 0) {
            // Store all films globally for filtering, sorted by release date (oldest first)
            allFilms = data.films.sort((a, b) => {
                // Handle missing release dates by putting them at the end
                if (!a.releaseDate && !b.releaseDate) return 0;
                if (!a.releaseDate) return 1;
                if (!b.releaseDate) return -1;
                return a.releaseDate.localeCompare(b.releaseDate);
            });
            
            // Calculate average vote count (excluding 0 vote films)
            const filmsWithVotes = allFilms.filter(film => film.voteCount > 0);
            averageVoteCount = filmsWithVotes.length > 0 
                ? filmsWithVotes.reduce((sum, film) => sum + film.voteCount, 0) / filmsWithVotes.length 
                : 0;
            
            // Setup slider event listener
            setupVoteFilter();
            
            // Display films with initial filter
            applyVoteFilter(10);
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
        applyVoteFilter(value);
    });
}

function applyVoteFilter(thresholdPercentage) {
    const threshold = averageVoteCount * (thresholdPercentage / 100);
    
    // Filter films: exclude 0 vote films and films below threshold
    const filteredFilms = allFilms.filter(film => 
        film.voteCount > 0 && film.voteCount >= threshold
    );
    
    displayFilteredFilms(filteredFilms, allFilms, threshold, queuedFilmIds);
}

export async function loadQueueFilms(queueId) {
    const queueFilmsContainer = document.getElementById('queueFilms');
    
    try {
        queueFilmsContainer.innerHTML = '<p>Loading queue films...</p>';
        
        const data = await api.getQueueFilms(queueId);
        
        if (data.films && data.films.length > 0) {
            // Update queued films set
            queuedFilmIds.clear();
            data.films.forEach(film => queuedFilmIds.add(film.tmdbId));
            
            displayQueueFilms(data.films);
            
            // Setup drag and drop functionality
            setupQueueDragAndDrop();
            updateQueueStats(data.films.length);
        } else {
            // Clear queued films set when queue is empty
            queuedFilmIds.clear();
            displayQueueFilms([]);
            updateQueueStats(0);
        }
        
        // Refresh filmography display to update queue indicators
        if (allFilms.length > 0) {
            const currentThreshold = document.getElementById('voteFilter')?.value || 10;
            applyVoteFilter(parseInt(currentThreshold));
        }
    } catch (error) {
        console.error('Error loading queue films:', error);
        queueFilmsContainer.innerHTML = '<p>Failed to load queue films. Please try again.</p>';
    }
}

export async function addFilmToQueue(filmId, filmTitle) {
    const queueId = sessionStorage.getItem('currentQueueId');
    
    if (!queueId) {
        alert('Error: No queue selected');
        return;
    }

    // Check if film is already in queue
    if (queuedFilmIds.has(filmId)) {
        return; // Film already in queue, do nothing
    }

    try {
        // Get film details from the displayed film data
        const film = allFilms.find(f => f.id == filmId);
        if (!film) {
            alert('Error: Film data not found');
            return;
        }

        const response = await api.addFilmToQueue(queueId, {
            tmdbId: film.id,
            title: film.title,
            originalTitle: film.originalTitle,
            releaseDate: film.releaseDate
        });

        if (response.ok) {
            alert(`"${filmTitle}" has been added to the queue!`);
            // Refresh the queue films list
            loadQueueFilms(queueId);
        } else {
            const errorText = await response.text();
            alert(`Failed to add film to queue: ${errorText}`);
        }
    } catch (error) {
        console.error('Error adding film to queue:', error);
        alert('Failed to add film to queue. Please try again.');
    }
}

export async function removeFilmFromQueue(filmId, filmTitle) {
    const queueId = sessionStorage.getItem('currentQueueId');
    
    if (!queueId) {
        alert('Error: No queue selected');
        return;
    }

    // Confirm removal
    if (!confirm(`Are you sure you want to remove "${filmTitle}" from the queue?`)) {
        return;
    }

    try {
        const response = await api.removeFilmFromQueue(queueId, filmId);

        if (response.ok) {
            alert(`"${filmTitle}" has been removed from the queue!`);
            // Refresh the queue films list
            loadQueueFilms(queueId);
        } else if (response.status === 404) {
            alert('Film not found in queue.');
        } else {
            const errorText = await response.text();
            alert(`Failed to remove film from queue: ${errorText}`);
        }
    } catch (error) {
        console.error('Error removing film from queue:', error);
        alert('Failed to remove film from queue. Please try again.');
    }
}

// Make functions available globally for onclick handlers
window.addFilmToQueue = addFilmToQueue;
window.removeFilmFromQueue = removeFilmFromQueue;