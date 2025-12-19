import type { AxiosRequestConfig } from 'axios';
import axios from 'axios';

const api = axios.create({
    baseURL: '/api/v1',
    headers: {
        'Content-Type': 'application/json',
    },
});

let isRefreshing = false;
let failedQueue: { resolve: (value?: unknown) => void; reject: (reason?: unknown) => void; config: AxiosRequestConfig }[] = [];

const processQueue = (error: unknown | null, token: string | null = null) => {
    failedQueue.forEach(prom => {
        if (error) {
            prom.reject(error);
        } else {
            prom.resolve(token);
        }
    });
    failedQueue = [];
};

// Add JWT token to requests if available
api.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('jwtToken');
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

// Add a response interceptor for token refresh
api.interceptors.response.use(
    (response) => response,
    async (error) => {
        const originalRequest = error.config;

        // If the error status is 401 and not already retrying
        if (error.response?.status === 401 && !originalRequest._retry) {
            // Prevent infinite loop by marking the original request as retried
            originalRequest._retry = true;

            if (isRefreshing) {
                // If a refresh is already in progress, queue the original request
                return new Promise(function (resolve, reject) {
                    failedQueue.push({ resolve, reject, config: originalRequest });
                })
                    .then(token => {
                        originalRequest.headers.Authorization = 'Bearer ' + token;
                        return api(originalRequest);
                    })
                    .catch(err => {
                        return Promise.reject(err);
                    });
            }

            isRefreshing = true;

            const refreshToken = localStorage.getItem('refreshToken');

            if (refreshToken) {
                try {
                    // Call refresh token endpoint - use global axios to avoid interceptor loop
                    const response = await axios.post('/api/v1/auth/refresh-token', { refreshToken });
                    const { token: newAccessToken, refreshToken: newRefreshToken } = response.data;

                    localStorage.setItem('jwtToken', newAccessToken);
                    if (newRefreshToken) {
                        localStorage.setItem('refreshToken', newRefreshToken);
                    }

                    // Retry all requests in the queue with the new token
                    processQueue(null, newAccessToken);

                    // Retry the original failed request with the new token
                    originalRequest.headers.Authorization = 'Bearer ' + newAccessToken;

                    // If the URL already starts with baseURL, strip it to avoid doubling when calling api()
                    if (originalRequest.url && originalRequest.baseURL && originalRequest.url.startsWith(originalRequest.baseURL)) {
                        originalRequest.url = originalRequest.url.substring(originalRequest.baseURL.length);
                    }

                    return api(originalRequest);
                } catch (refreshError) {
                    // Refresh failed, clear tokens and redirect to login
                    localStorage.removeItem('jwtToken');
                    localStorage.removeItem('refreshToken');
                    localStorage.removeItem('userId');
                    localStorage.removeItem('username');
                    processQueue(refreshError); // Reject all queued requests
                    window.location.href = '/login'; // Redirect to login
                    return Promise.reject(refreshError);
                } finally {
                    isRefreshing = false;
                }
            } else {
                // No refresh token, clear tokens and redirect to login
                localStorage.removeItem('jwtToken');
                localStorage.removeItem('userId');
                localStorage.removeItem('username');
                window.location.href = '/login';
                return Promise.reject(error);
            }
        }

        return Promise.reject(error);
    }
);

export default api;
