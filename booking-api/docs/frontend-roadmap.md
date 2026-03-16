# Frontend Roadmap — BookingAPI React Client

A step-by-step development roadmap for building a React frontend that integrates with the BookingAPI Spring Boot backend.

---

## Background

The BookingAPI backend is a complete REST API built with Spring Boot, PostgreSQL, and Spring Security. It supports JWT authentication, role-based access control (ADMIN / USER), CRUD for rooms, users, and bookings, and booking conflict detection. The API is documented via Swagger at `/swagger-ui.html`.

This roadmap guides you through building a React frontend that consumes the API and presents a clean, professional fullstack portfolio project.

---

## Backend API Reference

All endpoints require `Authorization: Bearer <token>` unless marked as public.

Error responses follow a consistent format: `{ status, message, timeStamp }`.

### Authentication (public)

| Method | Path | Request Body | Response | Notes |
|--------|------|-------------|----------|-------|
| POST | `/api/auth/login` | `{ email, password }` | `{ token }` | Returns JWT |
| POST | `/api/auth/register` | `{ email, password }` | `{ id, email, role }` | Creates USER account |

### Rooms

| Method | Path | Request Body | Response | Roles |
|--------|------|-------------|----------|-------|
| GET | `/api/rooms` | - | `[{ id, name, capacity }]` | ADMIN, USER |
| GET | `/api/rooms/{id}` | - | `{ id, name, capacity }` | ADMIN, USER |
| POST | `/api/rooms` | `{ name, capacity }` | `{ id, name, capacity }` (201) | ADMIN |
| PUT | `/api/rooms/{id}` | `{ name, capacity }` | `{ id, name, capacity }` | ADMIN |
| DELETE | `/api/rooms/{id}` | - | 204 No Content | ADMIN |

### Bookings

| Method | Path | Request Body | Response | Roles |
|--------|------|-------------|----------|-------|
| GET | `/api/bookings` | - | `[BookingResponse]` | ADMIN |
| GET | `/api/bookings/my` | - | `[BookingResponse]` | ADMIN, USER |
| GET | `/api/bookings/{id}` | - | `BookingResponse` | ADMIN, USER |
| POST | `/api/bookings` | `{ userId, roomId, startTime, endTime }` | `BookingResponse` (201) | ADMIN, USER |
| PUT | `/api/bookings/{id}` | `{ userId, roomId, startTime, endTime }` | `BookingResponse` | ADMIN, USER |
| DELETE | `/api/bookings/{id}` | - | 204 No Content | ADMIN, USER |

BookingResponse: `{ id, userId, userEmail, roomId, roomName, startTime, endTime, status }`

Date/time format: ISO-8601 (`2026-04-01T10:00:00`)

Booking statuses: `CONFIRMED`, `CANCELLED`

### Users (admin only)

| Method | Path | Request Body | Response | Roles |
|--------|------|-------------|----------|-------|
| GET | `/api/users` | - | `[{ id, email, role }]` | ADMIN |
| GET | `/api/users/{id}` | - | `{ id, email, role }` | ADMIN |
| POST | `/api/users` | `{ email, password, role }` | `{ id, email, role }` (201) | ADMIN |
| PUT | `/api/users/{id}` | `{ email, password, role }` | `{ id, email, role }` | ADMIN |
| DELETE | `/api/users/{id}` | - | 204 No Content | ADMIN |

---

## Project Structure

The frontend lives alongside the backend in the same repository:

```
booking-api/              (repo root)
├── booking-api/          (Spring Boot backend)
├── booking-frontend/     (React frontend)
│   ├── public/
│   ├── src/
│   │   ├── api/          API client and service functions
│   │   ├── components/   Reusable UI components
│   │   ├── context/      React Context providers
│   │   ├── hooks/        Custom hooks
│   │   ├── pages/        Route-level page components
│   │   └── types/        TypeScript types matching backend DTOs
│   ├── .env
│   ├── index.html
│   ├── package.json
│   ├── tsconfig.json
│   └── vite.config.ts
├── docker-compose.yml
└── README.md
```

---

## Phase 1 — Project Setup and Folder Structure

### What you will learn

- How to scaffold a React project with Vite and TypeScript
- How to establish a professional folder structure
- How to define TypeScript types that mirror backend DTOs
- How to create a configured Axios instance with automatic JWT injection

### Step 1 — Initialize the project

From the repo root (not inside `booking-api/`):

