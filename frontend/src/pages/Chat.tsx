import React, { useCallback, useEffect, useRef, useState } from 'react';
import { BarChart3, Bot, Check, Copy, FileText, LogOut, Menu, MessageSquare, Plus, Send, Trash2, User, X } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import ReactMarkdown from 'react-markdown';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { vscDarkPlus } from 'react-syntax-highlighter/dist/esm/styles/prism';
import axios from 'axios';
import api from '../services/api';
import { twMerge } from 'tailwind-merge';

interface Message {
    role: 'user' | 'assistant';
    content: string;
    isError?: boolean;
}

interface Conversation {
    id: string;
    title?: string;
}

interface CodeBlockProps {
    inline?: boolean;
    className?: string;
    children?: React.ReactNode;
}

const CodeBlock = ({ inline, className, children, ...props }: CodeBlockProps) => {
    const match = /language-(\w+)/.exec(className || '');
    const codeContent = String(children).replace(/\n$/, '');
    const [copied, setCopied] = useState(false);

    const handleCopy = () => {
        navigator.clipboard.writeText(codeContent);
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
    };

    return !inline && match ? (
        <div className="relative group mb-4 mt-2">
            <div className="absolute right-2 top-2 z-10">
                <button
                    onClick={handleCopy}
                    className="flex items-center gap-1 px-2 py-1 bg-gray-700 hover:bg-gray-600 rounded text-xs text-gray-300 transition-colors opacity-0 group-hover:opacity-100"
                    title="Copy code"
                >
                    {copied ? (
                        <>
                            <Check className="w-3 h-3" />
                            <span>Copied!</span>
                        </>
                    ) : (
                        <>
                            <Copy className="w-3 h-3" />
                            <span>Copy</span>
                        </>
                    )}
                </button>
            </div>
            <SyntaxHighlighter
                style={vscDarkPlus}
                language={match[1]}
                PreTag="div"
                className="rounded-lg !bg-gray-900 !mt-0"
                {...props}
            >
                {codeContent}
            </SyntaxHighlighter>
        </div>
    ) : (
        <code className="bg-gray-700 px-1.5 py-0.5 rounded text-sm font-mono text-green-400" {...props}>
            {children}
        </code>
    );
};

