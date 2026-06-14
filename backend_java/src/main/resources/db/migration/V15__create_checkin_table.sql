CREATE TABLE checkin (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    landmark_id UUID NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_checkin_user ON checkin(user_id);
CREATE INDEX idx_checkin_landmark ON checkin(landmark_id);
CREATE UNIQUE INDEX idx_checkin_user_landmark_date
    ON checkin(user_id, landmark_id, (created_at::date));
