// Valeurs par défaut alignées sur la stack docker-compose de ce projet (Gateway exposé sur
// le port hôte 9091). Pour un déploiement réel hors docker-compose, remplacer ces 3 URLs par
// celles du Gateway/Keycloak effectivement joignables depuis le navigateur des utilisateurs.
export const environment = {
  production: true,
  apiProduit: 'http://localhost:9091/api',
  apiChatbot: 'http://localhost:9091/api/chatbot',
  keycloak: {
    url: 'http://localhost:9090',
    realm: 'vermeg-realm',
    clientId: 'frontend-client'
  }
};
