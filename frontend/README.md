# Syncticket frontend

React 19 + Vite + TypeScript + React Router + TanStack Query.

## Dev

Backend must run on port 8080 (Vite proxies `/api`).

```bash
cd frontend
npm install
npm run dev
```

Open http://localhost:5173

Optional: `VITE_API_BASE_URL` (default `/api`).

## Scripts

- `npm run dev` — dev server
- `npm run build` — production build
- `npm test` — Vitest + Testing Library
- `npm run lint` — oxlint
