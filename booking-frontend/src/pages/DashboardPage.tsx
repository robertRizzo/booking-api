import { useState, useEffect } from "react";
import { Container, Title, SimpleGrid, Paper, Text, Loader, Stack,
} from "@mantine/core";
import dayjs from "dayjs";
import { useAuth } from "../context/AuthContext";
import { getRooms } from "../api/rooms";
import { getAllBookings, getMyBookings } from "../api/bookings";
import type { RoomResponse, BookingResponse } from "../types";

export default function DashboardPage()
{
  const { isAdmin, user } = useAuth();
  const [rooms, setRooms] = useState<RoomResponse[]>([]);
  const [bookings, setBookings] = useState<BookingResponse[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function loadData() {
      try {
        const [roomData, bookingData] = await Promise.all([
          getRooms(),
          isAdmin ? getAllBookings() : getMyBookings(),
        ]);
        setRooms(roomData);
        setBookings(bookingData);
      } finally {
        setLoading(false);
      }
    }
    loadData();
  }, []);

  const now = dayjs();
  const upcoming = bookings.filter(
    (b) => b.status === "CONFIRMED" && dayjs(b.startTime).isAfter(now)
  );
  const nextBooking = upcoming.sort((a, b) =>
    dayjs(a.startTime).diff(dayjs(b.startTime))
  )[0];

  if (loading) {
    return (
      <Container>
        <Loader mt="xl" />
      </Container>
    );
  }

  return (
    <Container>
      <Title order={2} mb="md">
        {isAdmin ? "Admin Dashboard" : `Welcome, ${user?.email}`}
      </Title>

      <SimpleGrid cols={{ base: 1, sm: 3 }} mb="xl">
        <Paper p="md" shadow="sm" radius="md" withBorder>
          <Text size="sm" c="dimmed">
            Total Rooms
          </Text>
          <Text size="xl" fw={700}>
            {rooms.length}
          </Text>
        </Paper>
        <Paper p="md" shadow="sm" radius="md" withBorder>
          <Text size="sm" c="dimmed">
            {isAdmin ? "Total Bookings" : "My Bookings"}
          </Text>
          <Text size="xl" fw={700}>
            {bookings.length}
          </Text>
        </Paper>
        <Paper p="md" shadow="sm" radius="md" withBorder>
          <Text size="sm" c="dimmed">
            Upcoming
          </Text>
          <Text size="xl" fw={700}>
            {upcoming.length}
          </Text>
        </Paper>
      </SimpleGrid>

      {nextBooking && (
        <Paper p="md" shadow="sm" radius="md" withBorder>
          <Stack gap="xs">
            <Text size="sm" c="dimmed">
              Next Upcoming Booking
            </Text>
            <Text fw={600}>{nextBooking.roomName}</Text>
            <Text size="sm">
              {dayjs(nextBooking.startTime).format("MMM D, YYYY HH:mm")} —{" "}
              {dayjs(nextBooking.endTime).format("HH:mm")}
            </Text>
          </Stack>
        </Paper>
      )}
    </Container>
  );
}