pipeline {
    agent any

    parameters {
        string(name: 'DOCKERHUB_REPOSITORY', defaultValue: 'your-dockerhub-username/todo-api', description: 'Docker Hub repository to push, without a tag.')
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
    }
}
