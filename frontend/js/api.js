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

    async searchMovies(query) {
        const response = await fetch(`${API_BASE}/films/search?q=${encodeURIComponent(query)}`);
        return response.json();
    },

    async searchTv(query) {
        const response = await fetch(`${API_BASE}/films/search/tv?q=${encodeURIComponent(query)}`);
        return response.json();
    },
};
