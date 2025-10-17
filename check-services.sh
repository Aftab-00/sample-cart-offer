#!/bin/bash

# Shell script by Aftab
# Dev Note:
# THIS IS MY SHELL. So DO NOT TOUCH anything recklessly.

# Service Status Checker
# This checks if the Cart Offer application and mock server are running properly

echo "Checking service status..."
echo

# Function to check if a port is responding or not
check_service() {
    local name="$1"
    local url="$2"
    local port="$3"

    printf "%-25s" "$name:"

    # Check if port is open first or not
    if ! nc -z localhost "$port" 2>/dev/null; then
        echo "DOWN (port $port not listening)"
        return 1
    fi

    # Check if service responds to HTTP requests or not
    if curl -s --connect-timeout 3 --max-time 5 "$url" >/dev/null 2>&1; then
        echo "UP and responding"
        return 0
    else
        echo "UP but not responding to HTTP"
        return 1
    fi
}

# Check both services
check_service "Mock Server" "http://localhost:1080/mockserver/status" "1080"
mock_status=$?

check_service "Spring Boot App" "http://localhost:9001" "9001"
app_status=$?

echo

# Show detailed information on the ports
echo "Port details:"
echo "-------------"

for port in 1080 9001; do
    service_name=""
    case $port in
        1080) service_name="Mock Server" ;;
        9001) service_name="Spring Boot" ;;
    esac

    printf "Port %-4s (%-12s): " "$port" "$service_name"

    if lsof -i ":$port" >/dev/null 2>&1; then
        # Get the process name and PID
        process_info=$(lsof -i ":$port" -t 2>/dev/null | head -1)
        if [ -n "$process_info" ]; then
            process_name=$(ps -p "$process_info" -o comm= 2>/dev/null | head -1)
            echo "Active (PID: $process_info, Process: $process_name)"
        else
            echo "Active"
        fi
    else
        echo "No process listening"
    fi
done

echo

# Check Docker containers
echo "Docker containers:"
echo "------------------"

if command -v docker >/dev/null 2>&1; then
    if docker info >/dev/null 2>&1; then
        container_count=$(docker ps -q | wc -l | tr -d ' ')
        if [ "$container_count" -gt 0 ]; then
            docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | head -10
        else
            echo "No containers running"
        fi
    else
        echo "Docker daemon not running"
    fi
else
    echo "Docker not installed"
fi

echo

# Summary
echo "Summary:"
echo "--------"

total_issues=0

if [ $mock_status -ne 0 ]; then
    echo "- Mock server needs attention"
    total_issues=$((total_issues + 1))
fi

if [ $app_status -ne 0 ]; then
    echo "- Spring Boot app needs attention"
    total_issues=$((total_issues + 1))
fi

if [ $total_issues -eq 0 ]; then
    echo "All services are running normally"
else
    echo "$total_issues service(s) need attention"
    echo
    echo "To start services:"
    echo "  Mock server: cd mockserver && docker-compose up -d"
    echo "  Spring Boot: ./mvnw spring-boot:run"
fi

exit $total_issues
