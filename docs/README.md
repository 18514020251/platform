# 企业智能知识库与流程协同平台

> 一个面向企业内部服务场景的智能化流程协同平台，围绕“用户认证、工单流转、知识库检索、RAG 问答、访问日志审计”等能力进行设计与实现。

本项目采用 Spring Boot + Spring Cloud Alibaba 构建多模块后端系统，通过网关统一入口、认证服务统一登录态、工单服务承载业务流程、知识库服务提供文档检索与向量召回、AI 服务提供知识问答与智能助手能力，同时通过自定义 starter 封装通用日志、Redis、Sa-Token、ID 生成等基础能力。

---

## 1. 项目背景

企业内部在处理员工问题、IT 支持、行政流程、业务咨询等场景时，通常会遇到以下问题：

- 常见问题重复咨询，人工支持成本较高；
- 工单创建、分配、接单、处理缺少统一流程；
- 知识文档分散，难以快速检索与复用；
- 后端服务缺少统一的访问日志、链路标识和审计能力；
- 普通知识库只能关键词搜索，无法结合语义理解完成智能问答。

因此，本项目设计了一个轻量级企业服务平台，将工单流程、知识库检索和 AI 问答结合起来，帮助用户通过自然语言查询知识、创建工单，并帮助管理员和支持人员完成工单处理与知识维护。

---

## 2. 核心功能

### 2.1 认证与权限

- 用户登录、登出、当前用户信息查询；
- 基于 Sa-Token 实现登录态管理；
- 基于 Redis 存储登录会话；
- 支持管理员、支持人员、普通员工等角色；
- 接口层使用角色注解控制访问权限；
- 登录用户身份统一从 Session 中获取，避免前端伪造用户身份。

### 2.2 工单管理

- 员工创建工单；
- 查询“我的工单”；
- 查看工单详情；
- 支持人员 / 管理员查询处理侧工单列表；
- 支持人员 / 管理员接单；
- 管理员分配工单；
- 支持更新工单状态；
- 支持 Elasticsearch 工单搜索；
- 工单来源区分手工创建与 AI Agent 创建。

当前工单状态包括：

| 状态 | 说明 |
|---|---|
| `PENDING` | 待处理 |
| `PROCESSING` | 处理中 |
| `RESOLVED` | 已解决 |
| `REJECTED` | 已拒绝 |

### 2.3 知识库管理

- 创建知识文档；
- 更新知识文档；
- 下线知识文档；
- 文档内容切片；
- 查询文档切片；
- 基于 Elasticsearch 搜索知识文档；
- 基于 Elasticsearch 搜索知识切片；
- 支持文本向量生成；
- 支持知识切片向量重建；
- 支持向量检索和混合检索。

当前知识文档状态包括：

| 状态 | 说明 |
|---|---|
| `DRAFT` | 草稿 |
| `PUBLISHED` | 已发布 |
| `OFFLINE` | 已下线 |

### 2.4 RAG 知识问答

- 支持用户基于知识库提问；
- 支持普通问答响应；
- 支持 SSE 流式问答；
- 支持按分类召回知识内容；
- 支持 topK 控制召回数量；
- 支持 RAG 上下文召回；
- 支持答案引用来源返回；
- 支持问题改写、相关性评估、Prompt 构建等 RAG 子流程。

### 2.5 智能助手

- 提供统一智能助手入口；
- 支持知识问答；
- 支持识别用户意图；
- 支持结合用户身份进行工单创建；
- 支持查询用户相关工单；
- 支持记录 AI Agent 执行日志。

### 2.6 访问日志与审计

- 自定义 `platform-log-starter`；
- 基于 AOP 采集接口访问日志；
- 支持 traceId 链路标识；
- 支持 MDC 上下文传递；
- 支持敏感字段脱敏；
- 日志写入 MongoDB；
- 管理员可按分页或 traceId 查询访问日志。

---

## 3. 技术栈

### 后端框架

| 技术 | 用途 |
|---|---|
| Spring Boot 3.3.5 | 服务基础框架 |
| Spring Cloud 2023.0.3 | 微服务基础能力 |
| Spring Cloud Alibaba 2023.0.1.2 | Nacos 服务注册与发现 |
| Spring Cloud Gateway | API 网关 |
| OpenFeign | 服务间 HTTP 调用 |
| MyBatis-Plus 3.5.11 | ORM 与基础 CRUD |
| Sa-Token 1.39.0 | 登录认证与权限控制 |

### 存储与中间件

| 技术 | 用途 |
|---|---|
| MySQL 8 | 业务数据存储 |
| Redis 7 | 登录态与缓存 |
| Elasticsearch 8 | 工单、知识文档、知识切片搜索 |
| MongoDB 7 | 访问日志存储 |
| Nacos 2.3.2 | 服务注册与发现 |
| Docker Compose | 本地开发环境编排 |

