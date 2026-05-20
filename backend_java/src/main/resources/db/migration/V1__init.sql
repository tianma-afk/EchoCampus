CREATE TABLE IF NOT EXISTS university (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS campus (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    university_id UUID,
    CONSTRAINT fk_campus_university
        FOREIGN KEY (university_id)
        REFERENCES university(id)
);

CREATE TABLE IF NOT EXISTS landmark_category (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS landmark_location (
    id UUID PRIMARY KEY,
    description TEXT
);

CREATE TABLE IF NOT EXISTS landmark (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    check_in_count INTEGER,
    landmark_category_id UUID,
    built_time VARCHAR(64),
    open_time VARCHAR(64),
    description TEXT,
    landmark_location_id UUID,
    recommendation_score_sum INTEGER,
    recommendation_score_count INTEGER,
    tags JSONB,
    location_description TEXT,
    campus_id UUID,
    CONSTRAINT fk_landmark_category
        FOREIGN KEY (landmark_category_id)
        REFERENCES landmark_category(id),
    CONSTRAINT fk_landmark_location
        FOREIGN KEY (landmark_location_id)
        REFERENCES landmark_location(id),
    CONSTRAINT fk_landmark_campus
        FOREIGN KEY (campus_id)
        REFERENCES campus(id)
);

CREATE TABLE IF NOT EXISTS floor (
    id UUID PRIMARY KEY,
    landmark_id UUID,
    floor_number INTEGER,
    floor_name VARCHAR(255),
    tags JSONB,
    CONSTRAINT fk_floor_landmark
        FOREIGN KEY (landmark_id)
        REFERENCES landmark(id)
);

CREATE TABLE IF NOT EXISTS album (
    id UUID PRIMARY KEY,
    landmark_id UUID,
    CONSTRAINT fk_album_landmark
        FOREIGN KEY (landmark_id)
        REFERENCES landmark(id)
);

CREATE TABLE IF NOT EXISTS image (
    id UUID PRIMARY KEY,
    url TEXT,
    album_id UUID,
    is_show BOOLEAN,
    CONSTRAINT fk_image_album
        FOREIGN KEY (album_id)
        REFERENCES album(id)
);