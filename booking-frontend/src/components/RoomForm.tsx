import { useState } from "react";
import { TextInput, NumberInput, Button, Stack } from "@mantine/core";
import type { RoomRequest } from "../types";

interface Props 
{
    initialValues?: RoomRequest;
    onSubmit: (data: RoomRequest) => void;
    loading?: boolean;
}

export default function RoomForm({ initialValues, onSubmit, loading }: Props)
{
    const [name, setName] = useState(initialValues?.name || "");
    const [capacity, setCapacity] = useState<number>(
        initialValues?.capacity || 1
    );
    const [errors, setErrors] = useState<{ name?: string; capacity?: string }> (
        {}
    );

    function handleSubmit(e: React.SyntheticEvent<HTMLFormElement>)
    {
        e.preventDefault();
        const newErrors: { name?: string; capacity?: string } = {};
        if (!name.trim()) newErrors.name = "Name is required";
        if (capacity < 1) newErrors.capacity = "Capacity must be at least 1";
        if (Object.keys(newErrors).length > 0)
        {
            setErrors(newErrors);
            return;
        }
        setErrors({});
        onSubmit({ name: name.trim(), capacity });
    }

    return (
        <form onSubmit={handleSubmit}>
            <Stack>
                <TextInput 
                    label="Name"
                    placeholder="Conference Room A"
                    value={name}
                    onChange={(e) => setName(e.currentTarget.value)}
                    error={errors.name}
                />
                <NumberInput 
                    label="Capacity"
                    placeholder="10"
                    min={1}
                    value={capacity}
                    onChange={(val) => setCapacity(typeof val === "number" ? val : 1)}
                    error={errors.capacity}
                />
                <Button type="submit" loading={loading}>
                    {initialValues ? "Update" : "Create"}
                </Button>
            </Stack>
        </form>
    );
}