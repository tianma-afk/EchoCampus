CREATE TABLE IF NOT EXISTS university (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    CONSTRAINT pk_university PRIMARY KEY (id),
    CONSTRAINT uk_university_name UNIQUE (name)
);
COMMENT ON TABLE university IS '大学表';
COMMENT ON COLUMN university.name IS '大学名称';




CREATE TABLE campus (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    university_id UUID NOT NULL,
    CONSTRAINT pk_campus PRIMARY KEY (id)
);
COMMENT ON TABLE campus IS '校区表';
COMMENT ON COLUMN campus.name IS '校区名称';
COMMENT ON COLUMN campus.university_id IS '逻辑外键，关联university表';



CREATE TABLE category (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL,
    CONSTRAINT pk_category PRIMARY KEY (id),
    CONSTRAINT uk_category_name UNIQUE (name)
);
COMMENT ON TABLE category IS '地标分类表';
COMMENT ON COLUMN category.name IS '分类名称';





CREATE TABLE IF NOT EXISTS landmark_location (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    description TEXT,
    CONSTRAINT pk_landmark_location PRIMARY KEY (id)
);





CREATE TABLE floor (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    landmark_id UUID NOT NULL,
    floor_number INT NOT NULL,
    floor_name VARCHAR(50),
    tags JSONB,
    CONSTRAINT pk_floor PRIMARY KEY (id)
);
COMMENT ON TABLE floor IS '楼层表';
COMMENT ON COLUMN floor.landmark_id IS '逻辑外键，关联地标表';
COMMENT ON COLUMN floor.floor_number IS '楼层编号';
COMMENT ON COLUMN floor.floor_name IS '楼层名称';



CREATE TABLE landmark (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    name VARCHAR(200) NOT NULL,
    rating NUMERIC(2,1),
    check_in_count INT DEFAULT 0,
    open_time VARCHAR(50),
    category_id UUID NOT NULL,
    tags JSONB,
    imgs JSONB,
    cover_img VARCHAR(256),
    build_year VARCHAR(10),
    open_time_detail VARCHAR(200),
    floors VARCHAR(20),
    location VARCHAR(200),
    description TEXT,
    campus_id UUID NOT NULL,
    total_floors INT DEFAULT 0,
    recommend_rate NUMERIC(5,2),
    CONSTRAINT pk_landmark PRIMARY KEY (id)
);
COMMENT ON TABLE landmark IS '地标表';
COMMENT ON COLUMN landmark.name IS '地标名称';
COMMENT ON COLUMN landmark.rating IS '评分0-5';
COMMENT ON COLUMN landmark.check_in_count IS '打卡人数';
COMMENT ON COLUMN landmark.category_id IS '逻辑外键，关联分类表';
COMMENT ON COLUMN landmark.campus_id IS '逻辑外键，关联校区表';