```bash
npm create vite@latest booking-frontend -- --template react-ts
cd booking-frontend
npm install
```

### Step 2 — Install dependencies

```bash
npm install axios react-router-dom @mantine/core @mantine/hooks @mantine/dates dayjs
```

Why these libraries:
- **axios** — HTTP client with interceptor support for automatic JWT injection
- **react-router-dom** — Client-side routing for single-page applications (types are bundled — no separate `@types` package needed)
- **@mantine/core** — Modern, accessible component library with clean defaults. Provides buttons, tables, modals, notifications, forms — professional look with minimal custom CSS
- **@mantine/dates** — Date/time picker components for booking start/end times
- **dayjs** — Lightweight date library required by Mantine dates

Mantine v8 requires explicit CSS imports in your entry point (`src/main.tsx`):

```typescript
import "@mantine/core/styles.css";
import "@mantine/dates/styles.css";
```

Without these, Mantine components render unstyled. Add them before any of your own styles.

### Step 3 — Create the folder structure

```bash
mkdir -p src/api src/components src/context src/hooks src/pages src/types
```

### Step 4 — Create TypeScript types

Create `src/types/index.ts` with interfaces matching every backend DTO:

```typescript
// --- Auth ---
export interface AuthRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
}

// --- Users ---
export interface UserRequest {
  email: string;
  password: string;
  role?: string;
}

export interface UserResponse {
  id: number;
  email: string;
  role: string;
}

// --- Rooms ---
export interface RoomRequest {
  name: string;
  capacity: number;
}

export interface RoomResponse {
  id: number;
  name: string;
  capacity: number;
}

// --- Bookings ---
export interface BookingRequest {
  userId: number;
  roomId: number;
  startTime: string;   // ISO-8601
  endTime: string;      // ISO-8601
}

export interface BookingResponse {
  id: number;
  userId: number;
  userEmail: string;
  roomId: number;
  roomName: string;
  startTime: string;
  endTime: string;
  status: string;
}

// --- Errors ---
export interface ErrorResponse {
  status: number;
  message: string;
  timeStamp: string;
}
```

These types are the contract between your frontend and backend. If the backend changes a field name, TypeScript will catch the mismatch at compile time.

### Step 5 — Create the Axios instance

Create `src/api/client.ts`:

```typescript
import axios from "axios";

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL || "http://localhost:8080",
  headers: {
    "Content-Type": "application/json",
  },
});

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export default apiClient;
```

Key decisions:
- `baseURL` reads from an environment variable so the same code works in development and Docker
- The request interceptor attaches the JWT automatically to every request — no component ever has to handle this manually

### Step 6 — Create the environment file

Create `booking-frontend/.env`:

```
VITE_API_URL=http://localhost:8080
```

Add `.env` to `.gitignore`.

### Git Checkpoint

```
git add -A
git commit -m "feat: initialize React project with TypeScript and folder structure"
```

---

## Phase 2 — Authentication (Login, Register, Auth Context)

### What you will learn

- How to manage global authentication state with React Context
- How to decode JWT tokens on the client to extract user information
- How to build login and register forms
- How to protect routes from unauthenticated access

### Step 1 — Create API functions for auth

Create `src/api/auth.ts`:

```typescript
import apiClient from "./client";
import { AuthRequest, AuthResponse, RegisterRequest, UserResponse } from "../types";

export async function loginUser(data: AuthRequest): Promise<AuthResponse> {
  const response = await apiClient.post<AuthResponse>("/api/auth/login", data);
  return response.data;
}

export async function registerUser(data: RegisterRequest): Promise<UserResponse> {
  const response = await apiClient.post<UserResponse>("/api/auth/register", data);
  return response.data;
}
```

Each API file exports simple async functions. Components never import Axios directly — they call these functions.

### Step 2 — Create a JWT decode helper

Create `src/utils/jwt.ts`:

```typescript
interface JwtPayload {
  sub: string;    // email
  role: string;   // ROLE_ADMIN or ROLE_USER (requires Option A below)
  iat: number;
  exp: number;
}

export function decodeToken(token: string): JwtPayload {
  const payload = token.split(".")[1];
  return JSON.parse(atob(payload));
}
```

The JWT payload is Base64-encoded (not encrypted). Decoding it gives you the user's email (`sub` claim) and role. No library is needed.

### Step 3 — Determine the user's role

