def call(status) {
    if (!env.CHANGE_ID) {
        return
    }
    
    def icon = (status == 'SUCCESS') ? '✅' : '❌'
    def verb = (status == 'SUCCESS') ? 'succeeded' : 'failed'
    def action = (status == 'SUCCESS') ? '.' : '. Check logs in Jenkins.'
    
    def message = "${icon} Jenkins build **${verb}** for this PR ([build #${env.BUILD_NUMBER}](${env.BUILD_URL}))$action"
    
    def jsonPayload = """{ "body": "${message}" }""".trim()
    
    sh """
    curl -sS -X POST \\
      -H "Authorization: token ${env.GITHUB_TOKEN}" \\
      -H "Accept: application/vnd.github+json" \\
      https://api.github.com/repos/${env.REPO}/issues/${env.CHANGE_ID}/comments \\
      -d @- <<EOF
${jsonPayload}
EOF
    """
}

return this