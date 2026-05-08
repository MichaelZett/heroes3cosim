import { Routes, Route } from 'react-router-dom';
import ConfigPage from './pages/ConfigPage';
import BattlePage from './pages/BattlePage';

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<ConfigPage />} />
      <Route path="/battle" element={<BattlePage />} />
    </Routes>
  );
}
