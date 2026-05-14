use platform_auth;

INSERT INTO sys_dept (id, dept_name, status)
VALUES
    (1, '平台部', 1),
    (2, '研发部', 1)
ON DUPLICATE KEY UPDATE
                     dept_name = VALUES(dept_name),
                     status = VALUES(status);


INSERT INTO sys_role (id, role_code, role_name, status, created_at, updated_at)
VALUES
    (1, 'ADMIN', '系统管理员', 1, NOW(), NOW()),
    (2, 'EMPLOYEE', '普通员工', 1, NOW(), NOW()),
    (3, 'SUPPORT', '服务台支持', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE
                     role_name = VALUES(role_name),
                     status = VALUES(status);


INSERT INTO sys_user (id, username, password, real_name, dept_id, status)
VALUES
    (1, 'admin', '$2a$10$.rh0mWj7a5FtkJRjTJhFY.d.Gzt.sbQr3AofwRZ7UILL2rsD81/ym', '系统管理员', 1, 1),
    (2, 'employee', '$2a$10$.rh0mWj7a5FtkJRjTJhFY.d.Gzt.sbQr3AofwRZ7UILL2rsD81/ym', '普通员工', 2, 1),
    (3, 'support', '$2a$10$.rh0mWj7a5FtkJRjTJhFY.d.Gzt.sbQr3AofwRZ7UILL2rsD81/ym', '服务台人员', 1, 1)
ON DUPLICATE KEY UPDATE
                     password = VALUES(password),
                     real_name = VALUES(real_name),
                     dept_id = VALUES(dept_id),
                     status = VALUES(status);


INSERT INTO sys_user_role (id, user_id, role_id)
VALUES
    (1, 1, 1),
    (2, 2, 2),
    (3, 3, 3)
ON DUPLICATE KEY UPDATE
                     user_id = VALUES(user_id),
                     role_id = VALUES(role_id);