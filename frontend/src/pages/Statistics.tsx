import React, {useEffect, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {Activity, BarChart3, FileText, MessageSquare, TrendingUp} from 'lucide-react';
import api from '../services/api';

interface Stats {
    totalUsers: number;
    totalConversations: number;
    totalMessages: number;
    totalDocuments: number;
    activeConversations24h: number;
}

export default function Statistics() {
    const [stats, setStats] = useState<Stats | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const navigate = useNavigate();

    useEffect(() => {
        fetchStatistics();
        // Auto-refresh every 30 seconds
        const interval = setInterval(fetchStatistics, 30000);
        return () => clearInterval(interval);
    }, []);

    const fetchStatistics = async () => {
        try {
            const userId = localStorage.getItem('userId');
            const url = userId ? `/statistics?userId=${userId}` : '/statistics';
            const response = await api.get(url);
            setStats(response.data);
            setError(null);
        } catch (err) {
            setError('Failed to load statistics');
        } finally {
            setLoading(false);
        }
    };

    const statCards = stats ? [
        {
            title: 'My Conversations',
            value: stats.totalUsers,
            icon: MessageSquare,
            color: 'green',
            bgColor: 'bg-green-900/30',
            borderColor: 'border-green-700',
            iconColor: 'text-green-400'
        },
        {
            title: 'My Messages',
            value: stats.totalConversations,
            icon: Activity,
            color: 'purple',
            bgColor: 'bg-purple-900/30',
            borderColor: 'border-purple-700',
            iconColor: 'text-purple-400'
        },
        {
            title: 'Total Messages',
            value: stats.totalMessages,
            icon: MessageSquare,
            color: 'blue',
            bgColor: 'bg-blue-900/30',
            borderColor: 'border-blue-700',
            iconColor: 'text-blue-400'
        },
        {
            title: 'Total Documents',
            value: stats.totalDocuments,
            icon: FileText,
            color: 'orange',
            bgColor: 'bg-orange-900/30',
            borderColor: 'border-orange-700',
            iconColor: 'text-orange-400'
        },
        {
            title: 'My Active (24h)',
            value: stats.activeConversations24h,
            icon: TrendingUp,
            color: 'cyan',
            bgColor: 'bg-cyan-900/30',
            borderColor: 'border-cyan-700',
            iconColor: 'text-cyan-400'
        }
    ] : [];

    return (
        <div className="flex h-screen bg-gray-900 text-white">
            {/* Main Content */}
            <div className="flex-1 flex flex-col">
                {/* Header */}
                <div className="p-4 bg-gray-800 border-b border-gray-700 flex items-center justify-between">
                    <div className="flex items-center space-x-3">
                        <BarChart3 className="w-6 h-6 text-blue-400" />
                        <h1 className="text-xl font-semibold text-gray-100">My Statistics</h1>
                    </div>
                    <button
                        onClick={() => navigate('/chat')}
                        className="px-4 py-2 bg-gray-700 hover:bg-gray-600 rounded-lg transition-colors text-sm"
                    >
                        Back to Chat
                    </button>
                </div>

                {/* Content */}
                <div className="flex-1 overflow-y-auto p-8">
                    <div className="max-w-6xl mx-auto">
                        {loading ? (
                            <div className="flex items-center justify-center h-64">
                                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-500"></div>
                            </div>
                        ) : error ? (
                            <div className="bg-red-900/30 border border-red-700 rounded-xl p-6 text-center">
                                <p className="text-red-200 text-lg">{error}</p>
                                <button
                                    onClick={fetchStatistics}
                                    className="mt-4 px-4 py-2 bg-red-600 hover:bg-red-500 rounded-lg transition-colors"
                                >
                                    Retry
                                </button>
                            </div>
                        ) : (
                            <>
                                {/* Info Banner */}
                                <div className="bg-blue-900/30 border border-blue-700 rounded-xl p-4 mb-8">
                                    <p className="text-blue-200 text-sm">
                                        📊 Your personal metrics • Auto-refreshes every 30 seconds
                                    </p>
                                </div>

                                {/* Statistics Cards */}
                                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                                    {statCards.map((stat, index) => {
                                        const Icon = stat.icon;
                                        return (
                                            <div
                                                key={index}
                                                className={`${stat.bgColor} border ${stat.borderColor} rounded-xl p-6 transition-all hover:scale-105 hover:shadow-xl`}
                                            >
                                                <div className="flex items-center justify-between mb-4">
                                                    <div className={`p-3 bg-gray-800/50 rounded-lg`}>
                                                        <Icon className={`w-6 h-6 ${stat.iconColor}`} />
                                                    </div>
                                                    <div className="text-right">
                                                        <p className="text-gray-400 text-sm font-medium">
                                                            {stat.title}
                                                        </p>
                                                        <p className={`text-3xl font-bold ${stat.iconColor} mt-1`}>
                                                            {stat.value.toLocaleString()}
                                                        </p>
                                                    </div>
                                                </div>
                                            </div>
                                        );
                                    })}
                                </div>

                                {/* Additional Info */}
                                <div className="mt-8 bg-gray-800 rounded-xl p-6 border border-gray-700">
                                    <h3 className="text-lg font-semibold mb-4 flex items-center">
                                        <Activity className="w-5 h-5 mr-2 text-blue-400" />
                                        My Activity
                                    </h3>
                                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                                        <div className="bg-gray-700/50 rounded-lg p-4">
                                            <p className="text-gray-400 text-sm">Avg Messages/Conversation</p>
                                            <p className="text-2xl font-bold text-white mt-1">
                                                {stats && stats.totalUsers > 0
                                                    ? (stats.totalConversations / stats.totalUsers).toFixed(1)
                                                    : '0'}
                                            </p>
                                        </div>
                                        <div className="bg-gray-700/50 rounded-lg p-4">
                                            <p className="text-gray-400 text-sm">% of Total Messages</p>
                                            <p className="text-2xl font-bold text-white mt-1">
                                                {stats && stats.totalMessages > 0
                                                    ? ((stats.totalConversations / stats.totalMessages) * 100).toFixed(1)
                                                    : '0'}%
                                            </p>
                                        </div>
                                        <div className="bg-gray-700/50 rounded-lg p-4">
                                            <p className="text-gray-400 text-sm">Activity Rate (24h)</p>
                                            <p className="text-2xl font-bold text-white mt-1">
                                                {stats && stats.totalUsers > 0
                                                    ? ((stats.activeConversations24h / stats.totalUsers) * 100).toFixed(0)
                                                    : '0'}%
                                            </p>
                                        </div>
                                    </div>
                                </div>
                            </>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
}
