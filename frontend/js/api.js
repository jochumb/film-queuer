const API_BASE = 'http://localhost:8080/api';

export const api = {
    async testConnection() {
        try {
            const response = await fetch('http://localhost:8080/');
            const text = await response.text();
            console.log('Backend connection:', text);
        } catch (error) {
            console.error('Backend connection failed:', error);
        }
    },

    async searchPersons(query) {
        const response = await fetch(`${API_BASE}/persons/search?q=${encodeURIComponent(query)}`);
        return response.json();
    },

    async selectPerson(tmdbId, name, department, imagePath) {
        const response = await fetch(`${API_BASE}/persons/select`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                tmdbId: tmdbId,
                name: name,
                department: department,
                imagePath: imagePath
            })
        });
        return response;
    },

    async getQueues() {
        const response = await fetch(`${API_BASE}/queues`);
        return response.json();
    },

    async getQueue(queueId) {
        const response = await fetch(`${API_BASE}/queues/${queueId}`);
        return response.json();
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
        const response = await fetch(`${API_BASE}/queues/${queueId}/films`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(filmData)
        });
        return response;
    },

    async removeFilmFromQueue(queueId, filmId) {
        const response = await fetch(`${API_BASE}/queues/${queueId}/films/${filmId}`, {
            method: 'DELETE'
        });
        return response;
    },

    async reorderQueueFilms(queueId, filmOrder) {
        const response = await fetch(`${API_BASE}/queues/${queueId}/films/reorder`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                filmOrder: filmOrder
            })
        });
        return response;
    },

    async reorderQueues(queueOrder) {
        const response = await fetch(`${API_BASE}/queues/reorder`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                queueOrder: queueOrder
            })
        });
        return response;
    },

    async getQueuePreviews(limit = 9, filmsLimit = 3) {
        const response = await fetch(`${API_BASE}/queues/previews?limit=${limit}&filmsLimit=${filmsLimit}`);
        return response.json();
    },

    async updatePersonDepartment(personTmdbId, department) {
        const response = await fetch(`${API_BASE}/persons/${personTmdbId}/department`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                department: department
            })
        });
        return response;
    },

    async searchMovies(query) {
        const response = await fetch(`${API_BASE}/films/search?q=${encodeURIComponent(query)}`);
        return response.json();
    },

    async searchTv(query) {
        const response = await fetch(`${API_BASE}/films/search/tv?q=${encodeURIComponent(query)}`);
        return response.json();
    }
};