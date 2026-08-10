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
  applicationCode?: string;
  summary?: string;
  complianceScore?: number;
  missingSections?: string;
  qualityScore?: number;
}

export default function App() {
  const [darkMode, setDarkMode] = useState(false);
  const [isLoggedIn, setIsLoggedIn] = useState(true);
  const [role, setRole] = useState<'ADMIN' | 'MAKER' | 'CHECKER' | 'APPROVER'>('MAKER');
  const [username] = useState('jules_maker');
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
      applicationCode: "PAY",
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
      applicationCode: "CRM",
      summary: "Detailed dataset tables detailing customer lookup schemas.",
      complianceScore: 78,
      missingSections: "Disaster Recovery topology mappings.",
      qualityScore: 82
    },
    {
      id: 3,
      documentCode: "ABC-P302",
      name: "Deployment Configuration.json",
      fileType: "JSON",
      fileSize: 22000,
      status: "UPLOADED",
      sdlcPhase: 3,
      version: 1,
      owner: "jules_maker",
      description: "Production manifest orchestrating deployment settings across high-availability clusters.",
      applicationCode: "ABC",
      summary: "Production service definitions mapping reverse proxies.",
      complianceScore: 85,
      missingSections: "Antivirus scanning details.",
      qualityScore: 88
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
      applicationCode: uploadAppCode,
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

    setTimeout(() => {
      const aiMsg = {
        role: 'ai' as const,
        text: `Based on the system specifications for the active SDLC phase documents: The compliance checks reveal an average of ${Math.round(documents.reduce((acc, d) => acc + (d.complianceScore || 0), 0) / documents.length)}% score. Please add details regarding load balancing architectures and anti-virus check thresholds to address current outstanding warnings.`
      };
      setChatMessages(prev => [...prev, aiMsg]);
    }, 1000);
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
          <button onClick={() => setActiveTab('dashboard')} className={`p-3 rounded-lg text-left ${activeTab === 'dashboard' ? 'bg-indigo-50 dark:bg-gray-700 text-indigo-600 font-bold' : 'hover:bg-gray-100 dark:hover:bg-gray-700'}`}>Dashboard</button>
          <button onClick={() => setActiveTab('sdlc')} className={`p-3 rounded-lg text-left ${activeTab === 'sdlc' ? 'bg-indigo-50 dark:bg-gray-700 text-indigo-600 font-bold' : 'hover:bg-gray-100 dark:hover:bg-gray-700'}`}>SDLC Pipeline</button>
          <button onClick={() => setActiveTab('documents')} className={`p-3 rounded-lg text-left ${activeTab === 'documents' ? 'bg-indigo-50 dark:bg-gray-700 text-indigo-600 font-bold' : 'hover:bg-gray-100 dark:hover:bg-gray-700'}`}>Document Manager</button>
          <button onClick={() => setActiveTab('rag')} className={`p-3 rounded-lg text-left ${activeTab === 'rag' ? 'bg-indigo-50 dark:bg-gray-700 text-indigo-600 font-bold' : 'hover:bg-gray-100 dark:hover:bg-gray-700'}`}>Semantic Search (RAG)</button>
          <button onClick={() => setActiveTab('admin')} className={`p-3 rounded-lg text-left ${activeTab === 'admin' ? 'bg-indigo-50 dark:bg-gray-700 text-indigo-600 font-bold' : 'hover:bg-gray-100 dark:hover:bg-gray-700'}`}>System Settings</button>
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
                <div className="p-4 border rounded bg-white dark:bg-gray-800">
                  <p className="text-sm font-semibold text-gray-500">Active Workflow Engine</p>
                  <p className="text-xl font-bold text-indigo-600">Maker-Checker-Approver</p>
                </div>
              </div>

              {/* Upload Panel */}
              <div className="p-6 border rounded bg-white dark:bg-gray-800">
                <h3 className="text-lg font-bold mb-4 flex items-center space-x-2">
                  <Upload className="w-5 h-5 text-indigo-600" />
                  <span>Upload Deliverable Document</span>
                </h3>
                <form onSubmit={handleUploadSubmit} className="space-y-4">
                  <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                    <input
                      type="text"
                      placeholder="Filename (e.g. Design.docx)"
                      value={uploadFile}
                      onChange={(e) => setUploadFile(e.target.value)}
                      className="p-2 border rounded bg-transparent text-sm w-full"
                      required
                    />
                    <input
                      type="text"
                      placeholder="App Code (e.g. PAY)"
                      value={uploadAppCode}
                      onChange={(e) => setUploadAppCode(e.target.value)}
                      maxLength={3}
                      className="p-2 border rounded bg-transparent text-sm w-full"
                    />
                    <select
                      value={uploadPhase}
                      onChange={(e) => setUploadPhase(Number(e.target.value))}
                      className="p-2 border rounded bg-transparent dark:bg-gray-800 text-sm w-full"
                    >
                      <option value={1}>Phase 1: Requirements</option>
                      <option value={2}>Phase 2: Architecture</option>
                      <option value={3}>Phase 3: Database</option>
                      <option value={4}>Phase 4: Build Verification</option>
                      <option value={5}>Phase 5: Performance</option>
                      <option value={6}>Phase 6: Compliance</option>
                      <option value={7}>Phase 7: Operations</option>
                    </select>
                  </div>
                  <textarea
                    placeholder="Enter document description..."
                    value={uploadDesc}
                    onChange={(e) => setUploadDesc(e.target.value)}
                    className="p-2 border rounded bg-transparent text-sm w-full h-20"
                  />
                  <div className="flex justify-end">
                    <button className="bg-indigo-600 text-white px-4 py-2 rounded text-sm font-semibold hover:bg-indigo-700">
                      Submit Deliverable
                    </button>
                  </div>
                </form>
              </div>
            </div>
          )}

          {activeTab === 'sdlc' && (
            <div className="space-y-6">
              <div className="flex border-b overflow-x-auto dark:border-gray-700">
                {[1, 2, 3, 4, 5, 6, 7].map(num => (
                  <button key={num} onClick={() => setSelectedPhase(num)} className={`px-4 py-2 font-semibold whitespace-nowrap ${selectedPhase === num ? 'border-b-2 border-indigo-600 text-indigo-600' : 'text-gray-500'}`}>
                    Phase {num}: {num === 1 ? 'Requirements' : num === 2 ? 'Architecture' : num === 3 ? 'Database' : num === 4 ? 'Build Logs' : num === 5 ? 'Performance' : num === 6 ? 'Compliance' : 'Release Operations'}
                  </button>
                ))}
              </div>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-4">
                {documents.filter(d => d.sdlcPhase === selectedPhase).map(doc => (
                  <div key={doc.id} className="p-4 border rounded bg-white dark:bg-gray-800">
                    <div className="flex justify-between items-start">
                      <h4 className="font-bold">{doc.name}</h4>
                      <span className="text-xs bg-indigo-100 dark:bg-indigo-900 text-indigo-600 px-2 py-0.5 rounded font-bold uppercase">{doc.documentCode}</span>
                    </div>
                    <p className="text-sm text-gray-500 mt-2">{doc.description}</p>
                    <div className="mt-4 pt-4 border-t dark:border-gray-700 text-xs">
                      <p className="font-semibold text-indigo-500 flex items-center space-x-1">
                        <Sparkles className="w-3.5 h-3.5" /> <span>Local Ollama AI recommendations:</span>
                      </p>
                      <p className="italic text-gray-500 mt-1">“{doc.summary}”</p>
                      <div className="flex justify-between mt-2 font-semibold">
                        <span className="text-red-500">Missing: {doc.missingSections}</span>
                        <span className="text-emerald-500">Quality Index: {doc.qualityScore}%</span>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {activeTab === 'documents' && (
            <div className="space-y-6">
              <div className="flex justify-between items-center">
                <h2 className="text-xl font-bold">Document Repository Manager</h2>
                <div className="relative">
                  <input
                    type="text"
                    placeholder="Search specifications..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    className="p-2 pl-8 border rounded bg-transparent text-sm w-64"
                  />
                  <Search className="w-4 h-4 text-gray-400 absolute left-2.5 top-3" />
                </div>
              </div>
              <div className="space-y-4">
                {documents.filter(d => d.name.toLowerCase().includes(searchQuery.toLowerCase())).map(doc => (
                  <div key={doc.id} className="p-4 border rounded bg-white dark:bg-gray-800 flex justify-between items-center">
                    <div>
                      <h4 className="font-bold">{doc.name}</h4>
                      <p className="text-xs text-gray-500">App: {doc.applicationCode} | Ver: {doc.version} | Status: <span className="font-bold text-indigo-500 uppercase">{doc.status}</span></p>
                    </div>
                    <div className="flex space-x-2">
                      {doc.status === 'PENDING_APPROVAL' && (
                        <>
                          <button onClick={() => handleWorkflowAction(doc.id, 'APPROVED')} className="bg-emerald-600 text-white px-3 py-1.5 rounded text-xs font-semibold">Approve</button>
                          <button onClick={() => handleWorkflowAction(doc.id, 'REJECTED')} className="bg-red-600 text-white px-3 py-1.5 rounded text-xs font-semibold">Reject</button>
                        </>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {activeTab === 'rag' && (
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              <div className="md:col-span-2 p-5 border rounded bg-white dark:bg-gray-800 flex flex-col h-[450px]">
                <h3 className="text-lg font-bold mb-4 flex items-center space-x-2">
                  <Sparkles className="w-5 h-5 text-indigo-600" />
                  <span>Local RAG Streaming Copilot</span>
                </h3>
                <div className="flex-1 overflow-y-auto space-y-4 mb-4 p-2">
                  {chatMessages.map((msg, idx) => (
                    <div key={idx} className={`flex ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}>
                      <div className={`max-w-[80%] p-3 rounded-lg text-sm ${msg.role === 'user' ? 'bg-indigo-600 text-white' : 'bg-gray-100 dark:bg-gray-700'}`}>
                        {msg.text}
                      </div>
                    </div>
                  ))}
                </div>
                <form onSubmit={handleChatSubmit} className="flex gap-2 border-t pt-4 dark:border-gray-700">
                  <input
                    type="text"
                    placeholder="Ask a technical or compliance question of your SDLC deliverables..."
                    value={chatQuery}
                    onChange={(e) => setChatQuery(e.target.value)}
                    className="flex-1 p-2 border rounded bg-transparent text-sm focus:outline-none"
                  />
                  <button className="bg-indigo-600 text-white px-5 py-2 rounded text-sm font-semibold">Send</button>
                </form>
              </div>
              <div className="p-5 border rounded bg-white dark:bg-gray-800 space-y-4">
                <h3 className="text-lg font-bold">RAG Models</h3>
                <div>
                  <label className="block text-xs font-semibold mb-1 uppercase text-gray-500">Chat Model</label>
                  <select className="p-2 border rounded bg-transparent text-sm w-full text-indigo-600 font-bold">
                    <option>llama3.2:latest</option>
                    <option>mistral:latest</option>
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-semibold mb-1 uppercase text-gray-500">Embedding Model</label>
                  <select className="p-2 border rounded bg-transparent text-sm w-full text-indigo-600 font-bold">
                    <option>nomic-embed-text</option>
                  </select>
                </div>
              </div>
            </div>
          )}

          {activeTab === 'admin' && (
            <div className="space-y-6">
              <h2 className="text-xl font-bold">System Administration Settings</h2>
              <div className="p-6 border rounded bg-white dark:bg-gray-800 space-y-4">
                <h3 className="text-lg font-bold">Configuration Management</h3>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-semibold mb-1">MFA Status</label>
                    <select className="p-2 border rounded bg-transparent text-sm w-full">
                      <option>Disabled by Configuration</option>
                      <option>SMS-Based Toggle</option>
                      <option>Email-Based Toggle</option>
                    </select>
                  </div>
                  <div>
                    <label className="block text-sm font-semibold mb-1">Database Mode</label>
                    <select className="p-2 border rounded bg-transparent text-sm w-full">
                      <option>PostgreSQL</option>
                      <option>Oracle 19c</option>
                      <option>SQL Server</option>
                    </select>
                  </div>
                </div>
              </div>
            </div>
          )}
        </main>
      </div>
    </div>
  );
}
