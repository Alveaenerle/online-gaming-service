void call(String imageName, String contextDir) {
    echo "Processing service: ${imageName} from ${contextDir}..."
    def fullImageName = "${NEXUS_URL}/${imageName}"

    sh """
        docker build \
        -t ${fullImageName}:${env.BUILD_NUMBER} \
        -t ${fullImageName}:latest \
        --target production \
        ${contextDir}
    """

    echo "Pushing ${imageName} to Nexus..."
    sh "docker push ${fullImageName}:${env.BUILD_NUMBER}"
    sh "docker push ${fullImageName}:latest"

    echo "Cleaning up local images..."
    sh "docker rmi ${fullImageName}:${env.BUILD_NUMBER} ${fullImageName}:latest"
}

return this
