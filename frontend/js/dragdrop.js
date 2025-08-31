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

export function setupQueueListDragAndDrop() {
    // Only setup drag-and-drop for priority queues (in the first column)
    const queueItems = document.querySelectorAll('.priority-queue-container .queue-item');
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
        });
    });

    // Handle drag and drop on the priority container
    const priorityContainer = document.querySelector('.priority-queue-container');
    if (priorityContainer) {
        // Set up drag and drop for all slots
        priorityContainer.querySelectorAll('.queue-slot').forEach(slot => {
            slot.addEventListener('dragover', function(e) {
                e.preventDefault();
                e.dataTransfer.dropEffect = 'move';
                
                if (draggedItem && this !== draggedItem.closest('.queue-slot')) {
                    this.classList.add('drag-target');
                    
                    // Move the dragged item to this slot during dragover for visual feedback
                    const draggedSlot = draggedItem.closest('.queue-slot');
                    const thisSlot = this;
                    
                    // Get the items currently in both slots
                    const thisItem = thisSlot.querySelector('.queue-item');
                    const draggedItemElement = draggedItem;
                    
                    // Clear both slots content
                    draggedSlot.innerHTML = '';
                    thisSlot.innerHTML = '';
                    
                    // Move dragged item to target slot
                    thisSlot.appendChild(draggedItemElement);
                    
                    // If there was an item in the target slot, move it to the dragged slot or create empty slot
                    if (thisItem && thisItem !== draggedItemElement) {
                        draggedSlot.appendChild(thisItem);
                    } else {
                        draggedSlot.innerHTML = '<div class="empty-queue-slot"><span class="empty-slot-text">Empty slot</span></div>';
                    }
                }
            });
            
            slot.addEventListener('dragleave', function(e) {
                // Only remove if not entering a child element
                if (!this.contains(e.relatedTarget)) {
                    this.classList.remove('drag-target');
                }
            });
        });
        
        priorityContainer.addEventListener('drop', function(e) {
            e.preventDefault();
            
            // Remove drag-target class from all slots
            this.querySelectorAll('.queue-slot').forEach(slot => {
                slot.classList.remove('drag-target');
            });
            
            if (draggedItem) {
                // Get new order of queue IDs based on current slot positions
                const newOrder = Array.from(this.querySelectorAll('.queue-slot'))
                    .map(slot => {
                        const queueItem = slot.querySelector('.queue-item');
                        return queueItem ? queueItem.dataset.queueId : null;
                    })
                    .filter(id => id !== null);
                
                // Send reorder request to backend
                reorderQueues(newOrder);
            }
        });

        priorityContainer.addEventListener('dragover', function(e) {
            e.preventDefault();
        });
    }
}

function getDragAfterElementQueue(container, y) {
    const draggableElements = [...container.querySelectorAll('.queue-item-with-rank')].filter(el => 
        !el.querySelector('.queue-item.dragging')
    );
    
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

async function reorderQueues(queueOrder) {
    try {
        const response = await api.reorderQueues(queueOrder);

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        console.log('Queues reordered successfully');
        
        // Reload queues to refresh the UI with correct rankings
        const { loadQueues } = await import('./queue.js');
        loadQueues();
    } catch (error) {
        console.error('Error reordering queues:', error);
        // Reload queues to reset to server state
        const { loadQueues } = await import('./queue.js');
        loadQueues();
    }
}