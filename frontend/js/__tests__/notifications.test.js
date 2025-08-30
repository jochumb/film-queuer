/**
 * Tests for the notification system
 */

// Mock DOM environment
const { JSDOM } = require('jsdom');
const dom = new JSDOM('<!DOCTYPE html><html><body></body></html>');
global.document = dom.window.document;
global.window = dom.window;

// Import the module to test
import { notifications } from '../notifications.js';

describe('NotificationSystem', () => {
    beforeEach(() => {
        // Clear any existing containers
        document.body.innerHTML = '';
        
        // Re-initialize the system
        notifications.init();
    });

    afterEach(() => {
        // Clean up
        notifications.clearAll();
    });

    describe('Toast Notifications', () => {
        test('should create success toast with correct styling', () => {
            // When
            const toast = notifications.success('Test success message');

            // Then
            expect(toast).toBeDefined();
            expect(toast.classList.contains('toast')).toBe(true);
            expect(toast.classList.contains('toast-success')).toBe(true);
            expect(toast.innerHTML).toContain('Test success message');
            expect(toast.innerHTML).toContain('✓'); // Success icon
        });

        test('should create error toast with correct styling', () => {
            // When
            const toast = notifications.error('Test error message');

            // Then
            expect(toast).toBeDefined();
            expect(toast.classList.contains('toast')).toBe(true);
            expect(toast.classList.contains('toast-error')).toBe(true);
            expect(toast.innerHTML).toContain('Test error message');
            expect(toast.innerHTML).toContain('⚠'); // Error icon
        });

        test('should create warning toast with correct styling', () => {
            // When
            const toast = notifications.warning('Test warning message');

            // Then
            expect(toast).toBeDefined();
            expect(toast.classList.contains('toast')).toBe(true);
            expect(toast.classList.contains('toast-warning')).toBe(true);
            expect(toast.innerHTML).toContain('Test warning message');
            expect(toast.innerHTML).toContain('⚠'); // Warning icon
        });

        test('should create info toast with correct styling', () => {
            // When
            const toast = notifications.info('Test info message');

            // Then
            expect(toast).toBeDefined();
            expect(toast.classList.contains('toast')).toBe(true);
            expect(toast.classList.contains('toast-info')).toBe(true);
            expect(toast.innerHTML).toContain('Test info message');
            expect(toast.innerHTML).toContain('ℹ'); // Info icon
        });

        test('should add toast to container', () => {
            // When
            notifications.success('Test message');

            // Then
            const container = document.getElementById('toast-container');
            expect(container).toBeDefined();
            expect(container.children.length).toBe(1);
        });

        test('should stack multiple toasts', () => {
            // When
            notifications.success('Message 1');
            notifications.error('Message 2');
            notifications.warning('Message 3');

            // Then
            const container = document.getElementById('toast-container');
            expect(container.children.length).toBe(3);
        });

        test('should remove toast when close button clicked', () => {
            // Given
            const toast = notifications.success('Test message');
            const closeButton = toast.querySelector('.toast-close');

            // When
            closeButton.click();

            // Then
            setTimeout(() => {
                const container = document.getElementById('toast-container');
                expect(container.children.length).toBe(0);
            }, 400); // Wait for animation
        });

        test('should auto-dismiss success toasts', (done) => {
            // Given
            const toast = notifications.success('Test message', 100); // 100ms duration

            // Then
            expect(document.getElementById('toast-container').children.length).toBe(1);
            
            setTimeout(() => {
                expect(document.getElementById('toast-container').children.length).toBe(0);
                done();
            }, 500);
        });

        test('should NOT auto-dismiss error toasts', (done) => {
            // Given
            notifications.error('Test error');

            // Then
            setTimeout(() => {
                expect(document.getElementById('toast-container').children.length).toBe(1);
                done();
            }, 1000);
        });
    });

    describe('Modal Confirmations', () => {
        test('should create modal with correct content', async () => {
            // When
            const confirmPromise = notifications.confirm('Test Title', 'Test message', 'OK', 'Cancel');

            // Then
            const modal = document.querySelector('.modal-overlay');
            expect(modal).toBeDefined();
            expect(modal.innerHTML).toContain('Test Title');
            expect(modal.innerHTML).toContain('Test message');
            expect(modal.innerHTML).toContain('OK');
            expect(modal.innerHTML).toContain('Cancel');

            // Clean up
            const cancelBtn = modal.querySelector('.modal-cancel');
            cancelBtn.click();
            await confirmPromise;
        });

        test('should resolve true when confirm button clicked', async () => {
            // When
            const confirmPromise = notifications.confirm('Test', 'Message', 'OK', 'Cancel');
            const modal = document.querySelector('.modal-overlay');
            const confirmBtn = modal.querySelector('.modal-confirm');
            
            confirmBtn.click();
            const result = await confirmPromise;

            // Then
            expect(result).toBe(true);
        });

        test('should resolve false when cancel button clicked', async () => {
            // When
            const confirmPromise = notifications.confirm('Test', 'Message', 'OK', 'Cancel');
            const modal = document.querySelector('.modal-overlay');
            const cancelBtn = modal.querySelector('.modal-cancel');
            
            cancelBtn.click();
            const result = await confirmPromise;

            // Then
            expect(result).toBe(false);
        });

        test('should resolve false when clicking outside modal', async () => {
            // When
            const confirmPromise = notifications.confirm('Test', 'Message');
            const modal = document.querySelector('.modal-overlay');
            
            // Simulate click on overlay (outside dialog)
            modal.click();
            const result = await confirmPromise;

            // Then
            expect(result).toBe(false);
        });

        test('should handle escape key', async () => {
            // When
            const confirmPromise = notifications.confirm('Test', 'Message');
            
            // Simulate escape key
            const escapeEvent = new KeyboardEvent('keydown', { key: 'Escape' });
            document.dispatchEvent(escapeEvent);
            
            const result = await confirmPromise;

            // Then
            expect(result).toBe(false);
        });
    });

    describe('Utility Methods', () => {
        test('should clear all toasts', () => {
            // Given
            notifications.success('Message 1');
            notifications.error('Message 2');
            notifications.warning('Message 3');

            // When
            notifications.clearAll();

            // Then
            setTimeout(() => {
                const container = document.getElementById('toast-container');
                expect(container.children.length).toBe(0);
            }, 400);
        });

        test('should get correct icon for each type', () => {
            const system = notifications;
            
            expect(system.getIcon('success')).toBe('✓');
            expect(system.getIcon('error')).toBe('⚠');
            expect(system.getIcon('warning')).toBe('⚠');
            expect(system.getIcon('info')).toBe('ℹ');
            expect(system.getIcon('unknown')).toBe('ℹ'); // Default
        });
    });
});