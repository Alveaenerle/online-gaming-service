#!/bin/bash

GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

set -o pipefail

IMAGE_NAME="online-gaming-mongo-test"
CONTAINER_NAME="mongo-test-ephemeral"
TEST_PORT=27018
MONGO_USER="testroot"
MONGO_PASS="testpass"

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
BACKEND_DIR="$(dirname "$SCRIPT_DIR")"
MVN_EXEC="$BACKEND_DIR/mvnw"

LOG_FILE=$(mktemp)

VERBOSE=false
if [[ "$1" == "--verbose" || "$1" == "-v" ]]; then
    VERBOSE=true
fi

cleanup() {
    if [ "$VERBOSE" = true ]; then echo -e "\n${GREEN}>>> Cleaning up container...${NC}"; fi
    docker stop $CONTAINER_NAME > /dev/null 2>&1
    docker rm $CONTAINER_NAME > /dev/null 2>&1
    
    if [ -f "$LOG_FILE" ]; then rm -f "$LOG_FILE"; fi
}
trap cleanup EXIT

run_technical_step() {
    local description="$1"
    shift
    
    if [ "$VERBOSE" = true ]; then
        echo -e "${GREEN}>>> $description${NC}"
        "$@"
    else
        echo -n -e ">>> $description... "
        "$@" >> "$LOG_FILE" 2>&1
        local status=$?
        
        if [ $status -eq 0 ]; then
            echo -e "${GREEN}[OK]${NC}"
        else
            echo -e "${RED}[FAILED]${NC}"
            echo -e "${RED}!!! Error logs !!!${NC}"
            cat "$LOG_FILE"
            exit $status
        fi
    fi
}

echo -e "${GREEN}=== Starting Integration Tests ===${NC}"

run_technical_step "Building Test Docker Image" docker build -t $IMAGE_NAME "$SCRIPT_DIR"

docker rm -f $CONTAINER_NAME > /dev/null 2>&1
run_technical_step "Starting ephemeral MongoDB container" docker run -d \
  --name $CONTAINER_NAME \
  -p $TEST_PORT:27017 \
  -e MONGO_INITDB_ROOT_USERNAME=$MONGO_USER \
  -e MONGO_INITDB_ROOT_PASSWORD=$MONGO_PASS \
  $IMAGE_NAME

if [ "$VERBOSE" = true ]; then echo "Waiting for MongoDB..."; else echo -n -e ">>> Waiting for MongoDB... "; fi

DB_READY=false
for i in {1..30}; do
    if docker exec $CONTAINER_NAME mongosh --eval "db.adminCommand('ping')" --quiet >> "$LOG_FILE" 2>&1; then
        DB_READY=true
        break
    fi
    sleep 1
done

if [ "$DB_READY" = true ]; then
    if [ "$VERBOSE" = false ]; then echo -e "${GREEN}[OK]${NC}"; fi
else
    if [ "$VERBOSE" = false ]; then echo -e "${RED}[FAILED]${NC}"; fi
    echo -e "${RED}Timeout waiting for DB! Logs:${NC}"
    cat "$LOG_FILE"
    exit 1
fi

echo -e "${GREEN}>>> Running Maven Tests...${NC}"

# ZMIANA 1: Dodano -Dsurefire.testng.verbose=1 aby TestNG wypisywał nazwy klas
MAVEN_CMD="$MVN_EXEC -f $BACKEND_DIR/pom.xml -pl authorization,social,menu test \
  -Dspring.profiles.active=test \
  -DTEST_DB_PORT=$TEST_PORT \
  -DTEST_DB_USER=$MONGO_USER \
  -DTEST_DB_PASS=$MONGO_PASS \
  -Dtest=com.online_games_service.*.integration.** \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dsurefire.testng.verbose=1"

if [ "$VERBOSE" = true ]; then
    $MAVEN_CMD
else
    # ZMIANA 2: Zaktualizowany grep. 
    # - "Running " (łapie wszystko co się uruchamia, nawet TestSuite)
    # - "\[TestNG\]" (łapie logi z nazwą klasy wewnątrz TestSuite)
    $MAVEN_CMD 2>&1 | tee -a "$LOG_FILE" | grep -E --line-buffered "Running |Tests run:|BUILD SUCCESS|BUILD FAILURE|FAILURE!|\[TestNG\]"
    
    MVN_EXIT_CODE=${PIPESTATUS[0]}
    
    if [ $MVN_EXIT_CODE -ne 0 ]; then
        echo -e "\n${RED}>>> TESTS FAILED! Full logs below:${NC}"
        echo "---------------------------------------------------"
        cat "$LOG_FILE"
        echo "---------------------------------------------------"
        exit $MVN_EXIT_CODE
    fi
fi