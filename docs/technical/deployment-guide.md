# Mayfly - 部署指南

**文档版本**：v1.0  
**编制日期**：2026-04-15  
**适用版本**：Mayfly 1.0.x

---

## 一、部署方式

Mayfly 支持多种部署方式：

| 部署方式 | 适用场景 | 难度 |
|---------|---------|------|
| Maven 依赖 | 集成到现有 Spring Boot 应用 | ⭐ |
| Docker 容器 | 独立部署或微服务架构 | ⭐⭐ |
| Kubernetes | 生产环境高可用部署 | ⭐⭐⭐ |
| 源码编译 | 定制化需求 | ⭐⭐⭐ |

---

## 二、Maven 依赖部署

### 2.1 添加依赖

在项目的 `pom.xml` 中添加：

```xml
<dependencies>
    <dependency>
        <groupId>io.mayfly</groupId>
        <artifactId>mayfly-spring-boot-starter</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

### 2.2 配置模型

在 `application.yml` 中配置：

```yaml
mayfly:
  enabled: true
  models:
    - name: zhipu-primary
      provider: zhipu
      api-key: ${ZHIPU_API_KEY:your-api-key-here}
      model: glm-4
      weight: 70
    
    - name: tongyi-backup
      provider: tongyi
      api-key: ${TONGYI_API_KEY:your-api-key-here}
      model: qwen-max
      weight: 30
```

### 2.3 环境变量配置

**生产环境强烈建议使用环境变量管理 API Key**：

```bash
# Linux/Mac
export ZHIPU_API_KEY="your-actual-api-key"
export TONGYI_API_KEY="your-actual-api-key"

# Windows PowerShell
$env:ZHIPU_API_KEY="your-actual-api-key"
$env:TONGYI_API_KEY="your-actual-api-key"
```

或在 Docker/K8s 中通过环境变量注入。

---

## 三、Docker 部署

### 3.1 构建镜像

项目根目录包含 `Dockerfile`：

```dockerfile
FROM eclipse-temurin:17-jdk-alpine

VOLUME /tmp