The JWT payload from your backend contains only `sub` (email), `iat`, and `exp` — the role is not included in the token. To get the role, you have two options:

**Option A (recommended)** — Add the role to the JWT claims on the backend by modifying `JwtService.generateToken()` to include a `role` claim. This is a one-line change:

```java
return Jwts.builder()
    .subject(userDetails.getUsername())
    .claim("role", userDetails.getAuthorities().iterator().next().getAuthority())
    .issuedAt(new Date())
    .expiration(new Date(System.currentTimeMillis() + jwtProperties.expiration()))
    .signWith(getSigningKey())
    .compact();
```

Then in the frontend, `decodeToken(token).role` gives you `"ROLE_ADMIN"` or `"ROLE_USER"`.

**Option B** — After login, make a separate API call to fetch the user's profile. This works but adds an extra network round-trip on every login.

Option A is cleaner. It makes the token self-contained — the frontend can determine the role instantly without any additional API call.

### Step 4 — Create AuthContext

Create `src/context/AuthContext.tsx`. This context holds the current user state and provides login/register/logout functions to the entire application:

```typescript
import { createContext, useContext, useState, useEffect, ReactNode } from "react";
import { loginUser, registerUser } from "../api/auth";
import { decodeToken } from "../utils/jwt";
import { AuthRequest, RegisterRequest } from "../types";

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
```

Key points:
- `useEffect` on mount restores the session from localStorage if the token exists and hasn't expired
- `login()` calls the API, stores the token, and decodes it to set user state
- `isAdmin` is a convenience boolean derived from the role
- `useAuth()` is a custom hook that any component can call to access auth state

### Step 5 — Create a ProtectedRoute component

Create `src/components/ProtectedRoute.tsx`:

```typescript
import { Navigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

interface Props {
  children: React.ReactNode;
  requireAdmin?: boolean;
}

export default function ProtectedRoute({ children, requireAdmin }: Props) {
  const { user, isAdmin } = useAuth();

  if (!user) return <Navigate to="/login" replace />;
  if (requireAdmin && !isAdmin) return <Navigate to="/dashboard" replace />;

  return <>{children}</>;
}
```

This component does two things:
- Redirects unauthenticated users to `/login`
- Optionally restricts admin-only pages (used for the Users page)

### Step 6 — Build LoginPage and RegisterPage

Create `src/pages/LoginPage.tsx` and `src/pages/RegisterPage.tsx`.

Each page should:
- Use Mantine's `TextInput`, `PasswordInput`, `Button` components
- Store form values in local state with `useState`
- Call `useAuth().login()` or `useAuth().register()` on submit
- Display error messages from the API (catch the error, read `error.response.data.message`)
- Navigate to `/dashboard` after successful login, or to `/login` after successful registration

Example structure for LoginPage:

```typescript
import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { isAxiosError } from "axios";
import { TextInput, PasswordInput, Button, Paper, Title, Text, Stack } from "@mantine/core";
import { useAuth } from "../context/AuthContext";

export default function LoginPage() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      await login({ email, password });
      navigate("/dashboard");
    } catch (err: unknown) {
      if (isAxiosError(err)) {
        setError(err.response?.data?.message || "Login failed");
      } else {
        setError("Login failed");
      }
    } finally {
      setLoading(false);
    }
  }

  // Render a centered Paper with a form containing the inputs,
  // a submit button, an error display, and a link to /register
}
```

Build RegisterPage following the same pattern but calling `register()` and navigating to `/login` on success.

### Step 7 — Set up React Router

Update `src/App.tsx`. React Router v7 replaces the old `BrowserRouter`/`Routes`/`Route` JSX tree with `createBrowserRouter` + `RouterProvider`. Routes are defined as an array of objects instead of nested JSX:

```typescript
import { createBrowserRouter, RouterProvider, Navigate } from "react-router-dom";
import { MantineProvider } from "@mantine/core";
import "@mantine/core/styles.css";
import "@mantine/dates/styles.css";
import { AuthProvider } from "./context/AuthContext";
import ProtectedRoute from "./components/ProtectedRoute";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";

// These pages will be created in later phases:
// import DashboardPage from "./pages/DashboardPage";

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
```

Why `createBrowserRouter`? The old `BrowserRouter` + JSX `<Routes>` pattern is deprecated in React Router v7. The data router pattern decouples route configuration from the React tree and enables features like data loaders and actions if you need them later.

