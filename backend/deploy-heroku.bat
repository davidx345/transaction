@echo off
REM Heroku Deployment Script for Windows
REM This script automates the Heroku deployment process

echo.
echo Starting Heroku Deployment Setup...
echo.

REM Check if Heroku CLI is installed
where heroku >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Heroku CLI is not installed!
    echo Please install it from: https://devcenter.heroku.com/articles/heroku-cli
    exit /b 1
)

echo ✓ Heroku CLI found
echo.

REM Check if logged in to Heroku
heroku auth:whoami >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo Not logged in to Heroku. Opening login...
    heroku login
)

echo ✓ Logged in to Heroku
echo.

REM Prompt for app name
set /p APP_NAME="Enter your Heroku app name (e.g., fintech-recon-prod): "

if "%APP_NAME%"=="" (
    echo ERROR: App name cannot be empty
    exit /b 1
)

echo.
echo Creating Heroku app: %APP_NAME%
echo.

REM Create Heroku app
heroku create %APP_NAME% 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo App might already exist. Adding remote...
    heroku git:remote -a %APP_NAME%
)

echo.
echo Adding PostgreSQL database...
echo.

REM Add PostgreSQL
heroku addons:create heroku-postgresql:essential-0 -a %APP_NAME% 2>nul

echo.
echo Configuring environment variables...
echo.

REM Set environment variables
heroku config:set SPRING_PROFILES_ACTIVE=prod -a %APP_NAME%
heroku config:set JAVA_OPTS="-Xmx512m -Xms256m" -a %APP_NAME%

echo.
echo RabbitMQ Setup
set /p ADD_RABBITMQ="Do you want to add CloudAMQP (RabbitMQ)? (y/N): "

if /i "%ADD_RABBITMQ%"=="y" (
    heroku addons:create cloudamqp:lemur -a %APP_NAME% 2>nul
    if %ERRORLEVEL% NEQ 0 (
        echo Failed to add CloudAMQP. Setting dummy values...
        heroku config:set RABBITMQ_HOST=localhost -a %APP_NAME%
        heroku config:set RABBITMQ_PORT=5672 -a %APP_NAME%
    )
) else (
    echo Skipping RabbitMQ. Setting dummy values...
    heroku config:set RABBITMQ_HOST=localhost -a %APP_NAME%
    heroku config:set RABBITMQ_PORT=5672 -a %APP_NAME%
)

echo.
echo Current Configuration:
heroku config -a %APP_NAME%

echo.
echo Building application...
echo.

REM Check if Maven is installed
where mvn >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo Building with Maven...
    call mvn clean package -DskipTests
    echo ✓ Build successful
) else (
    echo Maven not found locally. Heroku will build during deployment.
)

echo.
echo Deploying to Heroku...
echo.

REM Initialize git if needed
if not exist .git (
    git init
    git add .
    git commit -m "Initial commit for Heroku deployment"
)

REM Add Heroku remote if not exists
git remote get-url heroku >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    heroku git:remote -a %APP_NAME%
)

REM Deploy
echo Pushing to Heroku...
git push heroku main
if %ERRORLEVEL% NEQ 0 (
    git push heroku master
)

echo.
echo ✓ Deployment complete!
echo.

echo Checking app status...
timeout /t 5 /nobreak >nul
heroku ps -a %APP_NAME%

echo.
echo Recent logs:
heroku logs -n 50 -a %APP_NAME%

echo.
echo ========================================
echo Deployment Summary
echo ========================================
echo App Name: %APP_NAME%
echo App URL: https://%APP_NAME%.herokuapp.com
echo.
echo Next steps:
echo 1. Test your API: curl https://%APP_NAME%.herokuapp.com/api/disputes
echo 2. View logs: heroku logs --tail -a %APP_NAME%
echo 3. Update frontend env: VITE_API_BASE_URL=https://%APP_NAME%.herokuapp.com
echo.
echo ✓ All done!
echo.
pause
