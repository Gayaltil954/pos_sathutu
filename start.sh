#!/bin/bash

# POS System Quick Start Script for Mac/Linux

echo ""
echo "========================================"
echo "  POS SYSTEM - Quick Start"
echo "========================================"
echo ""

# Check if Java is installed
echo "Checking Java installation..."
if ! command -v java &> /dev/null; then
    echo "ERROR: Java 17+ not found!"
    echo "Please install Java from https://www.oracle.com/java/technologies/"
    exit 1
fi

# Check if Maven is installed
echo "Checking Maven installation..."
if ! command -v mvn &> /dev/null; then
    echo "ERROR: Maven not found!"
    echo "Please install Maven from https://maven.apache.org/"
    exit 1
fi

echo "Java and Maven are installed!"
echo ""

# Start MongoDB
echo ""
echo "Starting MongoDB..."
echo "Please ensure MongoDB is running on localhost:27017"
echo "Or update connection string in application.properties"
echo "For MongoDB Atlas, update the connection URI"
read -p "Press Enter to continue..."

# Build and start backend
echo ""
echo "========================================"
echo "   Building Backend..."
echo "========================================"
echo ""
cd pos-backend
mvn clean install

if [ $? -ne 0 ]; then
    echo "ERROR: Backend build failed!"
    exit 1
fi

echo ""
echo "Launching Backend Server..."
echo "Backend will start on http://localhost:8080"
echo ""
mvn spring-boot:run &
BACKEND_PID=$!

# Wait for backend to start
echo ""
echo "Waiting for backend to start (30 seconds)..."
sleep 30

# Initialize categories
echo ""
echo "Initializing categories..."
curl -X POST http://localhost:8080/api/categories/initialize

echo ""
echo "========================================"
echo "   Building Frontend..."
echo "========================================"
echo ""
cd ../pos-frontend
mvn clean install

if [ $? -ne 0 ]; then
    echo "ERROR: Frontend build failed!"
    kill $BACKEND_PID
    exit 1
fi

echo ""
echo "========================================"
echo "   Launching POS Application..."
echo "========================================"
echo ""
mvn javafx:run

echo ""
echo "========================================"
echo "   POS System Started Successfully!"
echo "========================================"
echo ""
echo "Backend: http://localhost:8080/api"
echo ""

# Cleanup on exit
kill $BACKEND_PID
