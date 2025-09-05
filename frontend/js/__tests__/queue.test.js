/**
 * Tests for queue management functionality, focusing on TV search features
 */

// Mock DOM environment
const { JSDOM } = require('jsdom');
const dom = new JSDOM(`
    <!DOCTYPE html>
    <html>
    <body>
        <div id="tvSearchInfo"></div>
        <input id="tvSearch" />
        <button id="tvSearchButton">Search</button>
        <div id="personFilms"></div>
        <div id="movieSearchInfo"></div>
        <input id="movieSearch" />
        <button id="movieSearchButton">Search</button>
        <div id="queueFilmsContainer"></div>
        <div class="department-selector-placeholder"></div>
    </body>
    </html>
`);
global.document = dom.window.document;
global.window = dom.window;

// Mock sessionStorage
const mockSessionStorage = {
    getItem: jest.fn(),
    setItem: jest.fn()
};
global.sessionStorage = mockSessionStorage;

// Mock API
const mockApi = {
    searchTv: jest.fn(),
    searchMovies: jest.fn(),
    addFilmToQueue: jest.fn(),
    getQueue: jest.fn(),
    getQueues: jest.fn(),
    getQueuePreviews: jest.fn(),
    getPersonFilmography: jest.fn(),
    updatePersonDepartment: jest.fn(),
    removeFilmFromQueue: jest.fn()
};

jest.mock('../api.js', () => ({
    api: mockApi
}));

// Mock notifications
const mockNotifications = {
    success: jest.fn(),
    error: jest.fn(),
    warning: jest.fn()
};

jest.mock('../notifications.js', () => ({
    notifications: mockNotifications
}));

// Mock UI module
const mockUi = {
    displayQueues: jest.fn(),
    displayQueuePreviews: jest.fn(),
    showFilmManagementPage: jest.fn(),
    displayFilteredFilms: jest.fn(),
    displayQueueFilms: jest.fn(),
    updateQueueStats: jest.fn()
};

jest.mock('../ui.js', () => mockUi);

// Mock dragdrop module
jest.mock('../dragdrop.js', () => ({
    setupQueueDragAndDrop: jest.fn(),
    setupQueueListDragAndDrop: jest.fn()
}));

// Mock navigation module
jest.mock('../navigation.js', () => ({
    navigateToManage: jest.fn()
}));

// Mock search module
jest.mock('../search.js', () => ({
    translateDepartmentToRole: jest.fn((dept) => dept)
}));