### AI 与检索

| 技术 | 用途 |
|---|---|
| LangChain4j | LLM 与 RAG 能力集成 |
| DashScope Embedding | 文本向量生成 |
| DeepSeek Chat | 大模型问答能力 |
| SSE | 流式问答输出 |
| Elasticsearch Vector Search | 向量检索 |
| Hybrid Search | 关键词检索 + 向量检索混合召回 |

### 自定义 Starter

| 模块 | 说明 |
|---|---|
| `platform-common-starter` | 通用异常处理、MyBatis-Plus 填充、Jackson 配置等 |
| `platform-satoken-starter` | Sa-Token 自动配置、会话工具、角色常量 |
| `platform-redis-starter` | RedisTemplate 与 Redis 健康检查 |
| `platform-id-starter` | 雪花 ID 生成器 |
| `platform-log-starter` | 接口访问日志采集、脱敏、traceId |

> 说明：RabbitMQ、MinIO、Jaeger 当前主要作为后续扩展组件预留，不属于当前核心业务链路的强依赖。

---

## 4. 系统模块

```text
enterprise-ai-platform
├── platform-gateway              # API 网关，统一路由入口
├── platform-auth-service         # 认证服务，负责登录、登出、用户身份
├── platform-workflow-service     # 工单服务，负责工单创建、查询、接单、分配、状态流转
├── platform-knowledge-service    # 知识库服务，负责知识文档、切片、搜索、向量召回
├── platform-ai-service           # AI 服务，负责 RAG 问答、智能助手、Embedding
├── platform-log-service          # 日志服务，负责访问日志查询
├── platform-api-contract         # 服务间 Feign 契约
├── platform-mq-contract          # MQ 消息契约预留
├── platform-common               # 通用返回体、异常、工具类
├── platform-common-starter       # 通用自动配置 starter
├── platform-satoken-starter      # 认证鉴权 starter
├── platform-redis-starter        # Redis starter
├── platform-id-starter           # ID 生成 starter
├── platform-log-starter          # 访问日志 starter
├── platform-minio-starter        # MinIO starter 预留
└── platform-mq-starter           # MQ starter 预留
```

---

## 5. 服务端口

| 服务 | 默认端口 | 说明 |
|---|---:|---|
| platform-gateway | 8080 | 网关统一入口 |
| platform-auth-service | 8081 | 认证服务 |
| platform-workflow-service | 8082 | 工单服务 |
| platform-knowledge-service | 8083 | 知识库服务 |
| platform-ai-service | 8084 | AI 服务 |
| platform-log-service | 8085 | 日志服务 |
| Nacos | 8848 | 服务注册与发现 |
| MySQL | 3306 | 业务数据库 |
| Redis | 6379 | 缓存与登录态 |
| Elasticsearch | 9200 | 搜索与向量检索 |
| Kibana | 5602 | ES 可视化 |
| MongoDB | 27017 | 访问日志存储 |

---

## 6. 网关路由

所有服务建议通过网关访问：

| 网关路径 | 转发服务 | StripPrefix 后路径 |
|---|---|---|
| `/auth/**` | platform-auth-service | `/**` |
| `/workflow/**` | platform-workflow-service | `/**` |
| `/knowledge/**` | platform-knowledge-service | `/**` |
| `/ai/**` | platform-ai-service | `/**` |
| `/log/**` | platform-log-service | `/**` |

示例：

```http
POST http://localhost:8080/auth/login
GET  http://localhost:8080/workflow/tickets/my
POST http://localhost:8080/ai/assistant/chat
```

---

## 7. 本地启动

### 7.1 环境要求

- JDK 17+
- Maven 3.8+
- Docker / Docker Compose
- MySQL 8+
- Redis 7+
- Elasticsearch 8+
- MongoDB 7+
- Nacos 2.3+

### 7.2 启动基础设施

```bash
docker compose up -d
```

启动后可访问：

| 组件 | 地址 |
|---|---|
| Nacos | `http://localhost:8848/nacos` |
| Kibana | `http://localhost:5602` |
| Elasticsearch | `http://localhost:9200` |

### 7.3 编译项目

```bash
mvn clean package -DskipTests
```

### 7.4 启动服务

建议按以下顺序启动：

```text
1. platform-gateway
2. platform-auth-service
3. platform-workflow-service
4. platform-knowledge-service
5. platform-ai-service
6. platform-log-service
```

也可以在 IDE 中分别启动各模块的 Application 类：

```text
GatewayApplication
AuthApplication
WorkflowApplication
KnowledgeApplication
AiApplication
LogApplication
```

