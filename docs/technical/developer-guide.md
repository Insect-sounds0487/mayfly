# Mayfly - 开发手册

**文档版本**：v1.0  
**编制日期**：2026-04-15  
**适用版本**：Mayfly 1.0.x

---

## 一、项目结构

```
mayfly/
├── pom.xml                                    # 父POM
├── README.md                                  # 项目说明
├── Dockerfile                                 # Docker构建文件
├── docker-compose.yml                         # Docker编排文件
├── docs/                                      # 文档目录
│   ├── market-analysis.md                     # 市场分析报告
│   ├── prd.md                                 # 产品需求文档
│   ├── architecture.md                        # 架构设计文档
│   ├── budget.md                              # 资金预算
│   ├── code-review-report.md                  # 代码审查报告v1
│   ├── code-review-report-v2.md               # 代码审查报告v2
│   ├── test-report.md                         # 测试报告
│   ├── user-guide.md                          # 用户手册
│   └── developer-guide.md                     # 开发手册
├── mayfly-core/                               # 核心模块
├── mayfly-router/                             # 路由策略模块
├── mayfly-loadbalancer/                       # 负载均衡模块
├── mayfly-failover/                           # 故障转移模块
├── mayfly-circuitbreaker/                     # 熔断限流模块
├── mayfly-adapter/                            # 模型适配器模块
├── mayfly-monitor/                            # 监控模块
└── mayfly-spring-boot-starter/                # Spring Boot Starter
```

---

## 二、开发环境搭建

### 2.1 环境要求

| 工具 | 版本 | 说明 |
|------|------|------|
| JDK | 17+ | 推荐Temurin 17 |
| Maven | 3.8+ | 构建工具 |
| IDE | - | IntelliJ IDEA推荐 |
| Git | 2.30+ | 版本控制 |

### 2.2 克隆项目

```bash
git clone https://github.com/mayfly-ai/mayfly.git
cd mayfly
```

### 2.3 构建项目

```bash
# 编译
mvn clean compile

# 打包
mvn clean package

# 安装到本地仓库
mvn clean install

# 跳过测试
mvn clean install -DskipTests
```

---

## 三、核心模块开发

### 3.1 mayfly-core（核心模块）

**职责**：定义核心接口和实体

**关键类**：
- `ModelRouter` - 路由核心接口
- `ModelRegistry` - 模型注册中心接口
- `ModelConfig` - 模型配置实体
- `ModelInstance` - 模型实例（运行时状态）
- `DefaultModelRouter` - 默认路由器实现
- `DefaultModelRegistry` - 默认注册中心实现

**开发规范**：
1. 接口定义要简洁明确
2. 实体类使用Lombok简化代码
3. 异常处理要完整

### 3.2 mayfly-router（路由策略模块）

**职责**：实现各种路由策略

**关键类**：
- `RouterStrategy` - 路由策略接口
- `RouterRule` - 路由规则实体
- `FixedRouterStrategy` - 固定路由
- `WeightedRouterStrategy` - 权重路由
- `RuleBasedRouterStrategy` - 规则路由（SpEL）

**扩展新策略**：
```java
@Component
public class MyRouterStrategy implements RouterStrategy {
    
    @Override
    public ModelInstance select(ChatRequest request, List<ModelInstance> candidates) {
        // 实现自定义逻辑
    }
    
    @Override
    public String getName() {
        return "my-strategy";
    }
}
```

### 3.3 mayfly-adapter（模型适配器模块）

**职责**：适配不同模型提供商

**关键类**：
- `ModelAdapter` - 适配器接口
- `ZhipuModelAdapter` - 智谱适配器
- `TongyiModelAdapter` - 通义适配器
- `DeepSeekModelAdapter` - DeepSeek适配器

**扩展新适配器**：
```java
@Component
public class MyModelAdapter implements ModelAdapter {
    
    @Override
    public ChatModel createChatModel(ModelConfig config) {
        // 创建ChatModel实例
        if (config.getApiKey() == null || config.getApiKey().trim().isEmpty()) {
            throw new IllegalArgumentException("API key must not be empty");
        }
        // ...
    }
    
    @Override
    public String getProvider() {
        return "my-provider";
    }
}
```

---

## 四、测试开发

### 4.1 单元测试

```java
class MyTest {
    
    @Test
    @DisplayName("测试用例描述")
    void testSomething() {
        // Given
        ModelConfig config = ModelConfig.builder()
            .name("test")
            .provider("test")
            .model("test-v1")
            .apiKey("test-key")
            .build();
        
        // When
        ModelInstance instance = new ModelInstance(config, null);
        
        // Then
        assertNotNull(instance);
        assertEquals("test", instance.getConfig().getName());
    }
}
```

### 4.2 集成测试

```java
@SpringBootTest
class IntegrationTest {
    
    @Autowired
    private ModelRouter modelRouter;
    
    @Test
    void testRouterIntegration() {
        // 测试完整调用链路
    }
}
```

### 4.3 运行测试

```bash
# 运行所有测试
mvn test

# 运行特定测试
mvn test -Dtest=ModelInstanceTest

# 生成覆盖率报告
mvn jacoco:report
```

---

## 五、代码规范

### 5.1 命名规范

| 类型 | 规范 | 示例 |
|------|------|------|
| 类名 | PascalCase | `ModelRouter` |
| 方法名 | camelCase | `selectModel` |
| 常量 | UPPER_SNAKE_CASE | `MAX_RETRIES` |
| 包名 | 小写 | `io.mayfly.core` |

### 5.2 注释规范

```java
/**
 * 模型路由核心接口
 * 所有模型调用的统一入口
 * 
 * @author Mayfly Team
 * @since 1.0.0
 */
public interface ModelRouter {
    
    /**
     * 同步调用
     * 
     * @param request 聊天请求
     * @return 聊天响应
     * @throws ModelUnavailableException 当没有可用模型时
     */
    ChatResponse chat(ChatRequest request);
}
```

### 5.3 异常处理

```java
try {
    // 业务逻辑
} catch (SpecificException e) {
    log.error("Specific error occurred: {}", e.getMessage(), e);
    throw new MayflyException("Operation failed", e);
} catch (Exception e) {
    log.error("Unexpected error: {}", e.getMessage(), e);
    throw new MayflyException("Unexpected error", e);
}
```

---

## 六、发布流程

### 6.1 版本管理

遵循语义化版本：`MAJOR.MINOR.PATCH`

- MAJOR：不兼容的API变更
- MINOR：向后兼容的功能新增
- PATCH：向后兼容的问题修复

### 6.2 发布步骤

```bash
# 1. 更新版本号
mvn versions:set -DnewVersion=1.0.0

# 2. 运行测试
mvn clean test

# 3. 构建
mvn clean package

# 4. 提交
git add .
git commit -m "Release v1.0.0"
git tag v1.0.0
git push origin main --tags

# 5. 发布到Maven中央仓库
mvn clean deploy -P release
```

---

## 七、常见问题

### Q1: 如何调试路由策略？

开启DEBUG日志：
```yaml
logging:
  level:
    io.mayfly: DEBUG
```

### Q2: 如何添加新模型提供商？

1. 在`mayfly-adapter`模块创建适配器类
2. 实现`ModelAdapter`接口
3. 添加`@Component`注解
4. 在`getProvider()`中返回提供商名称

### Q3: 如何自定义配置属性？

在`MayflyProperties`中添加新配置类，然后在自动配置类中使用。

---

**文档版本**：v1.0  
**更新日期**：2026-04-15
