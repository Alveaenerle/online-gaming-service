pipeline {
    agent any

    environment {
        GITHUB_TOKEN = credentials('90d79e6c-ebeb-4414-9f4c-f30e091789ee')
        REPO = 'Alveaenerle/online-gaming-service'

        NEXUS_URL = 'docker.yapyap.pl'
        NEXUS_CREDS = credentials('86a5c18e-996c-42ea-bf9e-190b2cb978bd')

        DEMO_IP = '10.10.0.171'
        DEMO_USER = 'pis'
        DEMO_SSH_ID = 'prod-ssh-key'
        
        PROD_IP = credentials('prod-server-ip')
        PROD_USER = credentials('prod-server-user')
        PROD_SSH_ID = 'prod-server-ssh-key'
    }

    stages {
        stage('Cleanup Docker') {
            steps {
                script {
                    // Clean up unused Docker resources to free disk space
                    sh 'docker system prune -af --volumes || true'
                }
            }
        }

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
                        sh "./mvnw -T 1C clean verify jacoco:report-aggregate"
                    }
                }
            }
            post {
                always {

                    publishHTML(target: [
                        allowMissing: true,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: 'backend/coverage-report/target/site/jacoco-aggregate',
                        reportFiles: 'index.html',
                        reportName: 'JaCoCo Aggregate Coverage Report',
                        reportTitles: 'Aggregate Code Coverage'
                    ])
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
                    def builds = [:]

                    backendModules.each { module ->
                        builds[module] = {
                            deployService("online-gaming-${module}", './backend', module)
                        }
                    }

                    builds['frontend'] = {
                        deployService('online-gaming-frontend', './frontend')
                    }

                    parallel builds

                    sh "docker logout ${NEXUS_URL}"
                }
            }
        }

        stage('Deploy to Demo') {
            when { branch 'main' }
            steps {
                script {
                    def deployToDemo = load('infra/deploy.groovy')
                    deployToDemo(env.DEMO_IP, env.DEMO_USER, env.DEMO_SSH_ID)
                }
            }
        }

        stage('Deploy to Prod') {
            when { expression { env.TAG_NAME != null && env.TAG_NAME.startsWith('v') } }
            steps {
                script {
                    def deployToProd = load('infra/deploy.groovy')
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
        }
        success {
            sh "docker image prune -f --filter 'label=stage=intermediate'"
        }
    }
}