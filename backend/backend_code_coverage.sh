#!/bin/bash

MODULE_NAME=$1

if [ -z "$MODULE_NAME" ]; then
  echo "Error: No module name provided."
  echo "Usage: ./backend_code_coverage.sh <module_name>"
  exit 1
fi

echo "========================================================"
echo "Running mvn tests with JaCoCo for module: $MODULE_NAME"
echo "========================================================"

./mvnw -pl $MODULE_NAME clean jacoco:prepare-agent test jacoco:report

echo ""
echo "CODE COVERAGE FOR: $MODULE_NAME"
echo "===================="

REPORT_FILE="$MODULE_NAME/target/site/jacoco/jacoco.csv"

if [ -f "$REPORT_FILE" ]; then
    tail -n +2 "$REPORT_FILE" | while IFS=',' read -r group package class instruction_missed instruction_covered branch_missed branch_covered line_missed line_covered complexity_missed complexity_covered method_missed method_covered; do
        if [ "$class" != "" ] && [ "$class" != "\"\"" ]; then
            total_instructions=$((instruction_missed + instruction_covered))
            total_lines=$((line_missed + line_covered))

            if [ $total_instructions -gt 0 ]; then
                instruction_coverage=$((instruction_covered * 100 / total_instructions))
            fi

            if [ $total_lines -gt 0 ]; then
                line_coverage=$((line_covered * 100 / total_lines))
            fi

            echo "Class: $class"
            echo "Line coverage: $line_coverage% ($line_covered/$total_lines)"
            echo "Instruction coverage: $instruction_coverage% ($instruction_covered/$total_instructions)"
            echo ""
        fi
    done
else
    echo "Error: JaCoCo report not found at $REPORT_FILE."
    echo "Tests failed or report generation skipped."
    exit 1
fi