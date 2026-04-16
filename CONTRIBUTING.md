# 贡献指南

感谢您对 Mayfly 项目的关注和贡献！我们欢迎任何形式的贡献，包括但不限于代码提交、文档改进、问题报告等。

## 📋 贡献流程

### 1. Fork 仓库
点击页面右上角的 "Fork" 按钮，将项目 fork 到您的个人账户下。

### 2. 克隆仓库
```bash
git clone https://github.com/your-username/mayfly.git
cd mayfly
```

### 3. 创建特性分支
```bash
git checkout -b feature/your-feature-name
```

### 4. 进行修改
- 确保代码符合项目编码规范
- 添加必要的单元测试
- 更新相关文档（如需要）

### 5. 提交更改
```bash
git add .
git commit -m "feat: 简要描述您的更改"
```

### 6. 推送分支
```bash
git push origin feature/your-feature-name
```

### 7. 创建 Pull Request
在 GitHub/GitCode 上创建 Pull Request，我们会尽快进行代码审查。

## 🧪 开发环境

### 环境要求
- JDK 17+
- Maven 3.8+
- Spring Boot 3.2+

### 本地构建
```bash
# 构建项目
mvn clean install

# 运行测试
mvn test

# 跳过测试构建（开发时使用）
mvn clean install -DskipTests
```

## 📝 代码规范

### 1. 代码风格
- 遵循 Google Java Style Guide
- 使用 Lombok 简化代码（已配置）
- 方法和类要有清晰的注释

### 2. 提交信息格式
```
<type>(<scope>): <subject>

<body>

<footer>
```

**Type 类型说明：**
- `feat`: 新功能
- `fix`: Bug 修复  
- `docs`: 文档更新
- `style`: 代码格式调整
- `refactor`: 代码重构
- `test`: 测试相关
- `chore`: 构建过程或辅助工具的变动

### 3. 分支命名
- `feature/xxx` - 新功能开发
- `bugfix/xxx` - Bug 修复
- `hotfix/xxx` - 紧急修复
- `release/xxx` - 发布准备

## 🧪 测试要求

### 1. 单元测试
- 核心逻辑必须有单元测试覆盖
- 测试覆盖率目标 ≥ 80%
- 使用 JUnit 5 + Mockito

### 2. 集成测试
- 涉及多个模块交互的功能需要集成测试
- 模拟真实使用场景

### 3. 测试命名规范
- 测试类名以 `Test` 结尾
- 测试方法使用 `@DisplayName` 注解描述功能

## 📚 文档更新

### 1. README.md
- 新功能需要在 README 中添加使用示例
- 配置选项需要在完整配置示例中体现

### 2. 用户手册
- 复杂功能需要在 `docs/` 目录下添加详细文档
- 提供最佳实践和常见问题解答

## 🐛 问题报告

如果您发现 Bug 或有改进建议，请：

1. 在 Issues 中搜索是否已有相关讨论
2. 如果没有，请创建新的 Issue
3. 提供详细的复现步骤和环境信息

## 🤝 联系方式

- **Issues**: [提交问题](https://gitcode.com/Topfogking/mayfly/issues)
- **邮箱**: dev@mayfly.io

---

**再次感谢您的贡献！让我们一起打造更好的 Mayfly 项目！** 🚀