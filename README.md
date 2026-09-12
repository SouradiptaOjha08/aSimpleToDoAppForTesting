# Todo API DevOps Learning Project

This small REST API is designed to let you learn one delivery tool at a time:

`Maven -> Docker -> Docker Compose -> Docker Hub -> Jenkins`

Todo data is stored in an in-memory H2 database. It is intentionally reset whenever the application container or pod restarts.

## Prerequisites

Install Java 21, Docker Desktop (running), and Jenkins. Jenkins needs an agent with Java 21 and Docker CLI access to a Docker daemon.

Verify the local tools:

```sh
java -version
docker version
```

## 1. Run and use the API locally

Build and test it:

```sh
./mvnw clean test package
java -jar target/todo-api.jar
```

In another terminal, create, list, and complete a todo:

```sh
curl -i -X POST http://localhost:8008/api/todos \
  -H 'Content-Type: application/json' \
  -d '{"title":"Learn Docker"}'

curl http://localhost:8008/api/todos
curl -X PATCH http://localhost:8008/api/todos/1/completed
curl http://localhost:8008/actuator/health
```

The API contract is deliberately small:

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/todos` | Create from `{"title":"..."}`; title must be 1–200 non-blank characters. |
| `GET` | `/api/todos` | List todos in creation order. |
| `PATCH` | `/api/todos/{id}/completed` | Mark a todo as completed. |

## 2. Docker and Docker Compose

Build and run a tagged image. Replace `YOUR_DOCKERHUB_USERNAME` with your Docker Hub username:

```sh
docker build -t YOUR_DOCKERHUB_USERNAME/todo-api:local .
docker run --rm -p 8008:8008 YOUR_DOCKERHUB_USERNAME/todo-api:local
```

`Dockerfile` is a multi-stage build: Maven creates the JAR in the builder stage and the runtime stage contains only the JRE, JAR, and `curl` for health checks.

Alternatively, let Compose build and run the single application service:

```sh
docker compose up --build
docker compose ps
docker compose down
```

Compose reports healthy only when `/actuator/health` returns success.

## 3. Push to Docker Hub

Create a **public** Docker Hub repository named `todo-api` first. Then authenticate, tag, push, and inspect it:

```sh
docker login
docker build -t YOUR_DOCKERHUB_USERNAME/todo-api:1 .
docker push YOUR_DOCKERHUB_USERNAME/todo-api:1
docker pull YOUR_DOCKERHUB_USERNAME/todo-api:1
```

## 4. Automate with Jenkins

The `Jenkinsfile` is declarative and performs:

1. Maven clean, test, and package.
2. Docker image build tagged as `DOCKERHUB_REPOSITORY:BUILD_NUMBER`.
3. Docker Hub login and push.

Before creating the job:

1. Put this directory in a Git repository and push it to your Git provider.
2. In Jenkins, create a Username with password credential with ID `dockerhub-credentials`. Use your Docker Hub username and a Docker Hub access token, not your account password.
3. Ensure the Jenkins agent can run `docker` and `./mvnw`.
4. Create a Pipeline job that loads `Jenkinsfile` from SCM. Choose the `DOCKERHUB_REPOSITORY` parameter, for example `YOUR_DOCKERHUB_USERNAME/todo-api`.

Run the pipeline to build, test, tag, and push the image. Its Jenkins build number becomes the Docker image tag, so each successful build produces a traceable image.

## Learning order

Do each step manually before making Jenkins do it. When a command fails, inspect the layer that owns it: Maven test output for application issues, `docker logs` for container issues, `docker compose ps` for Compose health, and Docker Hub repository tags for registry issues. This makes the Jenkins console output much easier to understand when you automate the same commands.
