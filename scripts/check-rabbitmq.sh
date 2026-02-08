#!/bin/bash

# RabbitMQ Health Check Script

echo "Checking RabbitMQ status..."

# Wait for RabbitMQ to be ready
echo "Waiting for RabbitMQ to be ready..."
max_attempts=30
attempt=0

while [ $attempt -lt $max_attempts ]; do
    if curl -s http://localhost:15672 > /dev/null 2>&1; then
        echo "RabbitMQ Management UI is accessible!"
        break
    fi
    attempt=$((attempt + 1))
    echo "Attempt $attempt/$max_attempts..."
    sleep 2
done

if [ $attempt -eq $max_attempts ]; then
    echo "RabbitMQ is not accessible after $max_attempts attempts"
    exit 1
fi

# Check queues using RabbitMQ API
echo ""
echo "Checking RabbitMQ queues..."
curl -s -u guest:guest http://localhost:15672/api/queues | jq '.[] | {name, messages, messages_ready, messages_unacknowledged}'

echo ""
echo "Checking exchanges..."
curl -s -u guest:guest http://localhost:15672/api/exchanges | jq '.[] | select(.name | contains("resource")) | {name, type}'

echo ""
echo "Checking bindings..."
curl -s -u guest:guest http://localhost:15672/api/bindings | jq '.[] | select(.source | contains("resource")) | {source, vhost, destination, routing_key}'

echo ""
echo "RabbitMQ check completed!"
echo "Access Management UI at: http://localhost:15672 (guest/guest)"
