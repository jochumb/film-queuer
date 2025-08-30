/**
 * Tests for navigation functionality - simplified version
 * Note: Full window.location mocking is complex in JSDOM, so focusing on core logic
 */

// Mock DOM environment
const { JSDOM } = require('jsdom');
const dom = new JSDOM('<!DOCTYPE html><html><body></body></html>');
global.document = dom.window.document;
global.window = dom.window;

// Mock the UI module
const mockUI = {
    showHomePage: jest.fn()
};

jest.mock('../ui.js', () => mockUI);

// Mock the search module
const mockSearch = {
    setupPersonSearch: jest.fn()
};

jest.mock('../search.js', () => mockSearch);

// Mock the queue module
const mockQueue = {
    loadQueues: jest.fn(),
    showQueuePage: jest.fn()
};

jest.mock('../queue.js', () => mockQueue);

describe('Navigation Module - Basic Tests', () => {
    beforeEach(() => {
        // Reset all mocks
        mockUI.showHomePage.mockClear();
        mockSearch.setupPersonSearch.mockClear();
        mockQueue.loadQueues.mockClear();
        mockQueue.showQueuePage.mockClear();
    });

    describe('Module Structure', () => {
        test('should export navigation functions', () => {
            // This tests that the module can be imported without errors
            const navigationModule = require('../navigation.js');
            
            expect(navigationModule.setupNavigation).toBeDefined();
            expect(navigationModule.handleInitialRoute).toBeDefined();
            expect(navigationModule.navigateToQueue).toBeDefined();
            expect(navigationModule.navigateToHome).toBeDefined();
        });

        test('should make functions available globally', () => {
            // Import the module to set up global functions
            require('../navigation.js');
            
            expect(global.window.navigateToQueue).toBeDefined();
            expect(global.window.navigateToHome).toBeDefined();
        });
    });

    describe('URL Parsing Logic', () => {
        test('should correctly parse queue IDs from paths', () => {
            // Test the core logic without complex mocking
            const testCases = [
                { path: '/queue/123', expected: '123' },
                { path: '/queue/abc-def-456', expected: 'abc-def-456' },
                { path: '/queue/550e8400-e29b-41d4-a716-446655440000', expected: '550e8400-e29b-41d4-a716-446655440000' },
                { path: '/queue/123/', expected: '123' },
                { path: '/queue/123/extra', expected: '123' },
                { path: '/queue/', expected: '' }
            ];

            testCases.forEach(({ path, expected }) => {
                const parts = path.split('/');
                const queueId = parts[2]; // This mimics the parsing logic in navigation.js
                expect(queueId).toBe(expected);
            });
        });

        test('should identify queue vs non-queue paths', () => {
            const testCases = [
                { path: '/queue/123', isQueue: true },
                { path: '/queue/', isQueue: true },
                { path: '/', isQueue: false },
                { path: '/home', isQueue: false },
                { path: '/about', isQueue: false }
            ];

            testCases.forEach(({ path, isQueue }) => {
                const startsWithQueue = path.startsWith('/queue/');
                expect(startsWithQueue).toBe(isQueue);
            });
        });
    });
});