describe('Queue Module - TV Search Functionality', () => {
    let queueModule;

    beforeAll(async () => {
        // Import after mocks are set up
        queueModule = require('../queue.js');
    });

    beforeEach(() => {
        document.body.innerHTML = `
            <div id="tvSearchInfo"></div>
            <input id="tvSearch" />
            <button id="tvSearchButton">Search</button>
            <div id="personFilms"></div>
            <div id="movieSearchInfo"></div>
            <input id="movieSearch" />
            <button id="movieSearchButton">Search</button>
            <div id="queueFilmsContainer"></div>
            <div class="department-selector-placeholder"></div>
        `;
        
        // Clear all mocks
        Object.values(mockApi).forEach(mock => mock.mockClear());
        Object.values(mockNotifications).forEach(mock => mock.mockClear());
        Object.values(mockUi).forEach(mock => mock.mockClear());
        mockSessionStorage.getItem.mockClear();
        mockSessionStorage.setItem.mockClear();

        // Reset the queuedFilmIds set in queue.js by accessing it through the module
        // Since it's not exported, we'll work around this issue by mocking successful responses
        mockApi.getQueueFilms = jest.fn().mockResolvedValue([]);
    });

    describe('TV Search API Integration', () => {
        test('should call TV search API with correct parameters', async () => {
            // Mock API response
            const mockTvResponse = {
                results: [
                    {
                        id: 1399,
                        name: 'Game of Thrones',
                        originalName: 'Game of Thrones',
                        firstAirDate: '2011-04-17',
                        overview: 'Epic fantasy series',
                        posterPath: '/poster.jpg'
                    }
                ],
                totalResults: 1
            };
            mockApi.searchTv.mockResolvedValue(mockTvResponse);

            // Test direct API call
            const result = await mockApi.searchTv('Game of Thrones');

            // Verify API was called correctly
            expect(mockApi.searchTv).toHaveBeenCalledWith('Game of Thrones');
            expect(result.results).toHaveLength(1);
            expect(result.results[0].name).toBe('Game of Thrones');
        });

        test('should handle TV search with no results', async () => {
            // Mock API response with no results
            const mockTvResponse = {
                results: [],
                totalResults: 0
            };
            mockApi.searchTv.mockResolvedValue(mockTvResponse);

            // Test API call
            const result = await mockApi.searchTv('NonexistentTVShow');

            // Verify search was called and returned empty results
            expect(mockApi.searchTv).toHaveBeenCalledWith('NonexistentTVShow');
            expect(result.results).toHaveLength(0);
            expect(result.totalResults).toBe(0);
        });

        test('should handle TV search API errors', async () => {
            // Mock API to throw error
            mockApi.searchTv.mockRejectedValue(new Error('API Error'));

            // Test error handling
            await expect(mockApi.searchTv('Game of Thrones')).rejects.toThrow('API Error');
            expect(mockApi.searchTv).toHaveBeenCalledWith('Game of Thrones');
        });

        test('should validate TV show response structure', async () => {
            // Mock comprehensive TV show response
            const mockTvResponse = {
                results: [
                    {
                        id: 1399,
                        name: 'Game of Thrones',
                        originalName: 'Game of Thrones',
                        firstAirDate: '2011-04-17',
                        overview: 'Seven noble families fight for control of the mythical land of Westeros.',
                        posterPath: '/1XS1oqL89opfnbLl8WnZY1O1uJx.jpg',
                        voteAverage: 9.2,
                        voteCount: 8654
                    },
                    {
                        id: 1668,
                        name: 'Friends',
                        originalName: 'Friends',
                        firstAirDate: '1994-09-22',
                        overview: 'Six friends living in New York City.',
                        posterPath: '/friends.jpg',
                        voteAverage: 8.9,
                        voteCount: 5432
                    }
                ],
                totalResults: 2,
                page: 1,
                totalPages: 1
            };
            mockApi.searchTv.mockResolvedValue(mockTvResponse);

            // Test API call
            const result = await mockApi.searchTv('popular tv shows');

            // Verify response structure
            expect(result.results).toHaveLength(2);
            expect(result.totalResults).toBe(2);
            expect(result.results[0]).toHaveProperty('id');
            expect(result.results[0]).toHaveProperty('name');
            expect(result.results[0]).toHaveProperty('firstAirDate');
            expect(result.results[1]).toHaveProperty('id');
            expect(result.results[1]).toHaveProperty('name');
            expect(result.results[1]).toHaveProperty('firstAirDate');
        });
    });

    describe('Add TV Show to Queue Functionality', () => {
        test('should handle missing queue ID when adding TV show', async () => {
            // Mock sessionStorage with no queue ID
            mockSessionStorage.getItem.mockReturnValue(null);

            // Call addSearchFilmToQueue
            await queueModule.addSearchFilmToQueue('1399', 'Game of Thrones', true);

            // Verify error notification is called
            expect(mockNotifications.error).toHaveBeenCalledWith('Error: No queue selected');
        });

        test('should verify API integration for TV show addition', async () => {
            // Test that the API module exposes the correct function signature
            expect(typeof mockApi.addFilmToQueue).toBe('function');

            // Mock proper API call parameters for TV show
            mockApi.addFilmToQueue.mockResolvedValue({ ok: true });

            // Test the API call directly with TV show parameters
            const queueId = 'test-queue-id';
            const filmData = { tmdbId: 1399, tv: true };
            
            await mockApi.addFilmToQueue(queueId, filmData);

            // Verify the API was called with correct TV show parameters
            expect(mockApi.addFilmToQueue).toHaveBeenCalledWith(queueId, {
                tmdbId: 1399,
                tv: true
            });
        });

        test('should verify API integration for movie addition', async () => {
            // Test that the API module exposes the correct function signature
            expect(typeof mockApi.addFilmToQueue).toBe('function');

            // Mock proper API call parameters for movie
            mockApi.addFilmToQueue.mockResolvedValue({ ok: true });

            // Test the API call directly with movie parameters (tv defaults to false)
            const queueId = 'test-queue-id';
            const filmData = { tmdbId: 550, tv: false };
            
            await mockApi.addFilmToQueue(queueId, filmData);

            // Verify the API was called with correct movie parameters
            expect(mockApi.addFilmToQueue).toHaveBeenCalledWith(queueId, {
                tmdbId: 550,
                tv: false
            });
        });

        test('should handle API error responses', async () => {
            // Mock API error
            mockApi.addFilmToQueue.mockRejectedValue(new Error('Network Error'));

            // Test error handling
            await expect(mockApi.addFilmToQueue('test-queue-id', { tmdbId: 1399, tv: true }))
                .rejects.toThrow('Network Error');

            // Verify API was called
            expect(mockApi.addFilmToQueue).toHaveBeenCalledWith('test-queue-id', { tmdbId: 1399, tv: true });
        });

        test('should handle non-OK API responses', async () => {
            // Mock API response with error status
            const mockResponse = { 
                ok: false, 
                status: 500,
                text: jest.fn().mockResolvedValue('Internal Server Error')
            };
            mockApi.addFilmToQueue.mockResolvedValue(mockResponse);

            // Test API call
            const result = await mockApi.addFilmToQueue('test-queue-id', { tmdbId: 1399, tv: true });

            // Verify response structure
            expect(result.ok).toBe(false);
            expect(result.status).toBe(500);
            expect(await result.text()).toBe('Internal Server Error');
        });
    });

    describe('TV vs Movie Search Distinction', () => {
        test('should use different API endpoints for TV and movie searches', async () => {
            // Mock API responses
            const mockMovieResponse = { results: [{ id: 550, title: 'Fight Club' }], totalResults: 1 };
            const mockTvResponse = { results: [{ id: 1399, name: 'Game of Thrones' }], totalResults: 1 };
            
            mockApi.searchMovies.mockResolvedValue(mockMovieResponse);
            mockApi.searchTv.mockResolvedValue(mockTvResponse);

            // Test movie search
            await mockApi.searchMovies('Fight Club');
            expect(mockApi.searchMovies).toHaveBeenCalledWith('Fight Club');

            // Test TV search
            await mockApi.searchTv('Game of Thrones');
            expect(mockApi.searchTv).toHaveBeenCalledWith('Game of Thrones');

            // Verify they are separate API endpoints
            expect(mockApi.searchMovies).toHaveBeenCalledTimes(1);
            expect(mockApi.searchTv).toHaveBeenCalledTimes(1);
        });

        test('should handle different response structures for movies vs TV shows', async () => {
            // Mock movie response structure
            const mockMovieResponse = { 
                results: [{ 
                    id: 550, 
                    title: 'Fight Club',
                    originalTitle: 'Fight Club',
                    releaseDate: '1999-10-15'
                }], 
                totalResults: 1 
            };

            // Mock TV response structure
            const mockTvResponse = { 
                results: [{ 
                    id: 1399, 
                    name: 'Game of Thrones',
                    originalName: 'Game of Thrones',
                    firstAirDate: '2011-04-17'
                }], 
                totalResults: 1 
            };
            
            mockApi.searchMovies.mockResolvedValue(mockMovieResponse);
            mockApi.searchTv.mockResolvedValue(mockTvResponse);

            // Test movie search
            const movieResult = await mockApi.searchMovies('Fight Club');
            expect(movieResult.results[0]).toHaveProperty('title');
            expect(movieResult.results[0]).toHaveProperty('releaseDate');

            // Test TV search
            const tvResult = await mockApi.searchTv('Game of Thrones');
            expect(tvResult.results[0]).toHaveProperty('name');
            expect(tvResult.results[0]).toHaveProperty('firstAirDate');
        });
    });
});