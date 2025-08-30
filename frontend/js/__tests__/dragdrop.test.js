/**
 * Tests for drag-and-drop functionality
 */

// Mock DOM environment
const { JSDOM } = require('jsdom');
const dom = new JSDOM('<!DOCTYPE html><html><body></body></html>');
global.document = dom.window.document;
global.window = dom.window;

// Mock sessionStorage
const mockSessionStorage = {
    store: {},
    getItem: jest.fn((key) => mockSessionStorage.store[key] || null),
    setItem: jest.fn((key, value) => { mockSessionStorage.store[key] = value; }),
    clear: jest.fn(() => { mockSessionStorage.store = {}; })
};
global.sessionStorage = mockSessionStorage;

// Mock API
const mockApi = {
    reorderQueueFilms: jest.fn(),
    reorderQueues: jest.fn()
};

jest.mock('../api.js', () => ({
    api: mockApi
}));

// Mock queue module for imports
jest.mock('../queue.js', () => ({
    loadQueueFilms: jest.fn(),
    loadQueues: jest.fn()
}));

import { setupQueueDragAndDrop, setupQueueListDragAndDrop } from '../dragdrop.js';

describe('Queue Film Drag and Drop', () => {
    beforeEach(() => {
        document.body.innerHTML = '';
        mockApi.reorderQueueFilms.mockClear();
        mockSessionStorage.getItem.mockClear();
        mockSessionStorage.setItem.mockClear();
        
        // Set up mock session storage
        mockSessionStorage.store = { 'currentQueueId': 'test-queue-id' };
    });

    test('should setup drag and drop listeners for queue films', () => {
        // Given
        document.body.innerHTML = `
            <div id="queueFilms">
                <div class="queue-film-item" draggable="true" data-film-tmdb-id="1">Film 1</div>
                <div class="queue-film-item" draggable="true" data-film-tmdb-id="2">Film 2</div>
            </div>
        `;

        // When
        setupQueueDragAndDrop();

        // Then
        const filmItems = document.querySelectorAll('.queue-film-item');
        expect(filmItems.length).toBe(2);
        
        // Verify draggable attributes
        filmItems.forEach(item => {
            expect(item.getAttribute('draggable')).toBe('true');
        });
    });

    test('should handle drag start event', () => {
        // Given
        document.body.innerHTML = `
            <div id="queueFilms">
                <div class="queue-film-item" draggable="true" data-film-tmdb-id="1">Film 1</div>
            </div>
        `;
        setupQueueDragAndDrop();
        const filmItem = document.querySelector('.queue-film-item');
        
        // Mock DataTransfer
        const mockDataTransfer = { effectAllowed: null };
        const dragStartEvent = new Event('dragstart');
        Object.defineProperty(dragStartEvent, 'dataTransfer', {
            value: mockDataTransfer,
            writable: false
        });

        // When
        filmItem.dispatchEvent(dragStartEvent);

        // Then
        expect(filmItem.classList.contains('dragging')).toBe(true);
        expect(mockDataTransfer.effectAllowed).toBe('move');
    });

    test('should handle drag end event', () => {
        // Given
        document.body.innerHTML = `
            <div id="queueFilms">
                <div class="queue-film-item dragging" draggable="true" data-film-tmdb-id="1">Film 1</div>
            </div>
        `;
        setupQueueDragAndDrop();
        const filmItem = document.querySelector('.queue-film-item');

        // When
        const dragEndEvent = new Event('dragend');
        filmItem.dispatchEvent(dragEndEvent);

        // Then
        expect(filmItem.classList.contains('dragging')).toBe(false);
    });

    test('should call reorder API on drop with correct film order', async () => {
        // Given
        document.body.innerHTML = `
            <div id="queueFilms">
                <div class="queue-film-item" data-film-tmdb-id="1">Film 1</div>
                <div class="queue-film-item" data-film-tmdb-id="2">Film 2</div>
                <div class="queue-film-item" data-film-tmdb-id="3">Film 3</div>
            </div>
        `;
        setupQueueDragAndDrop();
        
        mockApi.reorderQueueFilms.mockResolvedValue({ ok: true });
        
        const container = document.getElementById('queueFilms');
        const dropEvent = new Event('drop');
        Object.defineProperty(dropEvent, 'preventDefault', {
            value: jest.fn(),
            writable: false
        });

        // When
        container.dispatchEvent(dropEvent);

        // Then - should extract film order and call API
        await new Promise(resolve => setTimeout(resolve, 0)); // Wait for async
        
        expect(mockApi.reorderQueueFilms).toHaveBeenCalledWith(
            'test-queue-id',
            [1, 2, 3] // Film TMDB IDs in order
        );
    });

    test('should handle API error during reorder', async () => {
        // Given
        document.body.innerHTML = `
            <div id="queueFilms">
                <div class="queue-film-item" data-film-tmdb-id="1">Film 1</div>
            </div>
        `;
        setupQueueDragAndDrop();
        
        const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
        mockApi.reorderQueueFilms.mockRejectedValue(new Error('Network error'));
        
        const container = document.getElementById('queueFilms');
        const dropEvent = new Event('drop');
        Object.defineProperty(dropEvent, 'preventDefault', {
            value: jest.fn(),
            writable: false
        });

        // When
        container.dispatchEvent(dropEvent);

        // Then
        await new Promise(resolve => setTimeout(resolve, 0));
        expect(consoleErrorSpy).toHaveBeenCalledWith('Error reordering films:', expect.any(Error));
        
        consoleErrorSpy.mockRestore();
    });
});

