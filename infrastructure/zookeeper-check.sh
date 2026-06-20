#!/bin/bash

echo "Testing connection to Zookeeper on localhost:2181..."

# Check if port 2181 is open
if ! nc -z localhost 2181 2>/dev/null; then
  echo "Error: Zookeeper is not running or port 2181 is closed!"
  exit 1
fi

echo "Zookeeper port 2181 is OPEN."
echo "Sending 'ruok' command..."
RUOK=$(echo ruok | nc localhost 2181 2>/dev/null)

if [ "$RUOK" = "imok" ]; then
  echo "Response: imok (Zookeeper is healthy!)"
else
  echo "Warning: Zookeeper returned unexpected response: '$RUOK'"
fi

echo "Sending 'stat' command to fetch node statistics..."
echo stat | nc localhost 2181 | grep -E "Mode|Connections|Nodes" 2>/dev/null

exit 0
