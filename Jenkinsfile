pipeline {
    agent any

    environment {
        GITHUB_TOKEN = credentials('90d79e6c-ebeb-4414-9f4c-f30e091789ee')
        REPO = 'Alveaenerle/online-gaming-service'

        NEXUS_URL = 'docker.yapyap.pl'
        NEXUS_CREDS = credentials('86a5c18e-996c-42ea-bf9e-190b2cb978bd')
    }

    stages {
        stage('Build & Push to Nexus') {
            when {
                branch 'main'
            }
            steps {
                script {
                    def deployService = load('infra/buildAndDeployImage.groovy')

                    echo 'Logging into Nexus...'
                    sh "echo ${NEXUS_CREDS_PSW} | docker login ${NEXUS_URL} -u ${NEXUS_CREDS_USR} --password-stdin"

                    deployService('online-gaming-backend', './backend')
                    deployService('online-gaming-frontend', './frontend')

                    sh "docker logout ${NEXUS_URL}"
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
