import { showHomePage } from './ui.js';
import { setupPersonSearch } from './search.js';
import { loadQueues, showQueuePage } from './queue.js';

export function setupNavigation() {
    window.addEventListener('popstate', handleRouteChange);
}

export function handleInitialRoute() {
    const path = window.location.pathname;
    if (path.startsWith('/queue/')) {
        const queueId = path.split('/')[2];
        navigateToQueue(queueId, false); // false = don't push state
    } else {
        showHomePage(false);
    }
}

function handleRouteChange() {
    const path = window.location.pathname;
    if (path.startsWith('/queue/')) {
        const queueId = path.split('/')[2];
        showQueuePage(queueId);
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
    
    // Reset to main page HTML structure
    showHomePage();
    
    // Re-initialize the home page functionality
    setupPersonSearch();
    loadQueues();
}

// Make navigation functions available globally for onclick handlers
window.navigateToQueue = navigateToQueue;
window.navigateToHome = navigateToHome;