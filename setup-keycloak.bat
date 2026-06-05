@echo off
REM Keycloak Setup Script for Vermeg Assurance Project (Windows)

set KEYCLOAK_URL=http://localhost:9090
set ADMIN_USER=admin
set ADMIN_PASSWORD=admin
set REALM_NAME=vermeg-realm
set CLIENT_ID=frontend-client
set CLIENT_SECRET=frontend-secret

echo Waiting for Keycloak to start...
:wait_loop
curl -s -f %KEYCLOAK_URL%/health/ready >nul 2>&1
if errorlevel 1 (
    echo Keycloak not ready yet, waiting...
    timeout /t 5 /nobreak >nul
    goto wait_loop
)
echo Keycloak is ready!

echo Getting admin token...
curl -s -X POST "%KEYCLOAK_URL%/realms/master/protocol/openid-connect/token" ^
  -H "Content-Type: application/x-www-form-urlencoded" ^
  -d "username=%ADMIN_USER%" ^
  -d "password=%ADMIN_PASSWORD%" ^
  -d "grant_type=password" ^
  -d "client_id=admin-cli" > token_response.json

for /f "tokens=*" %%i in ('type token_response.json ^| jq -r ".access_token"') do set ADMIN_TOKEN=%%i

if "%ADMIN_TOKEN%"=="null" (
    echo Failed to get admin token
    del token_response.json
    exit /b 1
)

echo Admin token obtained

echo Creating realm: %REALM_NAME%
curl -s -X POST "%KEYCLOAK_URL%/admin/realms" ^
  -H "Authorization: Bearer %ADMIN_TOKEN%" ^
  -H "Content-Type: application/json" ^
  -d "{\"realm\":\"%REALM_NAME%\",\"enabled\":true,\"sslRequired\":\"external\",\"registrationAllowed\":false,\"loginWithEmailAllowed\":true,\"duplicateEmailsAllowed\":false,\"resetPasswordAllowed\":true,\"editUsernameAllowed\":true,\"bruteForceProtected\":true}" >nul

echo Creating client: %CLIENT_ID%
curl -s -X POST "%KEYCLOAK_URL%/admin/realms/%REALM_NAME%/clients" ^
  -H "Authorization: Bearer %ADMIN_TOKEN%" ^
  -H "Content-Type: application/json" ^
  -d "{\"clientId\":\"%CLIENT_ID%\",\"secret\":\"%CLIENT_SECRET%\",\"enabled\":true,\"clientAuthenticatorType\":\"client-secret\",\"redirectUris\":[\"http://localhost:4200/*\",\"http://localhost:4200/chatbot\"],\"webOrigins\":[\"http://localhost:4200\"],\"protocol\":\"openid-connect\",\"publicClient\":false,\"standardFlowEnabled\":true,\"implicitFlowEnabled\":false,\"directAccessGrantsEnabled\":false,\"serviceAccountsEnabled\":false}" >nul

echo Creating test user...
curl -s -X POST "%KEYCLOAK_URL%/admin/realms/%REALM_NAME%/users" ^
  -H "Authorization: Bearer %ADMIN_TOKEN%" ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"testuser\",\"firstName\":\"Test\",\"lastName\":\"User\",\"email\":\"test@vermeg.tn\",\"enabled\":true,\"emailVerified\":true,\"credentials\":[{\"type\":\"password\",\"value\":\"test123\",\"temporary\":false}]}" >nul

del token_response.json

echo.
echo Keycloak setup completed successfully!
echo Realm: %REALM_NAME%
echo Client ID: %CLIENT_ID%
echo Client Secret: %CLIENT_SECRET%
echo Test User: testuser / test123
echo.
echo Keycloak Console: %KEYCLOAK_URL%/admin
echo Account Console: %KEYCLOAK_URL%/realms/%REALM_NAME%/account
