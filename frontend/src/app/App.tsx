import {Route, Routes} from 'react-router-dom';
import ConfigPage from '../features/battle-config/ConfigPage';
import BattlePage from '../features/battle-replay/BattlePage';
import MatrixConfigPage from '../features/matrix-experiment/MatrixConfigPage';
import MatrixResultPage from '../features/matrix-experiment/MatrixResultPage';

export default function App() {
    return (
        <Routes>
            <Route path="/" element={<ConfigPage/>}/>
            <Route path="/battle" element={<BattlePage/>}/>
            <Route path="/matrix" element={<MatrixConfigPage/>}/>
            <Route path="/matrix/result" element={<MatrixResultPage/>}/>
        </Routes>
    );
}
