import { api } from './api.js';
import { setupPersonSearch } from './search.js';
import { loadQueues } from './queue.js';
import { setupNavigation, handleInitialRoute } from './navigation.js';

document.addEventListener('DOMContentLoaded', function() {
    console.log('Film Queuer app initialized');
    
    api.testConnection();
    setupPersonSearch();
    loadQueues();
    setupNavigation();
    handleInitialRoute();
});