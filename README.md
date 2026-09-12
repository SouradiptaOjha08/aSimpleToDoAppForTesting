# aSimpleToDoAppForTestingCICD

A production-ready, containerized Spring Boot 4 REST API demonstrating a complete, automated end-to-end **Continuous Integration & Continuous Delivery (CI/CD)** pipeline using **GitHub**, **Jenkins**, **Docker Hub**, and an **AWS EC2** deployment host.

---

## 🚀 Pipeline Architecture & Workflow

Every code change pushed to the `main` branch automatically triggers the automated pipeline:

```
[Developer Push to GitHub]
           │
           ▼
[GitHub Webhook: push event]
           │
           ▼
[Jenkins Pipeline on EC2]
   ├── 1. Test & Package (Maven Wrapper, JUnit 5, MockMvc)
   ├── 2. Build Docker Image (Multi-stage build, Temurin JDK 21)
   ├── 3. Push Image to Docker Hub (Tagged with Jenkins ${BUILD_NUMBER})
   └── 4. Deploy to EC2 (Pulls exact image tag, replaces running container)
           │
           ▼
[Live Container: http://<EC2_IP>:8008/api/todos]
```

### Flow Breakdown

1. **Trigger**: Developer pushes a commit to the GitHub repository (`origin/main`).
2. **Webhook**: GitHub delivers an HTTP POST event to Jenkins at `https://<JENKINS_URL>/github-webhook/`.
3. **Stage 1 (Test & Package)**: Jenkins executes `./mvnw -B clean test package`. Unit and integration tests validate the business logic and API endpoints.
4. **Stage 2 (Build Image)**: Docker builds a lightweight production image tagged dynamically as `<DOCKERHUB_REPO>:<BUILD_NUMBER>`.
5. **Stage 3 (Push Image)**: Jenkins authenticates with Docker Hub using stored credentials and pushes the newly built image.
6. **Stage 4 (Deploy to EC2)**: The pipeline pulls the exact image onto the host, stops and removes the prior `todo-api` container, and launches the updated container with `--restart unless-stopped` exposing port `8008`.

---

## 🛠️ Tech Stack & Tools

- **Language & Runtime**: Java 21 (Eclipse Temurin)
- **Framework**: Spring Boot 4.1.1
- **Persistence**: Spring Data JPA, Hibernate 7, H2 In-Memory Database
- **Validation**: Jakarta Bean Validation (`@NotBlank`, `@Size`, `@Valid`)
- **Observability**: Spring Boot Actuator (`/actuator/health`, `/actuator/info`)
- **Testing**: JUnit 5, MockMvc, Spring Boot Test
- **Build Tool**: Apache Maven Wrapper (`./mvnw`)
- **Containerization**: Docker (Multi-stage Dockerfile), Docker Compose
- **Container Registry**: Docker Hub
- **CI/CD Automation**: Jenkins Declarative Pipeline (`Jenkinsfile`)
- **Cloud Infrastructure**: AWS EC2 (Ubuntu / Amazon Linux)

---

## 📁 Codebase Structure

```
.
├── .dockerignore                            # Excludes build artifacts and git files from Docker context
├── Dockerfile                               # Production multi-stage build (Temurin 21 JDK -> JRE)
├── Jenkinsfile                              # Declarative CI/CD pipeline definition
├── compose.yaml                             # Local container orchestration with Actuator health checks
├── mvnw / mvnw.cmd                          # Maven wrapper executables
├── pom.xml                                  # Project dependencies and build settings
├── src
│   ├── main
│   │   ├── java/com/todoapp
│   │   │   ├── ASimpleToDoAppForTestingApplication.java  # Main application entry point
│   │   │   └── todo
│   │   │       ├── Todo.java                              # JPA Entity (id, title, completed)
│   │   │       ├── TodoRepository.java                    # Spring Data JPA repository
│   │   │       ├── TodoService.java                       # Business logic layer
│   │   │       └── TodoController.java                    # REST Controller with DTO records
│   │   └── resources
│   │       └── application.properties       # App configs, port 8008, JPA create-drop, Actuator
│   └── test
│       └── java/com/todoapp
│           ├── ASimpleToDoAppForTestingApplicationTests.java # Context loading test
│           └── todo
│               └── TodoControllerTests.java                 # WebMvc API integration tests
└── README.md
```

### Key Application Components

