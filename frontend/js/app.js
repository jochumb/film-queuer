import { api } from './api.js';
import { setupPersonSearch } from './search.js';
import { loadQueues } from './queue.js';
import { setupNavigation, handleInitialRoute } from './navigation.js';
import { notifications } from './notifications.js';

export function setupNamedQueueCreation() {
    const createQueueInput = document.getElementById('namedQueueInput');
    const createQueueButton = document.getElementById('createQueueButton');
    
    if (!createQueueInput || !createQueueButton) {
        return;
    }
    
    createQueueButton.addEventListener('click', createNamedQueue);
    createQueueInput.addEventListener('keypress', function(e) {
        if (e.key === 'Enter') {
            createNamedQueue();
        }
    });
    
    async function createNamedQueue() {
        const name = createQueueInput.value.trim();
        if (!name) {
            notifications.error('Please enter a queue name');
            return;
        }
        
        createQueueButton.disabled = true;
        createQueueButton.textContent = 'Creating...';
        
        try {
            const response = await api.createNamedQueue(name);
            
            if (response.ok) {
                const newQueue = await response.json();
                notifications.success(`Queue "${name}" created successfully!`);
                createQueueInput.value = '';
                
                // Reload the queues to show the new one
                await loadQueues();
            } else {
                const errorText = await response.text();
                notifications.error(`Failed to create queue: ${errorText}`);
            }
        } catch (error) {
            console.error('Error creating named queue:', error);
            notifications.error('Failed to create queue. Please try again.');
        } finally {
            createQueueButton.disabled = false;
            createQueueButton.textContent = 'Create Queue';
        }
    }
}

document.addEventListener('DOMContentLoaded', function() {
    console.log('Film Queuer app initialized');
    
    api.testConnection();
    setupPersonSearch();
    setupNamedQueueCreation();
    loadQueues();
    setupNavigation();
    handleInitialRoute();
});