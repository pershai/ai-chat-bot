import {BrowserRouter as Router, Navigate, Route, Routes} from 'react-router-dom';
import Login from './pages/Login';
import Chat from './pages/Chat';
import Documents from './pages/Documents';
import Statistics from './pages/Statistics';
import UserManagement from './pages/UserManagement';

import {AuthProvider} from './context/AuthContext';

function App() {
  return (
    <AuthProvider>
      <Router>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/chat" element={<Chat />} />
          <Route path="/documents" element={<Documents />} />
          <Route path="/statistics" element={<Statistics />} />
          <Route path="/users" element={<UserManagement />} />
          <Route path="/" element={<Navigate to="/login" replace />} />
        </Routes>
      </Router>
    </AuthProvider>
  );
}

export default App;