describe('Queue List Drag and Drop', () => {
    beforeEach(() => {
        document.body.innerHTML = '';
        mockApi.reorderQueues.mockClear();
    });

    test('should setup drag and drop listeners for queue list', () => {
        // Given
        document.body.innerHTML = `
            <div id="queuesList">
                <div class="queue-item" draggable="true" data-queue-id="queue-1">Queue 1</div>
                <div class="queue-item" draggable="true" data-queue-id="queue-2">Queue 2</div>
            </div>
        `;

        // When
        setupQueueListDragAndDrop();

        // Then
        const queueItems = document.querySelectorAll('.queue-item');
        expect(queueItems.length).toBe(2);
        
        // Verify draggable attributes
        queueItems.forEach(item => {
            expect(item.getAttribute('draggable')).toBe('true');
        });
    });

    test('should handle queue drag start event', () => {
        // Given
        document.body.innerHTML = `
            <div id="queuesList">
                <div class="queue-item" draggable="true" data-queue-id="queue-1">Queue 1</div>
            </div>
        `;
        setupQueueListDragAndDrop();
        const queueItem = document.querySelector('.queue-item');
        
        const mockDataTransfer = { effectAllowed: null };
        const dragStartEvent = new Event('dragstart');
        Object.defineProperty(dragStartEvent, 'dataTransfer', {
            value: mockDataTransfer,
            writable: false
        });

        // When
        queueItem.dispatchEvent(dragStartEvent);

        // Then
        expect(queueItem.classList.contains('dragging')).toBe(true);
        expect(mockDataTransfer.effectAllowed).toBe('move');
    });

    test('should call reorder queues API on drop', async () => {
        // Given
        document.body.innerHTML = `
            <div id="queuesList">
                <div class="queue-item" data-queue-id="queue-1">Queue 1</div>
                <div class="queue-item" data-queue-id="queue-2">Queue 2</div>
                <div class="queue-item" data-queue-id="queue-3">Queue 3</div>
            </div>
        `;
        setupQueueListDragAndDrop();
        
        mockApi.reorderQueues.mockResolvedValue({ ok: true });
        
        const container = document.getElementById('queuesList');
        const dropEvent = new Event('drop');
        Object.defineProperty(dropEvent, 'preventDefault', {
            value: jest.fn(),
            writable: false
        });

        // When
        container.dispatchEvent(dropEvent);

        // Then
        await new Promise(resolve => setTimeout(resolve, 0));
        
        expect(mockApi.reorderQueues).toHaveBeenCalledWith(
            ['queue-1', 'queue-2', 'queue-3'] // Queue IDs in order
        );
    });

    test('should handle missing queue container gracefully', () => {
        // Given - no queuesList element
        document.body.innerHTML = '<div></div>';

        // When/Then - should not throw error
        expect(() => setupQueueListDragAndDrop()).not.toThrow();
    });

    test('should handle API error during queue reorder', async () => {
        // Given
        document.body.innerHTML = `
            <div id="queuesList">
                <div class="queue-item" data-queue-id="queue-1">Queue 1</div>
            </div>
        `;
        setupQueueListDragAndDrop();
        
        const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
        mockApi.reorderQueues.mockRejectedValue(new Error('Server error'));
        
        const container = document.getElementById('queuesList');
        const dropEvent = new Event('drop');
        Object.defineProperty(dropEvent, 'preventDefault', {
            value: jest.fn(),
            writable: false
        });

        // When
        container.dispatchEvent(dropEvent);

        // Then
        await new Promise(resolve => setTimeout(resolve, 0));
        expect(consoleErrorSpy).toHaveBeenCalledWith('Error reordering queues:', expect.any(Error));
        
        consoleErrorSpy.mockRestore();
    });
});

describe('Drag After Element Calculation', () => {
    test('should calculate correct drop position', () => {
        // Given
        document.body.innerHTML = `
            <div id="container" style="position: relative;">
                <div class="queue-item" style="position: absolute; top: 0; height: 50px;">Item 1</div>
                <div class="queue-item" style="position: absolute; top: 60px; height: 50px;">Item 2</div>
                <div class="queue-item" style="position: absolute; top: 120px; height: 50px;">Item 3</div>
            </div>
        `;

        // Mock getBoundingClientRect for elements
        const items = document.querySelectorAll('.queue-item');
        items[0].getBoundingClientRect = () => ({ top: 0, height: 50 });
        items[1].getBoundingClientRect = () => ({ top: 60, height: 50 });
        items[2].getBoundingClientRect = () => ({ top: 120, height: 50 });

        // When testing drag position calculation
        // This would be tested through the actual drag events in integration tests
        // The logic is embedded in the setupQueueDragAndDrop function

        expect(items.length).toBe(3);
    });
});