-- 报表/大屏服务登录依赖 sys_client.client_code=forge_report（与 forge-report-ui 默认 userClient 一致）
-- 执行前请确认 id 不冲突；若 7 已占用可改 id
-- app_secret 与 forge-report-ui 中写死的 forage_pc123 一致（同 PC 端示例密钥）

INSERT INTO sys_client (id, client_code, client_name, app_id, app_secret, token_timeout, token_activity_timeout,
                        token_prefix, token_name, concurrent_login, share_token, enable_ip_limit, ip_whitelist,
                        enable_encrypt, encrypt_algorithm, max_user_count, max_online_count, auth_types, status,
                        description, tenant_id, create_time, create_by, update_time, update_by, create_dept)
VALUES (7, 'forge_report', '报表/大屏', 'forge_report', 'forage_pc123', 2592000, 7200, 'Bearer', 'Authorization', 0, 0, 0,
        NULL, 0, 'AES', -1, -1, 'password,password_captcha', 1, 'forge-report 服务客户端', 1, NOW(), 1, NOW(), 1, 2);
