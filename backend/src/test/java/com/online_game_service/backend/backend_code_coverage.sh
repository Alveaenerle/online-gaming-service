#!/bin/bash

echo "Running mvn tests with JaCoCo code coverage..."

./mvnw -B -q clean test jacoco:report

echo "CODE COVERAGE:"
echo "===================="

if [ -f "target/site/jacoco/jacoco.csv" ]; then
    tail -n +2 target/site/jacoco/jacoco.csv | while IFS=',' read -r group package class instruction_missed instruction_covered branch_missed branch_covered line_missed line_covered complexity_missed complexity_covered method_missed method_covered; do
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
    echo "Error: JaCoCo report not found. Tests failed or report generation skipped."
    exit 1
fi