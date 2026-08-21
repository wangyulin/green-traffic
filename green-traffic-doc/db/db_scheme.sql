-- 1. 创建数据库
CREATE DATABASE `green_traffic`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

-- 2. 创建专用数据库账号
CREATE USER 'green_traffic'@'%' IDENTIFIED BY '请替换为强密码';

-- 3. 授予该账号对 green_traffic 数据库的全部权限
GRANT ALL PRIVILEGES ON `green_traffic`.*
TO 'green_traffic'@'%';

-- 4. 刷新权限
FLUSH PRIVILEGES;

-- 5. 查看授权结果
SHOW GRANTS FOR 'green_traffic'@'%';