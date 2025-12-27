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
                anyOf {
                    changeRequest()
                    branch 'main'
                }
            }
            steps {
                dir('backend') {
                    script {
                        sh "chmod +x mvnw"
                        sh "./mvnw clean verify"
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
                    deployToProd(env.PROD_IP, env.PROD_USER, env.PROD_SSH_ID)
                }
            }
        }
    }

    post {
        always {
            script {
                try {
                    def postBuildStatus = load('infra/postBuildStatus.groovy')
                    postBuildStatus(currentBuild.currentResult)
                } catch (e) {
                    echo "Could not execute postBuildStatus: ${e.message}"
                }
            }
            // Publish JaCoCo HTML reports
            publishHTML(target: [
                allowMissing: true,
                alwaysLinkToLastBuild: true,
                keepAll: true,
                reportDir: 'backend/**/target/site/jacoco',
                reportFiles: 'index.html',
                reportName: 'JaCoCo Coverage Report',
                reportTitles: 'Code Coverage'
            ])
        }
        success {
            sh "docker image prune -f --filter 'label=stage=intermediate'"
        }
    }
}