### Step 8 — Verify

1. Start the backend: `cd booking-api && mvn spring-boot:run`
2. Start the frontend: `cd booking-frontend && npm run dev`
3. Open `http://localhost:5173/login`
4. Register a new user, then log in with those credentials
5. Verify that you're redirected to `/dashboard` and the token is in localStorage

### Git Checkpoint

```
git add -A
git commit -m "feat: add authentication with JWT login, register, and protected routes"
```

---

## Phase 3 — Layout and Navigation

### What you will learn

- How to create a persistent layout that wraps all authenticated pages
- How to show/hide navigation items based on the user's role

### Step 1 — Create the AppShell layout

Create `src/components/AppLayout.tsx`. Use Mantine's `AppShell` component which provides a ready-made layout with a sidebar (navbar) and header:

```typescript
import { AppShell, NavLink, Group, Text, Button } from "@mantine/core";
import { Outlet, useNavigate, useLocation } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function AppLayout() {
  const { user, logout, isAdmin } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  function handleLogout() {
    logout();
    navigate("/login");
  }

  return (
    <AppShell
      navbar={{ width: 250, breakpoint: "sm" }}
      header={{ height: 60 }}
      padding="md"
    >
      <AppShell.Header>
        <Group h="100%" px="md" justify="space-between">
          <Text fw={700} size="lg">BookingAPI</Text>
          <Group>
            <Text size="sm">{user?.email}</Text>
            <Button variant="subtle" size="sm" onClick={handleLogout}>
              Logout
            </Button>
          </Group>
        </Group>
      </AppShell.Header>

      <AppShell.Navbar p="md">
        <NavLink
          label="Dashboard"
          active={location.pathname === "/dashboard"}
          onClick={() => navigate("/dashboard")}
        />
        <NavLink
          label="Rooms"
          active={location.pathname.startsWith("/rooms")}
          onClick={() => navigate("/rooms")}
        />
        <NavLink
          label="Bookings"
          active={location.pathname.startsWith("/bookings")}
          onClick={() => navigate("/bookings")}
        />
        {isAdmin && (
          <NavLink
            label="Users"
            active={location.pathname.startsWith("/users")}
            onClick={() => navigate("/users")}
          />
        )}
      </AppShell.Navbar>

      <AppShell.Main>
        <Outlet />
      </AppShell.Main>
    </AppShell>
  );
}
```

Key decisions:
- The Users nav item is conditionally rendered with `{isAdmin && ...}` — only admins see it
- `<Outlet />` is where React Router renders the child route's component
- The header shows the logged-in user's email and a logout button

### Step 2 — Update routing to use the layout

Update the `router` in `src/App.tsx`. In the data router pattern, layout routes are objects with an `element` (the layout) and `children` (the pages rendered inside `<Outlet />`):

```typescript
import AppLayout from "./components/AppLayout";
import DashboardPage from "./pages/DashboardPage";
import RoomsPage from "./pages/RoomsPage";
import BookingsPage from "./pages/BookingsPage";
import UsersPage from "./pages/UsersPage";

const router = createBrowserRouter([
  { path: "/login", element: <LoginPage /> },
  { path: "/register", element: <RegisterPage /> },
  {
    element: <ProtectedRoute><AppLayout /></ProtectedRoute>,
    children: [
      { path: "/dashboard", element: <DashboardPage /> },
      { path: "/rooms", element: <RoomsPage /> },
      { path: "/bookings", element: <BookingsPage /> },
      {
        path: "/users",
        element: <ProtectedRoute requireAdmin><UsersPage /></ProtectedRoute>,
      },
    ],
  },
  { path: "*", element: <Navigate to="/login" /> },
]);
```

The outer `ProtectedRoute` ensures authentication. The nested `ProtectedRoute requireAdmin` on `/users` adds the admin check. All child routes share the same AppLayout (sidebar + header). The pathless parent route acts as a layout route — it matches whenever any of its children match.

### Git Checkpoint

```
git add -A
git commit -m "feat: add app shell layout with role-aware navigation"
```

---

## Phase 4 — Rooms (CRUD)

### What you will learn

- How to build a complete CRUD flow: list, detail, create, edit, delete
- How to separate API logic from UI components
- How to show/hide admin controls based on role

### Step 1 — Create API functions for rooms

Create `src/api/rooms.ts`:

