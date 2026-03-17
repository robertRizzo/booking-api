import { createContext, useContext, useState, useEffect } from "react";
import type { ReactNode } from "react";
import { loginUser, registerUser } from "../api/auth";
import { decodeToken } from "../utils/jwt";
import type { AuthRequest, RegisterRequest } from "../types";

interface AuthUser {
  email: string;
  role: string;
  token: string;
}

interface AuthContextType {
  user: AuthUser | null;
  login: (data: AuthRequest) => Promise<void>;
  register: (data: RegisterRequest) => Promise<void>;
  logout: () => void;
  isAdmin: boolean;
}

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);

  useEffect(() => {
    const token = localStorage.getItem("token");
    if (token) {
      try {
        const payload = decodeToken(token);
        if (payload.exp * 1000 > Date.now()) {
          setUser({
            email: payload.sub,
            role: payload.role,   // requires Option A above
            token,
          });
        } else {
          localStorage.removeItem("token");
        }
      } catch {
        localStorage.removeItem("token");
      }
    }
  }, []);

  async function login(data: AuthRequest) {
    const response = await loginUser(data);
    localStorage.setItem("token", response.token);
    const payload = decodeToken(response.token);
    setUser({ email: payload.sub, role: payload.role, token: response.token });
  }

  async function register(data: RegisterRequest) {
    await registerUser(data);
  }

  function logout() {
    localStorage.removeItem("token");
    setUser(null);
  }

  const isAdmin = user?.role === "ROLE_ADMIN";

  return (
    <AuthContext.Provider value={{ user, login, register, logout, isAdmin }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used within AuthProvider");
  return context;
}