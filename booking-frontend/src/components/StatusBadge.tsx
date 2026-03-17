import { Badge } from "@mantine/core";

interface Props
{
    status: string;
}

export default function StatusBadge( { status }: Props)
{
    const color = status === "CONFIRMED" ? "green" : status === "CANCELLED" ? "red" : "gray";
    return <Badge color={color}>{status}</Badge>
}