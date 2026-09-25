import { Navigate, Route, Routes } from 'react-router-dom'
import { NotFoundPage } from './pages/NotFoundPage'
import { TicketCreatePage } from './pages/TicketCreatePage'
import { TicketDetailPage } from './pages/TicketDetailPage'
import { TicketListPage } from './pages/TicketListPage'

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/tickets" replace />} />
      <Route path="/tickets" element={<TicketListPage />} />
      <Route path="/tickets/new" element={<TicketCreatePage />} />
      <Route path="/tickets/:id" element={<TicketDetailPage />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}
