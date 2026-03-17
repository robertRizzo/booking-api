import { createBrowserRouter, RouterProvider, Navigate } from "react-router-dom";
import { MantineProvider } from "@mantine/core";
import "@mantine/core/styles.css";
import "@mantine/dates/styles.css";
import { AuthProvider } from "./context/AuthContext";
import ProtectedRoute from "./components/ProtectedRoute";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import AppLayout from "./components/AppLayout";
import DashboardPage from "./pages/DashboardPage";
import RoomsPage from "./pages/RoomsPage";
import BookingsPage from "./pages/BookingsPage";
import UsersPage from "./pages/UsersPage";

const router = createBrowserRouter([
  { path: "/login", element: <LoginPage /> },
  { path: "/register", element: <RegisterPage /> },
  {
    element:<ProtectedRoute><AppLayout/></ProtectedRoute>,
    children: [
      { path: "/dashboard", element: <DashboardPage /> },
      { path: "/rooms", element: <RoomsPage /> },
      { path: "/bookings", element: <BookingsPage /> },
      {
        path: "/users",
        element: <ProtectedRoute requireAdmin><UsersPage /> </ProtectedRoute>
      },
      ],
  },
  { path: "*", element: <Navigate to="/login" /> }
  
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
