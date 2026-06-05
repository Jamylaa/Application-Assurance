#!/bin/bash

# Keycloak Setup Script for Vermeg Assurance Project
# This script creates the realm and client needed for authentication

KEYCLOAK_URL="http://localhost:9090"
ADMIN_USER="admin"
ADMIN_PASSWORD="admin"
REALM_NAME="vermeg-realm"
CLIENT_ID="frontend-client"
CLIENT_SECRET="frontend-secret"

# Wait for Keycloak to be ready
echo "Waiting for Keycloak to start..."
until curl -s -f "${KEYCLOAK_URL}/health/ready" > /dev/null; do
  echo "Keycloak not ready yet, waiting..."
  sleep 5
done
echo "Keycloak is ready!"

# Get admin token
echo "Getting admin token..."
ADMIN_TOKEN=$(curl -s -X POST "${KEYCLOAK_URL}/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=${ADMIN_USER}" \
  -d "password=${ADMIN_PASSWORD}" \
  -d "grant_type=password" \
  -d "client_id=admin-cli" | jq -r '.access_token')

if [ -z "$ADMIN_TOKEN" ] || [ "$ADMIN_TOKEN" == "null" ]; then
  echo "Failed to get admin token"
  exit 1
fi

echo "Admin token obtained"

# Create realm
echo "Creating realm: ${REALM_NAME}"
curl -s -X POST "${KEYCLOAK_URL}/admin/realms" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "realm": "'${REALM_NAME}'",
    "enabled": true,
    "sslRequired": "external",
    "registrationAllowed": false,
    "loginWithEmailAllowed": true,
    "duplicateEmailsAllowed": false,
    "resetPasswordAllowed": true,
    "editUsernameAllowed": true,
    "bruteForceProtected": true
  }' > /dev/null

# Create client
echo "Creating client: ${CLIENT_ID}"
curl -s -X POST "${KEYCLOAK_URL}/admin/realms/${REALM_NAME}/clients" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "'${CLIENT_ID}'",
    "secret": "'${CLIENT_SECRET}'",
    "enabled": true,
    "clientAuthenticatorType": "client-secret",
    "redirectUris": ["http://localhost:4200/*", "http://localhost:4200/chatbot"],
    "webOrigins": ["http://localhost:4200"],
    "protocol": "openid-connect",
    "publicClient": false,
    "standardFlowEnabled": true,
    "implicitFlowEnabled": false,
    "directAccessGrantsEnabled": false,
    "serviceAccountsEnabled": false,
    "validRedirectUris": ["http://localhost:4200/*", "http://localhost:4200/chatbot"],
    "attributes": {
      "access.token.lifespan": "3600"
    }
  }' > /dev/null

# Create a test user
echo "Creating test user..."
curl -s -X POST "${KEYCLOAK_URL}/admin/realms/${REALM_NAME}/users" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "firstName": "Test",
    "lastName": "User",
    "email": "test@vermeg.tn",
    "enabled": true,
    "emailVerified": true,
    "credentials": [{
      "type": "password",
      "value": "test123",
      "temporary": false
    }]
  }' > /dev/null

echo ""
echo "Keycloak setup completed successfully!"
echo "Realm: ${REALM_NAME}"
echo "Client ID: ${CLIENT_ID}"
echo "Client Secret: ${CLIENT_SECRET}"
echo "Test User: testuser / test123"
echo ""
echo "Keycloak Console: ${KEYCLOAK_URL}/admin"
echo "Account Console: ${KEYCLOAK_URL}/realms/${REALM_NAME}/account"
