import { api } from './api.js';
import { notifications } from './notifications.js';

const DEPARTMENT_ROLES = {
    'ACTING': 'Actor',
    'DIRECTING': 'Director', 
    'WRITING': 'Writer',
    'OTHER': 'Crew Member'
};

export function translateDepartmentToRole(department) {
    return DEPARTMENT_ROLES[department] || department;
}

export function setupPersonSearch() {
    const searchInput = document.getElementById('personSearch');
    const searchButton = document.getElementById('searchButton');
    const searchResults = document.getElementById('searchResults');

    if (!searchInput || !searchButton || !searchResults) {
        return;
    }

    searchButton.addEventListener('click', performSearch);
    searchInput.addEventListener('keypress', function(e) {
        if (e.key === 'Enter') {
            performSearch();
        }
    });

    async function performSearch() {
        const query = searchInput.value.trim();
        if (!query) return;

        searchButton.disabled = true;
        searchButton.textContent = 'Searching...';
        searchResults.innerHTML = '<p>Searching...</p>';

        try {
            const data = await api.searchPersons(query);

            if (data.results && data.results.length > 0) {
                displaySearchResults(data.results);
            } else {
                searchResults.innerHTML = '<p>No results found.</p>';
            }
        } catch (error) {
            console.error('Search failed:', error);
            searchResults.innerHTML = '<p>Search failed. Please try again.</p>';
        } finally {
            searchButton.disabled = false;
            searchButton.textContent = 'Search';
        }
    }

    function displaySearchResults(results) {
        searchResults.innerHTML = results.map(person => `
            <div class="person-card">
                <div class="person-image ${!person.profilePath ? 'no-image' : ''}">
                    ${person.profilePath ? `<img src="${person.profilePath}" alt="${person.name}">` : ''}
                </div>
                <div class="person-info">
                    <h3>${person.name}</h3>
                    ${person.department ? `<div class="person-department">${person.department}</div>` : ''}
                    ${person.knownFor.length > 0 ? `<div class="person-known-for">Known for: ${person.knownFor.slice(0, 3).join(', ')}</div>` : ''}
                    <button class="select-person-btn" onclick="selectPerson(${person.id}, '${person.name.replace(/'/g, "\\'")}', '${person.department || ''}')">
                        Select
                    </button>
                </div>
            </div>
        `).join('');
    }
}

export async function selectPerson(tmdbId, name, department) {
    try {
        const response = await api.selectPerson(tmdbId, name, department);

        if (response.ok) {
            notifications.success(`${name} has been saved successfully!`);
            
            // Clear search results and input
            const searchResults = document.getElementById('searchResults');
            const searchInput = document.getElementById('personSearch');
            if (searchResults) {
                searchResults.innerHTML = '';
            }
            if (searchInput) {
                searchInput.value = '';
            }
            
            // Import and call loadQueues dynamically to avoid circular dependency
            const { loadQueues } = await import('./queue.js');
            loadQueues();
        } else {
            notifications.error('Failed to save person. Please try again.');
        }
    } catch (error) {
        console.error('Error saving person:', error);
        notifications.error('Failed to save person. Please try again.');
    }
}

// Make selectPerson available globally for onclick handlers
window.selectPerson = selectPerson;