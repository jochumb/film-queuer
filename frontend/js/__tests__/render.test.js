/**
 * Tests for the pure HTML-template/helper functions in render.js
 */

const {
    roleLabel,
    yearOf,
    runtimeLabel,
    queueDisplayName,
    queueSubLabel,
    avatarHtml,
    renderQueuePreviews,
    renderPersonResults,
    renderRankList,
    renderQueueFilms,
    renderFilmGrid,
    renderDepartmentSelector,
} = require('../render.js');

function parse(html) {
    const container = document.createElement('div');
    container.innerHTML = html;
    return container;
}

describe('roleLabel', () => {
    test('translates known departments', () => {
        expect(roleLabel('ACTING')).toBe('Actor');
        expect(roleLabel('DIRECTING')).toBe('Director');
        expect(roleLabel('WRITING')).toBe('Writer');
        expect(roleLabel('OTHER')).toBe('Crew');
    });

    test('falls back to the raw value for unknown departments', () => {
        expect(roleLabel('PRODUCING')).toBe('PRODUCING');
    });

    test('returns empty string for missing department', () => {
        expect(roleLabel(null)).toBe('');
        expect(roleLabel(undefined)).toBe('');
    });
});

describe('yearOf', () => {
    test('extracts the year from an ISO date string', () => {
        expect(yearOf('1999-10-15')).toBe('1999');
    });

    test('returns N/A for missing dates', () => {
        expect(yearOf(null)).toBe('N/A');
        expect(yearOf(undefined)).toBe('N/A');
        expect(yearOf('')).toBe('N/A');
    });
});

describe('runtimeLabel', () => {
    test('formats sub-hour runtimes as minutes', () => {
        expect(runtimeLabel(45)).toBe('45m');
    });

    test('formats hour-plus runtimes as hours and minutes', () => {
        expect(runtimeLabel(139)).toBe('2h 19m');
        expect(runtimeLabel(120)).toBe('2h 0m');
    });

    test('returns an em dash for missing/zero runtime', () => {
        expect(runtimeLabel(0)).toBe('—');
        expect(runtimeLabel(null)).toBe('—');
        expect(runtimeLabel(undefined)).toBe('—');
    });
});

describe('queueDisplayName', () => {
    test('returns the person name for PERSON queues', () => {
        const queue = { type: 'PERSON', person: { name: 'Tom Hanks' } };
        expect(queueDisplayName(queue)).toBe('Tom Hanks');
    });

    test('falls back when a PERSON queue has no resolved person', () => {
        expect(queueDisplayName({ type: 'PERSON', person: null })).toBe('Unknown Person');
    });

    test('returns the name for NAMED queues', () => {
        expect(queueDisplayName({ type: 'NAMED', name: 'Weekend Watchlist' })).toBe('Weekend Watchlist');
    });

    test('falls back when a NAMED queue has no name', () => {
        expect(queueDisplayName({ type: 'NAMED', name: null })).toBe('Unnamed Queue');
    });

    test('falls back for unknown queue types', () => {
        expect(queueDisplayName({ type: 'UNKNOWN' })).toBe('Unknown Queue');
    });
});

describe('queueSubLabel', () => {
    test('shows the translated department for PERSON queues', () => {
        const queue = { type: 'PERSON', person: { department: 'DIRECTING' } };
        expect(queueSubLabel(queue)).toBe('Director');
    });

    test('shows the description for NAMED queues', () => {
        expect(queueSubLabel({ type: 'NAMED', description: 'My favorites' })).toBe('My favorites');
    });

    test('falls back to a generic label when a NAMED queue has no description', () => {
        expect(queueSubLabel({ type: 'NAMED', description: null })).toBe('Named queue');
    });
});

describe('avatarHtml', () => {
    test('renders an image for a PERSON queue with an image path', () => {
        const queue = { type: 'PERSON', person: { name: 'Tom Hanks', imagePath: 'https://example.com/tom.jpg' } };
        const dom = parse(avatarHtml(queue));
        const img = dom.querySelector('img');
        expect(img).not.toBeNull();
        expect(img.getAttribute('src')).toBe('https://example.com/tom.jpg');
    });

    test('renders an initial for a queue without an image', () => {
        const queue = { type: 'NAMED', name: 'Weekend Watchlist' };
        const dom = parse(avatarHtml(queue));
        expect(dom.querySelector('img')).toBeNull();
        expect(dom.querySelector('.avatar').textContent).toBe('W');
    });
});

describe('renderQueuePreviews', () => {
    test('renders an empty state when there are no queues', () => {
        const html = renderQueuePreviews([]);
        expect(html).toContain('empty-state');
        expect(html).toContain('No queues yet');
    });

    test('renders one table row per queue with its films as columns', () => {
        const previews = [
            {
                queue: { id: 'q1', type: 'NAMED', name: 'Weekend Watchlist', description: null },
                totalFilms: 2,
                films: [
                    { tmdbId: 550, title: 'Fight Club', releaseDate: '1999-10-15', runtime: 139, posterPath: null },
                    { tmdbId: 238, title: 'The Godfather', releaseDate: '1972-03-14', runtime: 175, posterPath: null },
                ],
            },
        ];

        const dom = parse(renderQueuePreviews(previews));
        const row = dom.querySelector('tr[data-nav-queue="q1"]');
        expect(row).not.toBeNull();
        expect(row.querySelector('.queue-card-title').textContent).toBe('Weekend Watchlist');
        expect(row.querySelector('.qo-count .badge').textContent).toBe('2');

        const filmCells = row.querySelectorAll('.mft-cell');
        expect(filmCells).toHaveLength(2);
        expect(filmCells[0].classList.contains('mft-cell-primary')).toBe(true);
        expect(filmCells[0].querySelector('.mft-title').textContent).toBe('Fight Club');
        expect(filmCells[0].querySelector('.mft-meta').textContent).toContain('1999');
        expect(filmCells[0].querySelector('.mft-meta').textContent).toContain('2h 19m');
        expect(filmCells[1].classList.contains('mft-cell-primary')).toBe(false);
    });

    test('renders a placeholder when a queue has no films yet', () => {
        const previews = [
            { queue: { id: 'q1', type: 'NAMED', name: 'Empty Queue' }, totalFilms: 0, films: [] },
        ];
        const html = renderQueuePreviews(previews);
        expect(html).toContain('No films added yet');
    });
});

