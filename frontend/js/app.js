const API_BASE = 'http://localhost:8080/api';

document.addEventListener('DOMContentLoaded', function() {
    console.log('Film Queuer app initialized');
    
    testApiConnection();
    setupPersonSearch();
    loadSavedPersons();
});

async function testApiConnection() {
    try {
        const response = await fetch('http://localhost:8080/');
        const text = await response.text();
        console.log('Backend connection:', text);
    } catch (error) {
        console.error('Backend connection failed:', error);
    }
}

function setupPersonSearch() {
    const searchInput = document.getElementById('personSearch');
    const searchButton = document.getElementById('searchButton');
    const searchResults = document.getElementById('searchResults');

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
            const response = await fetch(`${API_BASE}/persons/search?q=${encodeURIComponent(query)}`);
            const data = await response.json();

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

async function selectPerson(tmdbId, name, department) {
    try {
        const response = await fetch(`${API_BASE}/persons/select`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                tmdbId: tmdbId,
                name: name,
                department: department
            })
        });

        if (response.ok) {
            alert(`${name} has been saved successfully!`);
            loadSavedPersons();
        } else {
            alert('Failed to save person. Please try again.');
        }
    } catch (error) {
        console.error('Error saving person:', error);
        alert('Failed to save person. Please try again.');
    }
}

async function loadSavedPersons() {
    try {
        const response = await fetch(`${API_BASE}/persons`);
        const persons = await response.json();
        displaySavedPersons(persons);
    } catch (error) {
        console.error('Error loading saved persons:', error);
    }
}

function displaySavedPersons(persons) {
    const savedPersonsContainer = document.getElementById('savedPersons');
    if (persons.length > 0) {
        savedPersonsContainer.innerHTML = `
            <h3>Saved Persons</h3>
            <div class="saved-persons-list">
                ${persons.map(person => `
                    <div class="saved-person-item">
                        <strong>${person.name}</strong> - ${person.department}
                    </div>
                `).join('')}
            </div>
        `;
    } else {
        savedPersonsContainer.innerHTML = '<h3>No saved persons yet</h3>';
    }
}