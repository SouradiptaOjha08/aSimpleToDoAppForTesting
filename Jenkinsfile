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