```typescript
import apiClient from "./client";
import { RoomRequest, RoomResponse } from "../types";

export async function getRooms(): Promise<RoomResponse[]> {
  const res = await apiClient.get<RoomResponse[]>("/api/rooms");
  return res.data;
}

export async function getRoomById(id: number): Promise<RoomResponse> {
  const res = await apiClient.get<RoomResponse>(`/api/rooms/${id}`);
  return res.data;
}

export async function createRoom(data: RoomRequest): Promise<RoomResponse> {
  const res = await apiClient.post<RoomResponse>("/api/rooms", data);
  return res.data;
}

export async function updateRoom(id: number, data: RoomRequest): Promise<RoomResponse> {
  const res = await apiClient.put<RoomResponse>(`/api/rooms/${id}`, data);
  return res.data;
}

export async function deleteRoom(id: number): Promise<void> {
  await apiClient.delete(`/api/rooms/${id}`);
}
```

This pattern — one file per resource, one function per endpoint — keeps the API layer clean and predictable.

### Step 2 — Build RoomsPage

Create `src/pages/RoomsPage.tsx`:

- On mount (`useEffect`), call `getRooms()` and store the result in state
- Display a `Table` with columns: Name, Capacity, Actions
- If the user is admin, show a "Create Room" button at the top and Edit/Delete buttons per row
- If the user is USER, show the table without action buttons
- Use a loading state to show a spinner while rooms load
- Use an empty state if no rooms exist

### Step 3 — Build a Room form component

Create `src/components/RoomForm.tsx`:

- A reusable form with `TextInput` for name and `NumberInput` for capacity
- Accept optional initial values via props (for editing)
- Accept an `onSubmit` callback prop
- Validate that name is not empty and capacity is at least 1

This form is used by both the "Create Room" and "Edit Room" flows. The page component decides whether to call `createRoom()` or `updateRoom()`.

### Step 4 — Build the ConfirmModal component

Create `src/components/ConfirmModal.tsx`:

```typescript
import { Modal, Text, Group, Button } from "@mantine/core";

interface Props {
  opened: boolean;
  onClose: () => void;
  onConfirm: () => void;
  title: string;
  message: string;
  loading?: boolean;
}

export default function ConfirmModal({ opened, onClose, onConfirm, title, message, loading }: Props) {
  return (
    <Modal opened={opened} onClose={onClose} title={title}>
      <Text mb="lg">{message}</Text>
      <Group justify="flex-end">
        <Button variant="default" onClick={onClose}>Cancel</Button>
        <Button color="red" onClick={onConfirm} loading={loading}>Delete</Button>
      </Group>
    </Modal>
  );
}
```

This component is reusable — it will be used for deleting rooms, bookings, and users. It takes callbacks as props and knows nothing about what it's deleting.

### Step 5 — Wire up delete

When the admin clicks "Delete" on a room row:
1. Open the ConfirmModal
2. On confirm, call `deleteRoom(id)`
3. On success, remove the room from the local state list (or re-fetch)
4. Close the modal

### Git Checkpoint

```
git add -A
git commit -m "feat: add room management pages with admin CRUD"
```

---

## Phase 5 — Bookings

### What you will learn

- How to build a form with date/time pickers
- How to handle different views for different roles
- How to display booking conflict errors (409)

### Step 1 — Create API functions for bookings

Create `src/api/bookings.ts`:

```typescript
import apiClient from "./client";
import { BookingRequest, BookingResponse } from "../types";

export async function getAllBookings(): Promise<BookingResponse[]> {
  const res = await apiClient.get<BookingResponse[]>("/api/bookings");
  return res.data;
}

export async function getMyBookings(): Promise<BookingResponse[]> {
  const res = await apiClient.get<BookingResponse[]>("/api/bookings/my");
  return res.data;
}

export async function getBookingById(id: number): Promise<BookingResponse> {
  const res = await apiClient.get<BookingResponse>(`/api/bookings/${id}`);
  return res.data;
}

export async function createBooking(data: BookingRequest): Promise<BookingResponse> {
  const res = await apiClient.post<BookingResponse>("/api/bookings", data);
  return res.data;
}

export async function updateBooking(id: number, data: BookingRequest): Promise<BookingResponse> {
  const res = await apiClient.put<BookingResponse>(`/api/bookings/${id}`, data);
  return res.data;
}

export async function cancelBooking(id: number): Promise<void> {
  await apiClient.delete(`/api/bookings/${id}`);
}
```

