def call(List<String> modules) {
    if (!env.CHANGE_ID) {
        echo "Not a PR, skipping coverage report."
        return
    }

    def report = "### Code Coverage Report 📊\n\n"
    report += "| Module | Line Coverage | Instruction Coverage |\n"
    report += "| :--- | :--- | :--- |\n"

    boolean anyReportFound = false

    modules.each { module ->
        def csvFile = "backend/${module}/target/site/jacoco/jacoco.csv"
        if (fileExists(csvFile)) {
            anyReportFound = true
            def lines = readFile(csvFile).split('\n')
            
            long totalLinesMissed = 0
            long totalLinesCovered = 0
            long totalInstructionsMissed = 0
            long totalInstructionsCovered = 0

            for (int i = 1; i < lines.length; i++) {
                def line = lines[i]
                if (!line.trim()) continue

                def columns = line.split(',')
                if (columns.size() > 8) {
                    totalInstructionsMissed += columns[3].toInteger()
                    totalInstructionsCovered += columns[4].toInteger()
                    totalLinesMissed += columns[7].toInteger()
                    totalLinesCovered += columns[8].toInteger()
                }
            }

            def lineCoverage = 0
            def totalLines = totalLinesMissed + totalLinesCovered
            if (totalLines > 0) {
                lineCoverage = ((totalLinesCovered * 100.0) / totalLines).round(2)
            }

            def instructionCoverage = 0
            def totalInstructions = totalInstructionsMissed + totalInstructionsCovered
            if (totalInstructions > 0) {
                instructionCoverage = ((totalInstructionsCovered * 100.0) / totalInstructions).round(2)
            }

            report += "| **${module}** | ${lineCoverage}% | ${instructionCoverage}% |\n"
        } else {
            report += "| **${module}** | N/A | N/A |\n"
        }
    }

    if (!anyReportFound) {
        echo "No coverage reports found."
        return
    }

    def escapedBody = report.replace('\\', '\\\\').replace('"', '\\"').replace('\n', '\\n')
    def payload = "{\"body\": \"${escapedBody}\"}"

    echo "Posting coverage report to GitHub PR #${env.CHANGE_ID}..."

    sh """
    curl -sS -X POST \\
      -H "Authorization: token ${env.GITHUB_TOKEN}" \\
      -H "Accept: application/vnd.github+json" \\
      https://api.github.com/repos/${env.REPO}/issues/${env.CHANGE_ID}/comments \\
      -d '${payload}'
    """
}

return this