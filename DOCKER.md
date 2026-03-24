# Docker Setup for POS System

## Prerequisites
- Docker installed
- Docker Compose installed

## Start MongoDB with Docker

```bash
# Option 1: Using Docker Compose (Recommended)
docker-compose up -d

# Option 2: Using Docker directly
docker run --name pos-mongodb \
  -p 27017:27017 \
  -e MONGO_INITDB_ROOT_USERNAME=admin \
  -e MONGO_INITDB_ROOT_PASSWORD=password \
  -v mongodb_data:/data/db \
  -d mongo:5.0
```

## Docker Compose File

Create `docker-compose.yml` in the project root:

```yaml
version: '3.8'

services:
  mongodb:
    image: mongo:5.0
    container_name: pos-mongodb
    ports:
      - "27017:27017"
    environment:
      MONGO_INITDB_ROOT_USERNAME: admin
      MONGO_INITDB_ROOT_PASSWORD: password
      MONGO_INITDB_DATABASE: pos_db
    volumes:
      - mongodb_data:/data/db
      - mongo-init:/docker-entrypoint-initdb.d
    networks:
      - pos-network

  backend:
    image: pos-backend:1.0.0
    container_name: pos-backend
    build:
      context: ./pos-backend
      dockerfile: Dockerfile
    ports:
      - "8080:8080"
    environment:
      SPRING_DATA_MONGODB_URI: mongodb://admin:password@mongodb:27017/pos_db?authSource=admin
    depends_on:
      - mongodb
    networks:
      - pos-network

volumes:
  mongodb_data:
  mongo-init:

networks:
  pos-network:
    driver: bridge
```

## Build Docker Images

### Backend Image

Create `Dockerfile` in `pos-backend` directory:

```dockerfile
FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/pos-backend-1.0.0.jar pos-backend.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "pos-backend.jar"]
```

Build:
```bash
cd pos-backend
mvn clean package
docker build -t pos-backend:1.0.0 .
```

### Frontend Image

Create `Dockerfile` in `pos-frontend` directory:

```dockerfile
FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/pos-frontend-1.0.0.jar pos-frontend.jar

ENTRYPOINT ["java", "-jar", "pos-frontend.jar"]
```

Build:
```bash
cd pos-frontend
mvn clean package
docker build -t pos-frontend:1.0.0 .
```

## Running with Docker Compose

```bash
# Start all services
docker-compose up

# Stop all services
docker-compose down

# View logs
docker-compose logs -f

# View specific service logs
docker-compose logs -f mongodb
docker-compose logs -f backend
```

## MongoDB Access via Docker

```bash
# Connect to MongoDB container
docker exec -it pos-mongodb mongosh -u admin -p password --authenticationDatabase admin

# Create database and collections
use pos_db
db.products.insertOne({ name: "Test", price: 100 })
```

## Health Checks

```bash
# Check backend health
curl http://localhost:8080/api/categories

# Check MongoDB
docker exec pos-mongodb mongosh -u admin -p password --authenticationDatabase admin --eval "db.version()"
```

## Kubernetes Deployment (Optional)

Create `k8s-deployment.yaml`:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: pos-backend
spec:
  replicas: 1
  selector:
    matchLabels:
      app: pos-backend
  template:
    metadata:
      labels:
        app: pos-backend
    spec:
      containers:
      - name: pos-backend
        image: pos-backend:1.0.0
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_DATA_MONGODB_URI
          value: "mongodb://admin:password@mongodb:27017/pos_db?authSource=admin"
---
apiVersion: v1
kind: Service
metadata:
  name: pos-backend-service
spec:
  type: LoadBalancer
  selector:
    app: pos-backend
  ports:
  - port: 8080
    targetPort: 8080
```

Deploy:
```bash
kubectl apply -f k8s-deployment.yaml
```

## Troubleshooting

### MongoDB Connection Error
```bash
# Check if MongoDB container is running
docker ps | grep pos-mongodb

# View MongoDB logs
docker logs pos-mongodb

# Restart MongoDB
docker restart pos-mongodb
```

### Port Already in Use
```bash
# Change port mapping in docker-compose.yml
ports:
  - "27018:27017"  # Use 27018 instead of 27017
```

### Clean Docker Resources
```bash
# Remove containers
docker-compose down

# Remove images
docker rmi pos-backend:1.0.0

# Clean volumes
docker volume prune
```

---

For local development, the standard Maven/Spring Boot setup is recommended.
