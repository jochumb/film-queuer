import { showHomePage, showManagePage } from './ui.js';
import { setupPersonSearch } from './search.js';
import { loadQueues, loadQueuePreviews, showQueuePage } from './queue.js';

export function setupNavigation() {
    window.addEventListener('popstate', handleRouteChange);
}

export function handleInitialRoute() {
    const path = window.location.pathname;
    if (path.startsWith('/queue/')) {
        const queueId = path.split('/')[2];
        navigateToQueue(queueId, false); // false = don't push state
    } else if (path === '/manage') {
        navigateToManage(false);
    } else {
        navigateToHome(false);
    }
}

function handleRouteChange() {
    const path = window.location.pathname;
    if (path.startsWith('/queue/')) {
        const queueId = path.split('/')[2];
        showQueuePage(queueId);
    } else if (path === '/manage') {
        navigateToManage(false);
    } else {
        navigateToHome(false);
    }
}

export function navigateToQueue(queueId, pushState = true) {
    if (pushState) {
        history.pushState({ page: 'queue', queueId }, 'Queue', `/queue/${queueId}`);
    }
    showQueuePage(queueId);
}

export function navigateToHome(pushState = true) {
    if (pushState) {
        history.pushState({ page: 'home' }, 'Film Queuer', '/');
    }
    
    // Show the new home page and load queue previews
    showHomePage();
    loadQueuePreviews();
}

export function navigateToManage(pushState = true) {
    if (pushState) {
        history.pushState({ page: 'manage' }, 'Queue Management', '/manage');
    }
    
    // Show the manage page and initialize functionality
    showManagePage();
    setupPersonSearch();
    loadQueues();
}

// Make navigation functions available globally for onclick handlers
window.navigateToQueue = navigateToQueue;
window.navigateToHome = navigateToHome;
window.navigateToManage = navigateToManage;
window.loadQueuePreviews = loadQueuePreviews;