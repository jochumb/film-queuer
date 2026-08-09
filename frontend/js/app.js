import { api } from './api.js';
import { notifications } from './notifications.js';
import { enableDragReorder } from './dragdrop.js';
import {
    renderTopbar,
    renderHomeShell,
    renderQueuePreviews,
    renderManageShell,
    renderPersonResults,
    renderRankList,
    renderQueueDetailShell,
    renderDepartmentSelector,
    renderQueueFilms,
    renderFilmGrid,
    roleLabel,
} from './render.js';

const app = document.getElementById('app');

let currentQueueId = null;
let currentQueue = null;
let currentTab = 'filmography';
let allFilms = [];
let averageVoteCount = 0;
let queuedFilmIds = new Set();

function refreshIcons() {
    if (typeof feather !== 'undefined') feather.replace();
}

/* ===== Router ===== */
function navigateTo(path) {
    if (location.pathname !== path) history.pushState({}, '', path);
    handleRoute();
}

function handleRoute() {
    const path = location.pathname;
    if (path.startsWith('/queue/')) {
        showQueueDetail(path.split('/')[2]);
    } else if (path === '/manage') {
        showManage();
    } else {
        showHome();
    }
}

window.addEventListener('popstate', handleRoute);

/* ===== Home ===== */
function showHome() {
    app.innerHTML = renderTopbar('home') + renderHomeShell();
    loadHomePreviews();
}

async function loadHomePreviews() {
    const container = document.getElementById('queuePreviews');
    try {
        const data = await api.getQueuePreviews(10, 3);
        container.innerHTML = renderQueuePreviews(data.previews || []);
        refreshIcons();
    } catch (error) {
        console.error('Error loading queue previews:', error);
        container.innerHTML = '<div class="error-state">Unable to load queues. <span class="link-action" data-nav="manage">Go to Manage</span></div>';
    }
}

async function handleMarkWatched(queueId, filmId, title) {
    try {
        const response = await api.removeFilmFromQueue(queueId, filmId);
        if (response.ok) {
            notifications.success(`"${title}" marked as watched.`);
            loadHomePreviews();
        } else if (response.status === 404) {
            notifications.warning('Film not found in queue.');
        } else {
            notifications.error('Failed to update film.');
        }
    } catch (error) {
        console.error('Error marking film as watched:', error);
        notifications.error('Failed to mark film as watched. Please try again.');
    }
}

/* ===== Manage ===== */
function showManage() {
    app.innerHTML = renderTopbar('manage') + renderManageShell();

    document.getElementById('searchButton').addEventListener('click', performPersonSearch);
    document.getElementById('personSearch').addEventListener('keypress', (e) => {
        if (e.key === 'Enter') performPersonSearch();
    });
    document.getElementById('createQueueButton').addEventListener('click', createNamedQueue);
    document.getElementById('namedQueueInput').addEventListener('keypress', (e) => {
        if (e.key === 'Enter') createNamedQueue();
    });

    loadRankList();
}

async function performPersonSearch() {
    const input = document.getElementById('personSearch');
    const query = input.value.trim();
    const results = document.getElementById('searchResults');
    if (!query) return;

    results.innerHTML = '<p class="hint-text">Searching...</p>';
    try {
        const data = await api.searchPersons(query);
        results.innerHTML = renderPersonResults(data.results || []);
    } catch (error) {
        console.error('Person search failed:', error);
        results.innerHTML = '<p class="hint-text">Search failed. Please try again.</p>';
    }
}

async function selectPerson(button) {
    const { id, name, department, image } = button.dataset;
    try {
        const response = await api.selectPerson(Number(id), name, department, image);
        if (response.ok) {
            notifications.success(`${name} has been saved!`);
            document.getElementById('searchResults').innerHTML = '';
            document.getElementById('personSearch').value = '';
            loadRankList();
        } else {
            notifications.error('Failed to save person. Please try again.');
        }
    } catch (error) {
        console.error('Error saving person:', error);
        notifications.error('Failed to save person. Please try again.');
    }
}

