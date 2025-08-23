const API_BASE = 'http://backend:8080/api';

document.addEventListener('DOMContentLoaded', function() {
    console.log('Film Queuer app initialized');
    
    testApiConnection();
});

async function testApiConnection() {
    try {
        const response = await fetch('http://backend:8080/');
        const text = await response.text();
        console.log('Backend connection:', text);
    } catch (error) {
        console.error('Backend connection failed:', error);
    }
}