---

## 8. 核心接口概览

详细接口文档建议单独维护在 `docs/api.md` 中。这里仅展示核心入口。

### 认证服务

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/auth/login` | 用户登录 |
| POST | `/auth/logout` | 用户登出 |
| GET | `/auth/me` | 获取当前用户信息 |

### 工单服务

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/workflow/tickets` | 创建工单 |
| GET | `/workflow/tickets/my` | 查询我的工单 |
| GET | `/workflow/tickets/my/{ticketId}` | 查询工单详情 |
| GET | `/workflow/tickets` | 查询处理侧工单列表 |
| POST | `/workflow/tickets/{ticketId}/accept` | 接单 |
| PUT | `/workflow/tickets/{ticketId}/status` | 更新工单状态 |
| POST | `/workflow/tickets/manage/{ticketId}/assign` | 管理员分配工单 |
| GET | `/workflow/tickets/search` | 搜索处理侧工单 |
| GET | `/workflow/types/enabled-options` | 查询启用工单类型 |

### 知识库服务

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/knowledge/documents` | 创建知识文档 |
| PUT | `/knowledge/documents/{id}` | 更新知识文档 |
| PUT | `/knowledge/documents/{documentId}/offline` | 下线知识文档 |
| GET | `/knowledge/documents/search` | 搜索知识文档 |
| GET | `/knowledge/documents/{documentId}/chunks/test` | 查询文档切片 |
| POST | `/knowledge/documents/{documentId}/embeddings/rebuild` | 重建文档切片向量 |
| GET | `/knowledge/chunks/search` | 搜索知识切片 |
| POST | `/knowledge/chunks/vector-search` | 向量检索知识切片 |
| POST | `/knowledge/chunks/hybrid-search` | 混合检索知识切片 |
| POST | `/knowledge/rag/contexts` | 召回 RAG 上下文 |

### AI 服务

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/ai/rag/chat` | RAG 知识问答 |
| POST | `/ai/rag/chat/stream` | RAG 流式问答 |
| POST | `/ai/assistant/chat` | 智能助手对话 |
| POST | `/ai/embeddings` | 批量生成文本向量 |

### 日志服务

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/log/access-logs` | 分页查询访问日志 |
| GET | `/log/access-logs/trace/{traceId}` | 根据 traceId 查询访问日志 |

---

## 9. 设计亮点

### 9.1 登录身份可信化

工单创建、知识文档创建、智能助手创建工单等场景中，后端优先从 Sa-Token Session 获取当前登录用户身份，而不是完全相信前端传入的用户 ID 和用户名，降低越权和伪造风险。

### 9.2 工单处理侧权限收口

员工侧接口只允许查询本人创建的工单；支持人员和管理员通过处理侧接口进入工单处理流程；管理员额外拥有工单分配权限，避免普通用户访问处理侧数据。

### 9.3 知识库检索链路

知识文档创建后会进行内容切片，并写入 Elasticsearch。系统同时支持关键词检索、向量检索和混合检索，为 RAG 问答提供上下文召回能力。

### 9.4 RAG 问答链路

RAG 问答模块将用户问题、知识库召回结果、Prompt 构建和大模型回答串联起来，同时支持普通响应和 SSE 流式响应。

### 9.5 通用能力 starter 化

项目将日志、认证、Redis、ID 生成等能力封装为独立 starter，减少业务服务重复配置，提高模块复用性。

### 9.6 访问日志审计

通过 AOP 自动记录接口访问日志，结合 traceId、用户身份、请求路径、耗时、异常信息等字段，便于问题排查和后台审计。

---

## 10. 当前阶段说明

本项目当前以“轻量、可演示、主流程完整”为优先目标，因此没有在核心业务链路中强依赖 MQ。对于知识库文档处理、向量生成、ES 索引同步等耗时任务，后续可以通过“任务状态表 + 定时补偿”方式实现轻量异步处理；当任务量进一步增大时，再平滑演进为 MQ 消费模型。

---

## 11. 后续优化计划

- [ ] 补充核心单元测试与 Service 测试；
- [ ] 增加工单状态机，收口状态流转规则；
- [ ] 增加工单操作历史表，记录接单、分配、状态变更等流水；
- [ ] 增加工单评论与附件能力；
- [ ] 知识库文档处理改造为任务状态表 + 定时补偿；
- [ ] AI / RAG 接口增加登录校验与限流；
- [ ] 内部接口增加网关隔离，禁止外部直接访问 `/internal/**`；
- [ ] 配置文件中的密码、Token、API Key 改为环境变量；
- [ ] 补充 GitHub Actions，实现自动编译与测试；
- [ ] 补充完整接口文档与系统架构图。


