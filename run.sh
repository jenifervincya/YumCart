#!/bin/bash
set -e
mkdir -p out
find src -name "*.java" > sources.txt
javac -d out @sources.txt
echo "Build OK. Run with:"
echo "  java -cp out com.yumcart.Main"
