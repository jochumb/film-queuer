/**
 * Tests for the pure HTML-template/helper functions in render.js
 */

const {
    roleLabel,
    yearOf,
    runtimeLabel,
    esc,
    queueDisplayName,
    queueSubLabel,
    avatarHtml,
    renderQueuePreviews,
    renderPersonResults,
    renderRankList,
    renderQueueFilms,
    renderFilmGrid,
    renderDepartmentSelector,
    renderCollectionRows,
    renderPager,
    renderEditSortNameModal,
    renderEditSortTitleModal,
    renderLinkModal,
    renderLinkSearchResults,
} = require('../render.js');

function parse(html) {
    const container = document.createElement('div');
    container.innerHTML = html;
    return container;
}

function parseRows(html) {
    const table = document.createElement('table');
    table.innerHTML = `<tbody>${html}</tbody>`;
    return table.querySelector('tbody');
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

describe('esc', () => {
    test('HTML-escapes an apostrophe so it renders as a real apostrophe, not a literal backslash', () => {
        const escaped = esc("The Devil's Own");
        const container = document.createElement('div');
        container.innerHTML = `<span>${escaped}</span>`;
        expect(container.textContent).toBe("The Devil's Own");
        expect(escaped).not.toContain('\\');
    });

    test('escapes markup-significant characters so untrusted text cannot break out of an attribute or inject tags', () => {
        expect(esc('<script>alert(1)</script>')).toBe('&lt;script&gt;alert(1)&lt;/script&gt;');
        expect(esc('Tom & Jerry')).toBe('Tom &amp; Jerry');
        expect(esc('say "hi"')).toBe('say &quot;hi&quot;');
    });

    test('round-trips safely inside a double-quoted attribute', () => {
        const container = document.createElement('div');
        container.innerHTML = `<span data-title="${esc('Weird " Title')}"></span>`;
        expect(container.querySelector('span').dataset.title).toBe('Weird " Title');
    });

    test('treats null/undefined as an empty string', () => {
        expect(esc(null)).toBe('');
        expect(esc(undefined)).toBe('');
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
    test('renders an empty state when there are no queues and no random picks', () => {
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
        // A real queue's poster does have a mark-watched overlay button, scoped to that queue.
        expect(filmCells[0].querySelector('.mft-watched-btn').dataset.watchedQueue).toBe('q1');
    });

    test('renders a placeholder when a queue has no films yet', () => {
        const previews = [
            { queue: { id: 'q1', type: 'NAMED', name: 'Empty Queue' }, totalFilms: 0, films: [] },
        ];
        const html = renderQueuePreviews(previews);
        expect(html).toContain('No films added yet');
    });

    test('renders random picks as a "queue 0" row above the real queues, with a shuffle button and no watched overlay', () => {
        const previews = [
            { queue: { id: 'q1', type: 'NAMED', name: 'Weekend Watchlist' }, totalFilms: 1, films: [] },
        ];
        const randomPicks = [
            {
                id: 'ref-1',
                title: 'Se7en',
                filmTmdbId: 807,
                film: { tmdbId: 807, title: 'Se7en', releaseDate: '1995-09-22', runtime: 127, posterPath: '/se7en.jpg' },
            },
        ];

        const dom = parse(renderQueuePreviews(previews, randomPicks));
        const rows = dom.querySelectorAll('.queue-overview-table > tbody > tr');
        expect(rows).toHaveLength(2);

        const picksRow = rows[0];
        expect(picksRow.classList.contains('random-picks-row')).toBe(true);
        expect(picksRow.hasAttribute('data-nav-queue')).toBe(false);
        expect(picksRow.querySelector('.queue-card-title').textContent).toBe("Tonight's Picks");
        expect(picksRow.querySelector('.qo-count .badge').textContent).toBe('1');
        expect(picksRow.querySelector('.mft-title').textContent).toBe('Se7en');
        expect(picksRow.querySelector('.mft-watched-btn')).toBeNull();
        expect(picksRow.querySelector('[data-shuffle-picks]')).not.toBeNull();

        expect(rows[1].getAttribute('data-nav-queue')).toBe('q1');
    });

    test('skips unmatched random picks rather than rendering an empty tile for them', () => {
        const randomPicks = [
            { id: 'ref-1', title: 'Unmatched Film', filmTmdbId: null, film: null },
            {
                id: 'ref-2',
                title: 'Se7en',
                filmTmdbId: 807,
                film: { tmdbId: 807, title: 'Se7en', releaseDate: '1995-09-22', runtime: 127, posterPath: null },
            },
        ];
        const dom = parse(renderQueuePreviews([], randomPicks));
        const tiles = dom.querySelectorAll('.random-picks-row .mft-cell');
        expect(tiles).toHaveLength(1);
        expect(tiles[0].querySelector('.mft-title').textContent).toBe('Se7en');
    });

    test('still renders the table (not the empty state) when there are random picks but no real queues', () => {
        const randomPicks = [
            { id: 'ref-1', title: 'Se7en', filmTmdbId: 807, film: { tmdbId: 807, title: 'Se7en', posterPath: null } },
        ];
        const html = renderQueuePreviews([], randomPicks);
        expect(html).not.toContain('empty-state');
        expect(html).toContain('random-picks-row');
    });

    test('renders no random-picks row at all when randomPicks is omitted', () => {
        const previews = [
            { queue: { id: 'q1', type: 'NAMED', name: 'Weekend Watchlist' }, totalFilms: 0, films: [] },
        ];
        const dom = parse(renderQueuePreviews(previews));
        expect(dom.querySelector('.random-picks-row')).toBeNull();
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

describe('renderCollectionRows', () => {
    test('renders a placeholder row when there are no items', () => {
        const rows = parseRows(renderCollectionRows([]));
        expect(rows.textContent).toContain('No items found');
    });

    test('renders a matched item with poster, an editable director name, an editable title, and an enabled add-to-queue button', () => {
        const items = [{
            id: 'ref-1',
            title: 'Fight Club',
            year: 1999,
            filmTmdbId: 550,
            film: {
                tmdbId: 550,
                title: 'Fight Club',
                sortTitle: 'Fight Club',
                releaseDate: '1999-10-15',
                posterPath: '/poster.jpg',
                directors: [{ tmdbId: 7, name: 'David Fincher', sortName: 'Fincher' }],
            },
        }];
        const rows = parseRows(renderCollectionRows(items));
        const row = rows.querySelector('tr');
        expect(row.querySelector('.ct-title').textContent).toBe('Fight Club');
        expect(row.querySelector('.ct-year').textContent).toBe('1999');
        expect(row.querySelector('.ct-director').textContent).toBe('David Fincher');
        const directorEl = row.querySelector('.ct-director .director-name');
        expect(directorEl.dataset.directorId).toBe('7');
        expect(directorEl.dataset.directorSortName).toBe('Fincher');
        const titleEl = row.querySelector('.ct-title .editable-title');
        expect(titleEl.dataset.titleId).toBe('550');
        expect(titleEl.dataset.titleSort).toBe('Fight Club');
        expect(row.querySelector('.unmatched-icon')).toBeNull();
        expect(row.querySelector('.ct-thumb img').getAttribute('src')).toBe('/poster.jpg');
        expect(row.querySelector('.add-to-queue-btn').hasAttribute('disabled')).toBe(false);
        expect(row.querySelector('.row-menu-toggle')).not.toBeNull();
        expect(row.querySelector('.row-menu-dropdown .fix-match-btn')).not.toBeNull();
    });

    test('renders the sort title (not the display title) as the data attribute for editing', () => {
        const items = [{
            id: 'ref-4',
            title: 'The Godfather',
            year: 1972,
            filmTmdbId: 238,
            film: { tmdbId: 238, title: 'The Godfather', sortTitle: 'Godfather, The', releaseDate: '1972-03-14', posterPath: null },
        }];
        const rows = parseRows(renderCollectionRows(items));
        const titleEl = rows.querySelector('.ct-title .editable-title');
        expect(titleEl.textContent).toBe('The Godfather');
        expect(titleEl.dataset.titleSort).toBe('Godfather, The');
    });

    test('renders multiple directors joined by comma, each individually editable', () => {
        const items = [{
            id: 'ref-3',
            title: 'The Matrix',
            year: 1999,
            filmTmdbId: 603,
            film: {
                title: 'The Matrix',
                releaseDate: '1999-03-31',
                posterPath: null,
                directors: [
                    { tmdbId: 1, name: 'Lana Wachowski', sortName: 'Wachowski' },
                    { tmdbId: 2, name: 'Lilly Wachowski', sortName: 'Wachowski' },
                ],
            },
        }];
        const rows = parseRows(renderCollectionRows(items));
        const row = rows.querySelector('tr');
        expect(row.querySelector('.ct-director').textContent).toBe('Lana Wachowski, Lilly Wachowski');
        expect(row.querySelectorAll('.ct-director .director-name').length).toBe(2);
    });

    test('renders an unmatched item using the raw Letterboxd title/year with a disabled add-to-queue button', () => {
        const items = [{ id: 'ref-2', title: 'Some Obscure Film', year: 1975, filmTmdbId: null, film: null }];
        const rows = parseRows(renderCollectionRows(items));
        const row = rows.querySelector('tr');
        expect(row.querySelector('.ct-title').textContent).toContain('Some Obscure Film');
        expect(row.querySelector('.ct-title .editable-title')).toBeNull();
        expect(row.querySelector('.unmatched-icon')).not.toBeNull();
        expect(row.querySelector('.ct-director').textContent).toBe('—');
        expect(row.querySelector('.add-to-queue-btn').hasAttribute('disabled')).toBe(true);
        expect(row.querySelector('.fix-match-btn').dataset.id).toBe('ref-2');
    });

    test('renders a Remove button for a visible item and a Restore button for a removed one', () => {
        const items = [
            { id: 'ref-visible', title: 'Visible Film', year: 2000, filmTmdbId: null, film: null, removed: false },
            { id: 'ref-hidden', title: 'Hidden Film', year: 2001, filmTmdbId: null, film: null, removed: true },
        ];
        const rows = parseRows(renderCollectionRows(items)).querySelectorAll('tr');

        const visibleRow = rows[0];
        expect(visibleRow.querySelector('.remove-item-btn').dataset.id).toBe('ref-visible');
        expect(visibleRow.querySelector('.restore-item-btn')).toBeNull();

        const hiddenRow = rows[1];
        expect(hiddenRow.querySelector('.restore-item-btn').dataset.id).toBe('ref-hidden');
        expect(hiddenRow.querySelector('.remove-item-btn')).toBeNull();
    });
});

describe('renderPager', () => {
    test('disables Prev on the first page and enables Next when more pages remain', () => {
        const dom = parse(renderPager(0, 40, 100));
        expect(dom.querySelector('.pager-prev').hasAttribute('disabled')).toBe(true);
        expect(dom.querySelector('.pager-next').hasAttribute('disabled')).toBe(false);
        expect(dom.querySelector('.pager-info').textContent).toBe('1–40 of 100');
    });

    test('disables Next on the last page', () => {
        const dom = parse(renderPager(80, 40, 100));
        expect(dom.querySelector('.pager-prev').hasAttribute('disabled')).toBe(false);
        expect(dom.querySelector('.pager-next').hasAttribute('disabled')).toBe(true);
        expect(dom.querySelector('.pager-info').textContent).toBe('81–100 of 100');
    });

    test('shows a zero-based range and disables both buttons when there are no results', () => {
        const dom = parse(renderPager(0, 40, 0));
        expect(dom.querySelector('.pager-info').textContent).toBe('0–0 of 0');
        expect(dom.querySelector('.pager-prev').hasAttribute('disabled')).toBe(true);
        expect(dom.querySelector('.pager-next').hasAttribute('disabled')).toBe(true);
    });

    test('renders a page-jump input reflecting the current page and total pages', () => {
        const dom = parse(renderPager(80, 40, 100));
        const input = dom.querySelector('.pager-page-input');
        expect(input.value).toBe('3');
        expect(input.getAttribute('max')).toBe('3');
        expect(input.dataset.totalPages).toBe('3');
    });
});

describe('renderEditSortNameModal', () => {
    test('pre-fills the input with the current sort name', () => {
        const dom = parse(renderEditSortNameModal({ tmdbId: 7, name: 'David Fincher', sortName: 'Fincher' }));
        expect(dom.querySelector('.link-modal-source').textContent).toBe('David Fincher');
        expect(dom.querySelector('#sortNameInput').value).toBe('Fincher');
    });
});

describe('renderEditSortTitleModal', () => {
    test('pre-fills the input with the current sort title', () => {
        const dom = parse(renderEditSortTitleModal({ tmdbId: 238, title: 'The Godfather', sortTitle: 'Godfather, The' }));
        expect(dom.querySelector('.link-modal-source').textContent).toBe('The Godfather');
        expect(dom.querySelector('#sortTitleInput').value).toBe('Godfather, The');
    });
});

describe('renderLinkModal', () => {
    test('pre-fills the title and year, and defaults to the Movies tab', () => {
        const dom = parse(renderLinkModal({ id: 'ref-1', title: 'Chernobyl', year: 2019 }));
        expect(dom.querySelector('#linkSearchInput').value).toBe('Chernobyl');
        expect(dom.querySelector('#linkYearInput').value).toBe('2019');
        expect(dom.querySelector('[data-link-mode="movie"]').classList.contains('active')).toBe(true);
        expect(dom.querySelector('[data-link-mode="tv"]').classList.contains('active')).toBe(false);
    });

    test('leaves the year input blank when the item has no known year', () => {
        const dom = parse(renderLinkModal({ id: 'ref-2', title: 'Some Obscure Film', year: null }));
        expect(dom.querySelector('#linkYearInput').value).toBe('');
    });
});

describe('renderLinkSearchResults', () => {
    test('tags each result tile with its media type so the right link mode is used on selection', () => {
        const films = [
            { id: 87108, title: 'Chernobyl', releaseDate: '2019-05-06', posterPath: null, tv: true },
            { id: 550, title: 'Fight Club', releaseDate: '1999-10-15', posterPath: null, tv: false },
        ];
        const dom = parse(renderLinkSearchResults(films));
        const tiles = dom.querySelectorAll('.link-result-tile');
        expect(tiles[0].dataset.selectTv).toBe('1');
        expect(tiles[1].dataset.selectTv).toBe('0');
    });

    test('renders a message when there are no results', () => {
        const dom = parse(renderLinkSearchResults([]));
        expect(dom.textContent).toContain('No results');
    });
});
