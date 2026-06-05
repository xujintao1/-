-- 产业园厂房销售认购系统 数据库初始化脚本
-- MySQL 8.x

CREATE DATABASE IF NOT EXISTS park_sales DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE park_sales;

SET NAMES utf8mb4;

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    username    VARCHAR(64)  NOT NULL COMMENT '登录名',
    password    VARCHAR(128) NOT NULL COMMENT 'BCrypt密码',
    real_name   VARCHAR(64)           COMMENT '姓名',
    phone       VARCHAR(32)           COMMENT '手机号',
    roles       VARCHAR(255)          COMMENT '逗号分隔角色编码',
    enabled     TINYINT      NOT NULL DEFAULT 1,
    create_time DATETIME,
    update_time DATETIME,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '用户';

-- 角色表
CREATE TABLE IF NOT EXISTS sys_role (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    code        VARCHAR(64) NOT NULL COMMENT '角色编码',
    name        VARCHAR(64) NOT NULL COMMENT '角色名称',
    description VARCHAR(255),
    create_time DATETIME,
    update_time DATETIME,
    deleted     TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '角色';

-- 产业园项目
CREATE TABLE IF NOT EXISTS building_project (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(128) NOT NULL COMMENT '项目名称',
    address     VARCHAR(255),
    description VARCHAR(500),
    create_time DATETIME,
    update_time DATETIME,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '产业园项目';

-- 楼栋
CREATE TABLE IF NOT EXISTS building (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    project_id  BIGINT       NOT NULL,
    name        VARCHAR(64)  NOT NULL COMMENT '楼栋名称',
    floors      INT,
    create_time DATETIME,
    update_time DATETIME,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_building_project (project_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '楼栋';

-- 厂房单元（房源）
CREATE TABLE IF NOT EXISTS factory_unit (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    project_id  BIGINT,
    building_id BIGINT,
    unit_no     VARCHAR(64)   NOT NULL COMMENT '单元编号',
    area        DECIMAL(12,2)          COMMENT '建筑面积㎡',
    unit_price  DECIMAL(12,2)          COMMENT '单价 元/㎡',
    total_price DECIMAL(16,2)          COMMENT '总价 元',
    floor       INT,
    status      VARCHAR(32)   NOT NULL DEFAULT 'ON_SALE' COMMENT 'ON_SALE/SUBSCRIBED/SIGNED/SOLD',
    create_time DATETIME,
    update_time DATETIME,
    deleted     TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_unit_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '厂房单元';

-- 客户
CREATE TABLE IF NOT EXISTS customer (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    name           VARCHAR(128) NOT NULL COMMENT '客户/企业名称',
    contact_person VARCHAR(64),
    phone          VARCHAR(32),
    credit_code    VARCHAR(64)  COMMENT '统一社会信用代码',
    intent_level   VARCHAR(8)   COMMENT '意向等级 A/B/C',
    remark         VARCHAR(500),
    owner_id       BIGINT       COMMENT '跟进销售',
    create_time    DATETIME,
    update_time    DATETIME,
    deleted        TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '客户';

-- 认购单
CREATE TABLE IF NOT EXISTS subscription (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    subscription_no VARCHAR(64)   NOT NULL COMMENT '认购单号',
    customer_id     BIGINT        NOT NULL,
    factory_unit_id BIGINT        NOT NULL,
    deposit         DECIMAL(16,2) COMMENT '定金',
    total_price     DECIMAL(16,2) COMMENT '成交总价',
    status          VARCHAR(32)   NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/CANCELLED/CONTRACTED',
    sales_id        BIGINT,
    remark          VARCHAR(500),
    create_time     DATETIME,
    update_time     DATETIME,
    deleted         TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_subscription_no (subscription_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '认购单';

-- 合同
CREATE TABLE IF NOT EXISTS contract (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    contract_no      VARCHAR(64)   NOT NULL COMMENT '合同编号',
    subscription_id  BIGINT,
    customer_id      BIGINT,
    factory_unit_id  BIGINT,
    amount           DECIMAL(16,2) COMMENT '合同金额',
    discount         DECIMAL(16,2) COMMENT '优惠金额',
    payment_method   VARCHAR(32)   COMMENT 'FULL/INSTALLMENT/MORTGAGE',
    status           VARCHAR(32)   NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/APPROVING/APPROVED/REJECTED/EFFECTIVE',
    approval_flow_id BIGINT,
    sales_id         BIGINT,
    terms            VARCHAR(1000),
    create_time      DATETIME,
    update_time      DATETIME,
    deleted          TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_contract_no (contract_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '销售合同';

-- 审批流实例
CREATE TABLE IF NOT EXISTS approval_flow (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    biz_type     VARCHAR(32) NOT NULL COMMENT '业务类型 CONTRACT',
    biz_id       BIGINT      NOT NULL,
    gateway      VARCHAR(16) COMMENT 'internal/oa',
    external_no  VARCHAR(128) COMMENT '外部OA实例号',
    current_step INT         NOT NULL DEFAULT 1,
    status       VARCHAR(32) NOT NULL DEFAULT 'APPROVING' COMMENT 'APPROVING/APPROVED/REJECTED',
    create_time  DATETIME,
    update_time  DATETIME,
    deleted      TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_flow_biz (biz_type, biz_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '审批流';

-- 审批节点任务
CREATE TABLE IF NOT EXISTS approval_task (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    flow_id       BIGINT      NOT NULL,
    step          INT         NOT NULL COMMENT '步骤序号',
    node_name     VARCHAR(64) COMMENT '节点名称',
    approver_role VARCHAR(64) COMMENT '审批角色编码',
    approver_id   BIGINT      COMMENT '实际审批人',
    status        VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
    comment       VARCHAR(500),
    handled_time  DATETIME,
    create_time   DATETIME,
    update_time   DATETIME,
    deleted       TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_task_flow (flow_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '审批任务';

-- 回款记录
CREATE TABLE IF NOT EXISTS payment_record (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    contract_id  BIGINT        NOT NULL,
    customer_id  BIGINT,
    payment_type VARCHAR(32)   COMMENT 'DEPOSIT/DOWN_PAYMENT/INSTALLMENT/FINAL',
    amount       DECIMAL(16,2) NOT NULL,
    payment_date DATE,
    remark       VARCHAR(500),
    create_time  DATETIME,
    update_time  DATETIME,
    deleted      TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_payment_contract (contract_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '回款记录';

-- 系统参数配置（OA 对接 / 工作流 / 系统）
CREATE TABLE IF NOT EXISTS sys_config (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    config_group VARCHAR(64)  COMMENT '配置分组 oa_integration/workflow/system',
    config_key   VARCHAR(128) NOT NULL COMMENT '配置键',
    config_value VARCHAR(1000) COMMENT '配置值',
    value_type   VARCHAR(16)  DEFAULT 'string' COMMENT 'string/boolean/number',
    description  VARCHAR(255),
    create_time  DATETIME,
    update_time  DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uk_config_key (config_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '系统参数配置';

-- 部门（树形，对齐 HR 系统管理 - 部门管理）
CREATE TABLE IF NOT EXISTS sys_dept (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    dept_name   VARCHAR(128) NOT NULL COMMENT '部门名称',
    parent_id   BIGINT       NOT NULL DEFAULT 0 COMMENT '父部门ID，0为顶级',
    sort        INT          NOT NULL DEFAULT 0 COMMENT '排序',
    leader      VARCHAR(64)  COMMENT '负责人',
    phone       VARCHAR(32)  COMMENT '联系电话',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1正常 0禁用',
    create_time DATETIME,
    update_time DATETIME,
    PRIMARY KEY (id),
    KEY idx_dept_parent (parent_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '部门';

-- 菜单（树形，对齐 HR 系统管理 - 菜单管理）
CREATE TABLE IF NOT EXISTS sys_menu (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    menu_name   VARCHAR(64)  NOT NULL COMMENT '菜单名称',
    parent_id   BIGINT       NOT NULL DEFAULT 0 COMMENT '父菜单ID，0为顶级',
    path        VARCHAR(255) COMMENT '路由路径',
    component   VARCHAR(255) COMMENT '组件路径',
    icon        VARCHAR(64)  COMMENT '图标',
    sort        INT          NOT NULL DEFAULT 0 COMMENT '排序',
    menu_type   VARCHAR(8)   NOT NULL DEFAULT 'M' COMMENT 'M目录 C菜单 F按钮',
    visible     TINYINT      NOT NULL DEFAULT 1 COMMENT '1显示 0隐藏',
    perms       VARCHAR(128) COMMENT '权限标识',
    create_time DATETIME,
    PRIMARY KEY (id),
    KEY idx_menu_parent (parent_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '菜单';

-- 操作日志
CREATE TABLE IF NOT EXISTS sys_oper_log (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    module         VARCHAR(64)  COMMENT '操作模块',
    oper_type      VARCHAR(16)  COMMENT 'CREATE/UPDATE/DELETE/OTHER',
    title          VARCHAR(128) COMMENT '操作描述',
    method         VARCHAR(255) COMMENT '请求方法',
    request_method VARCHAR(16)  COMMENT 'HTTP方法',
    request_uri    VARCHAR(255) COMMENT '请求URI',
    oper_name      VARCHAR(64)  COMMENT '操作人',
    oper_param     VARCHAR(1000) COMMENT '请求参数',
    status         TINYINT      DEFAULT 1 COMMENT '1成功 0失败',
    error_msg      VARCHAR(1000) COMMENT '错误信息',
    cost_time      BIGINT       COMMENT '耗时(ms)',
    oper_time      DATETIME,
    PRIMARY KEY (id),
    KEY idx_log_module (module),
    KEY idx_log_time (oper_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '操作日志';

-- ============ 基础数据 ============

INSERT INTO sys_dept (id, dept_name, parent_id, sort, leader, phone, status, create_time, update_time) VALUES
 (1, '智造产业园', 0, 1, '王总', '0512-66000000', 1, NOW(), NOW()),
 (2, '销售中心',   1, 1, '老李', '0512-66000001', 1, NOW(), NOW()),
 (3, '销售一部',   2, 1, '张伟', '0512-66000002', 1, NOW(), NOW()),
 (4, '销售二部',   2, 2, '李娜', '0512-66000003', 1, NOW(), NOW()),
 (5, '财务部',     1, 2, '钱会计', '0512-66000004', 1, NOW(), NOW()),
 (6, '法务部',     1, 3, '赵律师', '0512-66000005', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE dept_name = VALUES(dept_name);

INSERT INTO sys_menu (id, menu_name, parent_id, path, component, icon, sort, menu_type, visible, perms, create_time) VALUES
 (1,  '销售看板', 0, '/dashboard',      'Dashboard',      'DataLine',       1, 'C', 1, 'dashboard:view',   NOW()),
 (2,  '房源管理', 0, '/factory-units',  'FactoryUnits',   'OfficeBuilding', 2, 'C', 1, 'unit:view',        NOW()),
 (3,  '客户管理', 0, '/customers',      'Customers',      'User',           3, 'C', 1, 'customer:view',    NOW()),
 (4,  '认购管理', 0, '/subscriptions',  'Subscriptions',  'Tickets',        4, 'C', 1, 'subscription:view',NOW()),
 (5,  '合同管理', 0, '/contracts',      'Contracts',      'Document',       5, 'C', 1, 'contract:view',    NOW()),
 (6,  '审批待办', 0, '/approvals',      'Approvals',      'Stamp',          6, 'C', 1, 'approval:view',    NOW()),
 (7,  '系统管理', 0, '',                '',               'Setting',        9, 'M', 1, '',                 NOW()),
 (71, '用户管理', 7, '/system/users',   'system/User',    '',               1, 'C', 1, 'system:user:view', NOW()),
 (72, '部门管理', 7, '/system/dept',    'system/Dept',    '',               2, 'C', 1, 'system:dept:view', NOW()),
 (73, '角色管理', 7, '/system/roles',   'system/Role',    '',               3, 'C', 1, 'system:role:view', NOW()),
 (74, '菜单管理', 7, '/system/menus',   'system/Menu',    '',               4, 'C', 1, 'system:menu:view', NOW()),
 (75, 'OA流程配置', 7, '/system/config', 'system/Config',  '',               5, 'C', 1, 'system:config:view', NOW()),
 (76, '操作日志', 7, '/system/oper-log','system/OperLog', '',               6, 'C', 1, 'system:log:view',  NOW())
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name);


INSERT INTO sys_config (config_group, config_key, config_value, value_type, description, create_time, update_time) VALUES
 ('oa_integration', 'oa_enabled',      'false', 'boolean', '是否启用外部 OA 审批', NOW(), NOW()),
 ('oa_integration', 'oa_api_url',      '',      'string',  'OA 开放接口根地址，如 http://oa-host:8080', NOW(), NOW()),
 ('oa_integration', 'oa_callback_url', '',      'string',  'OA 回调本系统地址（留空使用默认）', NOW(), NOW()),
 ('oa_integration', 'contract_workflow_type', 'CONTRACT_APPROVAL', 'string', '合同审批对应的 OA 流程标识', NOW(), NOW()),
 ('workflow',       'approval_gateway',        'internal', 'string', '审批网关 internal/oa', NOW(), NOW()),
 ('workflow',       'contract_approval_chain', 'SALES_MANAGER,FINANCE,LEGAL', 'string', '合同审批链（按序，逗号分隔角色）', NOW(), NOW()),
 ('system',         'system_name',     '产业园厂房销售认购系统', 'string', '系统名称', NOW(), NOW())
ON DUPLICATE KEY UPDATE description = VALUES(description);

INSERT INTO sys_role (code, name, description, create_time, update_time, deleted) VALUES
 ('ADMIN', '系统管理员', '全部权限', NOW(), NOW(), 0),
 ('SALES', '销售', '客户/认购/合同录入', NOW(), NOW(), 0),
 ('SALES_MANAGER', '销售经理', '商务条款审批+房源管理', NOW(), NOW(), 0),
 ('FINANCE', '财务', '价格/回款审批', NOW(), NOW(), 0),
 ('LEGAL', '法务', '合同条款审批', NOW(), NOW(), 0),
 ('GM', '总经理', '大额特批', NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO building_project (id, name, address, description, create_time, update_time, deleted) VALUES
 (1, '智造产业园一期', '江苏省苏州市工业园区星湖街328号', '标准化厂房，电力增容，适合智能制造企业', NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO building (id, project_id, name, floors, create_time, update_time, deleted) VALUES
 (1, 1, 'A栋', 4, NOW(), NOW(), 0),
 (2, 1, 'B栋', 4, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO factory_unit (id, project_id, building_id, unit_no, area, unit_price, total_price, floor, status, create_time, update_time, deleted) VALUES
 (1, 1, 1, 'A-101', 1200.00, 6500.00, 7800000.00, 1, 'ON_SALE', NOW(), NOW(), 0),
 (2, 1, 1, 'A-102', 1500.00, 6300.00, 9450000.00, 1, 'ON_SALE', NOW(), NOW(), 0),
 (3, 1, 1, 'A-201',  980.00, 6200.00, 6076000.00, 2, 'ON_SALE', NOW(), NOW(), 0),
 (4, 1, 2, 'B-101', 2000.00, 6000.00, 12000000.00, 1, 'ON_SALE', NOW(), NOW(), 0),
 (5, 1, 2, 'B-102', 1100.00, 6400.00, 7040000.00, 1, 'ON_SALE', NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE unit_no = VALUES(unit_no);

INSERT INTO customer (id, name, contact_person, phone, credit_code, intent_level, remark, owner_id, create_time, update_time, deleted) VALUES
 (1, '苏州精密机械有限公司', '张伟', '13800000001', '91320500MA1XXXXX1A', 'A', '需1000㎡以上一楼厂房', 2, NOW(), NOW(), 0),
 (2, '昆山新能源科技有限公司', '李娜', '13800000002', '91320500MA1XXXXX2B', 'B', '关注B栋', 2, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE name = VALUES(name);