### Step 2 — Build BookingsPage

Create `src/pages/BookingsPage.tsx`:

- On mount, fetch bookings based on the user's role:
  - Admin: `getAllBookings()`
  - User: `getMyBookings()`
- Display a table with columns: Room, Start Time, End Time, Status, Actions
- Format dates using `dayjs` for readability (e.g., "Apr 1, 2026 10:00")
- Show a cancel button per booking (only for CONFIRMED bookings)

### Step 3 — Build a StatusBadge component

Create `src/components/StatusBadge.tsx`:

```typescript
import { Badge } from "@mantine/core";

interface Props {
  status: string;
}

export default function StatusBadge({ status }: Props) {
  const color = status === "CONFIRMED" ? "green" : status === "CANCELLED" ? "red" : "gray";
  return <Badge color={color}>{status}</Badge>;
}
```

A small, reusable component that makes booking statuses visually distinct.

### Step 4 — Build the Create Booking form

Create `src/pages/CreateBookingPage.tsx` or use a modal on BookingsPage:

- Fetch the list of rooms with `getRooms()` to populate a dropdown/select
- Use Mantine's `DateTimePicker` for start and end times. In Mantine v8, `onChange` returns a **date string** instead of a `Date` object — this actually works well since the backend expects ISO-8601 strings
- On submit, call `createBooking()`
- If the response is `409 Conflict`, display the error message: "Room is already booked during this time"
- Handle the 409 specifically:

```typescript
import { isAxiosError } from "axios";

try {
  await createBooking(data);
  navigate("/bookings");
} catch (err: unknown) {
  if (isAxiosError(err) && err.response?.status === 409) {
    setError(err.response.data.message);
  } else {
    setError("Failed to create booking");
  }
}
```

Using `isAxiosError()` is the proper TypeScript pattern — it narrows the type from `unknown` to `AxiosError`, giving you type-safe access to `err.response`. Avoid `catch (err: any)` which disables all type checking.

This is a key feature of the project — it demonstrates that the frontend correctly surfaces backend conflict detection to the user.

### Git Checkpoint

```
git add -A
git commit -m "feat: add booking management with conflict detection"
```

---

## Phase 6 — User Management (Admin Only)

### What you will learn

- How to build an admin-only section
- How to combine route guarding with UI-level role checks

### Step 1 — Create API functions for users

Create `src/api/users.ts`:

```typescript
import apiClient from "./client";
import { UserRequest, UserResponse } from "../types";

export async function getUsers(): Promise<UserResponse[]> {
  const res = await apiClient.get<UserResponse[]>("/api/users");
  return res.data;
}

export async function getUserById(id: number): Promise<UserResponse> {
  const res = await apiClient.get<UserResponse>(`/api/users/${id}`);
  return res.data;
}

export async function createUser(data: UserRequest): Promise<UserResponse> {
  const res = await apiClient.post<UserResponse>("/api/users", data);
  return res.data;
}

export async function updateUser(id: number, data: UserRequest): Promise<UserResponse> {
  const res = await apiClient.put<UserResponse>(`/api/users/${id}`, data);
  return res.data;
}

export async function deleteUser(id: number): Promise<void> {
  await apiClient.delete(`/api/users/${id}`);
}
```

### Step 2 — Build UsersPage

Create `src/pages/UsersPage.tsx`:

- This page is only accessible to admins (the route is wrapped with `<ProtectedRoute requireAdmin>`)
- Fetch all users with `getUsers()`
- Display a table with columns: Email, Role, Actions
- Add buttons to create, edit, and delete users
- Reuse the `ConfirmModal` component for delete confirmations
- Build a `UserForm` component similar to `RoomForm` with email, password, and a role selector (USER / ADMIN)

### Git Checkpoint

```
git add -A
git commit -m "feat: add admin user management page"
```

---

## Phase 7 — Dashboard

### What you will learn

- How to build a summary page that aggregates data from multiple API endpoints
- How to show different content based on the user's role

### Step 1 — Build DashboardPage

Create `src/pages/DashboardPage.tsx`:

- On mount, fetch rooms and bookings
  - Admin: `getRooms()`, `getAllBookings()`
  - User: `getRooms()`, `getMyBookings()`
- Display summary cards using Mantine's `Card` or `Paper` components:
  - Total rooms
  - Total bookings (or "my bookings" for USER)
  - Upcoming bookings (filter bookings where `startTime` is in the future)
