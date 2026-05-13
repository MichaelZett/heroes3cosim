import type {ReactNode} from 'react';
import {useNavigate} from 'react-router-dom';
import {useTranslation} from 'react-i18next';

interface ModeSwitcherProps {
    current: 'single' | 'matrix';
}

export default function ModeSwitcher({current}: Readonly<ModeSwitcherProps>) {
    const navigate = useNavigate();
    const {t} = useTranslation();

    return (
        <nav className="flex flex-wrap gap-2 rounded-lg border border-slate-800 bg-slate-900 p-2 text-sm">
            <ModeButton active={current === 'single'} onClick={() => navigate('/')}>
                {t('mode.single')}
            </ModeButton>
            <ModeButton active={current === 'matrix'} onClick={() => navigate('/matrix')}>
                {t('mode.matrix')}
            </ModeButton>
            <ModeButton active={false} disabled>
                {t('mode.mixedArmy')}
            </ModeButton>
        </nav>
    );
}

function ModeButton({
                        active,
                        disabled,
                        onClick,
                        children,
                    }: Readonly<{
    active: boolean;
    disabled?: boolean;
    onClick?: () => void;
    children: ReactNode;
}>) {
    const base = 'rounded-md px-3 py-1.5 transition';
    const cls = buttonClass(base, active, disabled);
    return (
        <button type="button" className={cls} onClick={onClick} disabled={disabled}>
            {children}
        </button>
    );
}

function buttonClass(base: string, active: boolean, disabled?: boolean): string {
    if (active) return `${base} bg-amber-500 text-slate-950 font-semibold`;
    if (disabled) return `${base} text-slate-600 cursor-not-allowed`;
    return `${base} text-slate-300 hover:bg-slate-800 hover:text-amber-400`;
}
