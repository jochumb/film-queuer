function getElementAfterPointer(container, itemSelector, y) {
    const items = [...container.querySelectorAll(`${itemSelector}:not(.dragging)`)];
    return items.reduce((closest, child) => {
        const box = child.getBoundingClientRect();
        const offset = y - box.top - box.height / 2;
        if (offset < 0 && offset > closest.offset) {
            return { offset, element: child };
        }
        return closest;
    }, { offset: Number.NEGATIVE_INFINITY }).element;
}

/**
 * Wires up drag-to-reorder within `container` for direct children matching `itemSelector`.
 * Each draggable item must carry `data-drag-id`. Calls `onDrop(newIdOrder)` once a drop completes.
 */
export function enableDragReorder(container, itemSelector, onDrop) {
    if (!container) return;
    let draggedItem = null;

    container.querySelectorAll(itemSelector).forEach((item) => {
        item.setAttribute('draggable', 'true');

        item.addEventListener('dragstart', () => {
            draggedItem = item;
            item.classList.add('dragging');
        });

        item.addEventListener('dragend', () => {
            item.classList.remove('dragging');
            draggedItem = null;
        });
    });

    container.addEventListener('dragover', (e) => {
        e.preventDefault();
        if (!draggedItem) return;
        const afterElement = getElementAfterPointer(container, itemSelector, e.clientY);
        if (afterElement == null) {
            container.appendChild(draggedItem);
        } else {
            container.insertBefore(draggedItem, afterElement);
        }
    });

    container.addEventListener('drop', (e) => {
        e.preventDefault();
        if (!draggedItem) return;
        const newOrder = [...container.querySelectorAll(itemSelector)].map((el) => el.dataset.dragId);
        onDrop(newOrder);
    });
}
