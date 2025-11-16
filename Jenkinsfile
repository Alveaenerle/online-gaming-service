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
                if (env.CHANGE_ID) {   // only for PR builds
                    sh """
                    curl -sS -X POST \
                      -H "Authorization: token ${GITHUB_TOKEN}" \
                      -H "Accept: application/vnd.github+json" \
                      https://api.github.com/repos/${REPO}/issues/${CHANGE_ID}/comments \
                      -d @- <<EOF
    { "body": "✅ Jenkins build **succeeded** for this PR ([build #${env.BUILD_NUMBER}](${env.BUILD_URL}))." }
    
                    """
                }
            }
        }
        failure {
            script {
                if (env.CHANGE_ID) {
                    sh """
                    curl -sS -X POST \
                      -H "Authorization: token ${GITHUB_TOKEN}" \
                      -H "Accept: application/vnd.github+json" \
                      https://api.github.com/repos/${REPO}/issues/${CHANGE_ID}/comments \
                      -d @- <<EOF
    { "body": "❌ Jenkins build **failed** for this PR ([build #${env.BUILD_NUMBER}](${env.BUILD_URL})). Check logs in Jenkins." }
    
                    """
                }
            }
        }
    }
}
