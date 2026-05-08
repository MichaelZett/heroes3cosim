# Heroes 3 Combat Simulator — Frontend

React-19-Replay-UI für die deterministische Combat-Engine.

## Stack

- **Vite 8** + **React 19** + **TypeScript 6**
- **Tailwind CSS 4** via `@tailwindcss/vite`
- **TanStack Query 5** für API-Calls
- **Zustand 5** für Replay-State (Events/Cursor/Speed/Pause)
- **react-router-dom 7** fürs Routing

## Entwicklung

```bash
npm install
npm run dev          # Vite-Dev-Server auf http://localhost:5173
npm run build        # tsc + Vite-Build, Output unter dist/
npm run lint         # tsc --noEmit (Type-Check)
```

Während `npm run dev` läuft, leitet der Vite-Proxy `/api/*`-Requests transparent
an Spring Boot auf `http://localhost:8080` weiter (siehe `vite.config.ts`) —
keine CORS-Konfiguration im Browser nötig. Den Backend-Server vorher starten:
`./gradlew.bat bootRun` aus dem Projekt-Root.

## Struktur

```
src/
├── api/
│   ├── client.ts        # Hand-getippter fetch-Wrapper (POST/GET)
│   └── types.ts         # TS-Mirror der Backend-DTOs (BattleEvent als
│                        # discriminated union mit `type`-Tag)
├── pages/
│   ├── ConfigPage.tsx   # Phase D: Truppen-Konfiguration
│   └── BattlePage.tsx   # Phase E: Hex-Grid + Replay + Log
├── store/
│   └── battleStore.ts   # Zustand-Store für den Replay-Player
├── App.tsx              # Routing (BrowserRouter)
└── main.tsx             # Bootstrap (QueryClientProvider, BrowserRouter)
```
