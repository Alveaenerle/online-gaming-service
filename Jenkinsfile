pipeline {
    agent any

    environment {
        GITHUB_TOKEN = credentials('90d79e6c-ebeb-4414-9f4c-f30e091789ee')
        REPO = 'Alveaenerle/online-gaming-service'

        NEXUS_URL = 'docker.yapyap.pl'
        NEXUS_CREDS = credentials('86a5c18e-996c-42ea-bf9e-190b2cb978bd')

        PROD_IP = '10.10.0.171'
        PROD_USER = 'pis'
        PROD_SSH_ID = 'prod-ssh-key'
    }

    stages {
        stage('Run tests & Code Coverage') {
            when {
                changeRequest()
            }
            steps {
                dir('backend') {
                    script {
                        sh "chmod +x mvnw"
                        sh "chmod +x backend_code_coverage.sh"

                        def modules = ['social', 'menu', 'makao', 'ludo', 'authorization', 'tests']

                        modules.each { moduleName ->
                            sh "./backend_code_coverage.sh ${moduleName}"
                        }
                    }
                }
            }
        }

        stage('Build & Push to Nexus') {
            when { branch 'main' }
            steps {
                script {
                    def deployService = load('infra/buildAndDeployImage.groovy')

                    echo 'Logging into Nexus...'
                    sh "echo ${NEXUS_CREDS_PSW} | docker login ${NEXUS_URL} -u ${NEXUS_CREDS_USR} --password-stdin"

                    def backendModules = ['social', 'menu', 'makao', 'ludo', 'authorization']
                    backendModules.each { module ->
                        deployService("online-gaming-${module}", './backend', module)
                    }

                    // Frontend doesn't need the 3rd arg
                    deployService('online-gaming-frontend', './frontend')

                    sh "docker logout ${NEXUS_URL}"
                }
            }
        }

        stage('Deploy to Production') {
            when { branch 'main' }
            steps {
                script {
                    def deployToProd = load('infra/deployToProd.groovy')
                    withCredentials([usernamePassword(credentialsId: '86a5c18e-996c-42ea-bf9e-190b2cb978bd',
                                                      usernameVariable: 'NEXUS_CREDS_USR',
                                                      passwordVariable: 'NEXUS_CREDS_PSW')]) {
                        deployToProd(env.PROD_IP, env.PROD_USER, env.PROD_SSH_ID)
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                def postBuildStatus = load('infra/postBuildStatus.groovy')
                def result = currentBuild.currentResult
                if (result == 'SUCCESS' || result == 'FAILURE') {
                    postBuildStatus(result)
                }
            }
        }
    }
}