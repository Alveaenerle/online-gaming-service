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
                // Backend Tests
                dir('backend') {
                    script {
                        sh "chmod +x mvnw"
                        sh "chmod +x backend_code_coverage.sh"

                        sh "./mvnw -pl common,common-test-support -am clean install -DskipTests"

                        def modules = ['social', 'menu', 'makao', 'ludo', 'authorization']

                        modules.each { moduleName ->
                            sh "./backend_code_coverage.sh ${moduleName}"
                        }
                    }
                }
                
                // Frontend Tests
                dir('frontend') {
                    script {
                        // Run in a Docker container to ensure Node.js/npm is available
                        docker.image('node:20').inside {
                            sh "npm install"
                            sh "npm run test:cov"
                        }
                    }
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                script {
                    // Use the 'sonar-project.properties' file at the root
                    // Requires 'sonar-scanner' tool to be available in Jenkins or PATH
                    withSonarQubeEnv('Sonar') {
                        sh "sonar-scanner"
                    }
                }
            }
        }

        stage("Quality Gate") {
            steps {
                // Czekamy na wynik analizy (wymaga skonfigurowania Webhooka w Sonarze)
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
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
        }
        success {
            sh "docker image prune -f --filter 'label=stage=intermediate'"
        }
    }
}