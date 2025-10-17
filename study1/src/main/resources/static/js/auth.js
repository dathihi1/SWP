// Auth helper for JWT token management
class AuthManager {
    constructor() {
        this.baseUrl = this.getBaseUrl();
    }

    getBaseUrl() {
        const baseUrlMeta = document.querySelector('meta[name="base_url"]');
        return baseUrlMeta ? baseUrlMeta.content : '/';
    }

    getAccessToken() {
        return localStorage.getItem('accessToken');
    }

    getRefreshToken() {
        return localStorage.getItem('refreshToken');
    }

    setTokens(accessToken, refreshToken) {
        localStorage.setItem('accessToken', accessToken);
        localStorage.setItem('refreshToken', refreshToken);
    }

    clearTokens() {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
    }

    isAuthenticated() {
        return !!this.getAccessToken();
    }

    getAuthHeaders() {
        const token = this.getAccessToken();
        return token ? { 'Authorization': `Bearer ${token}` } : {};
    }

    async fetchWithAuth(url, options = {}) {
        const authHeaders = this.getAuthHeaders();
        const headers = {
            'Content-Type': 'application/json',
            ...authHeaders,
            ...options.headers
        };

        const response = await fetch(url, {
            ...options,
            headers
        });

        // If token is invalid (401), clear tokens and redirect to login
        if (response.status === 401) {
            this.clearTokens();
            window.location.href = '/login';
            return response;
        }

        return response;
    }

    async logout() {
        const token = this.getAccessToken();
        if (token) {
            try {
                await this.fetchWithAuth('/auth/logout', {
                    method: 'POST'
                });
            } catch (error) {
                console.error('Logout error:', error);
            }
        }
        
        this.clearTokens();
        window.location.href = '/';
    }

    getCurrentUser() {
        const token = this.getAccessToken();
        if (!token) return null;

        try {
            // Parse JWT token to get user info
            const payload = JSON.parse(atob(token.split('.')[1]));
            return {
                username: payload.sub,
                role: payload.role || 'USER',
                userId: payload.userId
            };
        } catch (error) {
            console.error('Error parsing token:', error);
            return null;
        }
    }
}

// Global auth manager instance
window.authManager = new AuthManager();

// Override fetch to automatically add auth headers
const originalFetch = window.fetch;
window.fetch = function(url, options = {}) {
    // Only add auth headers for same-origin requests
    if (typeof url === 'string' && (url.startsWith('/') || url.startsWith(window.location.origin))) {
        const authHeaders = window.authManager.getAuthHeaders();
        const headers = {
            ...options.headers,
            ...authHeaders
        };
        options.headers = headers;
    }
    return originalFetch.call(this, url, options);
};