export default function Chat() {
    const [conversations, setConversations] = useState<Conversation[]>([]);
    const [activeConversationId, setActiveConversationId] = useState<string | null>(null);
    const [messages, setMessages] = useState<Message[]>([]);
    const [input, setInput] = useState('');
    const [sidebarOpen, setSidebarOpen] = useState(true);
    const [loading, setLoading] = useState(false);
    const messagesEndRef = useRef<HTMLDivElement>(null);
    const navigate = useNavigate();

    const fetchConversations = useCallback(async () => {
        try {
            const userId = localStorage.getItem('userId');
            if (!userId) {
                console.warn('No userId found, skipping conversation fetch');
                setConversations([]);
                return;
            }
            const response = await api.get(`/conversations?userId=${userId}`);

            if (Array.isArray(response.data)) {
                setConversations(response.data);
            } else {
                console.error('Invalid conversations response:', response.data);
                setConversations([]);
            }

            if (response.data.length > 0 && !activeConversationId) {
                // Optionally select first one
            }
        } catch (error) {
            console.error('Failed to fetch conversations', error);
            setConversations([]);
        }
    }, [activeConversationId]);

    const scrollToBottom = () => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    };

    useEffect(() => {
        scrollToBottom();
    }, [messages]);

    useEffect(() => {
        fetchConversations();
    }, [fetchConversations]);

    useEffect(() => {
        if (activeConversationId) {
            fetchMessages(activeConversationId);
        } else {
            setMessages([]);
        }
    }, [activeConversationId]);



    const fetchMessages = async (id: string) => {
        try {
            const response = await api.get(`/conversations/${id}/messages`);
            setMessages(response.data);
        } catch (error) {
            console.error('Failed to fetch messages', error);
        }
    };

    const handleNewChat = () => {
        setActiveConversationId(null);
        setMessages([]);
        setInput('');
    };

    const handleLogout = () => {
        localStorage.removeItem('userId');
        localStorage.removeItem('jwtToken');
        localStorage.removeItem('username');
        navigate('/login');
    };

    const deleteConversation = async (e: React.MouseEvent, id: string) => {
        e.stopPropagation();
        if (!window.confirm('Are you sure you want to delete this conversation?')) return;

        try {
            await api.delete(`/conversations/${id}`);
            setConversations(prev => prev.filter(c => c.id !== id));

            if (activeConversationId === id) {
                setActiveConversationId(null);
                setMessages([]);
                setInput('');
            }
        } catch (error) {
            console.error('Failed to delete conversation', error);
        }
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!input.trim() || loading) return;

        const userMessage = input.trim();
        setInput('');
        setMessages(prev => [...prev, { role: 'user', content: userMessage }]);
        setLoading(true);

        try {
            const payload = {
                message: userMessage,
                conversationId: activeConversationId
            };

            const response = await api.post('/chat', payload);

            console.log('Chat response:', response.data); // Debug log

            // Extract response message and conversationId from ChatResponseDto
            const aiResponse = response.data.response || response.data.message || 'No response received';
            const newConversationId = response.data.conversationId;

            // Ensure aiResponse is a string
            if (typeof aiResponse === 'string' && aiResponse.length > 0) {
                setMessages(prev => [...prev, { role: 'assistant', content: aiResponse }]);
            } else {
                console.error('Invalid AI response:', aiResponse);
                setMessages(prev => [...prev, { role: 'assistant', content: 'Received invalid response from server' }]);
            }

            // Update conversationId if this was a new conversation
            if (!activeConversationId && newConversationId) {
                setActiveConversationId(newConversationId);
                fetchConversations(); // Refresh conversation list
            }

        } catch (error: unknown) {
            console.error('Error sending message', error);

            let errorMessage = 'Sorry, something went wrong.';

            if (axios.isAxiosError(error)) {
                if (error.response?.status === 429) {
                    const retryAfter = error.response.data?.retryAfter || 'a few moments';
                    errorMessage = `⚠️ API Quota Exceeded\n\nYou've reached the API rate limit.\n\nPlease retry in: ${retryAfter}\n\nTo increase limits, visit:\nhttps://ai.google.dev/gemini-api/docs/rate-limits`;
                } else if (error.response?.data?.detail) {
                    errorMessage = error.response.data.detail;
                }
            }

            setMessages(prev => [...prev, {
                role: 'assistant',
                content: errorMessage,
                isError: true
            }]);
        } finally {
            setLoading(false);
        }
    };


    return (
        <div className="flex h-screen bg-gray-900 text-white overflow-hidden">
            {/* Sidebar - Collapsible */}
            <div className={twMerge(
                "bg-gray-900 border-r border-gray-800 flex flex-col transition-all duration-300 ease-in-out",
                sidebarOpen ? "w-64" : "w-0 border-r-0"
            )}>
                <div className={twMerge("transition-opacity duration-300", sidebarOpen ? "opacity-100" : "opacity-0 pointer-events-none")}>
                    <div className="p-4 border-b border-gray-800">
                        <button
                            onClick={handleNewChat}
                            className="w-full bg-gray-800 hover:bg-gray-700 text-white font-medium py-3 px-4 rounded-lg flex items-center justify-center space-x-2 transition-colors border border-gray-700"
                        >
                            <Plus className="w-5 h-5" />
                            <span>New Chat</span>
                        </button>
                    </div>

                    <div className="flex-1 overflow-y-auto p-2 space-y-1 custom-scrollbar" style={{ maxHeight: 'calc(100vh - 200px)' }}>
                        {(conversations || []).map((conv, idx) => {
                            // Generate preview from title or show "New Chat"
                            const getConversationPreview = () => {
                                if (conv.title && conv.title.trim() !== '' && conv.title !== 'New Chat') {
                                    // If there's a meaningful title, use it (truncate if too long)
                                    return conv.title.length > 40 ? conv.title.substring(0, 40) + '...' : conv.title;
                                }
                                // Fallback: Show conversation number
                                return `Chat ${idx + 1}`;
                            };

                            return (
                                <div
                                    key={conv.id || idx}
                                    onClick={() => setActiveConversationId(conv.id)}
                                    className={twMerge(
                                        "w-full text-left p-3 rounded-lg flex items-center space-x-3 hover:bg-gray-800 transition-colors group cursor-pointer",
                                        activeConversationId === conv.id ? "bg-gray-800 border-l-4 border-blue-500" : "border-l-4 border-transparent"
                                    )}
                                >
                                    <MessageSquare className="w-4 h-4 text-gray-400 flex-shrink-0" />
                                    <div className="flex-1 min-w-0">
                                        <div className="truncate text-sm text-gray-200 font-medium">
                                            {getConversationPreview()}
                                        </div>
                                    </div>
                                    <button
                                        onClick={(e) => deleteConversation(e, conv.id)}
                                        className="p-1 hover:bg-gray-700 rounded text-gray-400 hover:text-red-400 opacity-0 group-hover:opacity-100 transition-all"
                                        title="Delete chat"
                                    >
                                        <Trash2 className="w-4 h-4" />
                                    </button>
                                </div>
                            );
                        })}
                    </div>

                    <div className="p-4 border-t border-gray-800 space-y-2">
                        <Link
                            to="/statistics"
                            className="flex items-center space-x-3 text-gray-400 hover:text-white w-full px-2 py-2 rounded-lg hover:bg-gray-800 transition-colors"
                        >
                            <BarChart3 className="w-5 h-5" />
                            <span>Statistics</span>
                        </Link>
                        <Link
                            to="/documents"
                            className="flex items-center space-x-3 text-gray-400 hover:text-white w-full px-2 py-2 rounded-lg hover:bg-gray-800 transition-colors"
                        >
                            <FileText className="w-5 h-5" />
                            <span>Upload Documents</span>
                        </Link>
                        <button
                            onClick={handleLogout}
                            className="flex items-center space-x-3 text-gray-400 hover:text-white w-full px-2 py-2 rounded-lg hover:bg-gray-800 transition-colors"
                        >
                            <LogOut className="w-5 h-5" />
                            <span>Sign out</span>
                        </button>
                    </div>
                </div>
            </div>

            {/* Main Chat Area */}
            <div className="flex-1 flex flex-col bg-gray-900">
                {/* Header with sidebar toggle */}
                <div className="p-3 bg-gray-800 border-b border-gray-700 flex items-center">
                    <button
                        onClick={() => setSidebarOpen(!sidebarOpen)}
                        className="p-2 rounded-lg hover:bg-gray-700 transition-colors mr-3 text-gray-300 hover:text-white"
                        title={sidebarOpen ? "Hide sidebar" : "Show sidebar"}
                    >
                        {sidebarOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
                    </button>
                    <h1 className="text-lg font-semibold text-gray-100">
                        {activeConversationId
                            ? conversations.find(c => c.id === activeConversationId)?.title || 'New Chat'
                            : 'New Chat'}
                    </h1>
                </div>

                {/* Messages */}
                <div className="flex-1 overflow-y-auto p-4 md:p-6 custom-scrollbar space-y-6">
                    {messages.length === 0 ? (
                        <div className="h-full flex flex-col items-center justify-center text-gray-500 opacity-50">
                            <Bot className="w-16 h-16 mb-4" />
                            <p className="text-xl font-medium">How can I help you today?</p>
                        </div>
                    ) : (
                        messages.map((msg, idx) => (
                            <div
                                key={idx}
                                className={twMerge(
                                    "flex space-x-4 max-w-3xl mx-auto",
                                    msg.role === 'user' ? "justify-end" : "justify-start"
                                )}
                            >
                                <div
                                    className={twMerge(
                                        "flex-shrink-0 w-8 h-8 rounded-full flex items-center justify-center",
                                        msg.role === 'assistant' ? "bg-green-600" : "bg-blue-600 order-2 ml-4"
                                    )}
                                >
                                    {msg.role === 'assistant' ? <Bot className="w-5 h-5 text-white" /> : <User className="w-5 h-5 text-white" />}
                                </div>

                                <div
                                    className={twMerge(
                                        "p-4 rounded-2xl shadow-md text-sm md:text-base leading-relaxed max-w-[80%]",
                                        msg.isError
                                            ? "bg-red-900/30 border-2 border-red-500 text-red-100 rounded-tl-none"
                                            : msg.role === 'assistant'
                                                ? "bg-gray-800 text-gray-100 rounded-tl-none border border-gray-700 markdown-content"
                                                : "bg-blue-600 text-white rounded-tr-none"
                                    )}
                                >
                                    {msg.isError ? (
                                        // Error messages show as plain text with line breaks
                                        <div className="whitespace-pre-wrap">{msg.content}</div>
                                    ) : msg.role === 'assistant' ? (
                                        <ReactMarkdown
                                            components={{
                                                // Enhanced code blocks with syntax highlighting and copy button
                                                code: CodeBlock,
                                                // Styled headings
                                                h1: ({ ...props }) => <h1 className="text-2xl font-bold mb-3 mt-4 text-white border-b border-gray-700 pb-2" {...props} />,
                                                h2: ({ ...props }) => <h2 className="text-xl font-bold mb-2 mt-3 text-white" {...props} />,
                                                h3: ({ ...props }) => <h3 className="text-lg font-bold mb-2 mt-2 text-gray-200" {...props} />,
                                                // Styled lists
                                                ul: ({ ...props }) => <ul className="list-disc list-inside mb-3 space-y-1 ml-2" {...props} />,
                                                ol: ({ ...props }) => <ol className="list-decimal list-inside mb-3 space-y-1 ml-2" {...props} />,
                                                li: ({ ...props }) => <li className="ml-4 text-gray-200" {...props} />,
                                                // Styled paragraphs
                                                p: ({ ...props }) => <p className="mb-3 text-gray-200 leading-relaxed" {...props} />,
                                                // Styled bold text
                                                strong: ({ ...props }) => <strong className="font-bold text-white" {...props} />,
                                                // Styled links
                                                a: ({ ...props }) => <a className="text-blue-400 hover:text-blue-300 underline hover:no-underline transition-colors" target="_blank" rel="noopener noreferrer" {...props} />,
                                                // Styled blockquotes
                                                blockquote: ({ ...props }) => (
                                                    <blockquote className="border-l-4 border-blue-500 pl-4 py-2 my-3 bg-gray-800/50 italic text-gray-300" {...props} />
                                                ),
                                                // Styled tables
                                                table: ({ ...props }) => (
                                                    <div className="overflow-x-auto my-4">
                                                        <table className="min-w-full border-collapse border border-gray-700" {...props} />
                                                    </div>
                                                ),
                                                thead: ({ ...props }) => <thead className="bg-gray-800" {...props} />,
                                                tbody: ({ ...props }) => <tbody {...props} />,
                                                tr: ({ ...props }) => <tr className="border-b border-gray-700 hover:bg-gray-800/30" {...props} />,
                                                th: ({ ...props }) => <th className="px-4 py-2 text-left font-bold text-white border border-gray-700" {...props} />,
                                                td: ({ ...props }) => <td className="px-4 py-2 text-gray-200 border border-gray-700" {...props} />,
                                                // Styled images
                                                img: ({ ...props }) => (
                                                    <img className="max-w-full h-auto rounded-lg my-3 border border-gray-700" {...props} />
                                                ),
                                                // Horizontal rules
                                                hr: ({ ...props }) => <hr className="my-4 border-gray-700" {...props} />,
                                            }}
                                        >
                                            {msg.content}
                                        </ReactMarkdown>
                                    ) : (
                                        msg.content
                                    )}
                                </div>
                            </div>
                        ))
                    )}
                    <div ref={messagesEndRef} />
                </div>

                {/* Input Area */}
                <div className="p-4 border-t border-gray-800 bg-gray-900">
                    <form onSubmit={handleSubmit} className="relative max-w-4xl mx-auto">
                        <textarea
                            value={input}
                            onChange={(e) => setInput(e.target.value)}
                            onKeyDown={(e) => {
                                // Submit on Enter, new line on Shift+Enter
                                if (e.key === 'Enter' && !e.shiftKey) {
                                    e.preventDefault();
                                    handleSubmit(e);
                                }
                            }}
                            placeholder="Message... (Shift+Enter for new line)"
                            className="w-full bg-gray-800 text-white border border-gray-700 rounded-xl pl-4 pr-14 py-3.5 focus:outline-none focus:ring-2 focus:ring-blue-500/50 focus:border-blue-500 transition-all shadow-lg placeholder-gray-500 resize-none min-h-[56px] max-h-[200px] overflow-y-auto text-base"
                            rows={1}
                            disabled={loading}
                            style={{
                                height: 'auto',
                                minHeight: '56px'
                            }}
                            onInput={(e) => {
                                const target = e.target as HTMLTextAreaElement;
                                target.style.height = 'auto';
                                target.style.height = Math.min(target.scrollHeight, 200) + 'px';
                            }}
                        />
                        <button
                            type="submit"
                            disabled={!input.trim() || loading}
                            className="absolute right-3 bottom-3.5 p-2.5 bg-blue-600 text-white rounded-lg hover:bg-blue-500 disabled:opacity-50 disabled:cursor-not-allowed transition-colors shadow-md"
                        >
                            <Send className="w-5 h-5" />
                        </button>
                    </form>
                    <p className="text-center text-xs text-gray-600 mt-2">
                        AI can make mistakes. Consider checking important information.
                    </p>
                </div>
            </div>
        </div>
    );
}
