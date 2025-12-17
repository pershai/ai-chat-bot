import { useCallback, useEffect, useState } from 'react';
import { useDropzone } from 'react-dropzone';
import { AlertCircle, Calendar, CheckCircle, File as FileIcon, FileText, Loader, Upload, X } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import api from '../services/api';
import { useAuth } from '../context/AuthContextValues';

interface UploadedFile {
    file: File;
    id: string;
}

interface JobStatus {
    jobId: string;
    status: string;
    totalFiles: number;
    processedFiles: number;
}

interface Document {
    id: number;
    filename: string;
    fileType?: string;
    summary?: string;
    uploadDate: string;
}

export default function Documents() {
    const { userId } = useAuth();
    const [files, setFiles] = useState<UploadedFile[]>([]);
    const [uploading, setUploading] = useState(false);
    const [, setJobId] = useState<string | null>(null);
    const [jobStatus, setJobStatus] = useState<JobStatus | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState(false);

    // Documents list state
    const [documents, setDocuments] = useState<Document[]>([]);
    const [loadingDocs, setLoadingDocs] = useState(true);

    const navigate = useNavigate();

    const fetchDocuments = useCallback(async () => {
        if (!userId) return;
        try {
            const response = await api.get('/documents', {
                params: { userId }
            });
            setDocuments(response.data);
        } catch (err) {
            console.error('Failed to fetch documents', err);
        } finally {
            setLoadingDocs(false);
        }
    }, [userId]);

    useEffect(() => {
        if (userId) {
            fetchDocuments();
        }
    }, [fetchDocuments, userId]);

    const onDrop = useCallback((acceptedFiles: File[]) => {
        const newFiles = acceptedFiles.map(file => ({
            file,
            id: Math.random().toString(36).substr(2, 9)
        }));
        setFiles(prev => [...prev, ...newFiles]);
        setError(null);
    }, []);

    const { getRootProps, getInputProps, isDragActive } = useDropzone({
        onDrop,
        accept: {
            'application/pdf': ['.pdf']
        },
        maxSize: 10 * 1024 * 1024 // 10MB
    });

    const removeFile = (id: string) => {
        setFiles(prev => prev.filter(f => f.id !== id));
    };

    const handleUpload = async () => {
        if (files.length === 0) return;

        setUploading(true);
        setError(null);
        setSuccess(false);

        try {
            const formData = new FormData();
            files.forEach(({ file }) => formData.append('files', file));

            // Add userId from context
            if (userId) {
                formData.append('userId', userId);
            }

            const response = await api.post('/documents/ingest', formData, {
                headers: { 'Content-Type': 'multipart/form-data' }
            });

            const newJobId = response.data.jobId;
            setJobId(newJobId);

            // Poll job status
            const pollInterval = setInterval(async () => {
                try {
                    const statusResponse = await api.get(`/documents/status/${newJobId}`);
                    setJobStatus(statusResponse.data);

                    if (statusResponse.data.status === 'COMPLETED') {
                        clearInterval(pollInterval);
                        setSuccess(true);
                        setUploading(false);
                        setFiles([]);
                        fetchDocuments(); // Refresh list
                        setTimeout(() => setSuccess(false), 5000);
                    } else if (statusResponse.data.status === 'FAILED') {
                        clearInterval(pollInterval);
                        setError('Upload failed. Please try again.');
                        setUploading(false);
                    }
                } catch {
                    clearInterval(pollInterval);
                    setError('Failed to check upload status.');
                    setUploading(false);
                }
            }, 2000);

        } catch (err: unknown) {
            if (axios.isAxiosError(err)) {
                setError(err.response?.data?.message || 'Upload failed. Please try again.');
            } else {
                setError('Upload failed. Please try again.');
            }
            setUploading(false);
        }
    };


    const progress = jobStatus
        ? Math.round((jobStatus.processedFiles / jobStatus.totalFiles) * 100)
        : 0;

    return (
        <div className="flex h-screen bg-gray-900 text-white">
            {/* Main Content */}
            <div className="flex-1 flex flex-col">
                {/* Header */}
                <div className="p-4 bg-gray-800 border-b border-gray-700 flex items-center justify-between">
                    <h1 className="text-xl font-semibold text-gray-100">Document Management</h1>
                    <button
                        onClick={() => navigate('/chat')}
                        className="px-4 py-2 bg-gray-700 hover:bg-gray-600 rounded-lg transition-colors text-sm"
                    >
                        Back to Chat
                    </button>
                </div>

                {/* Content */}
                <div className="flex-1 overflow-y-auto p-8">
                    <div className="max-w-4xl mx-auto space-y-8">

                        {/* Upload Section */}
                        <section className="space-y-4">
                            <h2 className="text-lg font-medium text-gray-300">Upload New Documents</h2>
                            {/* Info Box */}
                            <div className="bg-blue-900/30 border border-blue-700 rounded-xl p-4">
                                <p className="text-blue-200 text-sm">
                                    📚 Upload PDF documents to enhance the AI's knowledge base.
                                    The AI will be able to answer questions based on your uploaded content.
                                </p>
                            </div>

                            {/* Dropzone */}
                            <div
                                {...getRootProps()}
                                className={`border-2 border-dashed rounded-xl p-12 text-center cursor-pointer transition-all ${isDragActive
                                    ? 'border-blue-500 bg-blue-900/20'
                                    : 'border-gray-700 hover:border-gray-600 hover:bg-gray-800/50'
                                    } `}
                            >
                                <input {...getInputProps()} />
                                <Upload className="w-16 h-16 mx-auto mb-4 text-gray-400" />
                                {isDragActive ? (
                                    <p className="text-blue-400 text-lg font-medium">Drop files here...</p>
                                ) : (
                                    <>
                                        <p className="text-gray-300 text-lg font-medium mb-2">
                                            Drag & drop PDF files here
                                        </p>
                                        <p className="text-gray-500 text-sm">
                                            or click to browse (max 10MB per file)
                                        </p>
                                    </>
                                )}
                            </div>

                            {/* File List */}
                            {files.length > 0 && (
                                <div className="bg-gray-800 rounded-xl p-6 border border-gray-700">
                                    <h3 className="text-lg font-semibold mb-4 flex items-center">
                                        <FileText className="w-5 h-5 mr-2" />
                                        Selected Files ({files.length})
                                    </h3>
                                    <div className="space-y-2">
                                        {files.map(({ file, id }) => (
                                            <div
                                                key={id}
                                                className="flex items-center justify-between bg-gray-700/50 rounded-lg p-3"
                                            >
                                                <div className="flex items-center space-x-3 flex-1 min-w-0">
                                                    <FileText className="w-5 h-5 text-blue-400 flex-shrink-0" />
                                                    <div className="flex-1 min-w-0">
                                                        <p className="text-sm text-gray-200 truncate">{file.name}</p>
                                                        <p className="text-xs text-gray-500">
                                                            {(file.size / 1024).toFixed(1)} KB • {file.type || 'Unknown'}
                                                        </p>
                                                        {/* Summary is not available on File object directly, assuming it would be added to UploadedFile if needed */}
                                                        {/* {file.summary && (
                                                            <p className="text-xs text-gray-400 mt-1 italic line-clamp-2">
                                                                {file.summary}
                                                            </p>
                                                        )} */}
                                                    </div>
                                                </div>
                                                <button
                                                    onClick={() => removeFile(id)}
                                                    disabled={uploading}
                                                    className="p-1 hover:bg-gray-600 rounded transition-colors disabled:opacity-50"
                                                >
                                                    <X className="w-4 h-4 text-gray-400" />
                                                </button>
                                            </div>
                                        ))}
                                    </div>

                                    {/* Upload Button */}
                                    <button
                                        onClick={handleUpload}
                                        disabled={uploading || files.length === 0}
                                        className="w-full mt-4 bg-blue-600 hover:bg-blue-500 disabled:bg-gray-700 disabled:cursor-not-allowed text-white font-medium py-3 px-6 rounded-lg transition-colors flex items-center justify-center space-x-2"
                                    >
                                        {uploading ? (
                                            <>
                                                <Loader className="w-5 h-5 animate-spin" />
                                                <span>Processing...</span>
                                            </>
                                        ) : (
                                            <>
                                                <Upload className="w-5 h-5" />
                                                <span>Upload Files</span>
                                            </>
                                        )}
                                    </button>
                                </div>
                            )}

                            {/* Progress */}
                            {uploading && jobStatus && (
                                <div className="bg-gray-800 rounded-xl p-6 border border-gray-700">
                                    <h3 className="text-lg font-semibold mb-4">Processing...</h3>
                                    <div className="space-y-3">
                                        <div className="flex justify-between text-sm text-gray-400">
                                            <span>Progress</span>
                                            <span>{jobStatus.processedFiles} / {jobStatus.totalFiles} files</span>
                                        </div>
                                        <div className="w-full bg-gray-700 rounded-full h-3 overflow-hidden">
                                            <div
                                                className="bg-blue-600 h-full transition-all duration-500 rounded-full"
                                                style={{ width: `${progress}% ` }}
                                            />
                                        </div>
                                        <p className="text-center text-lg font-medium text-blue-400">{progress}%</p>
                                    </div>
                                </div>
                            )}

                            {/* Success Message */}
                            {success && (
                                <div className="bg-green-900/30 border border-green-700 rounded-xl p-4 flex items-center space-x-3">
                                    <CheckCircle className="w-6 h-6 text-green-400 flex-shrink-0" />
                                    <div>
                                        <p className="text-green-200 font-medium">Upload successful!</p>
                                        <p className="text-green-300 text-sm">Your documents have been processed and added to the knowledge base.</p>
                                    </div>
                                </div>
                            )}

                            {/* Error Message */}
                            {error && (
                                <div className="bg-red-900/30 border border-red-700 rounded-xl p-4 flex items-center space-x-3">
                                    <AlertCircle className="w-6 h-6 text-red-400 flex-shrink-0" />
                                    <div>
                                        <p className="text-red-200 font-medium">Upload failed</p>
                                        <p className="text-red-300 text-sm">{error}</p>
                                    </div>
                                </div>
                            )}
                        </section>

                        <hr className="border-gray-700" />

                        {/* Your Documents Section */}
                        <section className="space-y-4">
                            <h2 className="text-lg font-medium text-gray-300">Your Documents</h2>

                            {loadingDocs ? (
                                <div className="flex justify-center py-8">
                                    <Loader className="w-8 h-8 text-blue-500 animate-spin" />
                                </div>
                            ) : documents.length === 0 ? (
                                <div className="text-center py-8 text-gray-500 bg-gray-800/50 rounded-xl border border-gray-700 border-dashed">
                                    <FileIcon className="w-12 h-12 mx-auto mb-3 opacity-50" />
                                    <p>No documents found.</p>
                                    <p className="text-sm">Upload some files to get started.</p>
                                </div>
                            ) : (
                                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                                    {documents.map((doc) => (
                                        <div key={doc.id} className="bg-gray-800 border border-gray-700 rounded-xl p-4 hover:border-gray-600 transition-colors">
                                            <div className="flex items-start justify-between mb-2">
                                                <div className="p-2 bg-blue-900/30 rounded-lg">
                                                    <FileText className="w-6 h-6 text-blue-400" />
                                                </div>
                                                <span className="text-xs px-2 py-1 bg-gray-700 rounded text-gray-300 font-mono">
                                                    {doc.fileType}
                                                </span>
                                            </div>
                                            <h3 className="font-medium text-gray-200 truncate mb-1" title={doc.filename}>
                                                {doc.filename}
                                            </h3>
                                            <div className="flex items-center text-xs text-gray-500 mt-2">
                                                <Calendar className="w-3 h-3 mr-1" />
                                                {new Date(doc.uploadDate).toLocaleDateString()}
                                            </div>
                                            {doc.summary && (
                                                <div className="mt-3 pt-3 border-t border-gray-700">
                                                    <p className="text-xs text-gray-400 italic line-clamp-3">
                                                        "{doc.summary}"
                                                    </p>
                                                </div>
                                            )}
                                        </div>
                                    ))}
                                </div>
                            )}
                        </section>
                    </div>
                </div>
            </div>
        </div>
    );
}
