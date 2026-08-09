/**
 * Tests for the toast/modal notification system.
 */

describe('NotificationSystem', () => {
    let notifications;

    beforeEach(() => {
        jest.useFakeTimers();
        document.body.innerHTML = '';
        jest.resetModules();
        ({ notifications } = require('../notifications.js'));
    });

    afterEach(() => {
        jest.useRealTimers();
    });

    describe('toasts', () => {
        test('creates a success toast with the right styling and icon', () => {
            const toast = notifications.success('Test success message');

            expect(toast.classList.contains('toast')).toBe(true);
            expect(toast.classList.contains('toast-success')).toBe(true);
            expect(toast.innerHTML).toContain('Test success message');
            expect(toast.innerHTML).toContain('✓');
        });

        test('creates an error toast with the right styling and icon', () => {
            const toast = notifications.error('Test error message');

            expect(toast.classList.contains('toast-error')).toBe(true);
            expect(toast.innerHTML).toContain('⚠');
        });

        test('creates a warning toast with the right styling and icon', () => {
            const toast = notifications.warning('Test warning message');
            expect(toast.classList.contains('toast-warning')).toBe(true);
        });

        test('creates an info toast with the right styling and icon', () => {
            const toast = notifications.info('Test info message');
            expect(toast.classList.contains('toast-info')).toBe(true);
            expect(toast.innerHTML).toContain('ℹ');
        });

        test('appends toasts to the toast container', () => {
            notifications.success('Message 1');
            notifications.error('Message 2');

            const container = document.querySelector('.toast-container');
            expect(container.children).toHaveLength(2);
        });

        test('removes a toast when its close button is clicked', () => {
            const toast = notifications.success('Test message');
            toast.querySelector('.toast-close').click();

            jest.advanceTimersByTime(300);

            expect(document.querySelector('.toast-container').children).toHaveLength(0);
        });

        test('auto-dismisses success toasts after the given duration', () => {
            notifications.success('Test message', 100);
            expect(document.querySelector('.toast-container').children).toHaveLength(1);

            jest.advanceTimersByTime(400);

            expect(document.querySelector('.toast-container').children).toHaveLength(0);
        });

        test('does not auto-dismiss error toasts', () => {
            notifications.error('Test error');

            jest.advanceTimersByTime(10000);

            expect(document.querySelector('.toast-container').children).toHaveLength(1);
        });
    });

    describe('confirm modal', () => {
        test('renders the given title, message and button labels', async () => {
            const confirmPromise = notifications.confirm('Test Title', 'Test message', 'OK', 'Cancel');

            const modal = document.querySelector('.modal-overlay');
            expect(modal.innerHTML).toContain('Test Title');
            expect(modal.innerHTML).toContain('Test message');
            expect(modal.innerHTML).toContain('OK');
            expect(modal.innerHTML).toContain('Cancel');

            modal.querySelector('.modal-cancel').click();
            await confirmPromise;
        });

        test('resolves true when the confirm button is clicked', async () => {
            const confirmPromise = notifications.confirm('Test', 'Message', 'OK', 'Cancel');
            document.querySelector('.modal-confirm').click();

            await expect(confirmPromise).resolves.toBe(true);
        });

        test('resolves false when the cancel button is clicked', async () => {
            const confirmPromise = notifications.confirm('Test', 'Message', 'OK', 'Cancel');
            document.querySelector('.modal-cancel').click();

            await expect(confirmPromise).resolves.toBe(false);
        });

        test('resolves false when clicking outside the dialog', async () => {
            const confirmPromise = notifications.confirm('Test', 'Message');
            document.querySelector('.modal-overlay').click();

            await expect(confirmPromise).resolves.toBe(false);
        });

        test('resolves false on the Escape key', async () => {
            const confirmPromise = notifications.confirm('Test', 'Message');
            document.dispatchEvent(new window.KeyboardEvent('keydown', { key: 'Escape' }));

            await expect(confirmPromise).resolves.toBe(false);
        });
    });

    describe('getIcon', () => {
        test('returns the icon for each known type, and info as the default', () => {
            expect(notifications.getIcon('success')).toBe('✓');
            expect(notifications.getIcon('error')).toBe('⚠');
            expect(notifications.getIcon('warning')).toBe('⚠');
            expect(notifications.getIcon('info')).toBe('ℹ');
            expect(notifications.getIcon('unknown')).toBe('ℹ');
        });
    });
});
