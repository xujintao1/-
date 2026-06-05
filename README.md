# 产业园厂房销售认购系统

产业园厂房（标准厂房/独栋）销售认购全流程管理系统，覆盖 **房源 → 客户 → 认购 → 合同 → 审批 → 回款** 闭环，
合同审批支持「系统内置审批流」并预留「对接外部 OA」能力。

技术栈：**Spring Boot 3 + Vue 3 + MySQL 8 + Docker**

> 详细设计与"合同审批是否接入 OA"的分析见 [docs/执行方案.md](docs/执行方案.md)。

## 一键启动（Docker）

```bash
docker compose up -d --build
```

- 前端：http://localhost:8088
- 后端 API：http://localhost:8080/api
- MySQL：localhost:3307（root / root123）

首次启动时 `db/init.sql` 会自动建表并写入演示数据（产业园、厂房、客户），
后端启动时自动初始化演示账号。

### 演示账号

| 账号 | 密码 | 角色 | 说明 |
|---|---|---|---|
| admin | admin123 | 管理员 | 全部权限，可审批全部节点 |
| manager | manager123 | 销售经理 | 房源管理 + 销售经理审批 |
| finance | finance123 | 财务 | 财务审批 |
| legal | legal123 | 法务 | 法务审批 |
| sales | sales123 | 销售 | 客户/认购/合同录入 |

## 业务流程

1. **房源管理**：录入/维护厂房单元（面积、单价自动算总价、状态）。
2. **客户管理**：登记企业客户、意向等级。
3. **认购管理**：客户选房创建认购单，房源自动锁定为「已认购」，登记定金。
4. **合同管理**：基于认购单生成销售合同（优惠、付款方式），提交审批。
5. **审批待办**：销售经理 → 财务 → 法务 多级审批（可配置）；全部通过后合同生效、房源「已签约」。
6. **回款登记**：对生效合同登记定金/首付/分期/尾款回款。
7. **销售看板**：房源状态分布、合同额、回款额等统计。

## 合同审批网关切换

通过环境变量 `APPROVAL_GATEWAY` 切换审批网关：

- `internal`（默认）：系统内置审批流。
- `oa`：对接外部 OA（钉钉/企业微信/泛微/致远等，见 `OaApprovalAdapter`，需按实际 OA 接口完善）。

审批链通过 `app.approval.chain` 配置（默认 `SALES_MANAGER,FINANCE,LEGAL`）。

## 本地开发

后端：
```bash
cd backend
mvn spring-boot:run   # 需本地 MySQL，连接配置见 application.yml 环境变量
```

前端：
```bash
cd frontend
npm install
npm run dev           # http://localhost:5173 ，已配置 /api 代理到 8080
```

## 目录结构

```
backend/    Spring Boot 后端
frontend/   Vue 3 前端
db/init.sql 数据库初始化脚本
docs/       执行方案文档
docker-compose.yml  一键编排
```
