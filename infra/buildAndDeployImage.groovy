def call(imageName, contextDir, moduleName = null) {
    echo "Processing service: ${imageName} from ${contextDir}..."
    def fullImageName = "${env.NEXUS_URL}/${imageName}"
    def gitCommit = sh(returnStdout: true, script: 'git rev-parse --short HEAD').trim()

    def buildArgs = ""
    if (moduleName) {
        buildArgs = "--build-arg MODULE_NAME=${moduleName}"
    }

    if (imageName == 'online-gaming-frontend') {
        buildArgs += " --build-arg VITE_GOOGLE_CLIENT_ID=${env.GOOGLE_CLIENT_ID}"
    }

    sh """
        docker build \
        -t ${fullImageName}:${gitCommit} \
        -t ${fullImageName}:latest \
        --target production \
        ${buildArgs} \
        ${contextDir}
    """

    echo "Pushing ${imageName} to Nexus..."
    sh "docker push ${fullImageName}:${gitCommit}"
    sh "docker push ${fullImageName}:latest"

    echo "Cleaning up local images..."
    sh "docker rmi ${fullImageName}:${gitCommit} ${fullImageName}:latest"
}

return this