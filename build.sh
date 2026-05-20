#!/bin/bash

# Build script for Exam Supervisor Assignment System

echo "========================================="
echo "Building Exam Supervisor Assignment System"
echo "========================================="

# Check Maven installation
if ! command -v mvn &> /dev/null
then
    echo "ERROR: Maven is not installed or not in PATH"
    exit 1
fi

# Check Java installation
if ! command -v java &> /dev/null
then
    echo "ERROR: Java is not installed or not in PATH"
    exit 1
fi

echo ""
echo "Java version:"
java -version

echo ""
echo "Maven version:"
mvn --version

# Clean and build
echo ""
echo "Step 1: Cleaning..."
mvn clean

echo ""
echo "Step 2: Compiling..."
mvn compile

echo ""
echo "Step 3: Running tests..."
mvn test

echo ""
echo "Step 4: Packaging..."
mvn package

echo ""
echo "========================================="
echo "Build completed successfully!"
echo "========================================="
echo ""
echo "To start server: java -cp target/classes:target/dependency/* MainServer 8888"
echo "To start client: java -cp target/classes:target/dependency/* MainClient"
