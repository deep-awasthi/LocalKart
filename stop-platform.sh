#!/bin/bash

# Color codes
GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

PID_FILE=".services.pids"

echo -e "${CYAN}==================================================${NC}"
echo -e "${CYAN}        LocalKart Platform Shutdown Utility       ${NC}"
echo -e "${CYAN}==================================================${NC}"

# 1. Kill background Java microservices
if [ -f "$PID_FILE" ]; then
    echo -e "\n${YELLOW}[Step 1/2] Terminating background Java microservices...${NC}"
    while read -r pid; do
        if kill -0 "$pid" 2>/dev/null; then
            echo -e "Stopping process ${CYAN}$pid${NC}..."
            kill "$pid" 2>/dev/null
            
            # Wait up to 5 seconds for process to exit gracefully
            for i in {1..5}; do
                if ! kill -0 "$pid" 2>/dev/null; then
                    break
                fi
                sleep 1
            done
            
            # Force kill if still running
            if kill -0 "$pid" 2>/dev/null; then
                echo -e "${RED}Process $pid did not exit. Force killing...${NC}"
                kill -9 "$pid" 2>/dev/null
            fi
        else
            echo -e "Process ${YELLOW}$pid${NC} was already stopped."
        fi
    done < "$PID_FILE"
    
    rm -f "$PID_FILE"
    echo -e "${GREEN}All background Java microservices stopped!${NC}"
else
    echo -e "\n${YELLOW}[Step 1/2] No active Java process file ($PID_FILE) found.${NC}"
fi

# 2. Stop Docker infrastructure
echo -e "\n${YELLOW}[Step 2/2] Shutting down Docker infrastructure (databases, caches, brokers)...${NC}"
docker compose -f infrastructure/docker-compose.yml down
if [ $? -ne 0 ]; then
    echo -e "${RED}Warning: Failed to cleanly shutdown Docker infrastructure containers!${NC}"
else
    echo -e "${GREEN}Docker infrastructure successfully stopped!${NC}"
fi

echo -e "\n${GREEN}==================================================${NC}"
echo -e "${GREEN}      LocalKart Platform Cleanly Stopped          ${NC}"
echo -e "${GREEN}==================================================${NC}"
