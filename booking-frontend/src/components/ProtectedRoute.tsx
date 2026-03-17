import { Navigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import type { JSX, ReactNode } from "react";

interface Props
{
    children: ReactNode;
    requireAdmin?: boolean;
}

export default function ProtectedRoute({ children, requireAdmin }: Props): JSX.Element {
    const { user, isAdmin } = useAuth();

    if (!user) return <Navigate to="/login" replace />;
    if (requireAdmin && !isAdmin) return <Navigate to="/dashboard" replace />;

    return <>{children}</>;
}