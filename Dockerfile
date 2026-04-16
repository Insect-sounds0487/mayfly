# Mayfly - 多阶段构建Dockerfile
# 用于构建和运行Mayfly示例应用

# 第一阶段：构建
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# 复制POM文件
COPY pom.xml .
COPY mayfly-core/pom.xml mayfly-core/
COPY mayfly-router/pom.xml mayfly-router/
COPY mayfly-loadbalancer/pom.xml mayfly-loadbalancer/
COPY mayfly-failover/pom.xml mayfly-failover/
COPY mayfly-circuitbreaker/pom.xml mayfly-circuitbreaker/
COPY mayfly-adapter/pom.xml mayfly-adapter/
COPY mayfly-monitor/pom.xml mayfly-monitor/
COPY mayfly-spring-boot-starter/pom.xml mayfly-spring-boot-starter/

# 下载依赖
RUN mvn dependency:go-offline -B

# 复制源代码
COPY mayfly-core/src mayfly-core/src
COPY mayfly-router/src mayfly-router/src
COPY mayfly-loadbalancer/src mayfly-loadbalancer/src
COPY mayfly-failover/src mayfly-failover/src
COPY mayfly-circuitbreaker/src mayfly-circuitbreaker/src
COPY mayfly-adapter/src mayfly-adapter/src
COPY mayfly-monitor/src mayfly-monitor/src
COPY mayfly-spring-boot-starter/src mayfly-spring-boot-starter/src

# 构建项目
RUN mvn clean package -DskipTests -B

# 第二阶段：运行
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 创建非root用户
RUN addgroup -S mayfly && adduser -S mayfly -G mayfly

# 复制构建产物
COPY --from=build /app/mayfly-spring-boot-starter/target/*.jar app.jar

# 设置时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 暴露端口（示例应用）
EXPOSE 8080

# 切换到非root用户
USER mayfly

# JVM参数
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
