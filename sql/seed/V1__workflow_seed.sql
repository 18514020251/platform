USE platform_workflow;

INSERT INTO wf_ticket_type (
    id, type_code, type_name, status, description,
    default_priority, default_assignee_role, allow_ai_create,
    created_at, updated_at
) VALUES
      (1001, 'IT_REPAIR',       'IT报修',         1, '电脑、打印机、网络等故障报修',
       'HIGH',   'SUPPORT',         1, NOW(), NOW()),

      (1002, 'ACCOUNT_ISSUE',   '账号问题',       1, '账号登录、权限异常、密码重置等问题',
       'MEDIUM', 'SUPPORT',         1, NOW(), NOW()),

      (1003, 'VPN_APPLY',       'VPN申请',        1, '远程办公 VPN 权限申请',
       'MEDIUM', 'ADMIN',           1, NOW(), NOW()),

      (1004, 'OFFICE_ASSET',    '办公资产申请',    1, '键盘、鼠标、显示器等办公资产申请',
       'LOW',    'ADMIN',           0, NOW(), NOW()),

      (1005, 'HR_CONSULT',      '人事咨询',       1, '考勤、请假、社保等咨询',
       'LOW',    'ADMIN',           0, NOW(), NOW()),

      (1006, 'ENV_PERMISSION',  '环境权限申请',    1, '测试、生产、Git仓库等环境权限申请',
       'HIGH',   'ADMIN',           1, NOW(), NOW()),

      (1007, 'MEETING_SUPPORT', '会议支持',       0, '会议室设备调试与现场支持，当前停用',
       'LOW',    'SUPPORT',         0, NOW(), NOW());


USE platform_workflow;

INSERT INTO wf_ticket (
    id, ticket_no, ticket_type_id, ticket_type_code, ticket_type_name,
    title, content, status, priority, source, source_ref,
    creator_id, creator_name, assignee_id, assignee_name,
    closed_at, created_at, updated_at
) VALUES
      (
          2001, 'TK-2001', 1001, 'IT_REPAIR', 'IT报修',
          '办公电脑无法开机',
          '今天早上到工位后发现电脑无法正常开机，按电源键无反应，已检查插座正常。',
          'PENDING', 'HIGH', 'MANUAL', NULL,
          2, '普通员工', NULL, NULL,
          NULL, NOW(), NOW()
      ),
      (
          2002, 'TK-2002', 1002, 'ACCOUNT_ISSUE', '账号问题',
          'OA系统提示无权限访问',
          '登录 OA 后访问审批页面提示无权限，但该账号之前可以正常使用。',
          'PROCESSING', 'MEDIUM', 'MANUAL', NULL,
          2, '普通员工', 1, '系统管理员',
          NULL, NOW(), NOW()
      ),
      (
          2003, 'TK-2003', 1003, 'VPN_APPLY', 'VPN申请',
          '申请远程办公 VPN 权限',
          '因本周需在家办公两天，申请开通远程办公 VPN 权限。',
          'RESOLVED', 'MEDIUM', 'MANUAL', NULL,
          2, '普通员工', 1, '系统管理员',
          NULL, NOW(), NOW()
      ),
      (
          2004, 'TK-2004', 1006, 'ENV_PERMISSION', '环境权限申请',
          '申请测试环境发布权限',
          '当前需要协助联调，申请测试环境的服务发布权限。',
          'PENDING', 'HIGH', 'AI_AGENT', 'ai-session-20260420-001',
          2, '普通员工', NULL, NULL,
          NULL, NOW(), NOW()
      ),
      (
          2005, 'TK-2005', 1001, 'IT_REPAIR', 'IT报修',
          '打印机无法连接',
          '部门共享打印机突然离线，多人无法打印，怀疑网络或驱动异常。',
          'CLOSED', 'HIGH', 'MANUAL', NULL,
          1, '系统管理员', 1, '系统管理员',
          NOW(), NOW(), NOW()
      ),
      (
          2006, 'TK-2006', 1002, 'ACCOUNT_ISSUE', '账号问题',
          'Git仓库访问被拒绝',
          '拉取代码时提示无访问权限，怀疑仓库权限被移除。',
          'WAITING_INFO', 'MEDIUM', 'AI_AGENT', 'ai-session-20260420-002',
          2, '普通员工', 1, '系统管理员',
          NULL, NOW(), NOW()
      ),
      (
          2007, 'TK-2007', 1005, 'HR_CONSULT', '人事咨询',
          '咨询年假剩余天数',
          '想确认当前年度还剩余多少天年假可用。',
          'REJECTED', 'LOW', 'MANUAL', NULL,
          2, '普通员工', 1, '系统管理员',
          NULL, NOW(), NOW()
      );