- For USER: highlight their next upcoming booking (the closest future booking)
- For ADMIN: show system-wide counts

Example card layout:

```typescript
<SimpleGrid cols={3}>
  <Paper p="md" shadow="sm">
    <Text size="sm" c="dimmed">Total Rooms</Text>
    <Text size="xl" fw={700}>{rooms.length}</Text>
  </Paper>
  <Paper p="md" shadow="sm">
    <Text size="sm" c="dimmed">Total Bookings</Text>
    <Text size="xl" fw={700}>{bookings.length}</Text>
  </Paper>
  <Paper p="md" shadow="sm">
    <Text size="sm" c="dimmed">Upcoming</Text>
    <Text size="xl" fw={700}>{upcomingCount}</Text>
  </Paper>
</SimpleGrid>
```

### Git Checkpoint

```
git add -A
git commit -m "feat: add dashboard with booking summary"
```

---

## Phase 8 — Error Handling and Polish

### What you will learn

- How to handle API errors globally with Axios interceptors
- How to add loading and empty states for better UX
- How to show toast notifications

### Step 1 — Add a response interceptor for global error handling

Install the notifications package and its CSS:

```bash
npm install @mantine/notifications
```

Add the CSS import in `src/main.tsx` (after the other Mantine imports):

```typescript
import "@mantine/notifications/styles.css";
```

Add `<Notifications />` to your App component (inside MantineProvider, before `AuthProvider`):

```typescript
import { Notifications } from "@mantine/notifications";

export default function App() {
  return (
    <MantineProvider>
      <Notifications />
      <AuthProvider>
        <RouterProvider router={router} />
      </AuthProvider>
    </MantineProvider>
  );
}
```

Then update `src/api/client.ts` to add the response interceptor:

```typescript
import { AxiosError } from "axios";
import { notifications } from "@mantine/notifications";

apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    const status = error.response?.status;

    if (status === 401) {
      localStorage.removeItem("token");
      window.location.href = "/login";
    } else if (status === 403) {
      notifications.show({
        title: "Access Denied",
        message: "You don't have permission to perform this action",
        color: "red",
      });
    }

    return Promise.reject(error);
  }
);
```

Key behaviors:
- **401** — Token expired or invalid: clear token, redirect to login. The user has to log in again.
- **403** — Insufficient role: show an error toast. The button should already be hidden via role checks, so this is a safety net.
- **409** — Booking conflicts: these are handled locally in the booking form (see Phase 5), not globally.
- **Other errors** — Individual components catch these in try/catch blocks.

### Step 2 — Add loading states

Every page that fetches data should follow this pattern:

```typescript
const [loading, setLoading] = useState(true);
const [data, setData] = useState<DataType[]>([]);

useEffect(() => {
  fetchData()
    .then(setData)
    .catch(() => { /* error handled by interceptor or local catch */ })
    .finally(() => setLoading(false));
}, []);

if (loading) return <LoadingOverlay visible />;
```

Mantine provides `LoadingOverlay` and `Skeleton` components.

### Step 3 — Add empty states

When a list is loaded but empty, show a friendly message instead of a blank table:

```typescript
if (rooms.length === 0) {
  return (
    <Center h={300}>
      <Stack align="center">
        <Text size="lg" c="dimmed">No rooms found</Text>
        {isAdmin && <Button onClick={() => navigate("/rooms/create")}>Create first room</Button>}
      </Stack>
    </Center>
  );
}
```

### Step 4 — Add form validation feedback

Mirror the backend's validation rules on the frontend for instant feedback:

- Email: must match email format
- Password: minimum 6 characters
- Room name: required
- Room capacity: minimum 1
- Booking times: must be in the future, start must be before end

Use Mantine's `error` prop on form inputs to display validation messages inline.

### Git Checkpoint

```
git add -A
git commit -m "feat: add global error handling, loading states, and polish"
```

---

## Phase 9 — Docker and Production Build

### What you will learn

- How to containerize a React application with a multi-stage Docker build
- How to use Nginx as a static file server and reverse proxy
- How to connect frontend and backend containers via Docker Compose

### Step 1 — Create the frontend Dockerfile

Create `booking-frontend/Dockerfile`:

```dockerfile
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
```

Stage 1 builds the React app with Node. Stage 2 copies the static output into an Nginx image. The final image contains only Nginx and your built files — no Node, no source code.