ARG JAR_FILE=target/*.jar

COPY ${JAR_FILE} application.jar

ENTRYPOINT ["java","-jar","/application.jar"]
```

**构建命令**：

```bash
# 1. 构建项目
mvn clean package -DskipTests

# 2. 构建 Docker 镜像
docker build -t mayfly-demo:1.0.0 .
```

### 3.2 运行容器

```bash
docker run -d \
  --name mayfly-demo \
  -p 8080:8080 \
  -e ZHIPU_API_KEY=your-api-key \
  -e TONGYI_API_KEY=your-api-key \
  -v /path/to/config:/app/config \
  mayfly-demo:1.0.0
```

### 3.3 Docker Compose 部署

项目包含 `docker-compose.yml`：

```yaml
version: '3.8'

services:
  mayfly-demo:
    image: mayfly-demo:1.0.0
    container_name: mayfly-demo
    ports:
      - "8080:8080"
    environment:
      - ZHIPU_API_KEY=${ZHIPU_API_KEY}
      - TONGYI_API_KEY=${TONGYI_API_KEY}
    volumes:
      - ./config:/app/config
      - ./logs:/app/logs
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
    restart: unless-stopped
```

**启动命令**：

```bash
# 创建 .env 文件
cat > .env << EOF
ZHIPU_API_KEY=your-api-key
TONGYI_API_KEY=your-api-key
EOF

# 启动服务
docker-compose up -d

# 查看日志
docker-compose logs -f mayfly-demo

# 停止服务
docker-compose down
```

---

## 四、Kubernetes 部署

### 4.1 创建 ConfigMap

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: mayfly-config
  namespace: default
data:
  application.yml: |
    mayfly:
      enabled: true
      models:
        - name: zhipu-primary
          provider: zhipu
          api-key: ${ZHIPU_API_KEY}
          model: glm-4
          weight: 70
        - name: tongyi-backup
          provider: tongyi
          api-key: ${TONGYI_API_KEY}
          model: qwen-max
          weight: 30
      
      router:
        strategy: weighted
        rules: []
      
      failover:
        enabled: true
        max-retries: 2
        cooldown-duration: 60s
      
      circuit-breaker:
        enabled: true
        failure-rate-threshold: 50
        wait-duration-in-open-state: 60s
      
      rate-limiter:
        enabled: true
        limit-refresh-period: 1s
        limit-for-period: 100
      
      monitor:
        enabled: true
    
    server:
      port: 8080
    
    management:
      endpoints:
        web:
          exposure:
            include: health,info,prometheus,metrics
      endpoint:
        health:
          show-details: always
```

### 4.2 创建 Secret

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: mayfly-secrets
  namespace: default
type: Opaque
stringData:
  ZHIPU_API_KEY: your-zhipu-api-key
  TONGYI_API_KEY: your-tongyi-api-key
```

### 4.3 创建 Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: mayfly-deployment
  namespace: default
  labels:
    app: mayfly
spec:
  replicas: 3
  selector:
    matchLabels:
      app: mayfly
  template:
    metadata:
      labels:
        app: mayfly
    spec:
      containers:
      - name: mayfly
        image: mayfly-demo:1.0.0
        ports:
        - containerPort: 8080
        env:
        - name: ZHIPU_API_KEY
          valueFrom:
            secretKeyRef:
              name: mayfly-secrets
              key: ZHIPU_API_KEY
        - name: TONGYI_API_KEY
          valueFrom:
            secretKeyRef:
              name: mayfly-secrets
              key: TONGYI_API_KEY
        volumeMounts:
        - name: config-volume
          mountPath: /app/config
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
      volumes:
      - name: config-volume
        configMap:
          name: mayfly-config
```

### 4.4 创建 Service

```yaml
apiVersion: v1
kind: Service
metadata:
  name: mayfly-service
  namespace: default
spec:
  selector:
    app: mayfly
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8080
  type: ClusterIP
```

### 4.5 创建 Ingress（可选）

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: mayfly-ingress
  namespace: default
  annotations:
    kubernetes.io/ingress.class: nginx
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
spec:
  rules:
  - host: mayfly.example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: mayfly-service
            port:
              number: 80
  tls:
  - hosts:
    - mayfly.example.com
    secretName: mayfly-tls-secret
```

### 4.6 部署命令

```bash
# 应用配置
kubectl apply -f configmap.yaml
kubectl apply -f secret.yaml
kubectl apply -f deployment.yaml
kubectl apply -f service.yaml
kubectl apply -f ingress.yaml

# 查看状态
kubectl get pods -l app=mayfly
kubectl get svc mayfly-service
kubectl get ingress mayfly-ingress

# 查看日志
kubectl logs -f deployment/mayfly-deployment

# 扩缩容
kubectl scale deployment mayfly-deployment --replicas=5
```

---

## 五、生产环境配置

### 5.1 高可用配置

```yaml
mayfly:
  # 多模型配置
  models:
    - name: zhipu-primary
      provider: zhipu
      api-key: ${ZHIPU_API_KEY}
      model: glm-4
      weight: 50
      tags:
        - primary
        - production
    
    - name: zhipu-backup
      provider: zhipu
      api-key: ${ZHIPU_API_KEY_BACKUP}
      model: glm-4
      weight: 30
      tags:
        - backup
        - production
    
    - name: tongyi-backup
      provider: tongyi
      api-key: ${TONGYI_API_KEY}
      model: qwen-max
      weight: 20
      tags:
        - backup
        - production
  
  # 故障转移配置
  failover:
    enabled: true
    max-retries: 3
    cooldown-duration: 120s
    retryable-exceptions:
      - java.net.SocketTimeoutException
      - org.springframework.web.client.HttpServerErrorException
      - io.mayfly.core.ModelUnavailableException
  
  # 熔断器配置
  circuit-breaker:
    enabled: true
    failure-rate-threshold: 60
    wait-duration-in-open-state: 90s
    sliding-window-size: 20
    minimum-number-of-calls: 10
  
  # 限流器配置
  rate-limiter:
    enabled: true
    limit-refresh-period: 1s
    limit-for-period: 200
    timeout-duration: 0s
```

### 5.2 监控告警配置

#### Prometheus 配置

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'mayfly'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['mayfly-service:80']
    scrape_interval: 15s
```

#### Grafana 告警规则

```yaml
# alerting-rules.yml
groups:
  - name: mayfly-alerts
    rules:
      - alert: HighErrorRate
        expr: rate(mayfly_model_calls_failure_total[5m]) > 0.1
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "Mayfly 错误率过高"
          description: "模型 {{ $labels.model_name }} 的错误率超过 10%"
      
      - alert: ModelUnavailable
        expr: mayfly_model_calls_total == 0
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "模型不可用"
          description: "模型 {{ $labels.model_name }} 已 2 分钟无调用"
      
      - alert: HighLatency
        expr: histogram_quantile(0.95, rate(mayfly_model_latency_seconds_bucket[5m])) > 5
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "模型响应延迟过高"
          description: "模型 {{ $labels.model_name }} 的 P95 延迟超过 5 秒"
```

### 5.3 日志配置

```yaml
# application.yml
logging:
  level:
    root: INFO
    io.mayfly: INFO
    org.springframework.ai: WARN
  
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  
  file:
    name: /app/logs/mayfly.log
    max-size: 100MB
    max-history: 30
    total-size-cap: 3GB
```

### 5.4 性能调优

```yaml
# JVM 参数
JAVA_OPTS: >
  -Xms1g
  -Xmx2g
  -XX:+UseG1GC
  -XX:MaxGCPauseMillis=200
  -XX:+HeapDumpOnOutOfMemoryError
  -XX:HeapDumpPath=/app/logs/heapdump.hprof
  -Djava.security.egd=file:/dev/./urandom

# 连接池配置
spring:
  ai:
    chat:
      client:
        connection-timeout: 5000
        read-timeout: 30000
```

---

## 六、健康检查

### 6.1 健康检查端点

Mayfly 提供以下健康检查端点：

| 端点 | 说明 |
|------|------|
| `/actuator/health` | 整体健康状态 |
| `/actuator/health/mayfly` | Mayfly 组件健康状态 |
| `/actuator/prometheus` | Prometheus 监控指标 |

### 6.2 健康检查响应示例

```json
{
  "status": "UP",
  "components": {
    "mayfly": {
      "status": "UP",
      "details": {
        "availableModels": 3,
        "healthyModels": 2,
        "circuitBreakers": "CLOSED",
        "rateLimiters": "ACTIVE"
      }
    }
  }
}
```

---

## 七、故障排查

### 7.1 常见问题

#### 问题 1：模型调用失败

**现象**：日志中出现 `ModelUnavailableException`

**排查步骤**：
1. 检查 API Key 配置是否正确
2. 检查网络连接是否正常
3. 检查模型提供商服务状态
4. 查看熔断器状态

**解决方案**：
```bash
# 查看日志
kubectl logs deployment/mayfly-deployment | grep "ModelUnavailableException"

# 检查配置
kubectl get configmap mayfly-config -o yaml
```

#### 问题 2：熔断器频繁打开

**现象**：熔断器状态频繁变为 OPEN

**排查步骤**：
1. 检查错误率是否过高
2. 检查超时配置是否合理
3. 检查模型提供商限流策略

**解决方案**：
```yaml
# 调整熔断器配置
mayfly:
  circuit-breaker:
    failure-rate-threshold: 70  # 提高阈值
    wait-duration-in-open-state: 120s  # 延长等待时间
    minimum-number-of-calls: 15  # 增加最小调用数
```

#### 问题 3：负载均衡不均

**现象**：某些模型请求过多，某些过少

**排查步骤**：
1. 检查权重配置
2. 检查负载均衡策略
3. 查看监控指标分布

**解决方案**：
```yaml
# 调整权重
mayfly:
  models:
    - name: model-a
      weight: 60  # 调整权重
    - name: model-b
      weight: 40
  
  loadbalancer:
    strategy: weighted-round-robin  # 使用加权轮询
```

### 7.2 日志分析

```bash
# 查看错误日志
kubectl logs deployment/mayfly-deployment | grep ERROR

# 查看特定模型的日志
kubectl logs deployment/mayfly-deployment | grep "zhipu-primary"

# 实时查看日志
kubectl logs -f deployment/mayfly-deployment --tail=100
```

### 7.3 监控指标分析

```bash
# 查询错误率
curl http://prometheus:9090/api/v1/query?query=rate(mayfly_model_calls_failure_total[5m])

# 查询延迟
curl http://prometheus:9090/api/v1/query?query=histogram_quantile(0.95,rate(mayfly_model_latency_seconds_bucket[5m]))

# 查询可用模型数
curl http://prometheus:9090/api/v1/query?query=mayfly_model_calls_total
```

---

## 八、备份与恢复

### 8.1 配置备份

```bash
# 备份 ConfigMap
kubectl get configmap mayfly-config -o yaml > mayfly-config-backup.yaml

# 备份 Secret
kubectl get secret mayfly-secrets -o yaml > mayfly-secrets-backup.yaml

# 备份 Deployment
kubectl get deployment mayfly-deployment -o yaml > mayfly-deployment-backup.yaml
```

### 8.2 配置恢复

```bash
# 恢复配置
kubectl apply -f mayfly-config-backup.yaml
kubectl apply -f mayfly-secrets-backup.yaml
kubectl apply -f mayfly-deployment-backup.yaml
```

---

## 九、升级指南

### 9.1 版本升级

```bash
# 1. 备份当前配置
kubectl get deployment mayfly-deployment -o yaml > backup.yaml

# 2. 更新镜像版本
kubectl set image deployment/mayfly-deployment mayfly=mayfly-demo:1.1.0

# 3. 查看升级状态
kubectl rollout status deployment/mayfly-deployment

# 4. 回滚（如有问题）
kubectl rollout undo deployment/mayfly-deployment
```

### 9.2 滚动更新配置

```yaml
spec:
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  minReadySeconds: 30
```

---

**文档版本**：v1.0  
**更新日期**：2026-04-15
