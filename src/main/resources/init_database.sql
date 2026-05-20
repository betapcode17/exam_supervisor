-- =====================================================
-- DATABASE INITIALIZATION SCRIPT
-- Exam Supervisor Assignment System
-- =====================================================

DROP DATABASE IF EXISTS exam_supervisor_db;

CREATE DATABASE exam_supervisor_db
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE exam_supervisor_db;

-- =====================================================
-- 1. INVIGILATORS TABLE
-- tt đọc từ Excel/database
-- =====================================================

CREATE TABLE invigilators (

    tt INT PRIMARY KEY,

    ma_gv VARCHAR(20),

    ho_ten VARCHAR(100) NOT NULL,

    ngay_sinh DATE,

    don_vi_cong_tac VARCHAR(100)

) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- =====================================================
-- 2. ROOMS TABLE
-- stt đọc từ Excel/database
-- =====================================================

CREATE TABLE rooms (

    stt INT PRIMARY KEY,

    phong_thi VARCHAR(20),

    dia_diem VARCHAR(100)

) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- =====================================================
-- 3. ASSIGNMENTS TABLE
-- =====================================================

CREATE TABLE assignments (

    id INT AUTO_INCREMENT PRIMARY KEY,

    shift INT NOT NULL,

    room_stt INT NOT NULL,

    gv1_tt INT NOT NULL,

    gv2_tt INT NOT NULL,

    CONSTRAINT fk_assignment_room
        FOREIGN KEY (room_stt)
        REFERENCES rooms(stt),

    CONSTRAINT fk_assignment_gv1
        FOREIGN KEY (gv1_tt)
        REFERENCES invigilators(tt),

    CONSTRAINT fk_assignment_gv2
        FOREIGN KEY (gv2_tt)
        REFERENCES invigilators(tt),

    INDEX idx_shift (shift),
    INDEX idx_room (room_stt)

) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- =====================================================
-- 4. SUPERVISORS TABLE
-- =====================================================

CREATE TABLE supervisors (

    id INT AUTO_INCREMENT PRIMARY KEY,

    shift INT NOT NULL,

    gv_tt INT NOT NULL,

    from_room INT,

    to_room INT,

    CONSTRAINT fk_supervisor_gv
        FOREIGN KEY (gv_tt)
        REFERENCES invigilators(tt),

    CONSTRAINT fk_supervisor_from_room
        FOREIGN KEY (from_room)
        REFERENCES rooms(stt),

    CONSTRAINT fk_supervisor_to_room
        FOREIGN KEY (to_room)
        REFERENCES rooms(stt),

    INDEX idx_shift (shift),
    INDEX idx_gv (gv_tt)

) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- =====================================================
-- 5. PAIR HISTORY TABLE
-- =====================================================

CREATE TABLE pair_history (

    id INT AUTO_INCREMENT PRIMARY KEY,

    gv1_tt INT NOT NULL,

    gv2_tt INT NOT NULL,

    shift INT NOT NULL,

    CONSTRAINT fk_pair_gv1
        FOREIGN KEY (gv1_tt)
        REFERENCES invigilators(tt),

    CONSTRAINT fk_pair_gv2
        FOREIGN KEY (gv2_tt)
        REFERENCES invigilators(tt),

    INDEX idx_pair (gv1_tt, gv2_tt),
    INDEX idx_shift (shift)

) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- =====================================================
-- 6. ROOM HISTORY TABLE
-- =====================================================

CREATE TABLE room_history (

    id INT AUTO_INCREMENT PRIMARY KEY,

    gv_tt INT NOT NULL,

    room_stt INT NOT NULL,

    shift INT NOT NULL,

    CONSTRAINT fk_room_history_gv
        FOREIGN KEY (gv_tt)
        REFERENCES invigilators(tt),

    CONSTRAINT fk_room_history_room
        FOREIGN KEY (room_stt)
        REFERENCES rooms(stt),

    INDEX idx_gv_room (gv_tt, room_stt),
    INDEX idx_shift (shift)

) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;