### Step 2 — Create the Nginx configuration

Create `booking-frontend/nginx.conf`:

```nginx
server {
    listen 80;

    location / {
        root /usr/share/nginx/html;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://app:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

Key points:
- `try_files $uri $uri/ /index.html` — serves `index.html` for all client-side routes (React Router handles them)
- `location /api/` — proxies API requests to the backend container named `app` on port 8080. This eliminates CORS issues because frontend and API appear to be on the same origin.

### Step 3 — Update docker-compose.yml

Add the frontend service to the existing docker-compose file in the repo root:

```yaml
  frontend:
    build: ./booking-frontend
    container_name: booking-frontend
    restart: always
    ports:
      - "3000:80"
    depends_on:
      - app
```

Now `docker compose up --build` starts three containers:
1. **postgres** — Database
2. **app** — Spring Boot backend on port 8080
3. **frontend** — Nginx serving React on port 3000, proxying `/api/*` to backend

### Step 4 — Update the frontend environment for Docker

In the Docker setup, the frontend's Nginx proxy handles API requests — the React app calls `/api/*` (relative), and Nginx forwards to the backend. Update the Axios `baseURL` to be empty (or just `/`) in production, since the proxy makes backend and frontend share the same origin.

You can do this by setting the `VITE_API_URL` during the Docker build or by defaulting to an empty string when the env variable is not set.

### Step 5 — Verify

```bash
docker compose up --build
```

Open `http://localhost:3000`. The React app should load, and API calls should work through the Nginx proxy.

### Git Checkpoint

```
git add -A
git commit -m "feat: dockerize frontend with Nginx proxy"
```

---

## Phase 10 — README and Portfolio Presentation

### What you will learn

- How to present a fullstack project in a portfolio-ready way

### Step 1 — Update the root README.md

Rewrite the README to cover the entire fullstack project:

**Sections to include:**

1. **Project Title and Description** — "BookingAPI is a fullstack booking management system with a Spring Boot REST API and a React frontend. It supports JWT authentication, role-based access control, CRUD for rooms and users, and booking conflict detection."

2. **Tech Stack** — List both backend and frontend technologies:
   - Backend: Java 17, Spring Boot 3, Spring Security, Spring Data JPA, PostgreSQL, Flyway, Docker
   - Frontend: React, TypeScript, Vite, Mantine UI, Axios, React Router
   - Infrastructure: Docker Compose, Nginx

3. **Architecture Diagram** — A simple text diagram showing: Browser → Nginx → React (static) / API → Spring Boot → PostgreSQL

4. **How to Run** — A single command:
   ```bash
   docker compose up --build
   ```
   Then open `http://localhost:3000`.

5. **Test Users** — Document the seeded admin account (admin@test.com / admin123) and how to register a new user.

6. **API Documentation** — Link to Swagger UI: `http://localhost:8080/swagger-ui.html`

7. **Screenshots** — Add 2-3 screenshots showing the login page, dashboard, and booking creation form.

### Git Checkpoint

```
git add -A
git commit -m "docs: update README for fullstack portfolio presentation"
```

---

## Summary

| Phase | Focus | Key Deliverable |
|-------|-------|-----------------|
| 1 | Setup | Vite + TypeScript + Axios instance + DTO types |
| 2 | Auth | Login, register, JWT decode, AuthContext, ProtectedRoute |
| 3 | Layout | AppShell with sidebar, role-aware navigation |
| 4 | Rooms | Room list, create, edit, delete (admin), ConfirmModal |
| 5 | Bookings | Booking list (role-aware), create with date pickers, 409 handling |
| 6 | Users | Admin-only user management page |
| 7 | Dashboard | Summary cards with aggregated data |
| 8 | Polish | Global error interceptor, loading/empty states, validation |
| 9 | Docker | Multi-stage build, Nginx proxy, docker-compose |
| 10 | README | Portfolio-ready documentation |

## Architecture Decisions

| Decision | Reasoning |
|----------|-----------|
| Axios with interceptors | JWT is attached automatically; 401/403 handled globally |
| React Context for auth | Only global state needed; Redux would be overkill |
| TypeScript types mirroring DTOs | Catches contract mismatches at compile time |
| Mantine UI | Professional components with minimal CSS; high ROI for portfolio |
| Nginx reverse proxy | Eliminates CORS; single domain in production |
| Monorepo structure | One `git clone` and one `docker compose up` to run everything |
