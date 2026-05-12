import {Route, Routes} from 'react-router-dom';
import ConfigPage from '../features/battle-config/ConfigPage';
import BattlePage from '../features/battle-replay/BattlePage';

export default function App() {
    return (
        <Routes>
            <Route path="/" element={<ConfigPage/>}/>
            <Route path="/battle" element={<BattlePage/>}/>
        </Routes>
    );
}
