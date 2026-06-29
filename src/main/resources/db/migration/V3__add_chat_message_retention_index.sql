CREATE INDEX idx_chat_message_room_id_sent_at ON chat_message (chat_room_id, id, sent_at);
