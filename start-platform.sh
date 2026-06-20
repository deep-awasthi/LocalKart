#!/bin/bash

# Color codes
GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

PID_FILE=".services.pids"
LOGS_DIR="logs"

echo -e "${CYAN}==================================================${NC}"
echo -e "${CYAN}        LocalKart Platform Startup Utility        ${NC}"
echo -e "${CYAN}==================================================${NC}"

# 1. Check if another instance is already running
if [ -f "$PID_FILE" ]; then
    echo -e "${RED}Warning: A previous execution state file ($PID_FILE) was found.${NC}"
    echo -e "${YELLOW}Please run ./stop-platform.sh first to clear active services.${NC}"
    exit 1
fi

# 2. Package the Java services
echo -e "\n${YELLOW}[Step 1/4] Compiling and packaging microservices...${NC}"
mvn package -DskipTests
if [ $? -ne 0 ]; then
    echo -e "${RED}Error: Maven compilation and packaging failed!${NC}"
    exit 1
fi
echo -e "${GREEN}Build succeeded!${NC}"

# 3. Boot infrastructure
echo -e "\n${YELLOW}[Step 2/4] Starting Docker infrastructure (databases, caches, brokers)...${NC}"
docker compose -f infrastructure/docker-compose.yml up -d
if [ $? -ne 0 ]; then
    echo -e "${RED}Error: Failed to launch Docker infrastructure!${NC}"
    exit 1
fi

# 4. Wait for infrastructure ports to be open
echo -e "\n${YELLOW}[Step 3/4] Verifying infrastructure port readiness...${NC}"
ports=(
    "5432:PostgreSQL"
    "27017:MongoDB"
    "6379:Redis"
    "2181:Zookeeper"
    "9092:Kafka"
)

wait_for_port() {
    local port=$1
    local name=$2
    local max_attempts=30
    local attempt=1
    
    echo -n "Waiting for $name (port $port)... "
    while [ $attempt -le $max_attempts ]; do
        nc -z localhost $port 2>/dev/null
        if [ $? -eq 0 ]; then
            echo -e "${GREEN}Ready!${NC}"
            return 0
        fi
        sleep 1
        attempt=$((attempt + 1))
    done
    echo -e "${RED}Timeout waiting for port $port!${NC}"
    return 1
}

for item in "${ports[@]}"; do
    port="${item%%:*}"
    name="${item##*:}"
    wait_for_port $port "$name"
    if [ $? -ne 0 ]; then
        echo -e "${RED}Error: Core infrastructure is not healthy. Aborting startup.${NC}"
        exit 1
    fi
done

# 5. Create logs directory
mkdir -p "$LOGS_DIR"

# 6. Health check function — waits for actuator /health to return UP
wait_for_health() {
    local service_name=$1
    local port=$2
    local max_attempts=60
    local attempt=1
    
    echo -n "  Health check for $service_name (port $port)... "
    while [ $attempt -le $max_attempts ]; do
        health_status=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:$port/actuator/health" 2>/dev/null)
        if [ "$health_status" = "200" ]; then
            echo -e "${GREEN}UP ✓${NC}"
            return 0
        fi
        sleep 2
        attempt=$((attempt + 1))
    done
    echo -e "${RED}TIMEOUT (service may still be starting — check logs/$service_name.log)${NC}"
    return 1
}

# 7. Start each service in the background
echo -e "\n${YELLOW}[Step 4/4] Launching microservices in the background...${NC}"
services=(
    "auth-service:8081"
    "user-service:8082"
    "product-service:8083"
    "inventory-service:8084"
    "order-service:8085"
    "payment-service:8086"
    "notification-service:8087"
    "delivery-service:8088"
    "api-gateway:8080"
)

for item in "${services[@]}"; do
    service="${item%%:*}"
    port="${item##*:}"
    jar_file="$service/target/$service-1.0.0-SNAPSHOT.jar"
    
    if [ ! -f "$jar_file" ]; then
        echo -e "${RED}Error: JAR file not found at $jar_file!${NC}"
        # Stop everything else already started
        if [ -f "$PID_FILE" ]; then
            while read pid; do
                kill -9 $pid 2>/dev/null
            done < "$PID_FILE"
            rm -f "$PID_FILE"
        fi
        exit 1
    fi
    
    echo -e "Starting ${CYAN}$service${NC} on port ${YELLOW}$port${NC}..."
    nohup java -jar "$jar_file" > "$LOGS_DIR/$service.log" 2>&1 &
    pid=$!
    
    # Check if process died immediately
    sleep 1
    if ! kill -0 $pid 2>/dev/null; then
        echo -e "${RED}Failed to start $service! Check logs at $LOGS_DIR/$service.log${NC}"
        exit 1
    fi
    
    echo $pid >> "$PID_FILE"
    echo -e "-> ${CYAN}$service${NC} is running under PID: ${GREEN}$pid${NC}"
    
    # Wait for the service to become healthy before starting the next one
    wait_for_health "$service" "$port"
done

echo -e "\n${GREEN}==================================================${NC}"
echo -e "${GREEN}      All LocalKart Services Launched Successfully ${NC}"
echo -e "${GREEN}==================================================${NC}"
echo -e "\n${CYAN}Useful details:${NC}"
echo -e "* ${YELLOW}Control Dashboard:${NC}   http://localhost:8080/"
echo -e "* ${YELLOW}Log Outputs directory:${NC} ./logs"
echo -e "* ${YELLOW}Tail Logs Command:${NC}     tail -f logs/*.log"
echo -e "* ${YELLOW}Stop Platform Command:${NC}  ./stop-platform.sh"
echo -e "\nHave fun exploring the LocalKart Commerce Platform!"
