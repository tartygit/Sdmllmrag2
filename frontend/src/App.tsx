import React, { useState } from 'react';
import {
  FileText, Shield, Sparkles, Layout, Settings, AlertTriangle, CheckCircle,
  Search, Sun, Moon, LogIn, Upload, ArrowRight, Check, X, RefreshCw, BarChart2
} from 'lucide-react';

interface Document {
  id: number;
  documentCode: string;
  name: string;
  fileType: string;
  fileSize: number;
  status: string;
  sdlcPhase: number;
  version: number;
  owner: string;
  description: string;
  summary?: string;
  complianceScore?: number;
  missingSections?: string;
  qualityScore?: number;
}

export default function App() {
  const [darkMode, setDarkMode] = useState(false);
  const [isLoggedIn, setIsLoggedIn] = useState(true);
  const [role, setRole] = useState<'ADMIN' | 'MAKER' | 'CHECKER' | 'APPROVER'>('MAKER');
  const [username, setUsername] = useState('jules_maker');
  const [activeTab, setActiveTab] = useState<'dashboard' | 'sdlc' | 'documents' | 'rag' | 'admin'>('dashboard');
  const [selectedPhase, setSelectedPhase] = useState<number>(1);
  const [searchQuery, setSearchQuery] = useState('');

  const [chatQuery, setChatQuery] = useState('');
  const [chatMessages, setChatMessages] = useState<Array<{role: 'user' | 'ai', text: string}>>([
    { role: 'ai', text: 'Hello! I am your Local RAG Assistant. Ask me anything about your uploaded system architecture or compliance documents.' }
  ]);

  const [uploadFile, setUploadFile] = useState<string>('');
  const [uploadAppCode, setUploadAppCode] = useState('PAY');
  const [uploadPhase, setUploadPhase] = useState(1);
  const [uploadDesc, setUploadDesc] = useState('');

  const [documents, setDocuments] = useState<Document[]>([
    {
      id: 1,
      documentCode: "PAY-P101",
      name: "Payment Gateway Architecture Design.docx",
      fileType: "DOCX",
      fileSize: 412000,
      status: "APPROVED",
      sdlcPhase: 1,
      version: 1,
      owner: "jules_maker",
      description: "End-to-end payment service architecture design mapping routing topologies and security layers.",
      summary: "This document outlines the core specifications for the microservice integration routing to stripe and paypal merchants.",
      complianceScore: 94,
      missingSections: "None detected.",
      qualityScore: 96
    },
    {
      id: 2,
      documentCode: "CRM-P201",
      name: "Customer Portal API Contract.xlsx",
      fileType: "XLSX",
      fileSize: 185000,
      status: "PENDING_APPROVAL",
      sdlcPhase: 2,
      version: 1,
      owner: "jane_maker",
      description: "CRM interface endpoint contracts detailing query schemas and access tokens.",
      summary: "Detailed dataset tables detailing customer lookup schemas.",
      complianceScore: 78,
      missingSections: "Disaster Recovery topology mappings.",
      qualityScore: 82
    }
  ]);

  const toggleDarkMode = () => {
    setDarkMode(!darkMode);
    document.body.classList.toggle('dark-mode', !darkMode);
  };

  const handleUploadSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!uploadFile) return;

    const newDoc: Document = {
      id: documents.length + 1,
      documentCode: `${uploadAppCode}-P${100 + documents.length + 1}`,
      name: uploadFile,
      fileType: uploadFile.split('.').pop()?.toUpperCase() || 'TXT',
      fileSize: 120450,
      status: 'UPLOADED',
      sdlcPhase: uploadPhase,
      version: 1,
      owner: username,
      description: uploadDesc,
      summary: 'Analysing uploaded document using Local Ollama and Docling...',
      complianceScore: 85,
      missingSections: 'Calculated dynamically after ingestion completes.',
      qualityScore: 90
    };

    setDocuments([newDoc, ...documents]);
    setUploadFile('');
    setUploadDesc('');
    setActiveTab('documents');
  };

  const handleWorkflowAction = (id: number, newStatus: 'PENDING_APPROVAL' | 'APPROVED' | 'REJECTED') => {
    setDocuments(prev => prev.map(d => d.id === id ? { ...d, status: newStatus } : d));
  };

  const handleChatSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!chatQuery) return;

    const userMsg = { role: 'user' as const, text: chatQuery };
    setChatMessages(prev => [...prev, userMsg]);
    setChatQuery('');
  };

  return (
    <div className={`min-h-screen flex flex-col ${darkMode ? 'bg-gray-900 text-gray-100' : 'bg-gray-50 text-gray-800'}`}>
      <header className="px-6 py-4 border-b flex items-center justify-between bg-white dark:bg-gray-800 border-gray-200 dark:border-gray-700">
        <div className="flex items-center space-x-3">
          <Shield className="w-8 h-8 text-indigo-600" />
          <div>
            <h1 className="text-xl font-bold">Software Development Document Environment</h1>
            <p className="text-xs text-gray-500">Governance Portal & Security Pipeline</p>
          </div>
        </div>
        <div className="flex items-center space-x-4">
          <button onClick={toggleDarkMode}>
            {darkMode ? <Sun className="w-5 h-5 text-yellow-400" /> : <Moon className="w-5 h-5" />}
          </button>
        </div>
      </header>

      <div className="flex-1 flex">
        <aside className="w-64 p-4 border-r bg-white dark:bg-gray-800 border-gray-200 dark:border-gray-700 flex flex-col space-y-2">
          <button onClick={() => setActiveTab('dashboard')} className="p-3 rounded-lg text-left hover:bg-gray-100 dark:hover:bg-gray-700">Dashboard</button>
          <button onClick={() => setActiveTab('sdlc')} className="p-3 rounded-lg text-left hover:bg-gray-100 dark:hover:bg-gray-700">SDLC Pipeline</button>
          <button onClick={() => setActiveTab('documents')} className="p-3 rounded-lg text-left hover:bg-gray-100 dark:hover:bg-gray-700">Document Manager</button>
          <button onClick={() => setActiveTab('rag')} className="p-3 rounded-lg text-left hover:bg-gray-100 dark:hover:bg-gray-700">Semantic Search (RAG)</button>
          <button onClick={() => setActiveTab('admin')} className="p-3 rounded-lg text-left hover:bg-gray-100 dark:hover:bg-gray-700">System Settings</button>
        </aside>

        <main className="flex-1 p-6">
          {activeTab === 'dashboard' && (
            <div className="space-y-6">
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div className="p-4 border rounded bg-white dark:bg-gray-800">
                  <p className="text-sm font-semibold text-gray-500">Total Documents</p>
                  <p className="text-3xl font-bold">{documents.length}</p>
                </div>
                <div className="p-4 border rounded bg-white dark:bg-gray-800">
                  <p className="text-sm font-semibold text-gray-500">Average Compliance</p>
                  <p className="text-3xl font-bold">86%</p>
                </div>
              </div>
            </div>
          )}

          {activeTab === 'sdlc' && (
            <div className="space-y-6">
              <div className="flex border-b overflow-x-auto">
                {[1, 2, 3, 4, 5, 6, 7].map(num => (
                  <button key={num} onClick={() => setSelectedPhase(num)} className={`px-4 py-2 font-semibold ${selectedPhase === num ? 'border-b-2 border-indigo-600 text-indigo-600' : 'text-gray-500'}`}>
                    Phase {num}
                  </button>
                ))}
              </div>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-4">
                {documents.filter(d => d.sdlcPhase === selectedPhase).map(doc => (
                  <div key={doc.id} className="p-4 border rounded bg-white dark:bg-gray-800">
                    <h4 className="font-bold">{doc.name}</h4>
                    <p className="text-sm text-gray-500">{doc.description}</p>
                  </div>
                ))}
              </div>
            </div>
          )}
        </main>
      </div>
    </div>
  );
}
