import { createContext, useContext } from 'react';

export interface AuthContextType {
    isAuthenticated: boolean;
    userId: string | null;
    username: string | null;
    login: (token: string, refreshToken: string, id: string, user: string) => void;
    logout: () => void;
}

export const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const useAuth = () => {
    const context = useContext(AuthContext);
    if (context === undefined) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
};
