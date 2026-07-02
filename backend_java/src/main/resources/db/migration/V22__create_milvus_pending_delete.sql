CREATE TABLE IF NOT EXISTS milvus_pending_delete (
    id UUID PRIMARY KEY,
    image_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