- **[`TodoController.java`](file:///Users/souradiptaojha/Developer/intelliJ_workspace_python/aSimpleToDoAppForTesting/src/main/java/com/todoapp/todo/TodoController.java)**: Exposes REST endpoints under `/api/todos`. Uses modern Java record classes (`CreateTodoRequest`, `TodoResponse`) for clean request/response serialization and Jakarta Bean Validation to reject empty or oversized titles.
- **[`TodoService.java`](file:///Users/souradiptaojha/Developer/intelliJ_workspace_python/aSimpleToDoAppForTesting/src/main/java/com/todoapp/todo/TodoService.java)**: Manages transactions (`@Transactional`), sanitizes inputs (e.g., `.trim()`), retrieves todos ordered by ascending ID, and handles 404 `ResponseStatusException` when items are not found.
- **[`TodoRepository.java`](file:///Users/souradiptaojha/Developer/intelliJ_workspace_python/aSimpleToDoAppForTesting/src/main/java/com/todoapp/todo/TodoRepository.java)**: Spring Data repository supplying `findAllByOrderByIdAsc()`.
- **[`Todo.java`](file:///Users/souradiptaojha/Developer/intelliJ_workspace_python/aSimpleToDoAppForTesting/src/main/java/com/todoapp/todo/Todo.java)**: JPA entity configured with `@GeneratedValue(strategy = GenerationType.IDENTITY)`.

---

## 📡 REST API Reference

The application runs on port `8008`.

| Method | Endpoint | Description | Request Body | Success Code |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/todos` | Create a new todo item | `{"title": "string"}` | `201 Created` |
| `GET` | `/api/todos` | List all todos in creation order | None | `200 OK` |
| `PATCH` | `/api/todos/{id}/completed` | Mark a todo as completed | None | `200 OK` |
| `GET` | `/actuator/health` | Application & readiness health status | None | `200 OK` |

### Example Requests & Responses

#### 1. Create a Todo
```bash
curl -i -X POST http://localhost:8008/api/todos \
  -H 'Content-Type: application/json' \
  -d '{"title": "Implement CI/CD with Jenkins"}'
```
**Response (`201 Created`):**
```json
{
  "id": 1,
  "title": "Implement CI/CD with Jenkins",
  "completed": false
}
```

#### 2. List All Todos
```bash
curl http://localhost:8008/api/todos
```
**Response (`200 OK`):**
```json
[
  {
    "id": 1,
    "title": "Implement CI/CD with Jenkins",
    "completed": false
  }
]
```

#### 3. Complete a Todo
```bash
curl -X PATCH http://localhost:8008/api/todos/1/completed
```
**Response (`200 OK`):**
```json
{
  "id": 1,
  "title": "Implement CI/CD with Jenkins",
  "completed": true
}
```

#### 4. Health Check
```bash
curl http://localhost:8008/actuator/health
```
**Response (`200 OK`):**
```json
{
  "status": "UP"
}
```

---

## 💻 Local Development & Testing

### Prerequisites
- Java 21 JDK
- Docker / Docker Desktop

### 1. Run Unit & Integration Tests
Execute the test suite using the Maven wrapper:
```bash
./mvnw clean test
```

### 2. Run the Application Locally
```bash
./mvnw clean package
java -jar target/todo-api.jar
```
The application will start on `http://localhost:8008`.

---

## 🐳 Docker & Containerization

### Multi-Stage `Dockerfile`
The Docker build is structured into two distinct stages to optimize security and final image size:
1. **Builder Stage (`maven:3.9-eclipse-temurin-21`)**:
   - Copies Maven wrapper and `pom.xml`.
   - Runs `dependency:go-offline` to leverage layer caching for dependencies.
   - Compiles and packages `todo-api.jar`.
2. **Runtime Stage (`eclipse-temurin:21-jre`)**:
   - Installs `curl` for container health monitoring.
   - Creates an unprivileged system user (`todo`, UID 10001) for container security.
   - Copies only the executable JAR from the builder stage.
   - Exposes port `8008` and executes as user `todo`.

### Build & Run with Docker
```bash
# Build local image
docker build -t a-simple-todo-app:local .

# Run container
docker run --rm -d -p 8008:8008 --name todo-api a-simple-todo-app:local
```

### Run with Docker Compose
```bash
# Build and run with health check verification
docker compose up --build -d

# Check status and health
docker compose ps

# Stop containers
docker compose down
```

---

## ⚙️ Jenkins CI/CD Pipeline Setup

The [`Jenkinsfile`](file:///Users/souradiptaojha/Developer/intelliJ_workspace_python/aSimpleToDoAppForTesting/Jenkinsfile) defines the continuous delivery workflow:

```groovy
pipeline {
    agent any

    triggers {
        githubPush()
    }

    options {
        disableConcurrentBuilds()
        timestamps()
    }

    environment {
        DOCKERHUB_REPOSITORY = credentials('dockerhub-repository')
    }

    stages {
        stage('Test and package') {
            steps {
                sh './mvnw -B clean test package'
            }
        }
        stage('Build image') {
            steps {
                sh 'docker build --tag "${DOCKERHUB_REPOSITORY}:${BUILD_NUMBER}" .'
            }
        }
        stage('Push image') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub-credentials', usernameVariable: 'DOCKERHUB_USERNAME', passwordVariable: 'DOCKERHUB_TOKEN')]) {
                    sh '''#!/bin/sh
                        set +x
                        echo "$DOCKERHUB_TOKEN" | docker login --username "$DOCKERHUB_USERNAME" --password-stdin
                        docker push "${DOCKERHUB_REPOSITORY}:${BUILD_NUMBER}"
                        docker logout
                    '''
                }
            }
        }
        stage('Deploy to EC2') {
            steps {
                sh '''#!/bin/sh
                    set -eu
                    IMAGE="$DOCKERHUB_REPOSITORY:$BUILD_NUMBER"
                    docker pull "$IMAGE"
                    docker rm --force todo-api 2>/dev/null || true
                    docker run --detach \
                      --name todo-api \
                      --restart unless-stopped \
                      --publish 8008:8008 \
                      "$IMAGE"
                '''
            }
        }
    }
}
```

### Jenkins Configuration Steps

1. **Install Required Jenkins Plugins**:
   - Git plugin
   - GitHub plugin
   - Pipeline
   - Credentials Binding plugin

2. **Configure Credentials in Jenkins** (`Manage Jenkins` -> `Credentials`):
   - **`dockerhub-credentials`**: Type *Username with password*. Enter your Docker Hub username and access token.
   - **`dockerhub-repository`**: Type *Secret text*. Value should be your Docker Hub repository path (e.g. `yourusername/todo-api`).

3. **Configure the Pipeline Job**:
   - Create a new **Pipeline** job.
   - Under **Build Triggers**, select **GitHub hook trigger for GITScm polling**.
   - Under **Pipeline**, choose **Pipeline script from SCM**, select **Git**, enter your GitHub repository URL:
     `https://github.com/SouradiptaOjha08/aSimpleToDoAppForTestingCICD.git`
   - Set Branch Specifier to `*/main` and Script Path to `Jenkinsfile`.

4. **GitHub Webhook Setup**:
   - Go to your GitHub repository: **Settings** -> **Webhooks** -> **Add webhook**.
   - Payload URL: `http://<JENKINS_PUBLIC_IP_OR_DOMAIN>:8080/github-webhook/`
   - Content type: `application/json`
   - Trigger event: **Just the push event**.

---

## ☁️ AWS EC2 Host Configuration

When deploying Jenkins and the application container on the same EC2 instance:

### 1. User Permissions for Docker
Ensure the Jenkins service account has rights to interact with the Docker daemon:
```bash
sudo usermod -aG docker jenkins
sudo systemctl restart jenkins
```

### 2. EC2 Security Group Inbound Rules
Configure inbound firewall rules in your AWS Management Console:
- **Port 8080** (Jenkins): Restricted to your IP or management CIDR.
- **Port 8008** (Todo API Application): Restricted to your IP or open for public demo.
- **Port 22** (SSH): Restricted strictly to your IP.

---

## 🔄 Repository Renaming Details

The repository and project identifiers have been updated to **`aSimpleToDoAppForTestingCICD`**:
- **Maven Artifact**: `aSimpleToDoAppForTestingCICD` in [`pom.xml`](file:///Users/souradiptaojha/Developer/intelliJ_workspace_python/aSimpleToDoAppForTesting/pom.xml)
- **Spring Application Name**: `aSimpleToDoAppForTestingCICD` in [`src/main/resources/application.properties`](file:///Users/souradiptaojha/Developer/intelliJ_workspace_python/aSimpleToDoAppForTesting/src/main/resources/application.properties)
- **Git Remote Origin URL**:
  ```bash
  git remote set-url origin https://github.com/SouradiptaOjha08/aSimpleToDoAppForTestingCICD.git
  ```
