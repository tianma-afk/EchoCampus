-- ========================================
-- V10: 地标表添加经纬度坐标字段
-- 创建日期: 2026-06-06
-- 描述: 为地图模块添加 latitude/longitude 字段，并为 19 个测试地标写入坐标
-- ========================================

-- 1. 添加经纬度字段
ALTER TABLE landmark ADD COLUMN IF NOT EXISTS latitude  DECIMAL(10,7);
ALTER TABLE landmark ADD COLUMN IF NOT EXISTS longitude DECIMAL(10,7);

-- 2. 为 19 个地标写入高德地图可用坐标（GCJ-02）
-- 校园中心约 (113.3995, 23.0500)

-- 教学楼
UPDATE landmark SET latitude = 23.0500, longitude = 113.3990 WHERE id = 'd4e5f6a7-b8c9-0123-defa-234567890123'; -- 主楼
UPDATE landmark SET latitude = 23.0495, longitude = 113.4000 WHERE id = 'd4e5f6a7-b8c9-0123-defa-234567890129'; -- 科学馆
UPDATE landmark SET latitude = 23.0495, longitude = 113.4005 WHERE id = 'd4e5f6a7-b8c9-0123-defa-234567890134'; -- 信息科学技术大楼

-- 图书馆
UPDATE landmark SET latitude = 23.0505, longitude = 113.4000 WHERE id = 'd4e5f6a7-b8c9-0123-defa-234567890124'; -- 图书馆
UPDATE landmark SET latitude = 23.0510, longitude = 113.3985 WHERE id = 'f0000001-0000-4000-a000-000000000001'; -- 第4号图书馆

-- 体育场馆
UPDATE landmark SET latitude = 23.0490, longitude = 113.4010 WHERE id = 'd4e5f6a7-b8c9-0123-defa-234567890125'; -- 综合体育馆
UPDATE landmark SET latitude = 23.0495, longitude = 113.4020 WHERE id = 'd4e5f6a7-b8c9-0123-defa-234567890132'; -- 西大操场
UPDATE landmark SET latitude = 23.0500, longitude = 113.4030 WHERE id = 'd4e5f6a7-b8c9-0123-defa-234567890137'; -- 东大操场
UPDATE landmark SET latitude = 23.0505, longitude = 113.4025 WHERE id = 'f0000001-0000-4000-a000-000000000002'; -- 第1号体育馆

-- 生活区
UPDATE landmark SET latitude = 23.0485, longitude = 113.3995 WHERE id = 'd4e5f6a7-b8c9-0123-defa-234567890126'; -- 学生食堂
UPDATE landmark SET latitude = 23.0480, longitude = 113.3990 WHERE id = 'd4e5f6a7-b8c9-0123-defa-234567890131'; -- 紫荆园食堂
UPDATE landmark SET latitude = 23.0485, longitude = 113.4020 WHERE id = 'd4e5f6a7-b8c9-0123-defa-234567890136'; -- 紫操食堂
UPDATE landmark SET latitude = 23.0485, longitude = 113.4015 WHERE id = 'f0000001-0000-4000-a000-000000000003'; -- 第3号饭堂

-- 活动场馆
UPDATE landmark SET latitude = 23.0510, longitude = 113.4005 WHERE id = 'd4e5f6a7-b8c9-0123-defa-234567890127'; -- 大礼堂
UPDATE landmark SET latitude = 23.0505, longitude = 113.4015 WHERE id = 'd4e5f6a7-b8c9-0123-defa-234567890130'; -- 新清华学堂
UPDATE landmark SET latitude = 23.0515, longitude = 113.4010 WHERE id = 'd4e5f6a7-b8c9-0123-defa-234567890135'; -- 蒙民伟音乐厅

-- 景观景点
UPDATE landmark SET latitude = 23.0515, longitude = 113.3980 WHERE id = 'd4e5f6a7-b8c9-0123-defa-234567890128'; -- 近春园
UPDATE landmark SET latitude = 23.0505, longitude = 113.3995 WHERE id = 'd4e5f6a7-b8c9-0123-defa-234567890133'; -- 二校门
UPDATE landmark SET latitude = 23.0520, longitude = 113.3980 WHERE id = 'd4e5f6a7-b8c9-0123-defa-234567890138'; -- 水木清华
