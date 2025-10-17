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
        // Clear saved credentials when logging out
        localStorage.removeItem('savedUsername');
        localStorage.removeItem('savedPassword');
        localStorage.removeItem('rememberMe');
        // Clear cookie
        document.cookie = 'accessToken=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT';
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
        console.log('AuthManager logout initiated...');
        const token = this.getAccessToken();
        if (token) {
            try {
                console.log('Calling logout API...');
                // Call logout API directly without fetchWithAuth to avoid 401 redirect
                const response = await fetch('/api/auth/logout', {
                    method: 'POST',
                    headers: {
                        'Authorization': `Bearer ${token}`,
                        'Content-Type': 'application/json'
                    }
                });
                
                console.log('Logout API response:', response.status);
            } catch (error) {
                console.error('Logout API error:', error);
            }
        }
        
        // Always clear tokens and redirect, even if API call fails
        this.clearTokens();
        console.log('Tokens cleared, redirecting to home...');
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
