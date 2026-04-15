# 向量数据库工程实践指南（SmartCrew 项目版）

本文档聚焦“向量数据库”在本项目中的工程化落地与扩展设计，既覆盖当前已实现能力（远程 Chroma + 多知识库命名空间），也给出面向 Milvus / PgVector / Qdrant / Weaviate / Elasticsearch 的可迁移方法。

---

## 目录

1. [向量数据库基础](#1-向量数据库基础)
2. [本项目中的向量数据库定位](#2-本项目中的向量数据库定位)
3. [核心数据模型与命名空间设计](#3-核心数据模型与命名空间设计)
4. [配置体系与默认值](#4-配置体系与默认值)
5. [文档入库主链路（已落地）](#5-文档入库主链路已落地)
6. [远程 Chroma 实现要点（已落地）](#6-远程-chroma-实现要点已落地)
7. [跨向量库扩展架构（重点）](#7-跨向量库扩展架构重点)
8. [主流向量数据库对比](#8-主流向量数据库对比)
9. [从 Chroma 平滑迁移到其他向量库](#9-从-chroma-平滑迁移到其他向量库)
10. [性能、稳定性与成本优化](#10-性能稳定性与成本优化)
11. [故障排查手册](#11-故障排查手册)
12. [面试表达与简历要点](#12-面试表达与简历要点)
13. [落地检查清单](#13-落地检查清单)

---

## 1. 向量数据库基础

### 1.1 什么是向量数据库

向量数据库可以理解为一种“按语义查找内容”的数据库。

传统数据库更擅长做这类查询：

- 精确匹配：`name = '张三'`
- 范围查询：`age > 18`
- 关键词匹配：`title like '%RAG%'`

但如果用户问的是：

- “怎么做知识库问答？”
- “RAG 检索增强怎么接入？”
- “文档问答系统怎么实现？”

这三句话字面并不完全一样，但语义非常接近。普通关系数据库很难直接理解这种“意思相近”，而向量数据库就是为这种语义检索场景设计的。

它的核心思想是：

1. 先把文本、图片等内容通过 Embedding 模型转换成一组数字
2. 这组数字就叫“向量”
3. 将向量存入专门的存储系统
4. 查询时把用户问题也转换成向量
5. 在向量空间中找“距离最近”的内容

所以向量数据库本质上是在做：

```text
内容相似度检索，而不是字符串匹配
```

典型流程：

```text
文本 -> Embedding 模型 -> 向量 -> 向量库索引 -> 相似度检索
```

### 1.2 为什么 RAG 必须依赖向量库

RAG 的目标不是“查到包含同样关键词的文档”，而是“查到和用户问题最相关的知识片段”。

如果只依赖关系库或普通全文检索，往往会遇到这些问题：

- 用户换一种说法就搜不到
- 口语化表达和文档术语对不上
- 同义词、近义句无法命中
- 文档太长时，整篇检索命中但有效内容很少

向量检索可以更自然地处理：

- 同义改写
- 口语表达
- 上下文语义相近
- 跨句式的内容匹配

因此在 RAG 场景里，向量库承担的是“先找到最相关知识片段”的职责，然后 LLM 再基于这些片段生成答案。

### 1.3 什么是向量（Embedding）

向量本身可以先简单理解成“文本的机器语义坐标”。

例如一句文本：

```text
“如何在项目里接入 RAG？”
```

经过嵌入模型处理后，可能会变成：

```text
[0.126, -0.842, 0.337, ..., 0.519]
```

这串数字通常有几百到上千维，比如：

- 768 维
- 1024 维
- 1536 维

这些数字人类读不懂，但模型和向量库可以利用它们计算语义距离。

直观理解：

- 语义越接近，两个向量距离越近
- 语义越不相关，两个向量距离越远

例如：

- “RAG 怎么做” 和 “如何实现检索增强生成” 距离会很近
- “RAG 怎么做” 和 “今天天气如何” 距离会很远

### 1.4 为什么不能直接把向量放进普通数据库里随便查

理论上，向量也可以存进 MySQL / PostgreSQL 的普通字段里，但这并不意味着“普通数据库就等于向量数据库”。

原因在于向量检索需要两类能力：

1. **高维相似度计算**
   普通数据库的主要强项不是高维向量距离计算。

2. **近似最近邻索引（ANN）**
   当数据量变大后，如果每次都全表扫描计算距离，性能会非常差。

向量数据库的核心价值就是：

- 提供适合向量相似度检索的索引结构
- 在大规模数据下仍能较快返回近似最优结果
- 支持批量写入、过滤、TopK 检索等能力

所以“向量数据库”不只是“存向量”，更重要的是“高效检索向量”。

### 1.5 向量数据库和普通数据库的区别

| 对比项 | 普通关系数据库 | 向量数据库 |
|------|------|------|
| 主要查询方式 | 精确匹配、范围过滤、排序 | 相似度检索、TopK 召回 |
| 擅长处理 | 结构化数据 | 非结构化语义数据 |
| 典型数据 | 用户、订单、配置 | 文本切片、图片特征、Embedding |
| 典型索引 | B+Tree、Hash | ANN、HNSW、IVF 等 |
| 查询目标 | 找“值相等/条件满足”的记录 | 找“意思最接近”的记录 |

工程里两者通常不是互斥关系，而是协同关系：

- 关系库存业务主数据、状态、绑定关系、审计信息
- 向量库存语义向量和相似度检索索引

本项目也是这样设计的：

- MySQL/H2 保存 `knowledge_base / knowledge_document / document_chunk`
- Chroma 保存切片对应的向量索引

### 1.6 一个适合初学者的直观例子

假设知识库里有三段内容：

1. “RAG 通过检索相关文档增强大模型回答”
2. “Spring Boot 可以通过配置类装配 Bean”
3. “Chroma 是一种轻量级向量数据库”

用户提问：

```text
“怎么做文档问答系统？”
```

如果只做关键词匹配，可能因为没有“文档问答系统”这几个字而匹配效果一般。  
如果做向量检索，系统会认为它和“RAG 通过检索相关文档增强大模型回答”语义最接近，于是优先召回第 1 段。

这就是向量数据库的价值：

```text
用户说的是 A，系统能理解它真正想找的是 B
```

### 1.7 向量数据库在 RAG 中扮演什么角色

在 RAG 体系里，向量数据库处于“检索层”。

完整链路如下：

```text
离线阶段：
文档 -> 加载 -> 切片 -> Embedding -> 向量库

在线阶段：
用户问题 -> Embedding -> 向量检索 -> 取回相关切片 -> 交给 LLM 生成答案
```

其中：

- LLM 负责“生成答案”
- 向量数据库负责“找资料”

如果没有向量数据库，LLM 只能依赖自己训练时学到的旧知识；有了向量数据库后，LLM 才能在回答前先查项目知识库、产品文档、内部规范等外部知识。

### 1.8 相似度指标

| 指标 | 说明 | 常见场景 |
|------|------|----------|
| Cosine | 比较夹角，忽略向量长度 | 文本语义检索最常见 |
| Dot Product | 点积，受向量长度影响 | 一些模型默认使用 |
| L2 | 欧氏距离，距离越小越相近 | 特定向量索引实现 |

工程上最关键的是：**写入与检索必须使用同一嵌入模型及同一维度**。

### 1.9 初学者需要记住的三句话

1. 向量数据库不是按“字面是否一样”查，而是按“语义是否接近”查。
2. 向量数据库的关键价值不是“存向量”，而是“高效检索相似向量”。
3. 在 RAG 中，向量数据库负责找知识，LLM 负责组织答案。

---

## 2. 本项目中的向量数据库定位

### 2.1 当前能力边界

项目已完成向量数据库基础设施与后台管理闭环：

- 文档上传后自动完成切分、向量化、入库
- 支持多知识库（每个知识库独立命名空间）
- 支持文档重处理、删除时向量与切片联动清理
- 提供统一 `VectorStoreService` 接口，当前实现为远程 Chroma

### 2.2 当前关键实现类

| 能力 | 关键类 |
|------|--------|
| 向量库领域接口 | `com.smartcrew.agent.api.rag.service.VectorStoreService` |
| 远程 Chroma 实现 | `com.smartcrew.agent.core.rag.store.ChromaVectorStoreServiceImpl` |
| 文档处理编排 | `com.smartcrew.agent.core.rag.service.KnowledgeDocumentServiceImpl` |
| 异步处理调度 | `com.smartcrew.agent.core.rag.admin.KnowledgeBaseAdminServiceImpl` + `ragDocumentTaskExecutor` |
| 全局 RAG 配置 | `com.smartcrew.agent.common.config.SmartCrewProperties` |

---

## 3. 核心数据模型与命名空间设计

### 3.1 四张核心表职责

| 表名 | 职责 |
|------|------|
| `knowledge_base` | 知识库元数据，承载 `embedding_model`、`collection_name` |
| `knowledge_document` | 文档记录与状态（pending/processing/completed/failed） |
| `document_chunk` | 切片内容、`vector_id`、metadata 持久化 |
| `agent_knowledge_binding` | Agent 与知识库绑定关系 |

### 3.2 命名空间策略

- 业务层统一使用 `namespace`，而不是写死“Chroma collection”
- 当前映射关系：`namespace = knowledge_base.collection_name`
- 默认规则（后台创建知识库）：`kb_{baseCode}`

这意味着后续切换 Milvus/PgVector/Qdrant 时，业务编排层无需改变调用语义。

### 3.3 关键一致性约束（已实现）

- 知识库已有文档时，不允许修改 `collectionName`
- 知识库已有已完成文档时，不允许修改 `embeddingModel`

目的是避免“集合漂移”和“向量维度混用”导致的检索污染。

---

## 4. 配置体系与默认值

项目通过 `smartcrew.rag.*` 聚合配置，避免零散 `@Value`。

```yaml
smartcrew:
  rag:
    enabled: true
    embedding:
      provider: dashscope
      model: text-embedding-v3
      api-key: ${EMBEDDING_API_KEY:}
      base-url: ${EMBEDDING_BASE_URL:}
    vector-store:
      type: chroma
      chroma:
        base-url: ${CHROMA_BASE_URL:http://localhost:8000}
        timeout-seconds: 60
    document:
      upload-path: ./uploads/knowledge
      splitter:
        type: paragraph
        max-chunk-size: 200
        overlap-size: 50
```

### 4.1 项目默认行为

- `vector-store.type` 当前仅支持 `chroma`（远程模式）
- `embedding.model` 默认 `text-embedding-v3`
- 若未配置 `rag.embedding.api-key/base-url`，会回退使用 `smartcrew.llm.api-key/base-url`

---

## 5. 文档入库主链路（已落地）

### 5.1 一步式入库体验

后台采用“上传即自动入库”：

1. 上传文件并落盘，创建 `knowledge_document`，状态 `pending`
2. 投递异步任务（`ragDocumentTaskExecutor`）
3. 异步执行：
   - 加载文档
   - 文档切片
   - 批量向量化
   - 批量写入向量库
   - 持久化切片与 `vector_id`
4. 成功置 `completed`，失败置 `failed` 并记录 `errorMessage`

### 5.2 状态流转

```text
pending -> processing -> completed
pending -> processing -> failed
```

### 5.3 失败补偿

`KnowledgeDocumentServiceImpl` 在向量写入后若发生异常，会回滚本次新写入向量（`removeAll`），避免脏数据残留。

### 5.4 切片元数据增强

入库前为每个切片追加元数据：

- `base_id/base_code`
- `document_id/document_code/document_name`
- `chunk_index`

有助于检索回溯、调试与后续过滤扩展。

---

## 6. 远程 Chroma 实现要点（已落地）

### 6.1 适配实现结构

`ChromaVectorStoreServiceImpl` 实现 `VectorStoreService`，对外提供统一语义：

- `add/addAll`
- `remove/removeAll`
- `search(namespace, queryEmbedding, maxResults[, minScore])`

### 6.2 命名空间级缓存

实现使用 `ConcurrentHashMap<String, ChromaEmbeddingStore>` 按 `collectionName` 缓存 store：

- 首次访问某命名空间时懒加载创建
- 避免每次请求重复创建连接对象
- 更适合多知识库场景

### 6.3 远程模式关键参数

- `baseUrl`: Chroma 服务地址
- `collectionName`: 由业务层传入命名空间
- `timeout`: 请求超时秒数

### 6.4 输入保护

- `namespace` 为空时直接抛业务异常
- 批量写/删遇空集合直接短路返回

这类防御可减少无效远程调用与隐式错误。

---

## 7. 跨向量库扩展架构（重点）

### 7.1 当前抽象为什么可迁移

业务层依赖的是 `VectorStoreService`，不是 Chroma SDK，因此替换向量库只需要新增一个实现类。

### 7.2 推荐适配分层

```text
KnowledgeDocumentService / RetrievalService
            |
     VectorStoreService（业务接口）
            |
   XxxVectorStoreServiceImpl（供应商适配）
            |
    Vendor SDK / HTTP API
```

### 7.3 新增向量库时的最小改造面

1. 新增实现类，如 `MilvusVectorStoreServiceImpl`
2. 扩展 `SmartCrewProperties` 增加 `smartcrew.rag.vector-store.milvus.*`
3. 用 `@ConditionalOnProperty(... type=milvus)` 切换装配
4. 不改文档处理主链路、不改后台页面调用

### 7.4 接口抽象建议（可继续演进）

如果后续要增强过滤或混合检索，建议在 `VectorStoreService` 增加可选查询对象（例如 filter、topK、scoreThreshold、rerankHint），保持向后兼容。

---

## 8. 主流向量数据库对比

| 方案 | 优势 | 风险/成本 | 推荐场景 |
|------|------|-----------|----------|
| Chroma | 简单易上手，开发效率高 | 分布式能力相对弱 | 原型期、中小规模知识库 |
| Milvus | 大规模检索能力强，索引类型丰富 | 运维复杂度较高 | 大规模生产检索平台 |
| PgVector | 复用 PostgreSQL 生态，事务与运维统一 | 超大规模下检索性能受限 | 业务库就是 PG、追求统一栈 |
| Qdrant | API 友好，过滤能力强，部署灵活 | 生态广度稍弱于头部方案 | 中大规模、重过滤场景 |
| Weaviate | 内置 schema/模块能力，功能完整 | 学习和运维成本中等偏高 | 需要较完整 AI 数据平台能力 |
| Elasticsearch 向量检索 | 与全文检索融合好 | 向量专项能力不如纯向量库 | 关键词 + 向量混合检索 |

### 8.1 选型关键维度

- 数据规模（百万/千万/亿级向量）
- 实时写入频率与延迟要求
- 元数据过滤复杂度
- 混合检索需求（关键词 + 向量）
- 团队运维能力与现有技术栈

### 8.2 分阶段建议

1. 起步阶段：Chroma（低门槛，迭代快）
2. 业务放量：Qdrant / PgVector（按团队栈选择）
3. 大规模平台化：Milvus（高吞吐和大规模索引）

---

## 9. 从 Chroma 平滑迁移到其他向量库

### 9.1 迁移原则

- 不改业务接口：继续使用 `VectorStoreService`
- 不改业务主键：保留 `knowledge_base.collection_name` 作为逻辑命名空间
- 先双写再切换，最后清理

### 9.2 推荐步骤

1. 实现新向量库适配器并灰度环境压测
2. 增加“迁移任务”按知识库重建向量索引
3. 双写阶段：新上传文档同时写旧库和新库
4. 读流量灰度切新库，监控召回率/延迟/错误率
5. 全量切换后停止旧库写入并归档

### 9.3 高风险点

- 嵌入模型维度不一致
- 相似度度量差异导致召回分布变化
- 元数据过滤语法不兼容
- 批量写入 API 限制差异导致吞吐下降

---

## 10. 性能、稳定性与成本优化

### 10.1 切片参数调优建议

当前默认：

- `type=paragraph`
- `maxChunkSize=200`
- `overlapSize=50`

调优经验：

- 文档偏结构化（手册/规范）：优先 `paragraph`
- 问答偏短句匹配：可尝试 `sentence`
- 召回不准：适当减小 `maxChunkSize`
- 上下文断裂：适当增大 `overlapSize`

### 10.2 批量化策略

- 统一走 `embedAll + addAll`，减少网络往返
- 对超大文档可分批次提交，避免单次请求过大

### 10.3 异步并发控制

当前线程池：

- 核心线程：2
- 最大线程：4
- 队列：100

如果上传峰值升高，可按 CPU/网络/向量库吞吐联动调整，避免盲目加线程导致下游雪崩。

### 10.4 成本控制

- 大模型嵌入成本主要与“切片数量”线性相关
- 通过去重、清洗低价值文档、合理切片参数可显著降低费用

---

## 11. 故障排查手册

### 11.1 文档一直 pending

排查：

1. `smartcrew.rag.enabled` 是否开启
2. `ragDocumentTaskExecutor` 是否成功装配
3. 异步日志中是否有异常堆栈

### 11.2 文档 failed

常见原因：

- 文件解析失败（格式损坏/编码异常）
- 嵌入 API Key 缺失或限流
- Chroma 服务不可达或超时
- 向量返回 ID 数量与切片数量不一致

### 11.3 检索结果质量波动

重点检查：

- 是否误改 `embeddingModel`
- 是否误改 `collectionName`
- 切片参数是否变化过大
- 是否混入了不相关文档

---

## 12. 面试表达与简历要点

### 12.1 一句话版本

“我在 SmartCrew 中搭建了可替换向量库的 RAG 基础设施，用统一 `VectorStoreService` 抽象实现远程 Chroma，支持多知识库命名空间、异步入库、失败补偿与一致性约束，后续可低成本迁移 Milvus/PgVector/Qdrant。”

### 12.2 可展开的技术亮点

- 通过 `namespace -> collection_name` 建立业务与向量库解耦层
- 通过状态机和异步执行器保障后台可观测与稳定性
- 通过“字段冻结规则”避免生产向量污染
- 通过接口抽象预留多向量库扩展能力

### 12.3 面试高频追问建议

1. 为什么不直接在业务层写 Chroma API？
2. 为什么 `collectionName` 和 `embeddingModel` 要做只读约束？
3. 如果要迁移 Milvus，哪些地方可以不改？
4. 如何验证新旧向量库检索效果一致性？

---

## 13. 落地检查清单

### 13.1 开发检查

- [ ] `smartcrew.rag.enabled=true`
- [ ] `vector-store.type=chroma`
- [ ] `CHROMA_BASE_URL` 可访问
- [ ] 嵌入模型 API Key 可用
- [ ] `upload-path` 目录有写权限

### 13.2 质量检查

- [ ] 上传后状态可从 `pending` 流转到 `completed`
- [ ] 失败时 `errorMessage` 可定位问题
- [ ] 删除文档会同步删除切片与向量
- [ ] 重新处理可重建切片和向量

### 13.3 可扩展检查

- [ ] 业务层仅依赖 `VectorStoreService`
- [ ] 新增向量库实现不影响控制器与页面
- [ ] 迁移方案包含灰度与回滚路径

---

## 附：与本项目代码的映射速查

| 主题 | 代码位置 |
|------|----------|
| 向量库统一接口 | `smartcrew-modules-api/.../rag/service/VectorStoreService.java` |
| Chroma 远程实现 | `smartcrew-modules/.../rag/store/ChromaVectorStoreServiceImpl.java` |
| 文档处理编排 | `smartcrew-modules/.../rag/service/KnowledgeDocumentServiceImpl.java` |
| 后台异步触发 | `smartcrew-modules/.../rag/admin/KnowledgeBaseAdminServiceImpl.java` |
| 异步线程池配置 | `smartcrew-modules/.../config/RagAdminConfig.java` |
| RAG 配置聚合 | `smartcrew-common/.../config/SmartCrewProperties.java` |
