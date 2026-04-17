---
name: multi-agent-pipeline
description: Implements a pipeline-style multi-agent coordination system where tasks flow through 11 agents in sequence. Invoke when user needs multi-agent workflow coordination or pipeline task execution.
---

# 多Agent流水线协调系统

## 系统架构

本系统实现了一个流水线模式的多Agent协调框架，任务按照预定义的顺序依次经过各个Agent处理。

## 流水线阶段定义

### 阶段 1：需求分析阶段
**负责Agent**：`@market-analyst` → `@product-manager`

1. **@market-analyst**（市场与商业分析）
   - 输入：原始需求/想法
   - 输出：市场分析报告、竞品分析、商业可行性评估
   - 交付物：`docs/market-analysis.md`

2. **@product-manager**（产品需求分析）
   - 输入：市场分析报告
   - 输出：产品需求文档（PRD）、功能列表、优先级排序
   - 交付物：`docs/prd.md`

### 阶段 2：设计阶段
**负责Agent**：`@system-architect`

3. **@system-architect**（系统架构设计）
   - 输入：产品需求文档
   - 输出：技术架构设计、接口设计、数据库设计
   - 交付物：`docs/architecture.md`, `docs/api-design.md`

### 阶段 3：开发阶段
**负责Agent**：`@backend-developer` → `@frontend-developer`

4. **@backend-developer**（后端开发）
   - 输入：技术架构设计、接口设计
   - 输出：后端代码实现、API接口、单元测试
   - 交付物：`src/main/java/` 下的代码文件

5. **@frontend-developer**（前端开发）
   - 输入：接口设计、API文档
   - 输出：前端界面、组件、页面
   - 交付物：`src/` 下的前端代码文件

### 阶段 4：质量保障阶段
**负责Agent**：`@code-reviewer` → `@test-engineer` → `@bug-fixer`

6. **@code-reviewer**（代码审查）
   - 输入：前后端代码
   - 输出：代码审查报告、改进建议
   - 交付物：`docs/code-review.md`

7. **@test-engineer**（测试工程师）
   - 输入：代码、需求文档
   - 输出：测试用例、测试报告
   - 交付物：`src/test/` 下的测试文件, `docs/test-report.md`

8. **@bug-fixer**（Bug修复专家）
   - 输入：测试报告、代码审查报告
   - 输出：修复后的代码
   - 交付物：修复的代码文件

### 阶段 5：部署与文档阶段
**负责Agent**：`@devops-engineer` → `@document-writer`

9. **@devops-engineer**（DevOps与部署）
   - 输入：可运行的代码
   - 输出：部署配置、CI/CD脚本、Docker配置
   - 交付物：`Dockerfile`, `docker-compose.yml`, `.github/workflows/`

10. **@document-writer**（文档与知识管理）
    - 输入：所有阶段的交付物
    - 输出：完整的技术文档、用户手册
    - 交付物：`docs/` 下的文档

### 阶段 6：验收阶段
**负责Agent**：`@new-user-tester`

11. **@new-user-tester**（新用户体验）
    - 输入：完整的产品、文档
    - 输出：用户体验报告、改进建议
    - 交付物：`docs/ux-report.md`

## 流水线执行流程

```
[原始需求]
    ↓
┌─────────────────────────────────────────┐
│ 阶段1：需求分析                           │
│ @market-analyst → @product-manager       │
└─────────────────────────────────────────┘
    ↓
┌─────────────────────────────────────────┐
│ 阶段2：设计                               │
│ @system-architect                        │
└─────────────────────────────────────────┘
    ↓
┌─────────────────────────────────────────┐
│ 阶段3：开发                               │
│ @backend-developer → @frontend-developer │
└─────────────────────────────────────────┘
    ↓
┌─────────────────────────────────────────┐
│ 阶段4：质量保障                           │
│ @code-reviewer → @test-engineer → @bug-fixer │
└─────────────────────────────────────────┘
    ↓
┌─────────────────────────────────────────┐
│ 阶段5：部署与文档                         │
│ @devops-engineer → @document-writer      │
└─────────────────────────────────────────┘
    ↓
┌─────────────────────────────────────────┐
│ 阶段6：验收                               │
│ @new-user-tester                         │
└─────────────────────────────────────────┘
    ↓
[最终交付物]
```

## 使用方法

### 启动完整流水线

当用户提供一个完整的项目需求时，按以下顺序执行：

```
1. @market-analyst 分析市场需求
2. @product-manager 编写PRD
3. @system-architect 设计架构
4. @backend-developer 开发后端
5. @frontend-developer 开发前端
6. @code-reviewer 审查代码
7. @test-engineer 编写测试
8. @bug-fixer 修复问题
9. @devops-engineer 配置部署
10. @document-writer 编写文档
11. @new-user-tester 体验测试
```

### 启动部分流水线

如果用户只需要某个阶段的服务，可以只执行相关阶段：

```
# 只需要开发
@backend-developer 实现XX功能
@frontend-developer 实现XX界面

# 只需要质量保障
@code-reviewer 审查代码
@test-engineer 编写测试
```

## 任务传递规范

每个Agent完成任务后，需要：

1. **输出交付物**：将结果写入指定文件
2. **更新状态**：标记当前阶段完成
3. **传递上下文**：将关键信息传递给下一个Agent
4. **记录日志**：记录完成时间和关键决策

## 状态管理

使用 `.trae/pipeline-status.md` 文件跟踪流水线状态：

```markdown
# 流水线状态

## 当前阶段：阶段3 - 开发
## 当前Agent：@backend-developer
## 进度：4/11 完成

### 已完成
- [x] 阶段1：需求分析 (@market-analyst, @product-manager)
- [x] 阶段2：设计 (@system-architect)
- [ ] 阶段3：开发 (@backend-developer, @frontend-developer)
- [ ] 阶段4：质量保障
- [ ] 阶段5：部署与文档
- [ ] 阶段6：验收
```

## 异常处理

- **回滚机制**：如果某个Agent失败，可以回退到上一个成功的阶段
- **跳过机制**：可以跳过某些非必要的阶段
- **并行机制**：某些阶段可以并行执行（如前后端开发）

## 注意事项

1. 每个Agent必须等待前一个Agent完成才能开始
2. 交付物必须写入文件，不能只在对话中输出
3. 每个Agent需要明确标注输入和输出
4. CEO负责监控整个流水线的执行状态