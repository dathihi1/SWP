// Auth helper for JWT token management
class AuthManager {
    constructor() {
        this.baseUrl = this.getBaseUrl();
        this._refreshInFlight = null;
        this.maxInactivityMinutes = 15;
        // Background proactive refresh every 60s
        try {
            setInterval(() => {
                // No-op if not authenticated
                if (!this.isAuthenticated()) return;
                if (this.shouldForceLogoutForInactivity(this.maxInactivityMinutes)) {
                    this.clearTokens();
                    try { window.location.href = '/login'; } catch (e) {}
                    return;
                }
                this.maybeRefreshByRefreshToken(5);
            }, 60_000);
        } catch (e) {
            // ignore in environments without timers
        }
    }

    getBaseUrl() {
        const baseUrlMeta = document.querySelector('meta[name="base_url"]');
        return baseUrlMeta ? baseUrlMeta.content : '/';
    }

    // Extract exp (epoch seconds) from JWT; returns null on failure
    getJwtExpSeconds(token) {
        try {
            const parts = token.split('.');
            if (parts.length !== 3) return null;
            const payload = JSON.parse(atob(parts[1]));
            return typeof payload.exp === 'number' ? payload.exp : null;
        } catch (e) {
            return null;
        }
    }

    getAccessToken() {
        // First try localStorage
        let token = localStorage.getItem('accessToken');
        if (token) {
            return token;
        }
        
        // If not found in localStorage, try to get from cookie
        const cookies = document.cookie.split(';');
        for (let cookie of cookies) {
            const [name, value] = cookie.trim().split('=');
            if (name === 'accessToken' && value) {
                return value;
            }
        }
        
        return null;
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

    // Record user activity timestamp (ms)
    updateLastActivity() {
        try { localStorage.setItem('lastActivityAt', String(Date.now())); } catch (e) {}
    }

    // Check if inactivity exceeds threshold minutes
    shouldForceLogoutForInactivity(thresholdMinutes = 15) {
        try {
            const v = localStorage.getItem('lastActivityAt');
            const last = v ? parseInt(v, 10) : null;
            if (!last || Number.isNaN(last)) return false; // if unknown, don't force
            const ms = Date.now() - last;
            return ms >= thresholdMinutes * 60 * 1000;
        } catch (e) {
            return false;
        }
    }

    // Proactively refresh using refresh token when it is close to expiring
    async maybeRefreshByRefreshToken(thresholdMinutes = 5) {
        // Respect inactivity auto-logout policy
        if (this.shouldForceLogoutForInactivity(this.maxInactivityMinutes)) {
            this.clearTokens();
            try { window.location.href = '/login'; } catch (e) {}
            return;
        }

        const refreshToken = this.getRefreshToken();
        if (!refreshToken) return;

        const exp = this.getJwtExpSeconds(refreshToken);
        const now = Math.floor(Date.now() / 1000);
        if (!exp) return; // cannot parse; skip

        const remaining = exp - now;
        if (remaining <= 0) {
            // already expired -> force logout
            this.clearTokens();
            try { window.location.href = '/login'; } catch (e) {}
            return;
        }

        if (remaining > thresholdMinutes * 60) return; // not near expiry

        // Single-flight to avoid parallel refresh calls
        if (this._refreshInFlight) {
            try { await this._refreshInFlight; } catch (e) {}
            return;
        }

        this._refreshInFlight = (async () => {
            const res = await fetch('/api/auth/refresh', {
                method: 'POST',
                headers: { 'Authorization': `Bearer ${refreshToken}` }
            });
            if (!res.ok) throw new Error('refresh failed');
            const data = await res.json();
            this.setTokens(data.accessToken, data.refreshToken);
        })();

        try {
            await this._refreshInFlight;
        } catch (e) {
            this.clearTokens();
            try { window.location.href = '/login'; } catch (err) {}
        } finally {
            this._refreshInFlight = null;
        }
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
window.fetch = async function(url, options = {}) {
    // Track activity at call time (covers user-initiated network actions)
    try { window.authManager.updateLastActivity(); } catch (e) {}

    // If inactivity exceeded, force logout without attempting refresh
    if (window.authManager.shouldForceLogoutForInactivity(window.authManager.maxInactivityMinutes)) {
        window.authManager.clearTokens();
        try { window.location.href = '/login'; } catch (e) {}
        // Still perform the call without auth if desired, but return early
        return originalFetch.call(this, url, options);
    }

    // Proactive refresh before making the request
    try { await window.authManager.maybeRefreshByRefreshToken(5); } catch (e) {}

    // Only add auth headers for same-origin requests
    if (typeof url === 'string' && (url.startsWith('/') || url.startsWith(window.location.origin))) {
        const authHeaders = window.authManager.getAuthHeaders();
        const headers = {
            ...(options.headers || {}),
            ...authHeaders
        };
        options.headers = headers;
    }

    let response = await originalFetch.call(this, url, options);

    // If unauthorized, check inactivity first; if within window, try one-off refresh then retry once
    if (response.status === 401) {
        // If user has been inactive too long, do not attempt refresh
        if (window.authManager.shouldForceLogoutForInactivity(window.authManager.maxInactivityMinutes)) {
            window.authManager.clearTokens();
            try { window.location.href = '/login'; } catch (e) {}
            return response;
        }
        try {
            await window.authManager.maybeRefreshByRefreshToken(60); // force attempt regardless of proximity
            // Reattach headers with potentially new access token
            if (typeof url === 'string' && (url.startsWith('/') || url.startsWith(window.location.origin))) {
                const retryAuthHeaders = window.authManager.getAuthHeaders();
                const retryHeaders = {
                    ...(options.headers || {}),
                    ...retryAuthHeaders
                };
                options.headers = retryHeaders;
            }
            response = await originalFetch.call(this, url, options);
        } catch (e) {
            // fall through to logout
        }

        if (response.status === 401) {
            window.authManager.clearTokens();
            try { window.location.href = '/login'; } catch (e) {}
        }
    }

    return response;
};
