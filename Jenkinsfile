pipeline {
    agent any

    environment {
        GITHUB_TOKEN = credentials('90d79e6c-ebeb-4414-9f4c-f30e091789ee')
        REPO = 'Alveaenerle/online-gaming-service'           
    }

    stages {
        stage('Build') {
            steps {
                echo "Building..."
                // your build/test steps
            }
        }
    }

    post {
        success {
            script {
                if (env.CHANGE_ID) {          // only on PR builds
                    sh """
                    curl -s -X POST \
                      -H "Authorization: token ${GITHUB_TOKEN}" \
                      -H "Accept: application/vnd.github+json" \
                      https://api.github.com/repos/${REPO}/issues/${CHANGE_ID}/comments \
                      -d '{ "body": "✅ Jenkins build **succeeded** for this PR ([build #${BUILD_NUMBER}](${BUILD_URL})
)." }'
                    """
                }
            }
        }
        failure {
            script {
                if (env.CHANGE_ID) {
                    sh """
                    curl -s -X POST \
                      -H "Authorization: token ${GITHUB_TOKEN}" \
                      -H "Accept: application/vnd.github+json" \
                      https://api.github.com/repos/${REPO}/issues/${CHANGE_ID}/comments \
                      -d '{ "body": "❌ Jenkins build **failed** for this PR (build #${BUILD_NUMBER}). Check logs in Jenkins." }'
                    """
                }
            }
        }
    }
}
