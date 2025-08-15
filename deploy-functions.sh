#!/bin/bash

# Movie Matcher Cloud Functions Deployment Script

echo "🚀 Deploying Movie Matcher Cloud Functions..."

# Check if Firebase CLI is installed
if ! command -v firebase &> /dev/null; then
    echo "❌ Firebase CLI is not installed. Please install it first:"
    echo "npm install -g firebase-tools"
    exit 1
fi

# Check if user is logged in
if ! firebase projects:list &> /dev/null; then
    echo "❌ Not logged in to Firebase. Please run:"
    echo "firebase login"
    exit 1
fi

# Navigate to functions directory and install dependencies
echo "📦 Installing function dependencies..."
cd functions
npm install

# Build the functions
echo "🔨 Building functions..."
npm run build

if [ $? -ne 0 ]; then
    echo "❌ Build failed. Please check the errors above."
    exit 1
fi

# Go back to project root
cd ..

# Deploy functions
echo "🚀 Deploying functions to Firebase..."
firebase deploy --only functions

if [ $? -eq 0 ]; then
    echo "✅ Functions deployed successfully!"
    echo "📊 View logs: firebase functions:log"
    echo "🔍 Monitor: https://console.firebase.google.com/project/$(firebase use --current)/functions"
else
    echo "❌ Deployment failed. Please check the errors above."
    exit 1
fi