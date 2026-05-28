import {Route, Routes} from 'react-router-dom';
import ConfigPage from '../features/battle-config/ConfigPage';
import BattlePage from '../features/battle-replay/BattlePage';
import MatrixConfigPage from '../features/matrix-experiment/MatrixConfigPage';
import MatrixResultPage from '../features/matrix-experiment/MatrixResultPage';
import ArmyConfigPage from '../features/army-config/ArmyConfigPage';
import ArmyBattlePage from '../features/army-config/ArmyBattlePage';

export default function App() {
    return (
        <Routes>
            <Route path="/" element={<ConfigPage/>}/>
            <Route path="/battle" element={<BattlePage/>}/>
            <Route path="/matrix" element={<MatrixConfigPage/>}/>
            <Route path="/matrix/result" element={<MatrixResultPage/>}/>
            <Route path="/army" element={<ArmyConfigPage/>}/>
            <Route path="/army/battle" element={<ArmyBattlePage/>}/>
        </Routes>
    );
}
