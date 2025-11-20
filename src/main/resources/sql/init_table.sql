CREATE DATABASE IF NOT EXISTS learn_recommend DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE learn_recommend;

-- 用户表（可选，仅做外键约束）
CREATE TABLE `user` (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        username VARCHAR(50) NOT NULL UNIQUE,
                        pwd CHAR(60) NOT NULL,
                        created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 课程表
CREATE TABLE course (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        name VARCHAR(100) NOT NULL,
                        credit TINYINT DEFAULT 2,
                        created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 评分表
CREATE TABLE user_course_score (
                                   id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                   user_id BIGINT NOT NULL,
                                   course_id BIGINT NOT NULL,
                                   score TINYINT NOT NULL CHECK (score BETWEEN 1 AND 5),
                                   created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                   UNIQUE KEY uk_user_course (user_id, course_id),
                                   CONSTRAINT fk_ucs_user  FOREIGN KEY (user_id)  REFERENCES `user`(id),
                                   CONSTRAINT fk_ucs_course FOREIGN KEY (course_id) REFERENCES course(id)
);