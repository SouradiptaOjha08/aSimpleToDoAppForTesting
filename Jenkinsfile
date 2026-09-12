pipeline {
    agent any

    triggers {
        githubPush()
    }

    options {
        disableConcurrentBuilds()
        timestamps()
    }

    parameters {
        string(name: 'DOCKERHUB_REPOSITORY', defaultValue: 'your-dockerhub-username/todo-api', description: 'Docker Hub repository to push, without a tag.')
        string(name: 'EC2_HOST', defaultValue: 'your-ec2-public-dns-or-ip', description: 'Public DNS name or IP address of the EC2 instance that runs the app.')
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
                withCredentials([sshUserPrivateKey(credentialsId: 'ec2-ssh-key', keyFileVariable: 'EC2_SSH_KEY', usernameVariable: 'EC2_USER')]) {
                    sh '''#!/bin/sh
                        set -eu
                        ssh -i "$EC2_SSH_KEY" \
                          -o BatchMode=yes \
                          -o StrictHostKeyChecking=accept-new \
                          "$EC2_USER@$EC2_HOST" \
                          "IMAGE='$DOCKERHUB_REPOSITORY:$BUILD_NUMBER' bash -s" <<'REMOTE'
                        set -eu
                        docker pull "$IMAGE"
                        docker rm --force todo-api 2>/dev/null || true
                        docker run --detach \
                          --name todo-api \
                          --restart unless-stopped \
                          --publish 8008:8008 \
                          "$IMAGE"
REMOTE
                    '''
                }
            }
        }
    }
}
