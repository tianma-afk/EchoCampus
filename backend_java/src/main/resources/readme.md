查看SQL有什么数据：

Thought for 2s

用 psql 连进 Docker 的 PostgreSQL：

docker exec -it postgres psql -U echo_campus -d echo_campus

进去后常用命令：

-- 查看所有表
\dt

-- 查看 user 表结构
\d "user"

-- 查看 user 表所有数据
SELECT * FROM "user";

-- 只看用户邮箱和昵称
SELECT id, nickname, email FROM "user";