async function createNamedQueue() {
    const input = document.getElementById('namedQueueInput');
    const button = document.getElementById('createQueueButton');
    const name = input.value.trim();
    if (!name) {
        notifications.error('Please enter a queue name');
        return;
    }

    button.disabled = true;
    try {
        const response = await api.createNamedQueue(name);
        if (response.ok) {
            notifications.success(`Queue "${name}" created!`);
            input.value = '';
            loadRankList();
        } else {
            const errorText = await response.text();
            notifications.error(`Failed to create queue: ${errorText}`);
        }
    } catch (error) {
        console.error('Error creating named queue:', error);
        notifications.error('Failed to create queue. Please try again.');
    } finally {
        button.disabled = false;
    }
}

async function loadRankList() {
    const container = document.getElementById('queueRankList');
    try {
        const queues = await api.getQueues();

        container.innerHTML = renderRankList(queues);
        refreshIcons();
        if (queues.length > 0) {
            enableDragReorder(container, '.rank-row', (newOrder) => reorderQueues(newOrder));
        }
    } catch (error) {
        console.error('Error loading queues:', error);
        container.innerHTML = '<p class="muted-text">Failed to load queues.</p>';
    }
}

async function reorderQueues(queueOrder) {
    try {
        const response = await api.reorderQueues(queueOrder);
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        loadRankList();
    } catch (error) {
        console.error('Error reordering queues:', error);
        notifications.error('Failed to save new order. Reverting.');
        loadRankList();
    }
}

async function handleDeleteQueue(queueId, queueName) {
    const confirmed = await notifications.confirm(
        'Delete queue',
        `Delete "${queueName}" and all films in it? This cannot be undone.`,
        'Delete',
        'Cancel',
    );
    if (!confirmed) return;

    try {
        const response = await api.deleteQueue(queueId);
        if (response.ok) {
            notifications.success(`"${queueName}" deleted.`);
            loadRankList();
        } else if (response.status === 404) {
            notifications.warning('Queue not found.');
            loadRankList();
        } else {
            notifications.error('Failed to delete queue.');
        }
    } catch (error) {
        console.error('Error deleting queue:', error);
        notifications.error('Failed to delete queue. Please try again.');
    }
}

/* ===== Queue detail ===== */
async function showQueueDetail(queueId) {
    app.innerHTML = renderTopbar('') + '<div class="page"><p class="loading-text">Loading queue...</p></div>';

    try {
        currentQueue = await api.getQueue(queueId);
        currentQueueId = queueId;
        currentTab = currentQueue.type === 'PERSON' ? 'filmography' : 'search-movies';
        allFilms = [];
        averageVoteCount = 0;
        queuedFilmIds = new Set();

        app.innerHTML = renderTopbar('') + renderQueueDetailShell(currentQueue);
        wireQueueDetailPage();
        loadQueueFilms(queueId);
        if (currentQueue.type === 'PERSON') {
            loadFilmography(currentQueue.person.tmdbId, currentQueue.person.department);
        }
    } catch (error) {
        console.error('Error loading queue:', error);
        notifications.error('Queue not found or failed to load.');
        navigateTo('/manage');
    }
}

function wireQueueDetailPage() {
    document.getElementById('movieSearchButton')?.addEventListener('click', performMovieSearch);
    document.getElementById('movieSearch')?.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') performMovieSearch();
    });
    document.getElementById('tvSearchButton')?.addEventListener('click', performTvSearch);
    document.getElementById('tvSearch')?.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') performTvSearch();
    });
}

async function loadQueueFilms(queueId) {
    const container = document.getElementById('queueFilms');
    if (!container) return;
    container.innerHTML = '<p class="loading-text">Loading queue films...</p>';

    try {
        const data = await api.getQueueFilms(queueId);
        const films = data.films || [];
        queuedFilmIds = new Set(films.map((f) => f.tmdbId));

        container.innerHTML = renderQueueFilms(films);
        refreshIcons();

        const stats = document.getElementById('queueStats');
        if (stats) stats.textContent = `${films.length} film${films.length === 1 ? '' : 's'}`;

        if (films.length > 0) {
            enableDragReorder(container, '.queue-film-row', (newOrder) =>
                reorderQueueFilms(queueId, newOrder.map(Number)));
        }

        reconcileQueuedTiles();
    } catch (error) {
        console.error('Error loading queue films:', error);
        container.innerHTML = '<p class="muted-text">Failed to load queue films.</p>';
    }
}

async function reorderQueueFilms(queueId, filmOrder) {
    try {
        const response = await api.reorderQueueFilms(queueId, filmOrder);
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
    } catch (error) {
        console.error('Error reordering films:', error);
        loadQueueFilms(queueId);
    }
}