describe('renderPersonResults', () => {
    test('renders a hint when there are no results', () => {
        expect(renderPersonResults([])).toContain('No results found');
    });

    test('renders a select button with the person data attributes', () => {
        const results = [
            { id: 123, name: "O'Brien", department: 'ACTING', profilePath: null, knownFor: ['Movie A', 'Movie B'] },
        ];
        const dom = parse(renderPersonResults(results));
        const btn = dom.querySelector('.select-person-btn');
        expect(btn.dataset.id).toBe('123');
        expect(btn.dataset.department).toBe('ACTING');
        expect(dom.querySelector('.result-sub').textContent).toContain('Actor');
        expect(dom.querySelector('.result-sub').textContent).toContain('Movie A');
    });
});

describe('renderRankList', () => {
    test('renders an empty state when there are no queues', () => {
        expect(renderRankList([])).toContain('empty-state');
    });

    test('numbers rows and pluralizes the film count badge', () => {
        const queues = [
            { id: 'q1', type: 'PERSON', person: { name: 'Tom Hanks', department: 'ACTING' }, filmCount: 1 },
            { id: 'q2', type: 'NAMED', name: 'Faves', description: null, filmCount: 5 },
        ];
        const dom = parse(renderRankList(queues));
        const rows = dom.querySelectorAll('.rank-row');
        expect(rows).toHaveLength(2);
        expect(rows[0].querySelector('.rank-num').textContent).toBe('1');
        expect(rows[0].querySelector('.badge-accent').textContent).toBe('1 film');
        expect(rows[1].querySelector('.rank-num').textContent).toBe('2');
        expect(rows[1].querySelector('.badge-accent').textContent).toBe('5 films');
    });

    test('wires up the delete button data attributes', () => {
        const queues = [{ id: 'q1', type: 'NAMED', name: 'Faves', filmCount: 0 }];
        const dom = parse(renderRankList(queues));
        const deleteBtn = dom.querySelector('.delete-queue-btn');
        expect(deleteBtn.dataset.deleteQueue).toBe('q1');
        expect(deleteBtn.dataset.queueName).toBe('Faves');
    });
});

describe('renderQueueFilms', () => {
    test('renders an empty-queue message when there are no films', () => {
        expect(renderQueueFilms([])).toContain('No films in this queue yet');
    });

    test('renders a row per film with year and runtime', () => {
        const films = [
            { tmdbId: 550, title: 'Fight Club', releaseDate: '1999-10-15', runtime: 139, posterPath: null },
        ];
        const dom = parse(renderQueueFilms(films));
        const row = dom.querySelector('.queue-film-row');
        expect(row.dataset.dragId).toBe('550');
        expect(row.querySelector('.qf-title').textContent).toBe('Fight Club');
        expect(row.querySelector('.qf-sub').textContent).toBe('1999 · 139m');
        expect(row.querySelector('.remove-film-btn').dataset.id).toBe('550');
    });
});

describe('renderFilmGrid', () => {
    test('renders nothing by default when there are no films', () => {
        expect(renderFilmGrid([], new Set())).toBe('');
    });

    test('renders a custom empty message when provided', () => {
        expect(renderFilmGrid([], new Set(), { emptyMessage: 'No movies found.' })).toContain('No movies found.');
    });

    test('marks films already in the queue as disabled', () => {
        const films = [{ id: 550, title: 'Fight Club', releaseDate: '1999-10-15', voteAverage: 8.4 }];
        const dom = parse(renderFilmGrid(films, new Set([550])));
        const btn = dom.querySelector('.add-tile-btn');
        expect(btn.hasAttribute('disabled')).toBe(true);
        expect(btn.textContent.trim()).toBe('In queue');
        expect(dom.querySelector('.queued-check')).not.toBeNull();
        expect(dom.querySelector('.film-tile').classList.contains('in-queue')).toBe(true);
    });

    test('leaves films not yet in the queue enabled', () => {
        const films = [{ id: 551, title: 'Se7en', releaseDate: '1995-09-22', voteAverage: 0 }];
        const dom = parse(renderFilmGrid(films, new Set()));
        const btn = dom.querySelector('.add-tile-btn');
        expect(btn.hasAttribute('disabled')).toBe(false);
        expect(btn.textContent.trim()).toBe('Add to queue');
        expect(dom.querySelector('.rating-chip')).toBeNull();
    });
});

describe('renderDepartmentSelector', () => {
    test('renders nothing when there is one or no department', () => {
        expect(renderDepartmentSelector([], 'ACTING')).toBe('');
        expect(renderDepartmentSelector(['ACTING'], 'ACTING')).toBe('');
    });

    test('renders a select with the current department pre-selected', () => {
        const dom = parse(renderDepartmentSelector(['ACTING', 'DIRECTING'], 'DIRECTING'));
        const select = dom.querySelector('#departmentSelect');
        expect(select).not.toBeNull();
        const selected = select.querySelector('option[selected]');
        expect(selected.value).toBe('DIRECTING');
        expect(selected.textContent).toBe('Director');
    });
});
