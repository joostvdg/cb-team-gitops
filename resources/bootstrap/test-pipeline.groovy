pipeline {
    agent {
        kubernetes {
        label 'mypod'
        yaml """
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: maven
    image: maven:3-jdk-11-slim
    command: ['cat']
    tty: true
    volumeMounts:
      - name: maven-cache
        mountPath: /root/.m2/repository
    resources:
      requests:
        memory: "50Mi"
        cpu: "100m"
      limits:
        memory: "150Mi"
        cpu: "200m"
  resources:
    requests:
      memory: "512Mi"
      cpu: "250m"
    limits:
      memory: "1024Mi"
      cpu: "500m"
  volumes:
    - name: maven-cache
      hostPath:
        path: /tmp
        type: Directory
"""
        }
    }
    stages {
        stage('Run maven') {
            steps {
                git 'https://github.com/joostvdg/jx-maven-lib.git'
                container('maven') {
                    sh 'mvn -version'
                    sh 'mvn clean verify'
                }
            }
        }
    }
}

