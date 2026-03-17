import { useState, useEffect } from "react";
import { Container, Title, Table, Button, Group, Loader, Text, Modal, } from "@mantine/core";
import { useAuth } from "../context/AuthContext";
import { getRooms, createRoom, updateRoom, deleteRoom } from "../api/rooms";
import type { RoomResponse, RoomRequest } from "../types";
import RoomForm from "../components/RoomForm";
import ConfirmModal from "../components/ConfirmModal";

export default function RoomsPage()
{
    const { isAdmin } = useAuth();
    const [rooms, setRooms] = useState<RoomResponse[]>([]);
    const [loading, setLoading] = useState(true);

    const [formOpened, setFormOpened] = useState(false);
    const [editingRoom, setEditingRoom] = useState<RoomResponse | null>(null);
    const [formLoading, setFormLoading] = useState(false);

    const [deleteTarget, setDeleteTarget] = useState<RoomResponse | null>(null);
    const [deleteLoading, setDeleteLoading] = useState(false);

    useEffect(() => 
    {
        loadRooms();
    }, []);

    async function loadRooms()
    {
        try
        {
            const data = await getRooms();
            setRooms(data);
        } finally
        {
            setLoading(false);
        }
    }

    function openCreate()
    {
        setEditingRoom(null);
        setFormOpened(true);
    }

    function openEdit(room: RoomResponse)
    {
        setEditingRoom(room);
        setFormOpened(true);
    }

    async function handleFormSubmit(data: RoomRequest)
    {
        setFormLoading(true);
        try
        {
            if (editingRoom)
            {
                const updated = await updateRoom(editingRoom.id, data);
                setRooms((prev) =>
                    prev.map((r) => (r.id === updated.id ? updated : r))
            );
            } else
            {
                const created = await createRoom(data);
                setRooms((prev) => [...prev, created]);
            }
            setFormOpened(false);
        } finally
        {
            setFormLoading(false);
        } 
    }

    async function handleDelete()
    {
        if (!deleteTarget) return;
        setDeleteLoading(true);
        try
        {
            await deleteRoom(deleteTarget.id);
            setRooms((prev) => prev.filter((r) => r.id !== deleteTarget.id));
            setDeleteTarget(null);
        } finally
        {
            setDeleteLoading(false);
        }
    }

    if (loading) 
    {
        return (
        <Container>
            <Loader mt="xl" />
        </Container>
        );
    }
  return (
    <Container>
      <Group justify="space-between" mb="md">
        <Title order={2}>Rooms</Title>
        {isAdmin && <Button onClick={openCreate}>Create Room</Button>}
      </Group>

      {rooms.length === 0 ? (
        <Text c="dimmed">No rooms found.</Text>
      ) : (
        <Table striped highlightOnHover>
          <Table.Thead>
            <Table.Tr>
              <Table.Th>Name</Table.Th>
              <Table.Th>Capacity</Table.Th>
              {isAdmin && <Table.Th>Actions</Table.Th>}
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {rooms.map((room) => (
              <Table.Tr key={room.id}>
                <Table.Td>{room.name}</Table.Td>
                <Table.Td>{room.capacity}</Table.Td>
                {isAdmin && (
                  <Table.Td>
                    <Group gap="xs">
                      <Button
                        size="xs"
                        variant="light"
                        onClick={() => openEdit(room)}
                      >
                        Edit
                      </Button>
                      <Button
                        size="xs"
                        variant="light"
                        color="red"
                        onClick={() => setDeleteTarget(room)}
                      >
                        Delete
                      </Button>
                    </Group>
                  </Table.Td>
                )}
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
      )}

      <Modal
        opened={formOpened}
        onClose={() => setFormOpened(false)}
        title={editingRoom ? "Edit Room" : "Create Room"}
      >
        <RoomForm
          initialValues={
            editingRoom
              ? { name: editingRoom.name, capacity: editingRoom.capacity }
              : undefined
          }
          onSubmit={handleFormSubmit}
          loading={formLoading}
        />
      </Modal>

      <ConfirmModal
        opened={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleDelete}
        title="Delete Room"
        message={`Are you sure you want to delete "${deleteTarget?.name}"?`}
        loading={deleteLoading}
      />
    </Container>
  );
}