async function handleRemoveFilm(tmdbId, title) {
    const confirmed = await notifications.confirm(
        'Remove film',
        `Remove "${title}" from this queue?`,
        'Remove',
        'Cancel',
    );
    if (!confirmed) return;

    try {
        const response = await api.removeFilmFromQueue(currentQueueId, tmdbId);
        if (response.ok) {
            notifications.success(`"${title}" removed from the queue.`);
            loadQueueFilms(currentQueueId);
        } else if (response.status === 404) {
            notifications.warning('Film not found in queue.');
        } else {
            notifications.error('Failed to remove film.');
        }
    } catch (error) {
        console.error('Error removing film:', error);
        notifications.error('Failed to remove film. Please try again.');
    }
}

async function handleAddFilm(tmdbId, tv, title) {
    if (queuedFilmIds.has(tmdbId)) return;

    try {
        const response = await api.addFilmToQueue(currentQueueId, { tmdbId, tv });
        if (response.ok) {
            notifications.success(`"${title}" added to the queue!`);
            loadQueueFilms(currentQueueId);
        } else {
            const errorText = await response.text();
            notifications.error(`Failed to add film: ${errorText}`);
        }
    } catch (error) {
        console.error('Error adding film:', error);
        notifications.error('Failed to add film. Please try again.');
    }
}

function reconcileQueuedTiles() {
    document.querySelectorAll('#app [data-add-film]').forEach((button) => {
        const id = Number(button.dataset.addFilm);
        const isQueued = queuedFilmIds.has(id);
        button.disabled = isQueued;
        button.textContent = isQueued ? 'In queue' : 'Add to queue';
        button.closest('.film-tile')?.classList.toggle('in-queue', isQueued);
        const poster = button.closest('.film-tile')?.querySelector('.film-tile-poster');
        if (poster) {
            let check = poster.querySelector('.queued-check');
            if (isQueued && !check) {
                check = document.createElement('span');
                check.className = 'queued-check';
                check.textContent = '✓';
                poster.appendChild(check);
            } else if (!isQueued && check) {
                check.remove();
            }
        }
    });
}

/* Filmography tab */
async function loadFilmography(personTmdbId, department) {
    const grid = document.getElementById('filmographyGrid');
    if (!grid) return;
    grid.innerHTML = '<p class="loading-text">Loading films...</p>';

    try {
        const data = await api.getPersonFilmography(personTmdbId, department);

        const selector = document.getElementById('departmentSelector');
        if (selector) {
            selector.innerHTML = renderDepartmentSelector(data.availableDepartments, department);
            selector.querySelector('#departmentSelect')?.addEventListener('change', (e) => {
                changeDepartment(personTmdbId, e.target.value);
            });
        }

        allFilms = (data.films || []).slice().sort((a, b) => {
            if (!a.releaseDate && !b.releaseDate) return 0;
            if (!a.releaseDate) return 1;
            if (!b.releaseDate) return -1;
            return a.releaseDate.localeCompare(b.releaseDate);
        });

        const withVotes = allFilms.filter((f) => f.voteCount > 0);
        averageVoteCount = withVotes.length > 0
            ? withVotes.reduce((sum, f) => sum + f.voteCount, 0) / withVotes.length
            : 0;

        document.getElementById('voteFilter').value = 10;
        document.getElementById('votePercentage').textContent = '10';
        applyVoteFilter(10);
    } catch (error) {
        console.error('Error loading filmography:', error);
        grid.innerHTML = '<p class="muted-text">Failed to load films.</p>';
    }
}

function applyVoteFilter(percentage) {
    const threshold = averageVoteCount * (percentage / 100);
    const filtered = allFilms.filter((f) => f.voteCount > 0 && f.voteCount >= threshold);

    document.getElementById('filmographyGrid').innerHTML = renderFilmGrid(filtered, queuedFilmIds, {
        emptyMessage: 'No films match the current filter.',
    });
    document.getElementById('filterInfo').textContent =
        `${filtered.length} of ${allFilms.length} films · ≥ ${Math.round(threshold)} votes`;
}

