# Contribution Guide

Thank you for your interest in contributing to the Mayfly project! We welcome all forms of contributions, including code submissions, documentation improvements, and issue reports.

## 📋 Contribution Process

### 1. Fork the Repository
Click the "Fork" button in the top right corner to fork the project to your personal account.

### 2. Clone the Repository
```bash
git clone https://github.com/your-username/mayfly.git
cd mayfly
```

### 3. Create a Feature Branch
```bash
git checkout -b feature/your-feature-name
```

### 4. Make Changes
- Ensure code follows project coding standards
- Add necessary unit tests
- Update relevant documentation if needed

### 5. Commit Changes
```bash
git add .
git commit -m "feat: brief description of your changes"
```

### 6. Push Branch
```bash
git push origin feature/your-feature-name
```

### 7. Create Pull Request
Create a Pull Request on GitHub/GitCode, and we'll review your code as soon as possible.

## 🧪 Development Environment

### Requirements
- JDK 17+
- Maven 3.8+
- Spring Boot 3.2+

### Local Build
```bash
# Build project
mvn clean install

# Run tests
mvn test

# Skip tests for development
mvn clean install -DskipTests
```

## 📝 Code Standards

### 1. Code Style
- Follow Google Java Style Guide
- Use Lombok to simplify code (already configured)
- Add clear comments for methods and classes

### 2. Commit Message Format
```
<type>(<scope>): <subject>

<body>

<footer>
```

**Type Examples:**
- `feat`: New feature
- `fix`: Bug fix  
- `docs`: Documentation update
- `style`: Code formatting
- `refactor`: Code refactoring
- `test`: Test related
- `chore`: Build process or auxiliary tool changes

### 3. Branch Naming
- `feature/xxx` - New feature development
- `bugfix/xxx` - Bug fixes
- `hotfix/xxx` - Emergency fixes
- `release/xxx` - Release preparation

## 🧪 Testing Requirements

### 1. Unit Tests
- Core logic must have unit test coverage
- Test coverage target ≥ 80%
- Use JUnit 5 + Mockito

### 2. Integration Tests
- Features involving multiple module interactions need integration tests
- Simulate real-world usage scenarios

### 3. Test Naming Convention
- Test class names end with `Test`
- Test methods use `@DisplayName` annotation to describe functionality

## 📚 Documentation Updates

### 1. README.md
- New features should be added to README with usage examples
- Configuration options should be reflected in complete configuration examples

### 2. User Manual
- Complex features should have detailed documentation in the `docs/` directory
- Provide best practices and FAQ

## 🐛 Issue Reporting

If you find a bug or have improvement suggestions:

1. Search Issues to see if there's already a related discussion
2. If not, create a new Issue
3. Provide detailed reproduction steps and environment information

## 🤝 Contact

- **Issues**: [Submit issues](https://github.com/mayfly-ai/mayfly/issues)
- **Email**: git@xsjyby.asia

---

**Thank you again for your contribution! Let's build a better Mayfly project together!** 🚀