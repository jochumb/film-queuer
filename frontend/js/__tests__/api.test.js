/**
 * Tests for the API client's fetch contract against the backend.
 */

const { api } = require('../api.js');

const API_BASE = 'http://localhost:8080/api';

function mockJsonResponse(body) {
    return { ok: true, json: async () => body };
}

beforeEach(() => {
    global.fetch = jest.fn().mockResolvedValue(mockJsonResponse({}));
});

afterEach(() => {
    jest.restoreAllMocks();
});

describe('read endpoints', () => {
    test('searchPersons hits the search endpoint with an encoded query', async () => {
        await api.searchPersons('Tom Hanks & Co');
        expect(fetch).toHaveBeenCalledWith(`${API_BASE}/persons/search?q=Tom%20Hanks%20%26%20Co`);
    });

    test('getQueues hits the queues list endpoint', async () => {
        await api.getQueues();
        expect(fetch).toHaveBeenCalledWith(`${API_BASE}/queues`);
    });

    test('getQueue hits the single-queue endpoint', async () => {
        await api.getQueue('abc-123');
        expect(fetch).toHaveBeenCalledWith(`${API_BASE}/queues/abc-123`);
    });

    test('getPersonFilmography includes the department query param', async () => {
        await api.getPersonFilmography(123, 'ACTING');
        expect(fetch).toHaveBeenCalledWith(`${API_BASE}/persons/123/filmography?department=ACTING`);
    });

    test('getQueueFilms hits the queue films endpoint', async () => {
        await api.getQueueFilms('abc-123');
        expect(fetch).toHaveBeenCalledWith(`${API_BASE}/queues/abc-123/films`);
    });

    test('getQueuePreviews defaults limit and filmsLimit', async () => {
        await api.getQueuePreviews();
        expect(fetch).toHaveBeenCalledWith(`${API_BASE}/queues/previews?limit=9&filmsLimit=4`);
    });

    test('getQueuePreviews forwards explicit limit and filmsLimit', async () => {
        await api.getQueuePreviews(10, 0);
        expect(fetch).toHaveBeenCalledWith(`${API_BASE}/queues/previews?limit=10&filmsLimit=0`);
    });

    test('searchMovies and searchTv hit distinct endpoints', async () => {
        await api.searchMovies('fight club');
        expect(fetch).toHaveBeenCalledWith(`${API_BASE}/films/search?q=fight%20club`);

        await api.searchTv('breaking bad');
        expect(fetch).toHaveBeenCalledWith(`${API_BASE}/films/search/tv?q=breaking%20bad`);
    });
});

describe('write endpoints', () => {
    test('selectPerson POSTs the person payload', async () => {
        await api.selectPerson(123, 'Tom Hanks', 'ACTING', 'https://example.com/tom.jpg');
        expect(fetch).toHaveBeenCalledWith(`${API_BASE}/persons/select`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ tmdbId: 123, name: 'Tom Hanks', department: 'ACTING', imagePath: 'https://example.com/tom.jpg' }),
        });
    });

    test('deleteQueue issues a DELETE to the queue endpoint', async () => {
        await api.deleteQueue('abc-123');
        expect(fetch).toHaveBeenCalledWith(`${API_BASE}/queues/abc-123`, { method: 'DELETE' });
    });

    test('addFilmToQueue POSTs the film payload', async () => {
        await api.addFilmToQueue('abc-123', { tmdbId: 550, tv: false });
        expect(fetch).toHaveBeenCalledWith(`${API_BASE}/queues/abc-123/films`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ tmdbId: 550, tv: false }),
        });
    });

    test('removeFilmFromQueue issues a DELETE to the film endpoint', async () => {
        await api.removeFilmFromQueue('abc-123', 550);
        expect(fetch).toHaveBeenCalledWith(`${API_BASE}/queues/abc-123/films/550`, { method: 'DELETE' });
    });

    test('reorderQueueFilms PUTs the new film order', async () => {
        await api.reorderQueueFilms('abc-123', [550, 238]);
        expect(fetch).toHaveBeenCalledWith(`${API_BASE}/queues/abc-123/films/reorder`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ filmOrder: [550, 238] }),
        });
    });

    test('reorderQueues PUTs the new queue order', async () => {
        await api.reorderQueues(['q1', 'q2']);
        expect(fetch).toHaveBeenCalledWith(`${API_BASE}/queues/reorder`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ queueOrder: ['q1', 'q2'] }),
        });
    });

    test('createNamedQueue POSTs name and description', async () => {
        await api.createNamedQueue('Faves', 'My favorite movies');
        expect(fetch).toHaveBeenCalledWith(`${API_BASE}/queues/named`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name: 'Faves', description: 'My favorite movies' }),
        });
    });

    test('updatePersonDepartment PUTs the new department', async () => {
        await api.updatePersonDepartment(123, 'DIRECTING');
        expect(fetch).toHaveBeenCalledWith(`${API_BASE}/persons/123/department`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ department: 'DIRECTING' }),
        });
    });
});
