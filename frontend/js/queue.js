import { api } from './api.js';
import { displayQueues, displayQueuePreviews, showFilmManagementPage, displayFilteredFilms, displayQueueFilms, updateQueueStats } from './ui.js';
import { setupQueueDragAndDrop, setupQueueListDragAndDrop } from './dragdrop.js';
import { navigateToManage } from './navigation.js';
import { notifications } from './notifications.js';
import { translateDepartmentToRole } from './search.js';

let allFilms = [];
let averageVoteCount = 0;
let queuedFilmIds = new Set();

export async function loadQueues() {
    try {
        const queues = await api.getQueues();
        displayQueues(queues);
        
        // Setup drag-and-drop for queue reordering after displaying
        if (queues.length > 0) {
            setupQueueListDragAndDrop();
        }
    } catch (error) {
        console.error('Error loading queues:', error);
    }
}

export async function loadQueuePreviews() {
    try {
        const data = await api.getQueuePreviews(9, 2);
        displayQueuePreviews(data.previews);
    } catch (error) {
        console.error('Error loading queue previews:', error);
        // Display error state
        const container = document.getElementById('queuePreviews');
        if (container) {
            container.innerHTML = `
                <div class="error-state">
                    <p>Unable to load queues. <span class="retry-link" onclick="loadQueuePreviews()">Try again</span></p>
                </div>
            `;
        }
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
        notifications.error('Queue not found or error loading queue');
        navigateToManage();
    }
}

function showFilmManagementPageInternal() {
    const queueId = sessionStorage.getItem('currentQueueId');
    const personTmdbId = sessionStorage.getItem('currentPersonTmdbId');
    const personName = sessionStorage.getItem('currentPersonName');
    const department = sessionStorage.getItem('currentDepartment');
    
    showFilmManagementPage(queueId, personName);
    
    // Load person's films from TMDB and current queue films
    loadPersonFilms(personTmdbId, department);
    loadQueueFilms(queueId);
}

async function loadPersonFilms(personTmdbId, department) {
    const personFilmsContainer = document.getElementById('personFilms');
    
    try {
        personFilmsContainer.innerHTML = '<p>Loading films...</p>';
        
        const data = await api.getPersonFilmography(personTmdbId, department);
        
        // Handle available departments for department switching
        if (data.availableDepartments && data.availableDepartments.length > 1) {
            displayDepartmentSelector(data.availableDepartments, department, personTmdbId);
        }
        
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
        notifications.error('Error: No queue selected');
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
            notifications.error('Error: Film data not found');
            return;
        }

        const response = await api.addFilmToQueue(queueId, {
            tmdbId: film.id
        });

        if (response.ok) {
            notifications.success(`"${filmTitle}" has been added to the queue!`);
            // Refresh the queue films list
            loadQueueFilms(queueId);
        } else {
            const errorText = await response.text();
            notifications.error(`Failed to add film to queue: ${errorText}`);
        }
    } catch (error) {
        console.error('Error adding film to queue:', error);
        notifications.error('Failed to add film to queue. Please try again.');
    }
}

export async function removeFilmFromQueue(filmId, filmTitle) {
    const queueId = sessionStorage.getItem('currentQueueId');
    
    if (!queueId) {
        notifications.error('Error: No queue selected');
        return;
    }

    // Confirm removal
    const confirmed = await notifications.confirm(
        'Remove Film',
        `Are you sure you want to remove "${filmTitle}" from the queue?`,
        'Remove',
        'Cancel'
    );
    
    if (!confirmed) {
        return;
    }

    try {
        const response = await api.removeFilmFromQueue(queueId, filmId);

        if (response.ok) {
            notifications.success(`"${filmTitle}" has been removed from the queue!`);
            // Refresh the queue films list
            loadQueueFilms(queueId);
        } else if (response.status === 404) {
            notifications.warning('Film not found in queue.');
        } else {
            const errorText = await response.text();
            notifications.error(`Failed to remove film from queue: ${errorText}`);
        }
    } catch (error) {
        console.error('Error removing film from queue:', error);
        notifications.error('Failed to remove film from queue. Please try again.');
    }
}

function displayDepartmentSelector(availableDepartments, currentDepartment, personTmdbId) {
    const placeholder = document.querySelector('.department-selector-placeholder');
    if (!placeholder) return;
    
    placeholder.innerHTML = `
        <div class="department-selector">
            <label for="departmentSelect">Department:</label>
            <select id="departmentSelect" class="department-dropdown">
                ${availableDepartments.map(dept => `
                    <option value="${dept}" ${dept === currentDepartment ? 'selected' : ''}>${translateDepartmentToRole(dept)}</option>
                `).join('')}
            </select>
        </div>
    `;
    
    // Add event listener for department change
    const select = placeholder.querySelector('#departmentSelect');
    select.addEventListener('change', function() {
        const newDepartment = this.value;
        if (newDepartment !== currentDepartment) {
            changeDepartment(newDepartment, personTmdbId);
        }
    });
}

async function changeDepartment(newDepartment, personTmdbId) {
    try {
        // Update the person's department in the backend
        const response = await api.updatePersonDepartment(personTmdbId, newDepartment);
        
        if (response.ok) {
            // Update session storage
            sessionStorage.setItem('currentDepartment', newDepartment);
            
            // Update the page header
            updatePageHeader(sessionStorage.getItem('currentPersonName'));
            
            // Reload the filmography with the new department
            loadPersonFilms(personTmdbId, newDepartment);
            
            // Show success notification
            if (window.notifications) {
                notifications.success(`Department changed to ${newDepartment}`);
            }
        } else {
            console.error('Failed to update department');
            if (window.notifications) {
                notifications.error('Failed to update department');
            }
        }
    } catch (error) {
        console.error('Error updating department:', error);
        if (window.notifications) {
            notifications.error('Error updating department');
        }
    }
}

function updatePageHeader(personName) {
    const pageTitle = document.querySelector('header h1');
    
    if (pageTitle) {
        pageTitle.textContent = `${personName}'s Films`;
    }
}

export async function promoteQueue(queueId) {
    try {
        // Get current queues to determine new order
        const queues = await api.getQueues();
        
        // Find the queue to promote
        const queueIndex = queues.findIndex(q => q.id === queueId);
        if (queueIndex === -1) {
            notifications.error('Queue not found');
            return;
        }
        
        // Remove the queue from its current position
        const queueToPromote = queues.splice(queueIndex, 1)[0];
        
        // Insert the promoted queue at position 8 (9th position, lowest priority among priority queues)
        // If there are fewer than 9 queues total, just add it at the end of the current list
        const insertPosition = Math.min(8, queues.length);
        queues.splice(insertPosition, 0, queueToPromote);
        
        // Create new order array with all queue IDs
        const newOrder = queues.map(q => q.id);
        
        // Send reorder request to backend
        const response = await api.reorderQueues(newOrder);
        
        if (response.ok) {
            notifications.success(`"${queueToPromote.person?.name || 'Queue'}" promoted to priority queue!`);
            // Reload the queues to reflect the change
            loadQueues();
        } else {
            const errorText = await response.text();
            notifications.error(`Failed to promote queue: ${errorText}`);
        }
    } catch (error) {
        console.error('Error promoting queue:', error);
        notifications.error('Failed to promote queue. Please try again.');
    }
}

// Make functions available globally for onclick handlers
window.addFilmToQueue = addFilmToQueue;
window.removeFilmFromQueue = removeFilmFromQueue;
window.promoteQueue = promoteQueue;