def call(String serverIp, String serverUser, String sshCredentialId) {
    withCredentials([
        usernamePassword(credentialsId: 'mongo-root-creds', passwordVariable: 'MONGO_PASS', usernameVariable: 'MONGO_USER'),
        string(credentialsId: 'redis-password', variable: 'REDIS_PASS'),
        usernamePassword(credentialsId: '86a5c18e-996c-42ea-bf9e-190b2cb978bd', usernameVariable: 'NEXUS_USER', passwordVariable: 'NEXUS_PASS')
    ]) {
        
        def envFileContent = """
TAG=${env.BUILD_NUMBER}
NEXUS_URL=${env.NEXUS_URL}
MONGO_ROOT_USER=${MONGO_USER}
MONGO_ROOT_PASSWORD=${MONGO_PASS}
REDIS_PASSWORD=${REDIS_PASS}
""".trim()
        
        try {
            writeFile file: '.env.deploy', text: envFileContent
            sh "chmod 600 .env.deploy"

            sshagent([sshCredentialId]) {
                def remoteDir = "/home/${serverUser}/online-gaming"
                def sshOpts = "-o StrictHostKeyChecking=no"

                sh "ssh ${sshOpts} ${serverUser}@${serverIp} 'mkdir -p ${remoteDir}'"
                sh "scp ${sshOpts} docker-compose.prod.yml ${serverUser}@${serverIp}:${remoteDir}/docker-compose.yml"
                
                sh "scp ${sshOpts} .env.deploy ${serverUser}@${serverIp}:${remoteDir}/.env"
                sh "ssh ${sshOpts} ${serverUser}@${serverIp} 'chmod 600 ${remoteDir}/.env'"

                sh """
                    ssh ${sshOpts} ${serverUser}@${serverIp} '
                        cd ${remoteDir}
                        echo "${NEXUS_PASS}" | docker login ${env.NEXUS_URL} -u ${NEXUS_USER} --password-stdin
                        docker compose pull
                        docker compose up -d --remove-orphans
                        docker logout ${env.NEXUS_URL}
                    '
                """
            }
        } finally {
            sh "rm -f .env.deploy"
        }
    }
}
return this