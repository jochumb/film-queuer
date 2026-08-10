const API_BASE = 'http://localhost:8080/api';

export const api = {
    async searchPersons(query) {
        const response = await fetch(`${API_BASE}/persons/search?q=${encodeURIComponent(query)}`);
        return response.json();
    },

    async selectPerson(tmdbId, name, department, imagePath) {
        return fetch(`${API_BASE}/persons/select`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ tmdbId, name, department, imagePath }),
        });
    },

    async getQueues() {
        const response = await fetch(`${API_BASE}/queues`);
        return response.json();
    },

    async getQueue(queueId) {
        const response = await fetch(`${API_BASE}/queues/${queueId}`);
        return response.json();
    },

    async deleteQueue(queueId) {
        return fetch(`${API_BASE}/queues/${queueId}`, { method: 'DELETE' });
    },

    async getPersonFilmography(personTmdbId, department) {
        const response = await fetch(`${API_BASE}/persons/${personTmdbId}/filmography?department=${encodeURIComponent(department)}`);
        return response.json();
    },

    async getQueueFilms(queueId) {
        const response = await fetch(`${API_BASE}/queues/${queueId}/films`);
        return response.json();
    },

    async addFilmToQueue(queueId, filmData) {
        return fetch(`${API_BASE}/queues/${queueId}/films`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(filmData),
        });
    },

    async removeFilmFromQueue(queueId, filmId) {
        return fetch(`${API_BASE}/queues/${queueId}/films/${filmId}`, { method: 'DELETE' });
    },

    async reorderQueueFilms(queueId, filmOrder) {
        return fetch(`${API_BASE}/queues/${queueId}/films/reorder`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ filmOrder }),
        });
    },

    async reorderQueues(queueOrder) {
        return fetch(`${API_BASE}/queues/reorder`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ queueOrder }),
        });
    },

    async getQueuePreviews(limit = 9, filmsLimit = 4) {
        const response = await fetch(`${API_BASE}/queues/previews?limit=${limit}&filmsLimit=${filmsLimit}`);
        return response.json();
    },

    async getRandomPicks({ count, owned, watched, maxRuntime } = {}) {
        const params = new URLSearchParams();
        if (count !== undefined) params.set('count', count);
        if (owned !== undefined) params.set('owned', owned);
        if (watched !== undefined) params.set('watched', watched);
        if (maxRuntime !== undefined) params.set('maxRuntime', maxRuntime);
        const response = await fetch(`${API_BASE}/collection/random-picks?${params.toString()}`);
        return response.json();
    },

    async createNamedQueue(name, description = null) {
        return fetch(`${API_BASE}/queues/named`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name, description }),
        });
    },

    async updatePersonDepartment(personTmdbId, department) {
        return fetch(`${API_BASE}/persons/${personTmdbId}/department`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ department }),
        });
    },

    async searchMovies(query, year) {
        const yearParam = year ? `&year=${encodeURIComponent(year)}` : '';
        const response = await fetch(`${API_BASE}/films/search?q=${encodeURIComponent(query)}${yearParam}`);
        return response.json();
    },

    async searchTv(query, year) {
        const yearParam = year ? `&year=${encodeURIComponent(year)}` : '';
        const response = await fetch(`${API_BASE}/films/search/tv?q=${encodeURIComponent(query)}${yearParam}`);
        return response.json();
    },

    async getCollection({ owned, watched, unmatched, removed, sort, order, offset = 0, limit = 40 } = {}) {
        const params = new URLSearchParams({ offset, limit });
        if (owned !== undefined) params.set('owned', owned);
        if (watched !== undefined) params.set('watched', watched);
        if (unmatched !== undefined) params.set('unmatched', unmatched);
        if (removed !== undefined) params.set('removed', removed);
        if (sort !== undefined) params.set('sort', sort);
        if (order !== undefined) params.set('order', order);
        const response = await fetch(`${API_BASE}/collection?${params.toString()}`);
        return response.json();
    },

    async linkCollectionItem(id, tmdbId, tv = false) {
        return fetch(`${API_BASE}/collection/${id}/link`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ tmdbId, tv }),
        });
    },

    async setCollectionItemRemoved(id, removed) {
        return fetch(`${API_BASE}/collection/${id}/removed`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ removed }),
        });
    },

    async updateDirectorSortName(tmdbId, sortName) {
        return fetch(`${API_BASE}/persons/${tmdbId}/sort-name`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ sortName }),
        });
    },

    async updateFilmSortTitle(tmdbId, sortTitle) {
        return fetch(`${API_BASE}/films/${tmdbId}/sort-title`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ sortTitle }),
        });
    },
};
