# 接口文档

> 项目：企业智能知识库与流程协同平台  
> 版本：v1.0  
> 说明：本文档根据当前后端 Controller、DTO、VO、网关路由和权限注解整理，适合放在 `docs/api.md` 中作为 GitHub/简历项目接口说明。

---

## 1. 基础说明

### 1.1 网关地址

本项目通过 `platform-gateway` 统一转发请求：

```text
http://localhost:8080
```

各服务通过网关访问时需要增加模块前缀：

| 模块 | 网关前缀 | 后端服务 | 默认端口 |
|---|---|---|---:|
| 认证服务 | `/auth` | `platform-auth-service` | 8081 |
| 工单服务 | `/workflow` | `platform-workflow-service` | 8082 |
| 知识库服务 | `/knowledge` | `platform-knowledge-service` | 8083 |
| AI 服务 | `/ai` | `platform-ai-service` | 8084 |
| 日志服务 | `/log` | `platform-log-service` | 8085 |

示例：

```text
POST http://localhost:8080/auth/login
GET  http://localhost:8080/workflow/tickets/my
POST http://localhost:8080/ai/assistant/chat
```

---

### 1.2 认证方式

系统使用 Sa-Token。登录成功后，后续请求在 Header 中携带 token：

```http
Authorization: <token>
```

角色编码：

| 角色 | 说明 |
|---|---|
| `ADMIN` | 系统管理员 |
| `EMPLOYEE` | 普通员工 |
| `SUPPORT` | 支持人员/处理人员 |
| `KNOWLEDGE_ADMIN` | 知识库管理员，当前代码中暂未作为主要接口准入角色使用 |

内部服务调用接口使用：

```http
X-Internal-Token: <internal-token>
```

内部接口路径为 `/internal/**`，由 `InternalApiInterceptor` 拦截校验。

---

### 1.3 通用响应格式

大部分业务接口统一返回：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

