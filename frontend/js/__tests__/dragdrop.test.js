/**
 * Tests for the generic drag-to-reorder helper.
 */

const { enableDragReorder } = require('../dragdrop.js');

/**
 * Builds a container with three stacked .item elements (ids a, b, c), each 50px
 * tall, with a mocked getBoundingClientRect so dragover math is deterministic
 * (jsdom does not perform real layout).
 */
function buildList(ids) {
    const container = document.createElement('div');
    container.id = 'list';
    ids.forEach((id, index) => {
        const item = document.createElement('div');
        item.className = 'item';
        item.dataset.dragId = id;
        item.textContent = id;
        item.getBoundingClientRect = () => ({ top: index * 50, height: 50, bottom: index * 50 + 50 });
        container.appendChild(item);
    });
    document.body.appendChild(container);
    return container;
}

function domOrder(container) {
    return [...container.querySelectorAll('.item')].map((el) => el.dataset.dragId);
}

describe('enableDragReorder', () => {
    afterEach(() => {
        document.body.innerHTML = '';
    });

    test('does nothing when the container is missing', () => {
        expect(() => enableDragReorder(null, '.item', jest.fn())).not.toThrow();
    });

    test('marks matching items as draggable', () => {
        const container = buildList(['a', 'b', 'c']);
        enableDragReorder(container, '.item', jest.fn());

        container.querySelectorAll('.item').forEach((item) => {
            expect(item.getAttribute('draggable')).toBe('true');
        });
    });

    test('adds the dragging class on dragstart and removes it on dragend', () => {
        const container = buildList(['a', 'b', 'c']);
        enableDragReorder(container, '.item', jest.fn());
        const itemA = container.querySelector('[data-drag-id="a"]');

        itemA.dispatchEvent(new Event('dragstart', { bubbles: true }));
        expect(itemA.classList.contains('dragging')).toBe(true);

        itemA.dispatchEvent(new Event('dragend', { bubbles: true }));
        expect(itemA.classList.contains('dragging')).toBe(false);
    });

    test('does not call onDrop when nothing is being dragged', () => {
        const container = buildList(['a', 'b', 'c']);
        const onDrop = jest.fn();
        enableDragReorder(container, '.item', onDrop);

        container.dispatchEvent(new Event('drop', { bubbles: true, cancelable: true }));

        expect(onDrop).not.toHaveBeenCalled();
    });

    test('calls onDrop with the current DOM order of data-drag-id values', () => {
        const container = buildList(['a', 'b', 'c']);
        const onDrop = jest.fn();
        enableDragReorder(container, '.item', onDrop);
        const itemA = container.querySelector('[data-drag-id="a"]');

        itemA.dispatchEvent(new Event('dragstart', { bubbles: true }));
        container.dispatchEvent(new Event('drop', { bubbles: true, cancelable: true }));

        expect(onDrop).toHaveBeenCalledWith(['a', 'b', 'c']);
    });

    test('dragover moves the dragged item before the item under the cursor', () => {
        const container = buildList(['a', 'b', 'c']);
        enableDragReorder(container, '.item', jest.fn());
        const itemA = container.querySelector('[data-drag-id="a"]');
        itemA.dispatchEvent(new Event('dragstart', { bubbles: true }));

        // y=110 is in the upper half of item "c" (top 100, height 50, midpoint 125)
        const dragOverEvent = new Event('dragover', { bubbles: true, cancelable: true });
        dragOverEvent.clientY = 110;
        container.dispatchEvent(dragOverEvent);

        expect(domOrder(container)).toEqual(['b', 'a', 'c']);
    });

    test('dragover appends the dragged item at the end when the cursor is past every item', () => {
        const container = buildList(['a', 'b', 'c']);
        enableDragReorder(container, '.item', jest.fn());
        const itemA = container.querySelector('[data-drag-id="a"]');
        itemA.dispatchEvent(new Event('dragstart', { bubbles: true }));

        const dragOverEvent = new Event('dragover', { bubbles: true, cancelable: true });
        dragOverEvent.clientY = 500;
        container.dispatchEvent(dragOverEvent);

        expect(domOrder(container)).toEqual(['b', 'c', 'a']);
    });
});
