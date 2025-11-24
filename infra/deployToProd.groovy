def call(String serverIp, String serverUser, String sshCredentialId) {
    echo "Deploying build #${env.BUILD_NUMBER} to ${serverIp}..."
    def template = readFile('docker-compose.prod.yml')
    def composeContent = template.replace('${TAG}', env.BUILD_NUMBER)

    writeFile file: 'docker-compose-deploy.yml', text: composeContent

    sshagent([sshCredentialId]) {
        def remoteDir = "/home/${serverUser}/online-gaming"
        def sshOpts = "-o StrictHostKeyChecking=no"

        sh "ssh ${sshOpts} ${serverUser}@${serverIp} 'mkdir -p ${remoteDir}'"
        sh "scp ${sshOpts} docker-compose-deploy.yml ${serverUser}@${serverIp}:${remoteDir}/docker-compose.yml"

        sh """
            ssh ${sshOpts} ${serverUser}@${serverIp} '
                echo "Logging to Nexus..."
                echo ${env.NEXUS_CREDS_PSW} | docker login ${env.NEXUS_URL} -u ${env.NEXUS_CREDS_USR} --password-stdin

                cd ${remoteDir}

                echo "⬇Pulling images..."
                docker compose pull

                echo "Restarting containers..."
                docker compose up -d --remove-orphans

                echo "Cleanup..."
                docker logout ${env.NEXUS_URL}
            '
        """
    }
    echo "Deployment finished!"
}

return this
