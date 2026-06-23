export const environment = {
  production: false,
  apiUser: 'http://localhost:9094/api',
  apiProduit: 'http://localhost:9093/api',
  apiChatbot: 'http://localhost:9001/api/chatbot',
  keycloak: {
    url: 'http://localhost:9090',
    realm: 'vermeg-realm',
    clientId: 'frontend-client'
  }
};
