def call(String ip, String user, String sshId) {
    withCredentials([
        usernamePassword(credentialsId: 'mongo-root-creds', passwordVariable: 'MONGO_PASS', usernameVariable: 'MONGO_USER'),
        string(credentialsId: 'redis-password', variable: 'REDIS_PASS'),
        usernamePassword(credentialsId: '86a5c18e-996c-42ea-bf9e-190b2cb978bd', usernameVariable: 'NX_U', passwordVariable: 'NX_P')
    ]) {
        
        def envFileContent = """
TAG=${env.BUILD_NUMBER}
NEXUS_URL=${env.NEXUS_URL}
MONGO_ROOT_USER=${MONGO_USER}
MONGO_ROOT_PASSWORD=${MONGO_PASS}
REDIS_PASSWORD=${REDIS_PASS}
""".trim()
        
        writeFile file: '.env.deploy', text: envFileContent

        sshagent([sshId]) {
            def remoteDir = "/home/${user}/online-gaming"
            def sshOpts = "-o StrictHostKeyChecking=no"

            sh "ssh ${sshOpts} ${user}@${ip} 'mkdir -p ${remoteDir}'"
            sh "scp ${sshOpts} docker-compose.prod.yml ${user}@${ip}:${remoteDir}/docker-compose.yml"
            sh "scp ${sshOpts} .env.deploy ${user}@${ip}:${remoteDir}/.env"

            sh """
                ssh ${sshOpts} ${user}@${ip} '
                    cd ${remoteDir}
                    echo "${NX_P}" | docker login ${env.NEXUS_URL} -u ${NX_U} --password-stdin
                    docker compose pull
                    docker compose up -d --remove-orphans
                    docker logout ${env.NEXUS_URL}
                '
            """
        }
        sh "rm .env.deploy"
    }
}
return this