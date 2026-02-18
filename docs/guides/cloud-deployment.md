# Cloud Deployment Guide

## Overview

This guide covers **cloud deployment** strategies for microservices on Azure, AWS, and GCP with Kubernetes.

**Key Topics:**
- Docker containerization
- Kubernetes deployment
- Azure (AKS, App Service, managed services)
- AWS (EKS, ECS, managed services)
- GCP (GKE, Cloud Run, managed services)
- CI/CD pipelines
- Monitoring & observability

---

## 🐳 Docker Containerization

### Optimized Dockerfile

```dockerfile
# Multi-stage build
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy pom.xml first (layer caching)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source and build
COPY src ./src
RUN mvn package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

# Add non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy JAR from builder
COPY --from=builder /app/target/*.jar app.jar

# Set ownership
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# JVM options
ENV JAVA_OPTS="-Xms512m -Xmx512m \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

### Docker Compose (Local Development)

```yaml
# docker-compose.yml
version: '3.8'

services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: dev
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/mydb
      SPRING_REDIS_HOST: redis
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092
    depends_on:
      - postgres
      - redis
      - kafka
    networks:
      - app-network
    healthcheck:
      test: ["CMD", "wget", "--spider", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s
  
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: mydb
      POSTGRES_USER: user
      POSTGRES_PASSWORD: password
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data
    networks:
      - app-network
  
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    networks:
      - app-network
  
  kafka:
    image: confluentinc/cp-kafka:7.5.0
    environment:
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    ports:
      - "9092:9092"
    depends_on:
      - zookeeper
    networks:
      - app-network
  
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
    networks:
      - app-network

networks:
  app-network:
    driver: bridge

volumes:
  postgres-data:
```

---

## ☸️ Kubernetes Deployment

### Deployment Manifest

```yaml
# k8s/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: myapp
  namespace: production
  labels:
    app: myapp
    version: v1.0.0
spec:
  replicas: 3
  revisionHistoryLimit: 10
  selector:
    matchLabels:
      app: myapp
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  template:
    metadata:
      labels:
        app: myapp
        version: v1.0.0
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "8080"
        prometheus.io/path: "/actuator/prometheus"
    spec:
      serviceAccountName: myapp-sa
      
      # Init container
      initContainers:
      - name: wait-for-postgres
        image: busybox:1.35
        command: ['sh', '-c', 'until nc -z postgres 5432; do sleep 2; done']
      
      containers:
      - name: myapp
        image: myregistry.azurecr.io/myapp:v1.0.0
        imagePullPolicy: Always
        
        ports:
        - name: http
          containerPort: 8080
          protocol: TCP
        
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
        - name: SERVER_PORT
          value: "8080"
        - name: SPRING_DATASOURCE_URL
          valueFrom:
            secretKeyRef:
              name: database-credentials
              key: jdbc-url
        - name: SPRING_DATASOURCE_USERNAME
          valueFrom:
            secretKeyRef:
              name: database-credentials
              key: username
        - name: SPRING_DATASOURCE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: database-credentials
              key: password
        - name: JAVA_OPTS
          value: "-Xms1g -Xmx1g -XX:+UseG1GC -XX:MaxRAMPercentage=75.0"
        
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1000m"
        
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        
        volumeMounts:
        - name: config
          mountPath: /app/config
          readOnly: true
        - name: logs
          mountPath: /app/logs
      
      volumes:
      - name: config
        configMap:
          name: myapp-config
      - name: logs
        emptyDir: {}
      
      affinity:
        podAntiAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
          - weight: 100
            podAffinityTerm:
              labelSelector:
                matchExpressions:
                - key: app
                  operator: In
                  values:
                  - myapp
              topologyKey: kubernetes.io/hostname
```

### Service & Ingress

```yaml
# k8s/service.yaml
apiVersion: v1
kind: Service
metadata:
  name: myapp-service
  namespace: production
  labels:
    app: myapp
spec:
  type: ClusterIP
  selector:
    app: myapp
  ports:
  - name: http
    port: 80
    targetPort: 8080
    protocol: TCP
  sessionAffinity: ClientIP

---
# k8s/ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: myapp-ingress
  namespace: production
  annotations:
    kubernetes.io/ingress.class: nginx
    cert-manager.io/cluster-issuer: letsencrypt-prod
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/rate-limit: "100"
spec:
  tls:
  - hosts:
    - api.example.com
    secretName: myapp-tls
  rules:
  - host: api.example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: myapp-service
            port:
              number: 80
```

### ConfigMap & Secrets

```yaml
# k8s/configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: myapp-config
  namespace: production
data:
  application.yml: |
    server:
      port: 8080
    spring:
      application:
        name: myapp
      jpa:
        show-sql: false
    logging:
      level:
        root: INFO

---
# k8s/secret.yaml
apiVersion: v1
kind: Secret
metadata:
  name: database-credentials
  namespace: production
type: Opaque
data:
  jdbc-url: amRiYzpwb3N0Z3Jlc3FsOi8vcG9zdGdyZXM6NTQzMi9teWRi  # base64 encoded
  username: dXNlcg==  # base64 encoded
  password: cGFzc3dvcmQ=  # base64 encoded
```

### Horizontal Pod Autoscaler

```yaml
# k8s/hpa.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: myapp-hpa
  namespace: production
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: myapp
  minReplicas: 3
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
      - type: Percent
        value: 50
        periodSeconds: 60
    scaleUp:
      stabilizationWindowSeconds: 0
      policies:
      - type: Percent
        value: 100
        periodSeconds: 30
```

---

## ☁️ Azure Deployment

### Azure Kubernetes Service (AKS)

```bash
# Create resource group
az group create --name myapp-rg --location eastus

# Create AKS cluster
az aks create \
  --resource-group myapp-rg \
  --name myapp-aks \
  --node-count 3 \
  --node-vm-size Standard_D4s_v3 \
  --enable-managed-identity \
  --enable-addons monitoring \
  --generate-ssh-keys

# Get credentials
az aks get-credentials --resource-group myapp-rg --name myapp-aks

# Deploy application
kubectl apply -f k8s/
```

### Azure Container Registry (ACR)

```bash
# Create ACR
az acr create \
  --resource-group myapp-rg \
  --name myappregistry \
  --sku Premium

# Build and push image
az acr build \
  --registry myappregistry \
  --image myapp:v1.0.0 \
  .

# Attach ACR to AKS
az aks update \
  --resource-group myapp-rg \
  --name myapp-aks \
  --attach-acr myappregistry
```

### Azure Managed Services

```yaml
# Azure PostgreSQL
apiVersion: dbforpostgresql.azure.com/v1beta1
kind: FlexibleServer
metadata:
  name: myapp-postgres
spec:
  location: eastus
  resourceGroup: myapp-rg
  version: "15"
  sku:
    name: Standard_D4s_v3
    tier: GeneralPurpose
  storage:
    storageSizeGB: 128

---
# Azure Redis Cache
apiVersion: cache.azure.com/v1alpha1
kind: Redis
metadata:
  name: myapp-redis
spec:
  location: eastus
  resourceGroup: myapp-rg
  properties:
    sku:
      name: Premium
      family: P
      capacity: 1
    enableNonSslPort: false
    redisVersion: "6"
```

---

## 🚀 AWS Deployment

### Elastic Kubernetes Service (EKS)

```bash
# Create EKS cluster with eksctl
eksctl create cluster \
  --name myapp-eks \
  --region us-east-1 \
  --nodegroup-name standard-workers \
  --node-type m5.xlarge \
  --nodes 3 \
  --nodes-min 3 \
  --nodes-max 10 \
  --managed

# Update kubeconfig
aws eks update-kubeconfig --region us-east-1 --name myapp-eks

# Deploy application
kubectl apply -f k8s/
```

### Elastic Container Registry (ECR)

```bash
# Create ECR repository
aws ecr create-repository --repository-name myapp

# Login to ECR
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS \
  --password-stdin 123456789012.dkr.ecr.us-east-1.amazonaws.com

# Build and push
docker build -t myapp:v1.0.0 .
docker tag myapp:v1.0.0 123456789012.dkr.ecr.us-east-1.amazonaws.com/myapp:v1.0.0
docker push 123456789012.dkr.ecr.us-east-1.amazonaws.com/myapp:v1.0.0
```

### AWS RDS & ElastiCache

```yaml
# application-aws.yml
spring:
  datasource:
    url: jdbc:postgresql://myapp-db.abc123.us-east-1.rds.amazonaws.com:5432/mydb
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  
  redis:
    host: myapp-redis.abc123.cache.amazonaws.com
    port: 6379
    ssl: true
```

---

## 🌐 GCP Deployment

### Google Kubernetes Engine (GKE)

```bash
# Create GKE cluster
gcloud container clusters create myapp-gke \
  --region us-central1 \
  --num-nodes 3 \
  --machine-type n1-standard-4 \
  --enable-autoscaling \
  --min-nodes 3 \
  --max-nodes 10 \
  --enable-stackdriver-kubernetes

# Get credentials
gcloud container clusters get-credentials myapp-gke --region us-central1

# Deploy application
kubectl apply -f k8s/
```

### Container Registry (GCR)

```bash
# Configure Docker for GCR
gcloud auth configure-docker

# Build and push
docker build -t gcr.io/my-project/myapp:v1.0.0 .
docker push gcr.io/my-project/myapp:v1.0.0
```

### Cloud SQL & Memorystore

```yaml
# application-gcp.yml
spring:
  cloud:
    gcp:
      sql:
        database-name: mydb
        instance-connection-name: my-project:us-central1:myapp-db
  
  redis:
    host: 10.0.0.3  # Memorystore Redis IP
    port: 6379
```

---

## 🔄 CI/CD Pipeline

### GitHub Actions

```yaml
# .github/workflows/deploy.yml
name: Deploy to Production

on:
  push:
    branches: [main]
    tags: ['v*']

env:
  REGISTRY: myregistry.azurecr.io
  IMAGE_NAME: myapp

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 21
      uses: actions/setup-java@v3
      with:
        java-version: '21'
        distribution: 'temurin'
        cache: maven
    
    - name: Build with Maven
      run: mvn clean package -DskipTests
    
    - name: Run tests
      run: mvn test
    
    - name: Upload artifact
      uses: actions/upload-artifact@v3
      with:
        name: app-jar
        path: target/*.jar
  
  docker:
    needs: build
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3
    
    - name: Download artifact
      uses: actions/download-artifact@v3
      with:
        name: app-jar
        path: target/
    
    - name: Login to ACR
      uses: azure/docker-login@v1
      with:
        login-server: ${{ env.REGISTRY }}
        username: ${{ secrets.ACR_USERNAME }}
        password: ${{ secrets.ACR_PASSWORD }}
    
    - name: Build and push Docker image
      run: |
        TAG=${GITHUB_REF#refs/tags/}
        docker build -t ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:$TAG .
        docker push ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:$TAG
  
  deploy:
    needs: docker
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up kubectl
      uses: azure/setup-kubectl@v3
    
    - name: Azure Login
      uses: azure/login@v1
      with:
        creds: ${{ secrets.AZURE_CREDENTIALS }}
    
    - name: Get AKS credentials
      run: |
        az aks get-credentials \
          --resource-group myapp-rg \
          --name myapp-aks
    
    - name: Deploy to AKS
      run: |
        TAG=${GITHUB_REF#refs/tags/}
        kubectl set image deployment/myapp \
          myapp=${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:$TAG \
          -n production
        
        kubectl rollout status deployment/myapp -n production
```

---

## 📊 Monitoring & Observability

### Prometheus & Grafana

```yaml
# k8s/prometheus.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: prometheus-config
  namespace: monitoring
data:
  prometheus.yml: |
    global:
      scrape_interval: 15s
    
    scrape_configs:
    - job_name: 'kubernetes-pods'
      kubernetes_sd_configs:
      - role: pod
      relabel_configs:
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_scrape]
        action: keep
        regex: true
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_path]
        action: replace
        target_label: __metrics_path__
        regex: (.+)
      - source_labels: [__address__, __meta_kubernetes_pod_annotation_prometheus_io_port]
        action: replace
        regex: ([^:]+)(?::\d+)?;(\d+)
        replacement: $1:$2
        target_label: __address__

---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: prometheus
  namespace: monitoring
spec:
  replicas: 1
  selector:
    matchLabels:
      app: prometheus
  template:
    metadata:
      labels:
        app: prometheus
    spec:
      containers:
      - name: prometheus
        image: prom/prometheus:v2.45.0
        ports:
        - containerPort: 9090
        volumeMounts:
        - name: config
          mountPath: /etc/prometheus
        - name: data
          mountPath: /prometheus
      volumes:
      - name: config
        configMap:
          name: prometheus-config
      - name: data
        persistentVolumeClaim:
          claimName: prometheus-pvc
```

### Application Insights / CloudWatch

```yaml
# Azure Application Insights
spring:
  application:
    insights:
      instrumentation-key: ${APPINSIGHTS_INSTRUMENTATIONKEY}
      
# AWS CloudWatch
logging:
  level:
    root: INFO
  pattern:
    console: "%d{ISO8601} [%thread] %-5level %logger{36} - %msg%n"

cloudwatch:
  config:
    cloudwatch:
      namespace: MyApp
      batchSize: 10000
```

---

## Best Practices

### ✅ DO

```yaml
# ✅ Use health checks
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
readinessProbe:
  httpGet:
    path: /actuator/health/readiness

# ✅ Set resource limits
resources:
  requests:
    memory: "1Gi"
    cpu: "500m"
  limits:
    memory: "2Gi"
    cpu: "1000m"

# ✅ Use secrets for credentials
valueFrom:
  secretKeyRef:
    name: database-credentials

# ✅ Enable horizontal autoscaling
HorizontalPodAutoscaler with CPU/memory metrics

# ✅ Use non-root user
USER appuser
```

### ❌ DON'T

```yaml
# ❌ NÃO use :latest tag
image: myapp:latest  # ❌ Not reproducible!

# ❌ NÃO rode como root
# Default user in Docker is root

# ❌ NÃO ignore resource limits
# Can cause OOM kills

# ❌ NÃO hardcode credentials
- name: DB_PASSWORD
  value: "password123"  # ❌ Use secrets!

# ❌ NÃO use single replica em produção
replicas: 1  # ❌ No high availability!
```

---

## Ver Também

- [Observability](./observability.md) - Monitoring setup
- [Security](./security-best-practices.md) - Cloud security
- [Performance](./performance-optimization.md) - Optimization
