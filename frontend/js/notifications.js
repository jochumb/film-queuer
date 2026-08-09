class NotificationSystem {
    constructor() {
        this.container = document.createElement('div');
        this.container.className = 'toast-container';
        document.body.appendChild(this.container);

        this.modalContainer = document.createElement('div');
        document.body.appendChild(this.modalContainer);
    }

    showToast(message, type = 'info', duration = 4000) {
        const toast = document.createElement('div');
        toast.className = `toast toast-${type}`;
        toast.innerHTML = `
            <div class="toast-content">
                <span class="toast-icon">${this.getIcon(type)}</span>
                <span class="toast-message">${message}</span>
                <button class="toast-close">&times;</button>
            </div>
        `;
        toast.querySelector('.toast-close').onclick = () => this.removeToast(toast);

        this.container.appendChild(toast);
        setTimeout(() => toast.classList.add('toast-show'), 10);

        if (duration > 0) {
            setTimeout(() => {
                if (toast.parentNode) this.removeToast(toast);
            }, duration);
        }
        return toast;
    }

    removeToast(toast) {
        toast.classList.add('toast-hide');
        setTimeout(() => toast.remove(), 250);
    }

    getIcon(type) {
        return { success: '✓', error: '⚠', warning: '⚠', info: 'ℹ' }[type] || 'ℹ';
    }

    success(message, duration = 4000) { return this.showToast(message, 'success', duration); }
    error(message) { return this.showToast(message, 'error', 0); }
    warning(message, duration = 6000) { return this.showToast(message, 'warning', duration); }
    info(message, duration = 4000) { return this.showToast(message, 'info', duration); }

    confirm(title, message, confirmText = 'Confirm', cancelText = 'Cancel') {
        return new Promise((resolve) => {
            const modal = document.createElement('div');
            modal.className = 'modal-overlay';
            modal.innerHTML = `
                <div class="modal-dialog">
                    <div class="modal-header"><h3>${title}</h3></div>
                    <div class="modal-body"><p>${message}</p></div>
                    <div class="modal-footer">
                        <button class="btn modal-cancel">${cancelText}</button>
                        <button class="btn btn-danger modal-confirm">${confirmText}</button>
                    </div>
                </div>
            `;
            this.modalContainer.appendChild(modal);

            const finish = (result) => {
                modal.classList.add('modal-hide');
                setTimeout(() => modal.remove(), 250);
                resolve(result);
            };

            modal.querySelector('.modal-confirm').onclick = () => finish(true);
            modal.querySelector('.modal-cancel').onclick = () => finish(false);
            modal.onclick = (e) => { if (e.target === modal) finish(false); };

            const onKey = (e) => {
                if (e.key === 'Escape') {
                    document.removeEventListener('keydown', onKey);
                    finish(false);
                }
            };
            document.addEventListener('keydown', onKey);

            setTimeout(() => modal.classList.add('modal-show'), 10);
        });
    }
}

export const notifications = new NotificationSystem();
