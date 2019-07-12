pipeline {
    agent {
        kubernetes {
        label 'team-automation'
        yaml """
kind: Pod
spec:
  containers:
  - name: cli
    image: caladreas/cbcore-cli:latest
    imagePullPolicy: Always
    command:
    - cat
    tty: true
    resources:
      requests:
        memory: "50Mi"
        cpu: "150m"
      limits:
        memory: "50Mi"
        cpu: "150m"
  resources:
    requests:
      memory: "512Mi"
      cpu: "250m"
    limits:
      memory: "1024Mi"
      cpu: "500m"
"""
        }
    }
    environment {
        CREDS   = credentials('jenkins-api')
        CLI     = "java -jar /usr/bin/jenkins-cli.jar -noKeyAuth -s http://cjoc.jx-production/cjoc -auth"
    }
    stages {
        stage('Update Team Recipes') {
            when { changeset "recipes/recipes.json" }
            steps {
                container('cli') {
                    sh 'ls -lath'
                    sh 'ls -lath recipes/'
                    sh '${CLI} ${CREDS} team-creation-recipes --put < "recipes/recipes.json"'
                }
            }
        }
        stage('Update Teams') {
            when { changeset "teams/*.json" }
            steps {
                container('cli') {
                    
                    // cbc teams hex --put < team-hex.json
                    sh '${CLI} ${CREDS} teams hex --put < teams/team-hex.json'
                }
            }
        }
    }
}
