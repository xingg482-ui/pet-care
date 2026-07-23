# 宠物管理订单系统

这是一个面向实习学习和 MVP 演示的单门店宠物服务订单管理系统。

## 项目结构

```text
pet-care/
  backend/   Spring Boot 后端 API
  frontend/  Vue 3 管理后台
  prd/       原始 PRD 和学习清单
  docs/      需求分析和开发说明
```

## 技术栈

- 后端：Java 21、Spring Boot 3、Maven Wrapper、MyBatis-Plus
- 数据库：SQLite，本地数据库文件计划放在 `backend/data/dog_care.db`
- 前端：Vue 3、Vite、JavaScript、Element Plus、Axios

## 默认账号

第一阶段使用简单登录：

- 用户名：`admin`
- 密码：`admin123`

## 启动后端

```powershell
cd D:\code\pet-care\backend
.\mvnw.cmd spring-boot:run
```

后端默认端口：`8080`

健康检查：

```text
GET http://localhost:8080/api/system/health
```

## 启动前端

```powershell
cd D:\code\pet-care\frontend
npm install
npm run dev
```

前端默认端口：`5173`

## 数据库初始化

项目已准备初始化 SQL：

- `backend/src/main/resources/db/schema.sql`
- `backend/src/main/resources/db/data.sql`

后续开发数据库访问功能时，可以让 Spring Boot 启动时执行这些 SQL，生成本地 SQLite 数据库文件。

## 当前完成状态

已完成阶段 0：

- 项目骨架
- PRD 归档
- 基础依赖
- 后端健康检查接口
- SQLite 建表脚本和演示数据
- Git 仓库初始化

已完成阶段 1：

- 简单登录接口：`POST /api/auth/login`
- 简单退出接口：`POST /api/auth/logout`
- 前端登录页：`/login`
- 后台统一布局：顶部栏、左侧菜单、退出登录
- 客户分页查询：`GET /api/customers`
- 客户详情：`GET /api/customers/{id}`
- 新增客户：`POST /api/customers`
- 编辑客户：`PUT /api/customers/{id}`
- 启用/停用客户：`PUT /api/customers/{id}/status`
- 前端客户管理页面：查询、分页、新增、编辑、启用、停用

已完成阶段 2：

- 宠物分页查询：`GET /api/pets`
- 宠物详情：`GET /api/pets/{id}`
- 客户名下宠物查询：`GET /api/customers/{customerId}/pets`
- 新增宠物：`POST /api/pets`
- 编辑宠物：`PUT /api/pets/{id}`
- 启用/停用宠物：`PUT /api/pets/{id}/status`
- 前端宠物管理页面：客户选择、查询、分页、新增、编辑、启用、停用
- 服务项目分页查询：`GET /api/service-items`
- 启用服务项目查询：`GET /api/service-items/enabled`
- 服务项目详情：`GET /api/service-items/{id}`
- 新增服务项目：`POST /api/service-items`
- 编辑服务项目：`PUT /api/service-items/{id}`
- 启用/停用服务项目：`PUT /api/service-items/{id}/status`
- 前端服务项目页面：查询、分页、新增、编辑、启用、停用

已完成阶段 3：

- 订单分页查询：`GET /api/orders`
- 订单详情：`GET /api/orders/{id}`
- 创建订单：`POST /api/orders`
- 修改订单状态：`PUT /api/orders/{id}/status`
- 修改预约时间：`PUT /api/orders/{id}/appointment-time`
- 查询订单状态日志：`GET /api/orders/{id}/status-logs`
- 创建订单时校验启用客户、启用宠物、启用服务项目
- 创建订单时校验宠物必须属于所选客户
- 创建订单时由后端按服务项目价格快照计算总金额
- 创建订单时保存订单明细和初始状态日志
- 状态流转按 PRD 限制：待确认、已确认、服务中、结束状态
- 前端订单列表页面：查询、分页、详情入口、状态操作
- 前端新建订单页面：客户/宠物联动、服务项目多选、金额预估、提交订单
- 前端订单详情页面：基础信息、服务明细、状态日志、状态操作、修改预约时间

已完成阶段 4：

- 首页统计接口：`GET /api/dashboard/summary`
- 首页接入真实统计数据，并支持手动刷新
- 前端请求错误提示优化，后端未启动时提示检查 `8080`
- 演示数据补充：客户、宠物、服务项目、待确认订单
- 验收清单整理：`docs/验收清单.md`
- README 补充阶段完成状态

已完成阶段 5：

- 健康记录汇总查询：`GET /api/pets/{petId}/health`
- 疫苗记录：新增、编辑、删除
- 驱虫记录：新增、编辑、删除
- 体重记录：新增、编辑、删除
- 宠物管理页面增加“健康记录”入口
- 健康记录弹窗按疫苗、驱虫、体重三个标签页展示
- 演示数据补充：疫苗记录、驱虫记录、体重记录

阶段 0 到阶段 5 已覆盖 MVP P0 核心闭环和 P1 健康记录增强。
