import React, {useCallback, useEffect, useState} from 'react';
import {
    ChevronLeft,
    LogOut,
    MoreVertical,
    Search,
    Shield,
    Trash2,
    User,
    UserMinus,
    UserPlus,
    Users as UsersIcon
} from 'lucide-react';
import {Link, useNavigate} from 'react-router-dom';
import axios from 'axios';
import api from '../services/api';
import {useAuth} from '../context/AuthContextValues';

interface TenantUser {
    id: string;
    username: string;
    roles: string[];
    status: 'ACTIVE' | 'INACTIVE';
}

export default function UserManagement() {
    const [users, setUsers] = useState<TenantUser[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [searchQuery, setSearchQuery] = useState('');
    const [isCreating, setIsCreating] = useState(false);
    const [newUsername, setNewUsername] = useState('');
    const [newPassword, setNewPassword] = useState('');
    const [actionLoading, setActionLoading] = useState<string | null>(null);
    const { logout, roles } = useAuth();
    const navigate = useNavigate();

    const isAdmin = roles.includes('ADMIN');

    const fetchUsers = useCallback(async () => {
        try {
            setLoading(true);
            const response = await api.get('/tenants/users');
            setUsers(response.data);
            setError(null);
        } catch (err: unknown) {
            console.error('Failed to fetch users:', err);
            if (axios.isAxiosError(err)) {
                setError(err.response?.data?.message || 'Failed to load users');
                if (err.response?.status === 403) {
                    navigate('/chat');
                }
            } else {
                setError('Failed to load users');
            }
        } finally {
            setLoading(false);
        }
    }, [navigate]);

    useEffect(() => {
        if (!isAdmin) {
            navigate('/chat');
            return;
        }
        fetchUsers();
    }, [fetchUsers, isAdmin, navigate]);

    const handleCreateUser = async (e: React.FormEvent) => {
        e.preventDefault();
        setActionLoading('create');
        try {
            await api.post('/tenants/users', {
                username: newUsername,
                password: newPassword
            });
            setNewPassword('');
            setIsCreating(false);
            fetchUsers();
        } catch (err: unknown) {
            if (axios.isAxiosError(err)) {
                alert(err.response?.data?.message || 'Failed to create user');
            } else {
                alert('Failed to create user');
            }
        } finally {
            setActionLoading(null);
        }
    };

    const toggleUserStatus = async (user: TenantUser) => {
        const newStatus = user.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
        setActionLoading(user.id);
        try {
            await api.put(`/tenants/users/${user.id}`, {
                status: newStatus
            });
            setUsers(prev => prev.map(u =>
                u.id === user.id ? { ...u, status: newStatus } : u
            ));
        } catch {
            alert('Failed to update user status');
        } finally {
            setActionLoading(null);
        }
    };

    const handleDeleteUser = async (userId: string) => {
        if (!window.confirm('Are you sure? This will delete the user and all their context/documents permanently.')) return;

        setActionLoading(userId);
        try {
            await api.delete(`/tenants/users/${userId}`);
            setUsers(prev => prev.filter(u => u.id !== userId));
        } catch {
            alert('Failed to delete user');
        } finally {
            setActionLoading(null);
        }
    };

    const [editingUser, setEditingUser] = useState<TenantUser | null>(null);
    const [editUsername, setEditUsername] = useState('');

    const filteredUsers = users.filter(user =>
        user.username.toLowerCase().includes(searchQuery.toLowerCase())
    );

    const handleEditUser = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!editingUser) return;
        setActionLoading(editingUser.id);
        try {
            await api.put(`/tenants/users/${editingUser.id}`, {
                username: editUsername
            });
            setUsers(prev => prev.map(u =>
                u.id === editingUser.id ? { ...u, username: editUsername } : u
            ));
            setEditingUser(null);
        } catch {
            alert('Failed to update username');
        } finally {
            setActionLoading(null);
        }
    };

    return (
        <div className="flex h-screen bg-gray-900 text-white overflow-hidden">
            {/* Sidebar */}
            <div className="w-64 bg-gray-900 border-r border-gray-800 flex flex-col">
                <div className="p-4 border-b border-gray-800">
                    <Link to="/chat" className="flex items-center space-x-2 text-gray-300 hover:text-white transition-colors">
                        <ChevronLeft className="w-5 h-5" />
                        <span className="font-semibold">Back to Chat</span>
                    </Link>
                </div>

                <div className="flex-1 p-4">
                    <div className="flex items-center space-x-3 text-blue-400 mb-8 px-2">
                        <UsersIcon className="w-6 h-6" />
                        <h2 className="text-xl font-bold">User Management</h2>
                    </div>

                    <nav className="space-y-1">
                        <div className="px-3 py-2 text-xs font-semibold text-gray-500 uppercase tracking-wider">
                            Tenant Admin
                        </div>
                        <button
                            onClick={fetchUsers}
                            className="w-full flex items-center space-x-3 text-gray-400 hover:text-white px-3 py-2 rounded-lg hover:bg-gray-800 transition-colors"
                        >
                            <User className="w-5 h-5" />
                            <span>All Users</span>
                        </button>
                    </nav>
                </div>

                <div className="p-4 border-t border-gray-800">
                    <button
                        onClick={logout}
                        className="flex items-center space-x-3 text-gray-400 hover:text-white w-full px-3 py-2 rounded-lg hover:bg-gray-800 transition-colors"
                    >
                        <LogOut className="w-5 h-5" />
                        <span>Sign out</span>
                    </button>
                </div>
            </div>

            {/* Main Content */}
            <div className="flex-1 flex flex-col bg-gray-900 overflow-y-auto custom-scrollbar">
                <header className="p-6 border-b border-gray-800 flex justify-between items-center bg-gray-900/50 backdrop-blur-md sticky top-0 z-10">
                    <div>
                        <h1 className="text-2xl font-bold text-white">Users</h1>
                        <p className="text-gray-400 text-sm">Manage your organization's users and their roles.</p>
                    </div>

                    <button
                        onClick={() => setIsCreating(true)}
                        className="bg-blue-600 hover:bg-blue-500 text-white px-4 py-2 rounded-lg flex items-center space-x-2 transition-all shadow-lg active:scale-95"
                    >
                        <UserPlus className="w-5 h-5" />
                        <span>Add User</span>
                    </button>
                </header>

                <div className="p-6 max-w-6xl mx-auto w-full">
                    {/* Search and Filter */}
                    <div className="mb-6 relative">
                        <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-500 w-5 h-5" />
                        <input
                            type="text"
                            placeholder="Search by username..."
                            value={searchQuery}
                            onChange={(e) => setSearchQuery(e.target.value)}
                            className="w-full bg-gray-800 border border-gray-700 rounded-xl py-3 pl-10 pr-4 text-white focus:outline-none focus:ring-2 focus:ring-blue-500/50 transition-all"
                        />
                    </div>

                    {/* Create User Form Overlay */}
                    {isCreating && (
                        <div className="mb-8 p-6 bg-gray-800 border border-blue-500/30 rounded-2xl shadow-2xl animate-in fade-in slide-in-from-top-4">
                            <h3 className="text-lg font-bold mb-4 flex items-center gap-2">
                                <UserPlus className="w-5 h-5 text-blue-400" />
                                Create New User
                            </h3>
                            <form onSubmit={handleCreateUser} className="grid grid-cols-1 md:grid-cols-3 gap-4">
                                <input
                                    type="text"
                                    placeholder="Username"
                                    required
                                    value={newUsername}
                                    onChange={(e) => setNewUsername(e.target.value)}
                                    className="bg-gray-700 border border-gray-600 rounded-lg px-4 py-2 text-white focus:ring-2 focus:ring-blue-500 outline-none"
                                />
                                <input
                                    type="password"
                                    placeholder="Password"
                                    required
                                    value={newPassword}
                                    onChange={(e) => setNewPassword(e.target.value)}
                                    className="bg-gray-700 border border-gray-600 rounded-lg px-4 py-2 text-white focus:ring-2 focus:ring-blue-500 outline-none"
                                />
                                <div className="flex gap-2">
                                    <button
                                        type="submit"
                                        disabled={actionLoading === 'create'}
                                        className="flex-1 bg-blue-600 hover:bg-blue-500 text-white rounded-lg font-medium transition-colors disabled:opacity-50"
                                    >
                                        {actionLoading === 'create' ? 'Saving...' : 'Create User'}
                                    </button>
                                    <button
                                        type="button"
                                        onClick={() => setIsCreating(false)}
                                        className="px-4 py-2 bg-gray-700 hover:bg-gray-600 text-gray-300 rounded-lg transition-colors"
                                    >
                                        Cancel
                                    </button>
                                </div>
                            </form>
                        </div>
                    )}

                    {/* Edit User Form Overlay */}
                    {editingUser && (
                        <div className="mb-8 p-6 bg-gray-800 border border-blue-500/30 rounded-2xl shadow-2xl animate-in fade-in slide-in-from-top-4">
                            <h3 className="text-lg font-bold mb-4 flex items-center gap-2">
                                <MoreVertical className="w-5 h-5 text-blue-400" />
                                Edit User: {editingUser.username}
                            </h3>
                            <form onSubmit={handleEditUser} className="grid grid-cols-1 md:grid-cols-2 gap-4">
                                <input
                                    type="text"
                                    placeholder="Username"
                                    required
                                    value={editUsername}
                                    onChange={(e) => setEditUsername(e.target.value)}
                                    className="bg-gray-700 border border-gray-600 rounded-lg px-4 py-2 text-white focus:ring-2 focus:ring-blue-500 outline-none"
                                />
                                <div className="flex gap-2">
                                    <button
                                        type="submit"
                                        disabled={actionLoading === editingUser.id}
                                        className="flex-1 bg-blue-600 hover:bg-blue-500 text-white rounded-lg font-medium transition-colors disabled:opacity-50"
                                    >
                                        {actionLoading === editingUser.id ? 'Saving...' : 'Update Username'}
                                    </button>
                                    <button
                                        type="button"
                                        onClick={() => setEditingUser(null)}
                                        className="px-4 py-2 bg-gray-700 hover:bg-gray-600 text-gray-300 rounded-lg transition-colors"
                                    >
                                        Cancel
                                    </button>
                                </div>
                            </form>
                        </div>
                    )}

                    {loading ? (
                        <div className="flex flex-col items-center justify-center py-20 text-gray-500">
                            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-500 mb-4"></div>
                            <p>Loading users...</p>
                        </div>
                    ) : error ? (
                        <div className="bg-red-900/20 border border-red-500/50 p-6 rounded-xl text-center">
                            <p className="text-red-400 mb-4">{error}</p>
                            <button onClick={fetchUsers} className="text-white bg-red-600 hover:bg-red-500 px-4 py-2 rounded-lg">Retry</button>
                        </div>
                    ) : filteredUsers.length === 0 ? (
                        <div className="text-center py-20 bg-gray-800/50 border border-gray-800 rounded-2xl border-dashed">
                            <UserMinus className="w-16 h-16 text-gray-600 mx-auto mb-4" />
                            <p className="text-gray-400">No users found.</p>
                        </div>
                    ) : (
                        <div className="grid grid-cols-1 gap-4">
                            {filteredUsers.map(user => (
                                <div
                                    key={user.id}
                                    className="bg-gray-800 hover:bg-gray-750 border border-gray-700 rounded-2xl p-5 flex items-center justify-between group transition-all"
                                >
                                    <div className="flex items-center space-x-4">
                                        <div className={`w-12 h-12 rounded-xl flex items-center justify-center ${user.status === 'ACTIVE' ? 'bg-blue-600/20 text-blue-400' : 'bg-gray-700 text-gray-500'}`}>
                                            <User className="w-6 h-6" />
                                        </div>
                                        <div>
                                            <div className="flex items-center gap-2">
                                                <h3 className="font-bold text-lg text-white">{user.username}</h3>
                                                {user.roles.includes('ADMIN') && (
                                                    <span className="bg-purple-900/30 text-purple-400 text-[10px] uppercase tracking-widest font-bold px-2 py-0.5 rounded-full border border-purple-500/20 flex items-center gap-1">
                                                        <Shield className="w-3 h-3" />
                                                        Admin
                                                    </span>
                                                )}
                                                <span className={`text-[10px] uppercase font-bold px-2 py-0.5 rounded-full ${user.status === 'ACTIVE'
                                                    ? 'bg-green-900/30 text-green-400 border border-green-500/20'
                                                    : 'bg-red-900/30 text-red-400 border border-red-500/20'
                                                    }`}>
                                                    {user.status}
                                                </span>
                                            </div>
                                            <p className="text-xs text-gray-500 mt-1">ID: {user.id}</p>
                                        </div>
                                    </div>

                                    <div className="flex items-center gap-3">
                                        <button
                                            onClick={() => {
                                                setEditingUser(user);
                                                setEditUsername(user.username);
                                            }}
                                            className="p-2 text-gray-400 hover:text-blue-400 hover:bg-blue-400/10 rounded-lg transition-all"
                                            title="Edit User"
                                        >
                                            <MoreVertical className="w-5 h-5" />
                                        </button>
                                        <button
                                            onClick={() => toggleUserStatus(user)}
                                            disabled={actionLoading === user.id}
                                            className={`px-4 py-2 rounded-lg text-sm font-medium transition-all ${user.status === 'ACTIVE'
                                                ? 'bg-gray-700 hover:bg-gray-600 text-gray-300'
                                                : 'bg-green-600 hover:bg-green-500 text-white'
                                                }`}
                                        >
                                            {actionLoading === user.id ? '...' : (user.status === 'ACTIVE' ? 'Deactivate' : 'Activate')}
                                        </button>

                                        <button
                                            onClick={() => handleDeleteUser(user.id)}
                                            disabled={actionLoading === user.id}
                                            className="p-2 text-gray-500 hover:text-red-400 hover:bg-red-400/10 rounded-lg transition-all"
                                            title="Delete User"
                                        >
                                            <Trash2 className="w-5 h-5" />
                                        </button>
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}
