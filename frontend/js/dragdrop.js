import { api } from './api.js';

export function setupQueueDragAndDrop() {
    const queueItems = document.querySelectorAll('.queue-film-item');
    let draggedItem = null;

    queueItems.forEach(item => {
        item.addEventListener('dragstart', function(e) {
            draggedItem = this;
            this.classList.add('dragging');
            e.dataTransfer.effectAllowed = 'move';
        });

        item.addEventListener('dragend', function() {
            this.classList.remove('dragging');
            draggedItem = null;
        });

        item.addEventListener('dragover', function(e) {
            e.preventDefault();
            e.dataTransfer.dropEffect = 'move';
            
            const afterElement = getDragAfterElement(this.parentNode, e.clientY);
            if (afterElement == null) {
                this.parentNode.appendChild(draggedItem);
            } else {
                this.parentNode.insertBefore(draggedItem, afterElement);
            }
        });
    });

    // Handle drop event on container
    const queueContainer = document.getElementById('queueFilms');
    queueContainer.addEventListener('drop', function(e) {
        e.preventDefault();
        if (draggedItem) {
            // Get new order of film IDs
            const newOrder = Array.from(this.querySelectorAll('.queue-film-item'))
                .map(item => parseInt(item.dataset.filmTmdbId));
            
            // Send reorder request to backend
            reorderQueueFilms(newOrder);
        }
    });

    queueContainer.addEventListener('dragover', function(e) {
        e.preventDefault();
    });
}

function getDragAfterElement(container, y) {
    const draggableElements = [...container.querySelectorAll('.queue-film-item:not(.dragging)')];
    
    return draggableElements.reduce((closest, child) => {
        const box = child.getBoundingClientRect();
        const offset = y - box.top - box.height / 2;
        
        if (offset < 0 && offset > closest.offset) {
            return { offset: offset, element: child };
        } else {
            return closest;
        }
    }, { offset: Number.NEGATIVE_INFINITY }).element;
}

async function reorderQueueFilms(filmOrder) {
    const queueId = sessionStorage.getItem('currentQueueId');
    
    if (!queueId) {
        console.error('No queue selected');
        return;
    }

    try {
        const response = await api.reorderQueueFilms(queueId, filmOrder);

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        console.log('Films reordered successfully');
    } catch (error) {
        console.error('Error reordering films:', error);
        // Reload queue to reset to server state
        const { loadQueueFilms } = await import('./queue.js');
        loadQueueFilms(queueId);
    }
}