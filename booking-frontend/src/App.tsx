import { createBrowserRouter, RouterProvider, Navigate } from "react-router-dom";
import { MantineProvider } from "@mantine/core";
import "@mantine/core/styles.css";
import "@mantine/dates/styles.css";
import { AuthProvider } from "./context/AuthContext";
import ProtectedRoute from "./components/ProtectedRoute";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";

const router = createBrowserRouter([
  { path: "/login", element: <LoginPage /> },
  { path: "/register", element: <RegisterPage /> },
  {
    path: "/dashboard",
    element: (
      <ProtectedRoute>
        <div>Dashboard (coming in Phase 7)</div>
      </ProtectedRoute>
    ),
  },
  { path: "*", element: <Navigate to="/login" /> },
]);

export default function App() {
  return (
    <MantineProvider>
      <AuthProvider>
        <RouterProvider router={router} />
      </AuthProvider>
    </MantineProvider>
  );
}
