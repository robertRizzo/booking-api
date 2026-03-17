import { useState } from "react";
import { TextInput, PasswordInput, Select, Button, Stack } from "@mantine/core";
import type { UserRequest } from "../types";

interface Props
{
  initialValues?: { email: string; role: string };
  onSubmit: (data: UserRequest) => void;
  loading?: boolean;
}

export default function UserForm({ initialValues, onSubmit, loading }: Props) 
{
  const [email, setEmail] = useState(initialValues?.email || "");
  const [password, setPassword] = useState("");
  const [role, setRole] = useState<string>(initialValues?.role || "USER");
  const [errors, setErrors] = useState<{
    email?: string;
    password?: string;
  }>({});

  function handleSubmit(e: React.SyntheticEvent<HTMLFormElement>)
  {
    e.preventDefault();
    const newErrors: { email?: string; password?: string } = {};
    if (!email.trim()) newErrors.email = "Email is required";
    if (!initialValues && password.length < 6)
      newErrors.password = "Password must be at least 6 characters";
    if (Object.keys(newErrors).length > 0)
        {
      setErrors(newErrors);
      return;
    }
    setErrors({});
    const data: UserRequest = { email: email.trim(), password, role };
    onSubmit(data);
  }

  return (
    <form onSubmit={handleSubmit}>
      <Stack>
        <TextInput
          label="Email"
          placeholder="user@example.com"
          value={email}
          onChange={(e) => setEmail(e.currentTarget.value)}
          error={errors.email}
        />
        <PasswordInput
          label={initialValues ? "New Password (leave blank to keep)" : "Password"}
          placeholder="••••••"
          value={password}
          onChange={(e) => setPassword(e.currentTarget.value)}
          error={errors.password}
        />
        <Select
          label="Role"
          data={[
            { value: "USER", label: "User" },
            { value: "ADMIN", label: "Admin" },
          ]}
          value={role}
          onChange={(val) => setRole(val || "USER")}
        />
        <Button type="submit" loading={loading}>
          {initialValues ? "Update" : "Create"}
        </Button>
      </Stack>
    </form>
  );
}