CREATE INDEX idx_chat_message_room_sent_at_id ON chat_message (chat_room_id, sent_at, id);
