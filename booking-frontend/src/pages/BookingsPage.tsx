import { useState, useEffect } from "react";
import {Container, Title, Table, Button, Group, Loader, Text, Modal, Select, Stack, Alert,
} from "@mantine/core";
import { DateTimePicker } from "@mantine/dates";
import { isAxiosError } from "axios";
import dayjs from "dayjs";
import { useAuth } from "../context/AuthContext";
import {
  getAllBookings,
  getMyBookings,
  createBooking,
  cancelBooking,
} from "../api/bookings";
import { getRooms } from "../api/rooms";
import type { BookingResponse, RoomResponse } from "../types";
import StatusBadge from "../components/StatusBadge";
import ConfirmModal from "../components/ConfirmModal";

export default function BookingsPage() {
  const { isAdmin, user } = useAuth();
  const [bookings, setBookings] = useState<BookingResponse[]>([]);
  const [rooms, setRooms] = useState<RoomResponse[]>([]);
  const [loading, setLoading] = useState(true);

  const [createOpened, setCreateOpened] = useState(false);
  const [selectedRoomId, setSelectedRoomId] = useState<string | null>(null);
  const [startTime, setStartTime] = useState<string | null>(null);
  const [endTime, setEndTime] = useState<string | null>(null);
  const [createLoading, setCreateLoading] = useState(false);
  const [createError, setCreateError] = useState("");

  const [cancelTarget, setCancelTarget] = useState<BookingResponse | null>(
    null
  );
  const [cancelLoading, setCancelLoading] = useState(false);

  useEffect(() => {
    loadData();
  }, []);

  async function loadData() {
    try {
      const [bookingData, roomData] = await Promise.all([
        isAdmin ? getAllBookings() : getMyBookings(),
        getRooms(),
      ]);
      setBookings(bookingData);
      setRooms(roomData);
    } finally {
      setLoading(false);
    }
  }

  function openCreate() {
    setSelectedRoomId(null);
    setStartTime(null);
    setEndTime(null);
    setCreateError("");
    setCreateOpened(true);
  }

  async function handleCreate() {
    if (!selectedRoomId || !startTime || !endTime) return;
    setCreateLoading(true);
    setCreateError("");
    try {
      const created = await createBooking({
        userId: user!.userId,
        roomId: Number(selectedRoomId),
        startTime: startTime,
        endTime: endTime,
      });
      setBookings((prev) => [...prev, created]);
      setCreateOpened(false);
    } catch (err: unknown) {
      if (isAxiosError(err) && err.response?.status === 409) {
        setCreateError(err.response.data.message);
      } else {
        setCreateError("Failed to create booking");
      }
    } finally {
      setCreateLoading(false);
    }
  }

  async function handleCancel() {
    if (!cancelTarget) return;
    setCancelLoading(true);
    try {
      await cancelBooking(cancelTarget.id);
      setBookings((prev) =>
        prev.map((b) =>
          b.id === cancelTarget.id ? { ...b, status: "CANCELLED" } : b
        )
      );
      setCancelTarget(null);
    } finally {
      setCancelLoading(false);
    }
  }

  if (loading) {
    return (
      <Container>
        <Loader mt="xl" />
      </Container>
    );
  }

  return (
    <Container>
      <Group justify="space-between" mb="md">
        <Title order={2}>Bookings</Title>
        <Button onClick={openCreate}>Create Booking</Button>
      </Group>

      {bookings.length === 0 ? (
        <Text c="dimmed">No bookings found.</Text>
      ) : (
        <Table striped highlightOnHover>
          <Table.Thead>
            <Table.Tr>
              {isAdmin && <Table.Th>User</Table.Th>}
              <Table.Th>Room</Table.Th>
              <Table.Th>Start Time</Table.Th>
              <Table.Th>End Time</Table.Th>
              <Table.Th>Status</Table.Th>
              <Table.Th>Actions</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {bookings.map((booking) => (
              <Table.Tr key={booking.id}>
                {isAdmin && <Table.Td>{booking.userEmail}</Table.Td>}
                <Table.Td>{booking.roomName}</Table.Td>
                <Table.Td>
                  {dayjs(booking.startTime).format("MMM D, YYYY HH:mm")}
                </Table.Td>
                <Table.Td>
                  {dayjs(booking.endTime).format("MMM D, YYYY HH:mm")}
                </Table.Td>
                <Table.Td>
                  <StatusBadge status={booking.status} />
                </Table.Td>
                <Table.Td>
                  {booking.status === "CONFIRMED" && (
                    <Button
                      size="xs"
                      variant="light"
                      color="red"
                      onClick={() => setCancelTarget(booking)}
                    >
                      Cancel
                    </Button>
                  )}
                </Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
      )}

      <Modal
        opened={createOpened}
        onClose={() => setCreateOpened(false)}
        title="Create Booking"
      >
        <Stack>
          <Select
            label="Room"
            placeholder="Select a room"
            data={rooms.map((r) => ({
              value: String(r.id),
              label: `${r.name} (capacity: ${r.capacity})`,
            }))}
            value={selectedRoomId}
            onChange={setSelectedRoomId}
          />
          <DateTimePicker
            label="Start Time"
            placeholder="Pick start date and time"
            value={startTime}
            onChange={setStartTime}
            minDate={new Date()}
          />
          <DateTimePicker
            label="End Time"
            placeholder="Pick end date and time"
            value={endTime}
            onChange={setEndTime}
            minDate={startTime ? new Date(startTime) : new Date()}
          />
          {createError && (
            <Alert color="red" title="Booking Error">
              {createError}
            </Alert>
          )}
          <Button
            onClick={handleCreate}
            loading={createLoading}
            disabled={!selectedRoomId || !startTime || !endTime}
          >
            Create Booking
          </Button>
        </Stack>
      </Modal>

      <ConfirmModal
        opened={!!cancelTarget}
        onClose={() => setCancelTarget(null)}
        onConfirm={handleCancel}
        title="Cancel Booking"
        message={`Are you sure you want to cancel the booking for "${cancelTarget?.roomName}"?`}
        loading={cancelLoading}
      />
    </Container>
  );
}