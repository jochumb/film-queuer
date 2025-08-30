class NotificationSystem {
    constructor() {
        this.container = null;
        this.modalContainer = null;
        this.init();
    }

    init() {
        // Create toast container
        this.container = document.createElement('div');
        this.container.id = 'toast-container';
        this.container.className = 'toast-container';
        document.body.appendChild(this.container);

        // Create modal container
        this.modalContainer = document.createElement('div');
        this.modalContainer.id = 'modal-container';
        this.modalContainer.className = 'modal-container';
        document.body.appendChild(this.modalContainer);
    }

    showToast(message, type = 'info', duration = 4000) {
        const toast = document.createElement('div');
        toast.className = `toast toast-${type}`;
        
        const icon = this.getIcon(type);
        toast.innerHTML = `
            <div class="toast-content">
                <span class="toast-icon">${icon}</span>
                <span class="toast-message">${message}</span>
                <button class="toast-close" onclick="this.parentElement.parentElement.remove()">×</button>
            </div>
        `;

        // Add toast to container
        this.container.appendChild(toast);

        // Animate in
        setTimeout(() => toast.classList.add('toast-show'), 10);

        // Auto remove for success and info messages
        if (type === 'success' || type === 'info') {
            setTimeout(() => {
                if (toast.parentNode) {
                    this.removeToast(toast);
                }
            }, duration);
        }

        return toast;
    }

    removeToast(toast) {
        toast.classList.add('toast-hide');
        setTimeout(() => {
            if (toast.parentNode) {
                toast.remove();
            }
        }, 300);
    }

    getIcon(type) {
        const icons = {
            success: '✓',
            error: '⚠',
            warning: '⚠',
            info: 'ℹ'
        };
        return icons[type] || icons.info;
    }

    // Toast convenience methods
    success(message, duration = 4000) {
        return this.showToast(message, 'success', duration);
    }

    error(message) {
        return this.showToast(message, 'error', 0); // Don't auto-dismiss errors
    }

    warning(message, duration = 6000) {
        return this.showToast(message, 'warning', duration);
    }

    info(message, duration = 4000) {
        return this.showToast(message, 'info', duration);
    }

    // Modal confirmation
    confirm(title, message, confirmText = 'Confirm', cancelText = 'Cancel') {
        return new Promise((resolve) => {
            const modal = document.createElement('div');
            modal.className = 'modal-overlay';
            modal.innerHTML = `
                <div class="modal-dialog">
                    <div class="modal-header">
                        <h3>${title}</h3>
                    </div>
                    <div class="modal-body">
                        <p>${message}</p>
                    </div>
                    <div class="modal-footer">
                        <button class="btn btn-secondary modal-cancel">${cancelText}</button>
                        <button class="btn btn-danger modal-confirm">${confirmText}</button>
                    </div>
                </div>
            `;

            this.modalContainer.appendChild(modal);

            // Handle clicks
            const handleClick = (result) => {
                modal.classList.add('modal-hide');
                setTimeout(() => {
                    if (modal.parentNode) {
                        modal.remove();
                    }
                }, 300);
                resolve(result);
            };

            modal.querySelector('.modal-confirm').onclick = () => handleClick(true);
            modal.querySelector('.modal-cancel').onclick = () => handleClick(false);
            
            // Close on overlay click
            modal.onclick = (e) => {
                if (e.target === modal) {
                    handleClick(false);
                }
            };

            // Close on Escape key
            const handleKeyPress = (e) => {
                if (e.key === 'Escape') {
                    document.removeEventListener('keydown', handleKeyPress);
                    handleClick(false);
                }
            };
            document.addEventListener('keydown', handleKeyPress);

            // Animate in
            setTimeout(() => modal.classList.add('modal-show'), 10);
        });
    }

    // Clear all notifications
    clearAll() {
        const toasts = this.container.querySelectorAll('.toast');
        toasts.forEach(toast => this.removeToast(toast));
    }
}

// Create global instance
const notifications = new NotificationSystem();

// Export for ES modules
export { notifications };

// Also make available globally for onclick handlers
window.notifications = notifications;