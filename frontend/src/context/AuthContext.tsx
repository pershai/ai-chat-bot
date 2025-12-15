import React, { useCallback, useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import api from '../services/api';
import { AuthContext } from './AuthContextValues';

export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
    const [isAuthenticated, setIsAuthenticated] = useState<boolean>(() => {
        return !!(localStorage.getItem('jwtToken') && localStorage.getItem('userId') && localStorage.getItem('username'));
    });
    const [userId, setUserId] = useState<string | null>(() => localStorage.getItem('userId'));
    const [username, setUsername] = useState<string | null>(() => localStorage.getItem('username'));

    const login = useCallback((token: string, refreshToken: string, id: string, user: string) => {
        localStorage.setItem('jwtToken', token);
        localStorage.setItem('refreshToken', refreshToken);
        localStorage.setItem('userId', id);
        localStorage.setItem('username', user);
        setIsAuthenticated(true);
        setUserId(id);
        setUsername(user);
    }, []);

    const logout = useCallback(() => {
        localStorage.removeItem('jwtToken');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('userId');
        localStorage.removeItem('username');
        setIsAuthenticated(false);
        setUserId(null);
        setUsername(null);
        // Redirect to login page upon logout
        window.location.href = '/login';
    }, []);

    // Set up Axios interceptor for 401 errors to trigger logout
    useEffect(() => {
        const interceptor = api.interceptors.response.use(
            (response) => response,
            (error) => {
                // If error is 401 and it's not a refresh token request trying to get new token
                // (which is handled by the api.ts interceptor itself)
                if (error.response?.status === 401 && !error.config._retry) {
                    console.log('401 Unauthorized detected by AuthContext, triggering logout...');
                    logout();
                }
                return Promise.reject(error);
            }
        );

        return () => {
            api.interceptors.response.eject(interceptor);
        };
    }, [logout]);

    return (
        <AuthContext.Provider value={{ isAuthenticated, userId, username, login, logout }}>
            {children}
        </AuthContext.Provider>
    );
};
