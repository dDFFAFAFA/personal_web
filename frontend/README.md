# Frontend

## Development
1. Install dependencies: `npm install`
2. Start dev server: `npm run dev`

Vite proxies `/api` to `http://localhost:8080` (see `frontend/vite.config.ts`).

## Mock Data
Mock data is disabled by default. To enable it, set:

`VITE_USE_MOCK=true`

Example:

`frontend/.env.local`
`VITE_USE_MOCK=true`
