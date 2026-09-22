import { StrictMode, Suspense } from 'react';
import { createRoot } from 'react-dom/client';
import { router } from './routes/router';
import { RouterProvider } from 'react-router-dom';
import './index.css';
import LoadingPage from './pages/LoadingPage';

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <Suspense fallback={<LoadingPage />}>
      <RouterProvider router={router} />
    </Suspense>
  </StrictMode>,
);