async function changeDepartment(personTmdbId, newDepartment) {
    try {
        const response = await api.updatePersonDepartment(personTmdbId, newDepartment);
        if (response.ok) {
            currentQueue.person.department = newDepartment;
            notifications.success(`Department changed to ${roleLabel(newDepartment)}`);
            loadFilmography(personTmdbId, newDepartment);
        } else {
            notifications.error('Failed to update department.');
        }
    } catch (error) {
        console.error('Error updating department:', error);
        notifications.error('Failed to update department.');
    }
}

/* Search tabs */
async function performMovieSearch() {
    const query = document.getElementById('movieSearch').value.trim();
    const info = document.getElementById('movieSearchInfo');
    const grid = document.getElementById('movieSearchGrid');
    if (!query) {
        info.textContent = 'Please enter a search term.';
        return;
    }

    info.textContent = 'Searching...';
    grid.innerHTML = '<p class="loading-text">Searching...</p>';
    try {
        const data = await api.searchMovies(query);
        const results = data.results || [];
        info.textContent = `${data.totalResults} results for "${query}"`;
        grid.innerHTML = renderFilmGrid(results, queuedFilmIds, { emptyMessage: 'No movies found.' });
    } catch (error) {
        console.error('Error searching movies:', error);
        info.textContent = 'Search failed. Please try again.';
        grid.innerHTML = '';
    }
}

async function performTvSearch() {
    const query = document.getElementById('tvSearch').value.trim();
    const info = document.getElementById('tvSearchInfo');
    const grid = document.getElementById('tvSearchGrid');
    if (!query) {
        info.textContent = 'Please enter a search term.';
        return;
    }

    info.textContent = 'Searching...';
    grid.innerHTML = '<p class="loading-text">Searching...</p>';
    try {
        const data = await api.searchTv(query);
        const results = data.results || [];
        info.textContent = `${data.totalResults} results for "${query}"`;
        grid.innerHTML = renderFilmGrid(results, queuedFilmIds, { emptyMessage: 'No TV shows found.' });
    } catch (error) {
        console.error('Error searching TV shows:', error);
        info.textContent = 'Search failed. Please try again.';
        grid.innerHTML = '';
    }
}

function switchTab(tabName) {
    currentTab = tabName;
    document.querySelectorAll('.tab').forEach((t) => t.classList.toggle('active', t.dataset.tab === tabName));
    document.querySelectorAll('.tab-panel').forEach((p) => p.classList.toggle('active', p.dataset.panel === tabName));
}

/* ===== Global delegated events ===== */
document.addEventListener('click', (e) => {
    if (e.target.closest('.drag-handle')) return;

    const watchedBtn = e.target.closest('[data-watched-film]');
    if (watchedBtn) {
        handleMarkWatched(watchedBtn.dataset.watchedQueue, Number(watchedBtn.dataset.watchedFilm), watchedBtn.dataset.watchedTitle);
        return;
    }

    const deleteQueueBtn = e.target.closest('[data-delete-queue]');
    if (deleteQueueBtn) {
        handleDeleteQueue(deleteQueueBtn.dataset.deleteQueue, deleteQueueBtn.dataset.queueName);
        return;
    }

    const navEl = e.target.closest('[data-nav]');
    if (navEl) {
        navigateTo(navEl.dataset.nav === 'manage' ? '/manage' : '/');
        return;
    }

    const queueEl = e.target.closest('[data-nav-queue]');
    if (queueEl) {
        navigateTo(`/queue/${queueEl.dataset.navQueue}`);
        return;
    }

    const selectBtn = e.target.closest('.select-person-btn');
    if (selectBtn) {
        selectPerson(selectBtn);
        return;
    }

    const removeBtn = e.target.closest('.remove-film-btn');
    if (removeBtn) {
        handleRemoveFilm(Number(removeBtn.dataset.id), removeBtn.dataset.title);
        return;
    }

    const addBtn = e.target.closest('[data-add-film]');
    if (addBtn && !addBtn.disabled) {
        handleAddFilm(Number(addBtn.dataset.addFilm), addBtn.dataset.tv === '1', addBtn.dataset.title);
        return;
    }

    const tabBtn = e.target.closest('.tab');
    if (tabBtn) {
        switchTab(tabBtn.dataset.tab);
    }
});

document.addEventListener('input', (e) => {
    if (e.target.id === 'voteFilter') {
        const value = Number(e.target.value);
        document.getElementById('votePercentage').textContent = value;
        applyVoteFilter(value);
    }
});

document.addEventListener('DOMContentLoaded', () => {
    handleRoute();
});
