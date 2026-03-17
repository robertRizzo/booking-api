import { useState, useEffect } from "react";
import { Container, Title, Table, Button, Group, Loader, Text, Modal, Badge,
} from "@mantine/core";
import { getUsers, createUser, updateUser, deleteUser } from "../api/users";
import type { UserResponse, UserRequest } from "../types";
import UserForm from "../components/UserForm";
import ConfirmModal from "../components/ConfirmModal";

export default function UsersPage() {
  const [users, setUsers] = useState<UserResponse[]>([]);
  const [loading, setLoading] = useState(true);

  const [formOpened, setFormOpened] = useState(false);
  const [editingUser, setEditingUser] = useState<UserResponse | null>(null);
  const [formLoading, setFormLoading] = useState(false);

  const [deleteTarget, setDeleteTarget] = useState<UserResponse | null>(null);
  const [deleteLoading, setDeleteLoading] = useState(false);

  useEffect(() => {
    loadUsers();
  }, []);

  async function loadUsers() {
    try {
      const data = await getUsers();
      setUsers(data);
    } finally {
      setLoading(false);
    }
  }

  function openCreate() {
    setEditingUser(null);
    setFormOpened(true);
  }

  function openEdit(u: UserResponse) {
    setEditingUser(u);
    setFormOpened(true);
  }

  async function handleFormSubmit(data: UserRequest) {
    setFormLoading(true);
    try {
      if (editingUser) {
        const updated = await updateUser(editingUser.id, data);
        setUsers((prev) =>
          prev.map((u) => (u.id === updated.id ? updated : u))
        );
      } else {
        const created = await createUser(data);
        setUsers((prev) => [...prev, created]);
      }
      setFormOpened(false);
    } finally {
      setFormLoading(false);
    }
  }

  async function handleDelete() {
    if (!deleteTarget) return;
    setDeleteLoading(true);
    try {
      await deleteUser(deleteTarget.id);
      setUsers((prev) => prev.filter((u) => u.id !== deleteTarget.id));
      setDeleteTarget(null);
    } finally {
      setDeleteLoading(false);
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
        <Title order={2}>Users</Title>
        <Button onClick={openCreate}>Create User</Button>
      </Group>

      {users.length === 0 ? (
        <Text c="dimmed">No users found.</Text>
      ) : (
        <Table striped highlightOnHover>
          <Table.Thead>
            <Table.Tr>
              <Table.Th>Email</Table.Th>
              <Table.Th>Role</Table.Th>
              <Table.Th>Actions</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {users.map((u) => (
              <Table.Tr key={u.id}>
                <Table.Td>{u.email}</Table.Td>
                <Table.Td>
                  <Badge color={u.role === "ADMIN" ? "blue" : "gray"}>
                    {u.role}
                  </Badge>
                </Table.Td>
                <Table.Td>
                  <Group gap="xs">
                    <Button
                      size="xs"
                      variant="light"
                      onClick={() => openEdit(u)}
                    >
                      Edit
                    </Button>
                    <Button
                      size="xs"
                      variant="light"
                      color="red"
                      onClick={() => setDeleteTarget(u)}
                    >
                      Delete
                    </Button>
                  </Group>
                </Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
      )}

      <Modal
        opened={formOpened}
        onClose={() => setFormOpened(false)}
        title={editingUser ? "Edit User" : "Create User"}
      >
        <UserForm
          initialValues={
            editingUser
              ? { email: editingUser.email, role: editingUser.role }
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
        title="Delete User"
        message={`Are you sure you want to delete "${deleteTarget?.email}"?`}
        loading={deleteLoading}
      />
    </Container>
  );
}