# Todo API DevOps Learning Project

This small REST API is designed to let you learn one delivery tool at a time:

`GitHub push -> Jenkins -> Maven test -> Docker image -> Docker Hub -> EC2`

Todo data is stored in an in-memory H2 database. It is intentionally reset whenever the application container restarts.

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

## 4. Continuous delivery: GitHub to EC2 with Jenkins

Every push to `main` will start Jenkins through a GitHub webhook. Jenkins tests the commit, builds an image, tags it with its build number, pushes that image to Docker Hub, then connects to EC2 through SSH. The EC2 host pulls that exact image and replaces the running `todo-api` container. Access the deployed app at:

```text
http://YOUR_EC2_PUBLIC_DNS_OR_IP:8008/actuator/health
```

### Prepare the EC2 host once

Use an EC2 instance with a public IPv4 address or public DNS name. Install Docker on it, ensure the login user can run `docker`, then verify it:

```sh
docker version
docker ps
```

In the EC2 security group, add an inbound TCP rule for port `8008` from **your own public IP address**. Also allow SSH (port `22`) only from the Jenkins machine's public IP address. Do not open SSH to the world.

The first version assumes the Docker Hub repository is public, so EC2 can pull it without registry credentials. Keep the Docker daemon running on EC2.

### Configure Jenkins once

1. Install the Jenkins **Pipeline**, **Git**, **GitHub**, **Credentials Binding**, and **SSH Credentials** plugins.
2. Ensure the Jenkins agent has Java 21, Docker CLI access to a Docker daemon, and the `ssh` client. The agent must be allowed to connect to the EC2 instance on port 22.
3. Create a Jenkins credential of type **Username with password** with ID `dockerhub-credentials`. Use your Docker Hub username and a Docker Hub access token.
4. Create a Jenkins credential of type **SSH Username with private key** with ID `ec2-ssh-key`. Use the EC2 login username (commonly `ubuntu` for Ubuntu or `ec2-user` for Amazon Linux) and the private key that matches the EC2 key pair.
5. Create a Pipeline job using **Pipeline script from SCM**. Select Git, use this GitHub repository URL, set the branch specifier to `*/main`, and set the script path to `Jenkinsfile`.
6. In the job's parameter defaults, set `DOCKERHUB_REPOSITORY` to `YOUR_DOCKERHUB_USERNAME/todo-api` and `EC2_HOST` to your EC2 public DNS name or IP address. Save these real values before enabling the webhook.
7. In the GitHub repository, add a webhook to `https://YOUR_JENKINS_PUBLIC_URL/github-webhook/`, set content type to `application/json`, and choose **Just the push event**.

The `Jenkinsfile` will then perform these stages automatically for each GitHub push to `main`:

1. Maven clean, test, and package.
2. Docker image build tagged as `DOCKERHUB_REPOSITORY:BUILD_NUMBER`.
3. Docker Hub login and push.
4. SSH deployment to EC2: pull that tag, remove the previous `todo-api` container, and run the replacement with `--restart unless-stopped` and port `8008` exposed.

After a successful build, test the running deployment:

```sh
curl http://YOUR_EC2_PUBLIC_DNS_OR_IP:8008/actuator/health
curl http://YOUR_EC2_PUBLIC_DNS_OR_IP:8008/api/todos
```

If a deployment fails, read the Jenkins console first. To inspect the EC2 service, connect through SSH and run:

```sh
docker ps
docker logs todo-api
```

## Learning order

Do each step manually before making Jenkins do it. When a command fails, inspect the layer that owns it: Maven test output for application issues, `docker logs` for container issues, `docker compose ps` for Compose health, Docker Hub repository tags for registry issues, and the Jenkins console for delivery failures.
