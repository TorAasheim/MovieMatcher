# Movie Matcher Cloud Functions Deployment Script

Write-Host "🚀 Deploying Movie Matcher Cloud Functions..." -ForegroundColor Green

# Check if Firebase CLI is installed
try {
    firebase --version | Out-Null
} catch {
    Write-Host "❌ Firebase CLI is not installed. Please install it first:" -ForegroundColor Red
    Write-Host "npm install -g firebase-tools" -ForegroundColor Yellow
    exit 1
}

# Check if user is logged in
try {
    firebase projects:list | Out-Null
} catch {
    Write-Host "❌ Not logged in to Firebase. Please run:" -ForegroundColor Red
    Write-Host "firebase login" -ForegroundColor Yellow
    exit 1
}

# Navigate to functions directory and install dependencies
Write-Host "📦 Installing function dependencies..." -ForegroundColor Blue
Set-Location functions
npm install

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Failed to install dependencies. Please check the errors above." -ForegroundColor Red
    exit 1
}

# Build the functions
Write-Host "🔨 Building functions..." -ForegroundColor Blue
npm run build

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Build failed. Please check the errors above." -ForegroundColor Red
    exit 1
}

# Go back to project root
Set-Location ..

# Deploy functions
Write-Host "🚀 Deploying functions to Firebase..." -ForegroundColor Blue
firebase deploy --only functions

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Functions deployed successfully!" -ForegroundColor Green
    Write-Host "📊 View logs: firebase functions:log" -ForegroundColor Cyan
    $currentProject = firebase use --current
    Write-Host "🔍 Monitor: https://console.firebase.google.com/project/$currentProject/functions" -ForegroundColor Cyan
} else {
    Write-Host "❌ Deployment failed. Please check the errors above." -ForegroundColor Red
    exit 1
}