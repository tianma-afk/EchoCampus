-- V8: 插入测试图片数据，为图书馆/二校门/主楼各插入若干图片并设置封面图
-- 用于测试图像搜索回调 → 识别结果 的完整链路

-- 图书馆 (d4e5f6a7-b8c9-0123-defa-234567890124)
INSERT INTO image (id, landmark_id, is_vectorized, file_ext) VALUES
('e1000001-1111-4000-a001-000000000001', 'd4e5f6a7-b8c9-0123-defa-234567890124', TRUE, 'jpg'),
('e1000001-1111-4000-a001-000000000002', 'd4e5f6a7-b8c9-0123-defa-234567890124', TRUE, 'jpg'),
('e1000001-1111-4000-a001-000000000003', 'd4e5f6a7-b8c9-0123-defa-234567890124', TRUE, 'jpg');

-- 二校门 (d4e5f6a7-b8c9-0123-defa-234567890133)
INSERT INTO image (id, landmark_id, is_vectorized, file_ext) VALUES
('e1000001-2222-4000-a001-000000000001', 'd4e5f6a7-b8c9-0123-defa-234567890133', TRUE, 'jpg'),
('e1000001-2222-4000-a001-000000000002', 'd4e5f6a7-b8c9-0123-defa-234567890133', TRUE, 'jpg'),
('e1000001-2222-4000-a001-000000000003', 'd4e5f6a7-b8c9-0123-defa-234567890133', TRUE, 'jpg'),
('e1000001-2222-4000-a001-000000000004', 'd4e5f6a7-b8c9-0123-defa-234567890133', TRUE, 'jpg');

-- 主楼 (d4e5f6a7-b8c9-0123-defa-234567890123)
INSERT INTO image (id, landmark_id, is_vectorized, file_ext) VALUES
('e1000001-3333-4000-a001-000000000001', 'd4e5f6a7-b8c9-0123-defa-234567890123', TRUE, 'jpg'),
('e1000001-3333-4000-a001-000000000002', 'd4e5f6a7-b8c9-0123-defa-234567890123', TRUE, 'jpg'),
('e1000001-3333-4000-a001-000000000003', 'd4e5f6a7-b8c9-0123-defa-234567890123', TRUE, 'jpg');

-- 设置封面图（每栋建筑第一张图片）
UPDATE landmark SET cover_image_id = 'e1000001-1111-4000-a001-000000000001'
    WHERE id = 'd4e5f6a7-b8c9-0123-defa-234567890124';

UPDATE landmark SET cover_image_id = 'e1000001-2222-4000-a001-000000000001'
    WHERE id = 'd4e5f6a7-b8c9-0123-defa-234567890133';

UPDATE landmark SET cover_image_id = 'e1000001-3333-4000-a001-000000000001'
    WHERE id = 'd4e5f6a7-b8c9-0123-defa-234567890123';
