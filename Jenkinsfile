pipeline {
    agent {
        kubernetes {
            label 'jenkins-agent'
            yaml '''
apiVersion: v1
kind: Pod
spec:
  serviceAccountName: jenkins
  containers:
  - name: cli
    image: caladreas/cbcore-cli:2.164.3.2
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
  - name: kubectl
    image: bitnami/kubectl:latest
    command: ["cat"]
    tty: true
    resources:
      requests:
        memory: "50Mi"
        cpu: "100m"
      limits:
        memory: "150Mi"
        cpu: "200m"
  - name: yq
    image: mikefarah/yq
    command: ['cat']
    tty: true
    resources:
      requests:
        memory: "50Mi"
        cpu: "100m"
      limits:
        memory: "50Mi"
        cpu: "100m"
  - name: jpb
    image: caladreas/jpb
    command:
    - cat
    tty: true
    resources:
      requests:
        memory: "50Mi"
        cpu: "100m"
      limits:
        memory: "50Mi"
        cpu: "100m"
  securityContext:
    runAsUser: 1000
    fsGroup: 1000
'''
        }
    }
    options {
        disableConcurrentBuilds()
        buildDiscarder logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '5', numToKeepStr: '5')
    }
    environment {
        RESET_NAMESPACE     = 'jx-production'
        CREDS               = credentials('jenkins-api')
        CLI                 = "java -jar /usr/bin/jenkins-cli.jar -noKeyAuth -s http://cjoc.jx-production/cjoc -auth"
        COMMIT_INFO         = ''
        TEAM                = ''
    }
    stages {
        stage('Create Team') {
            when { allOf { branch 'master'; changeset "teams/**/team.*" } }
            parallel {
                stage('Main') {
                    stages {
                        stage('Parse Changelog') {
                            steps {
                                script {
                                    scmVars = git 'https://github.com/joostvdg/cb-team-gitops.git'
                                    COMMIT_INFO = "${scmVars.GIT_COMMIT} ${scmVars.GIT_PREVIOUS_COMMIT}"
                                    def changeSetData = sh returnStdout: true, script: "git diff-tree --no-commit-id --name-only -r ${COMMIT_INFO}"
                                    changeSetData = changeSetData.replace("\n", "\\n")
                                    container('jpb') {
                                        changeSetFolders = sh returnStdout: true, script: "/usr/bin/jpb/bin/jpb GitChangeListToFolder '${changeSetData}' 'teams/'"
                                        changeSetFolders = changeSetFolders.split(',')
                                    }
                                    if (changeSetFolders.length > 0) {
                                        TEAM = changeSetFolders[0]
                                    } else {
                                        TEAM = ''
                                    }
                                    echo "Team that changed: ${TEAM}"
                                }
                            }
                        }
                        stage('Create Namespace') {
                            when { not { environment name: 'TEAM', value: '' } }
                            environment {
                                NAMESPACE   = "cb-teams-${TEAM}"
                                RECORD_LOC  = "teams/${TEAM}"
                            }
                            steps {
                                container('kubectl') {
                                    sh '''
                                        cat ${RECORD_LOC}/team.yaml
                                        kubectl apply -f ${RECORD_LOC}/team.yaml
                                    '''
                                }
                            }
                        }
                        stage('Change OC Namespace') {
                            when { not { environment name: 'TEAM', value: '' } }
                            environment {
                                NAMESPACE   = "cb-teams-${TEAM}"
                            }
                            steps {
                                container('cli') {
                                    sh 'echo ${NAMESPACE}'
                                    script {
                                        def response = sh encoding: 'UTF-8', label: 'create team', returnStatus: true, script: '${CLI} ${CREDS} groovy = < resources/bootstrap/configure-oc-namespace.groovy ${NAMESPACE}'
                                        println "Response: ${response}"
                                    }
                                }
                            }
                        }
                        stage('Create Team Master') {
                            when { not { environment name: 'TEAM', value: '' } }
                            environment {
                                TEAM_NAME = "${TEAM}"
                            }
                            steps {
                                container('cli') {
                                    println "TEAM_NAME=${TEAM_NAME}"
                                    sh 'ls -lath'
                                    sh 'ls -lath teams/'
                                    script {
                                        def response = sh encoding: 'UTF-8', label: 'create team', returnStatus: true, script: '${CLI} ${CREDS} teams ${TEAM_NAME} --put < "teams/${TEAM_NAME}/team.json"'
                                        println "Response: ${response}"
                                    }
                                }
                            }
                        }
                    }
                }
                stage('Dummy') {
                    steps {
                        println 'dummy'
                    }
                }
            }
        }
        stage('Test CLI Connection') {
            steps {
                container('cli') {
                    script {
                        def response = sh encoding: 'UTF-8', label: 'retrieve version', returnStatus: true, script: '${CLI} ${CREDS} version'
                        println "Response: ${response}"
                    }
                }
            }
        }
        stage('Update Team Recipes') {
            when { allOf { branch 'master'; changeset "recipes/recipes.json" } }
            steps {
                container('cli') {
                    sh 'ls -lath'
                    sh 'ls -lath recipes/'
                    script {
                        def response = sh encoding: 'UTF-8', label: 'update team recipe', returnStatus: true, script: '${CLI} ${CREDS} team-creation-recipes --put < "recipes/recipes.json"'
                        println "Response: ${response}"
                    }
                }
            }
        }
    }
    post {
        always {
            container('cli') {
                sh '${CLI} ${CREDS} groovy = < resources/bootstrap/configure-oc-namespace.groovy ${RESET_NAMESPACE}'
            }
        }
    }
}        

