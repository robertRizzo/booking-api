import { Modal, Text, Group, Button } from "@mantine/core";

interface Props 
{
    opened: boolean;
    onClose: () => void;
    onConfirm: () => void;
    title: string;
    message: string;
    loading?: boolean;
}

export default function ConfirmModal( { opened, onClose, onConfirm, title, message, loading }: Props) 
{
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