分页接口返回：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "total": 100,
    "pageNum": 1,
    "pageSize": 10,
    "records": []
  }
}
```

说明：`RAG` 与 `Embedding` 部分接口当前直接返回业务对象，没有额外包裹 `Result`。

---

## 2. 认证接口

### 2.1 用户登录

```http
POST /auth/login
```

权限：无需登录。

请求体：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `username` | string | 是 | 用户名 |
| `password` | string | 是 | 密码 |

示例：

```json
{
  "username": "admin",
  "password": "123456"
}
```

响应 `data`：

| 字段 | 类型 | 说明 |
|---|---|---|
| `token` | string | 登录 token |
| `userId` | long | 用户 ID |
| `username` | string | 用户名 |
| `realName` | string | 真实姓名 |
| `roleCodes` | array | 角色编码列表 |

---

### 2.2 用户登出

```http
POST /auth/logout
```

权限：登录用户。

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

---

### 2.3 当前用户信息

```http
GET /auth/me
```

权限：登录用户。

响应 `data`：

| 字段 | 类型 | 说明 |
|---|---|---|
| `userId` | long | 用户 ID |
| `username` | string | 用户名 |
| `realName` | string | 真实姓名 |
| `deptName` | string | 部门名称 |
| `roleCodes` | array | 角色编码列表 |

---

## 3. 工单接口

工单状态：

| 状态 | 说明 |
|---|---|
| `PENDING` | 待处理 |
| `PROCESSING` | 处理中 |
| `RESOLVED` | 已解决 |
| `REJECTED` | 已拒绝 |

工单来源：

| 来源 | 说明 |
|---|---|
| `MANUAL` | 用户手工创建 |
| `AI_AGENT` | AI Agent 创建 |

---

### 3.1 创建工单

```http
POST /workflow/tickets
```

权限：登录用户。

说明：创建人信息从当前登录态获取，前端不允许传入 `creatorId`、`creatorName`。

请求体：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `ticketTypeCode` | string | 是 | 工单类型编码 |
| `title` | string | 是 | 工单标题，最长 128 字符 |
| `content` | string | 是 | 工单内容，最长 2000 字符 |
| `priority` | string | 否 | 优先级 |

示例：

```json
{
  "ticketTypeCode": "IT_SUPPORT",
  "title": "无法访问知识库系统",
  "content": "登录后点击知识库页面一直加载失败",
  "priority": "HIGH"
}
```

响应 `data`：

| 字段 | 类型 | 说明 |
|---|---|---|
| `ticketId` | long | 工单 ID |
| `ticketNo` | string | 工单编号 |
| `status` | string | 工单状态 |

---

### 3.2 我的工单列表

```http
GET /workflow/tickets/my
```

权限：登录用户。

说明：只查询当前登录用户本人创建的工单。

查询参数：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `status` | string | 否 | 工单状态 |
| `pageNum` | int | 否 | 页码，默认 1 |
| `pageSize` | int | 否 | 每页数量，默认 10 |

响应 `data.records[]`：

| 字段 | 类型 | 说明 |
|---|---|---|
| `ticketId` | long | 工单 ID |
| `ticketNo` | string | 工单编号 |
| `ticketTypeCode` | string | 工单类型编码 |
| `ticketTypeName` | string | 工单类型名称 |
| `title` | string | 标题 |
| `status` | string | 状态 |
| `priority` | string | 优先级 |
| `source` | string | 来源 |
| `assigneeName` | string | 当前处理人 |
| `createdAt` | datetime | 创建时间 |
| `updatedAt` | datetime | 更新时间 |

---

### 3.3 工单详情

```http
GET /workflow/tickets/my/{ticketId}
```

权限：登录用户。

说明：管理员可查看所有工单，非管理员只能查看自己的工单。

路径参数：

| 字段 | 类型 | 说明 |
|---|---|---|
| `ticketId` | long | 工单 ID |

响应 `data`：

| 字段 | 类型 | 说明 |
|---|---|---|
| `ticketId` | long | 工单 ID |
| `ticketNo` | string | 工单编号 |
| `ticketTypeId` | long | 工单类型 ID |
| `ticketTypeCode` | string | 工单类型编码 |
| `ticketTypeName` | string | 工单类型名称 |
| `title` | string | 标题 |
| `content` | string | 内容 |
| `status` | string | 状态 |
| `priority` | string | 优先级 |
| `source` | string | 来源 |
| `sourceRef` | string | 来源引用 |
| `creatorId` | long | 创建人 ID |
| `creatorName` | string | 创建人名称 |
| `assigneeId` | long | 处理人 ID |
| `assigneeName` | string | 处理人名称 |
| `statusRemark` | string | 状态说明 |
| `closedAt` | datetime | 关闭时间 |
| `createdAt` | datetime | 创建时间 |
| `updatedAt` | datetime | 更新时间 |

---

### 3.4 处理侧工单列表

```http
GET /workflow/tickets
```

权限：`ADMIN` 或 `SUPPORT`。

说明：支持人员和管理员查看处理工作台中的工单。

查询参数：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `keyword` | string | 否 | 关键词 |
| `status` | string | 否 | 工单状态 |
| `ticketTypeCode` | string | 否 | 工单类型编码 |
| `source` | string | 否 | 工单来源 |
| `creatorId` | long | 否 | 创建人 ID |
| `assigneeId` | long | 否 | 处理人 ID |
| `mineOnly` | boolean | 否 | 是否仅看当前登录人负责的工单 |
| `unassignedOnly` | boolean | 否 | 是否仅看未分派工单 |
| `pageNum` | int | 否 | 页码，默认 1 |
| `pageSize` | int | 否 | 每页数量，默认 10 |

响应 `data.records[]`：

| 字段 | 类型 | 说明 |
|---|---|---|
| `ticketId` | long | 工单 ID |
| `ticketNo` | string | 工单编号 |
| `ticketTypeCode` | string | 工单类型编码 |
| `ticketTypeName` | string | 工单类型名称 |
| `title` | string | 标题 |
| `status` | string | 状态 |
| `priority` | string | 优先级 |
| `source` | string | 来源 |
| `creatorId` | long | 创建人 ID |
| `creatorName` | string | 创建人名称 |
| `assigneeId` | long | 处理人 ID |
| `assigneeName` | string | 处理人名称 |
| `createdAt` | datetime | 创建时间 |
| `updatedAt` | datetime | 更新时间 |

---

### 3.5 接单

```http
POST /workflow/tickets/{ticketId}/accept
```

权限：`ADMIN` 或 `SUPPORT`。

说明：将“待处理且未分派”的工单认领为当前处理人。并发接单场景由 Service 层通过条件更新控制。

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

---

### 3.6 更新工单状态

```http
PUT /workflow/tickets/{ticketId}/status
```

权限：`ADMIN` 或 `SUPPORT`。

说明：当前阶段主要用于将处理中工单更新为已解决或已拒绝。

请求体：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `targetStatus` | string | 是 | 目标状态，目前主要为 `RESOLVED` / `REJECTED` |
| `statusRemark` | string | 是 | 状态说明，例如处理结果或拒绝原因 |

示例：

```json
{
  "targetStatus": "RESOLVED",
  "statusRemark": "已重置用户账号权限，问题已解决"
}
```

---

### 3.7 管理员分配工单

```http
POST /workflow/tickets/manage/{ticketId}/assign
```

权限：`ADMIN`。

请求体：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `assigneeId` | long | 是 | 被分配处理人 ID |
| `assigneeName` | string | 是 | 被分配处理人名称 |

> 优化建议：当前 `assigneeName` 由前端传入，后续建议改为后端根据 `assigneeId` 查询用户信息，避免前端伪造处理人名称。

---

### 3.8 ES 搜索处理侧工单

```http
GET /workflow/tickets/search
```

权限：`ADMIN` 或 `SUPPORT`。

查询参数同“处理侧工单列表”。

说明：基于 Elasticsearch 搜索处理侧工单。

---

### 3.9 查询启用工单类型

```http
GET /workflow/types/enabled-options
```

权限：登录用户。

响应 `data[]`：

| 字段 | 类型 | 说明 |
|---|---|---|
| `ticketTypeId` | long | 工单类型 ID |
| `typeCode` | string | 类型编码 |
| `typeName` | string | 类型名称 |
| `defaultPriority` | string | 默认优先级 |

---

## 4. 知识库接口

知识文档状态：

| 状态 | 说明 |
|---|---|
| `DRAFT` | 草稿 |
| `PUBLISHED` | 已发布 |
| `OFFLINE` | 已下线 |

知识切片状态：

| 状态 | 说明 |
|---|---|
| `ACTIVE` | 可用 |
| `OFFLINE` | 已下线 |

---

### 4.1 创建知识文档

```http
POST /knowledge/documents
```

权限：`ADMIN` 或 `SUPPORT`。

说明：创建知识库文档，并同步写入 Elasticsearch。

请求体：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `title` | string | 是 | 文档标题，最长 128 字符 |
| `summary` | string | 否 | 摘要，最长 512 字符 |
| `content` | string | 是 | 正文，最长 20000 字符 |
| `categoryId` | long | 否 | 分类 ID |
| `categoryName` | string | 否 | 分类名称，最长 64 字符 |
| `tags` | string | 否 | 标签，最长 255 字符 |

示例：

```json
{
  "title": "VPN 使用说明",
  "summary": "企业 VPN 常见问题与处理步骤",
  "content": "1. 下载客户端... 2. 输入账号...",
  "categoryId": 1,
  "categoryName": "IT支持",
  "tags": "VPN,网络,账号"
}
```

响应：`Result<Void>`。

---

### 4.2 更新知识文档

```http
PUT /knowledge/documents/{id}
```

权限：`ADMIN` 或 `SUPPORT`。

请求体同“创建知识文档”。

---

### 4.3 下线知识文档

```http
PUT /knowledge/documents/{documentId}/offline
```

权限：`ADMIN` 或 `SUPPORT`。

说明：将知识文档状态更新为已下线，并同步 Elasticsearch。

---

### 4.4 搜索知识文档

```http
GET /knowledge/documents/search
```

权限：登录用户。

查询参数：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `keyword` | string | 否 | 关键词 |
| `status` | string | 否 | 文档状态 |
| `categoryId` | long | 否 | 分类 ID |
| `pageNum` | int | 否 | 页码，默认 1 |
| `pageSize` | int | 否 | 每页数量，默认 10 |

响应 `data.records[]`：

| 字段 | 类型 | 说明 |
|---|---|---|
| `documentId` | long | 文档 ID |
| `title` | string | 标题 |
| `summary` | string | 摘要 |
| `categoryId` | long | 分类 ID |
| `categoryName` | string | 分类名称 |
| `tags` | string | 标签 |
| `status` | string | 状态 |
| `creatorName` | string | 创建人 |
| `publishedAt` | datetime | 发布时间 |
| `updatedAt` | datetime | 更新时间 |

---

### 4.5 查询文档切片

```http
GET /knowledge/documents/{documentId}/chunks/test
```

权限：`ADMIN` 或 `SUPPORT`。

说明：查询指定知识文档自动生成的切片列表。

响应 `data[]`：

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | long | 切片 ID |
| `documentId` | long | 文档 ID |
| `chunkNo` | int | 切片序号 |
| `chunkText` | string | 切片文本 |
| `chunkHash` | string | 切片哈希 |
| `tokenCount` | int | token 数量 |
| `status` | string | 切片状态 |
| `createdAt` | datetime | 创建时间 |
| `updatedAt` | datetime | 更新时间 |

---

### 4.6 重建文档切片向量

```http
POST /knowledge/documents/{documentId}/embeddings/rebuild
```

权限：`ADMIN` 或 `SUPPORT`。

说明：调用 AI 服务为知识文档切片生成 embedding，并返回摘要信息。

响应 `data`：

| 字段 | 类型 | 说明 |
|---|---|---|
| `documentId` | long | 文档 ID |
| `modelName` | string | embedding 模型名称 |
| `dimension` | int | 向量维度 |
| `vectorCount` | int | 生成向量数量 |

---

### 4.7 ES 搜索知识切片

```http
GET /knowledge/chunks/search
```

权限：`ADMIN` 或 `SUPPORT`。

查询参数：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `keyword` | string | 否 | 关键词 |
| `documentId` | long | 否 | 文档 ID |
| `categoryId` | long | 否 | 分类 ID |
| `status` | string | 否 | 切片状态 |
| `pageNum` | int | 否 | 页码，默认 1 |
| `pageSize` | int | 否 | 每页数量，默认 10 |

响应 `data.records[]`：

| 字段 | 类型 | 说明 |
|---|---|---|
| `chunkId` | long | 切片 ID |
| `documentId` | long | 文档 ID |
| `chunkNo` | int | 切片序号 |
| `chunkText` | string | 切片文本 |
| `documentTitle` | string | 文档标题 |
| `categoryId` | long | 分类 ID |
| `categoryName` | string | 分类名称 |
| `tags` | string | 标签 |
| `status` | string | 状态 |
| `tokenCount` | int | token 数量 |
| `updatedAt` | datetime | 更新时间 |

---

### 4.8 向量检索知识切片

```http
POST /knowledge/chunks/vector-search
```

权限：`ADMIN` 或 `SUPPORT`。

请求体：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `question` | string | 是 | 用户问题，最长 500 字符 |
| `topK` | int | 否 | 返回数量，1 到 10，默认 5 |
| `categoryId` | long | 否 | 分类 ID |

响应 `data[]`：

| 字段 | 类型 | 说明 |
|---|---|---|
| `chunkId` | long | 切片 ID |
| `documentId` | long | 文档 ID |
| `chunkNo` | int | 切片序号 |
| `chunkText` | string | 切片文本 |
| `documentTitle` | string | 文档标题 |
| `categoryId` | long | 分类 ID |
| `categoryName` | string | 分类名称 |
| `tags` | string | 标签 |
| `score` | float | 相似度得分 |

---

### 4.9 混合检索知识切片

```http
POST /knowledge/chunks/hybrid-search
```

权限：`ADMIN` 或 `SUPPORT`。

说明：融合全文检索和向量检索结果，返回相关知识文档切片。

请求体同“向量检索知识切片”。

响应 `data[]`：

| 字段 | 类型 | 说明 |
|---|---|---|
| `chunkId` | long | 切片 ID |
| `documentId` | long | 文档 ID |
| `chunkNo` | int | 切片序号 |
| `chunkText` | string | 切片文本 |
| `documentTitle` | string | 文档标题 |
| `categoryId` | long | 分类 ID |
| `categoryName` | string | 分类名称 |
| `tags` | string | 标签 |
| `textRankScore` | float | 全文检索排名得分 |
| `vectorRankScore` | float | 向量检索排名得分 |
| `vectorRawScore` | float | 向量检索原始得分 |
| `finalScore` | float | 融合后得分 |
| `matchType` | string | 命中类型：`TEXT` / `VECTOR` / `BOTH` |

---

### 4.10 召回 RAG 上下文

```http
POST /knowledge/rag/contexts
```

权限：`ADMIN` 或 `SUPPORT`。

说明：基于混合检索召回适合大模型使用的知识片段。

请求体：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `question` | string | 是 | 用户问题 |
| `topK` | int | 否 | 召回数量 |
| `categoryId` | long | 否 | 分类 ID |

响应 `data[]`：

| 字段 | 类型 | 说明 |
|---|---|---|
| `chunkId` | long | 切片 ID |
| `documentId` | long | 文档 ID |
| `chunkNo` | int | 切片序号 |
| `documentTitle` | string | 文档标题 |
| `categoryName` | string | 分类名称 |
| `content` | string | 干净的知识片段内容 |
| `finalScore` | float | 融合检索得分 |
| `matchType` | string | 命中类型 |

---

## 5. AI 接口

### 5.1 智能助手对话

```http
POST /ai/assistant/chat
```

权限：登录用户。

说明：统一承接知识问答、流程咨询和 AI 创建工单等自然语言请求。

请求体：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `question` | string | 是 | 用户问题，最长 500 字符 |
| `topK` | int | 否 | RAG 召回数量，1 到 10，默认 5 |
| `categoryId` | long | 否 | 知识分类 ID |
| `autoCreateTicket` | boolean | 否 | 是否允许 Agent 自动创建工单，默认 true |

> 说明：当前 Controller 已从 Sa-Token Session 获取登录用户身份，`creatorId`、`creatorName` 不应由前端传入。

示例：

```json
{
  "question": "我无法登录系统，帮我创建一个工单",
  "topK": 5,
  "autoCreateTicket": true
}
```

响应 `data`：

| 字段 | 类型 | 说明 |
|---|---|---|
| `intent` | string | 意图，例如 `KNOWLEDGE_QA` / `TICKET_CREATE` / `TICKET_QUERY` |
| `answer` | string | 返回给用户的自然语言结果 |
| `toolExecuted` | boolean | 是否执行 Tool |
| `toolName` | string | Tool 名称 |
| `ticket` | object | 工单创建结果 |
| `rag` | object | RAG 问答结果 |

`ticket` 字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| `ticketId` | long | 工单 ID |
| `ticketNo` | string | 工单编号 |
| `status` | string | 工单状态 |
| `ticketTypeCode` | string | 工单类型编码 |
| `title` | string | 工单标题 |

---

### 5.2 RAG 知识库问答

```http
POST /ai/rag/chat
```

权限：当前代码中未加登录注解，建议后续补充 `@SaCheckLogin` 和限流。

请求体：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `question` | string | 是 | 用户问题，最长 500 字符 |
| `topK` | int | 否 | 召回数量，1 到 10，默认 5 |
| `categoryId` | long | 否 | 知识分类 ID |

响应：

| 字段 | 类型 | 说明 |
|---|---|---|
| `answer` | string | 大模型回答 |
| `citations` | array | 引用来源 |
| `rejected` | boolean | 是否因知识库证据不足拒答 |
| `rejectReason` | string | 拒答原因 |

`citations[]`：

| 字段 | 类型 | 说明 |
|---|---|---|
| `chunkId` | long | 切片 ID |
| `documentId` | long | 文档 ID |
| `documentTitle` | string | 文档标题 |
| `chunkNo` | int | 切片序号 |
| `categoryName` | string | 分类名称 |
| `contentPreview` | string | 引用片段预览 |
| `score` | float | 检索得分 |
| `matchType` | string | 命中类型 |

---

### 5.3 RAG 流式问答

```http
POST /ai/rag/chat/stream
```

权限：当前代码中未加登录注解，建议后续补充 `@SaCheckLogin` 和限流。

响应类型：

```http
Content-Type: text/event-stream
```

说明：基于 SSE 返回流式回答。

---

### 5.4 批量生成文本向量

```http
POST /ai/embeddings
```

权限：当前代码中未加登录注解。该接口主要供知识库服务内部调用，建议后续改为 `/internal/embeddings` 或增加内部 token 校验。

请求体：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `texts` | array | 是 | 待向量化文本列表 |

响应：

| 字段 | 类型 | 说明 |
|---|---|---|
| `modelName` | string | 向量模型名称 |
| `dimension` | int | 向量维度 |
| `vectors` | array | 向量结果 |

---

## 6. 日志接口

### 6.1 分页查询接口访问日志

```http
GET /log/access-logs
```

权限：`ADMIN`。

查询参数：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `pageNum` | long | 否 | 页码，默认 1 |
| `pageSize` | long | 否 | 每页数量，默认 20，最大 100 |
| `traceId` | string | 否 | 链路 ID |
| `serviceName` | string | 否 | 服务名 |
| `url` | string | 否 | 请求 URL |
| `userId` | string | 否 | 用户 ID |
| `success` | boolean | 否 | 是否成功 |
| `startTime` | datetime | 否 | 开始时间，格式 `yyyy-MM-dd HH:mm:ss` |
| `endTime` | datetime | 否 | 结束时间，格式 `yyyy-MM-dd HH:mm:ss` |

响应 `data.records[]`：

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | string | 日志 ID |
| `traceId` | string | 链路 ID |
| `serviceName` | string | 服务名称 |
| `operation` | string | 操作说明 |
| `httpMethod` | string | HTTP 方法 |
| `url` | string | 请求地址 |
| `classMethod` | string | 类方法 |
| `clientIp` | string | 客户端 IP |
| `userAgent` | string | User-Agent |
| `userId` | string | 用户 ID |
| `success` | boolean | 是否成功 |
| `errorType` | string | 异常类型 |
| `errorMessage` | string | 异常信息 |
| `costMs` | long | 耗时毫秒 |
| `requestTime` | instant | 请求时间 |
| `createdAt` | instant | 创建时间 |

---

### 6.2 根据 traceId 查询访问日志

```http
GET /log/access-logs/trace/{traceId}
```

权限：`ADMIN`。

响应 `data[]`：`AccessLogItemVO` 列表。

---

## 7. 内部接口

内部接口仅用于服务间调用，不建议暴露给前端或外部调用。

### 7.1 AI 创建工单

```http
POST /workflow/internal/ai/tickets
```

权限：内部 token。

Header：

```http
X-Internal-Token: <internal-token>
```

请求体：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `creatorId` | long | 是 | 创建人 ID |
| `creatorName` | string | 是 | 创建人姓名 |
| `ticketTypeCode` | string | 是 | 工单类型编码 |
| `title` | string | 是 | 标题，最长 128 字符 |
| `content` | string | 是 | 内容，最长 2000 字符 |
| `priority` | string | 否 | 优先级 |
| `sourceRef` | string | 否 | AI 来源引用 |

响应 `data`：

| 字段 | 类型 | 说明 |
|---|---|---|
| `ticketId` | long | 工单 ID |
| `ticketNo` | string | 工单编号 |
| `status` | string | 工单状态 |

---

### 7.2 AI 查询工单

```http
POST /workflow/internal/ai/tickets/query
```

权限：内部 token。

请求体：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `creatorId` | long | 否 | 创建人 ID |
| `assigneeId` | long | 否 | 处理人 ID |
| `scope` | string | 否 | 查询范围，如 `RELATED_TO_ME` / `CREATED_BY_ME` / `ASSIGNED_TO_ME` |
| `ticketNo` | string | 否 | 工单编号 |
| `status` | string | 否 | 工单状态 |
| `pageNum` | int | 否 | 页码 |
| `pageSize` | int | 否 | 每页数量 |

响应 `data`：

| 字段 | 类型 | 说明 |
|---|---|---|
| `queryType` | string | 查询类型 |
| `total` | int | 总数量 |
| `tickets` | array | 工单列表 |

---

### 7.3 内部 RAG 上下文召回

```http
POST /knowledge/internal/rag/contexts
```

权限：内部 token。

说明：供 AI 服务调用，基于知识库召回可放入 Prompt 的上下文片段。

请求体：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `question` | string | 是 | 用户问题 |
| `topK` | int | 否 | 召回数量 |
| `categoryId` | long | 否 | 分类 ID |

响应 `data[]`：

| 字段 | 类型 | 说明 |
|---|---|---|
| `chunkId` | long | 切片 ID |
| `documentId` | long | 文档 ID |
| `chunkNo` | int | 切片序号 |
| `documentTitle` | string | 文档标题 |
| `categoryName` | string | 分类名称 |
| `content` | string | 干净的知识片段内容 |
| `finalScore` | float | 融合检索得分 |
| `matchType` | string | 命中类型 |

---

## 8. 当前接口层优化建议

这些问题不影响当前演示，但建议后续迭代时处理，能提升项目的后端专业度：

1. `/ai/rag/chat`、`/ai/rag/chat/stream` 建议增加登录校验和用户级限流。
2. `/ai/embeddings` 建议改为内部接口，避免模型 API 被外部刷接口消耗。
3. 管理员分配工单时，`assigneeName` 建议由后端根据 `assigneeId` 查询，避免前端伪造。
4. `pageSize` 建议在所有分页查询中统一设置最大值，例如最大 100。
5. 知识文档创建、更新、下线建议返回文档 ID 或操作结果摘要，便于前端刷新状态。
6. RAG 流式问答建议补充超时、异常 SSE 事件和客户端断开处理说明。
7. 内部 token 当前为开发默认值，后续应通过环境变量注入，不要提交真实密钥。

---
。
