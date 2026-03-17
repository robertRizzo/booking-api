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