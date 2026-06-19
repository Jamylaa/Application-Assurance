#!/bin/bash

# 🧪 Script de test automatisé pour Docker + Frontend + Keycloak

echo "================================================"
echo "🔍 DIAGNOSTIC - Docker Frontend Setup"
echo "================================================"
echo ""

# Couleurs
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 1. Vérifier que Docker est en cours d'exécution
echo -e "${YELLOW}[1/8]${NC} Vérification de Docker..."
if ! docker ps > /dev/null 2>&1; then
    echo -e "${RED}❌ Docker n'est pas en cours d'exécution${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Docker est en cours d'exécution${NC}"
echo ""

# 2. Vérifier les images
echo -e "${YELLOW}[2/8]${NC} Vérification des images Docker..."
if docker image ls | grep -q "frontend"; then
    IMAGE_SIZE=$(docker image ls | grep frontend | awk '{print $7}')
    echo -e "${GREEN}✅ Image frontend trouvée (${IMAGE_SIZE})${NC}"
else
    echo -e "${RED}❌ Image frontend non trouvée${NC}"
    echo "    Exécutez: docker-compose build frontend"
    exit 1
fi
echo ""

# 3. Vérifier les conteneurs en cours d'exécution
echo -e "${YELLOW}[3/8]${NC} Vérification des conteneurs en cours d'exécution..."
if docker ps | grep -q "frontend"; then
    STATUS=$(docker ps | grep frontend | awk '{print $NF}')
    echo -e "${GREEN}✅ Conteneur frontend en cours d'exécution (State: ${STATUS})${NC}"
else
    echo -e "${RED}❌ Conteneur frontend n'est pas en cours d'exécution${NC}"
    echo "    Exécutez: docker-compose up -d frontend"
    exit 1
fi
echo ""

# 4. Vérifier le contenu du répertoire Nginx
echo -e "${YELLOW}[4/8]${NC} Vérification du contenu Nginx dans le conteneur..."
FILES_COUNT=$(docker exec frontend ls -la /usr/share/nginx/html/ 2>/dev/null | wc -l)
if [ "$FILES_COUNT" -gt 5 ]; then
    echo -e "${GREEN}✅ Fichiers trouvés dans /usr/share/nginx/html/ (${FILES_COUNT} entrées)${NC}"
    docker exec frontend ls -la /usr/share/nginx/html/ | head -10
else
    echo -e "${RED}❌ Pas assez de fichiers dans /usr/share/nginx/html/ (${FILES_COUNT} entrées)${NC}"
    echo "    Le Dockerfile n'a peut-être pas copié les fichiers correctement"
    echo "    Vérifiez que dist/frontend existe et contient les fichiers compilés"
    exit 1
fi
echo ""

# 5. Vérifier que index.html existe
echo -e "${YELLOW}[5/8]${NC} Vérification de index.html..."
if docker exec frontend test -f /usr/share/nginx/html/index.html; then
    echo -e "${GREEN}✅ index.html trouvé${NC}"
else
    echo -e "${RED}❌ index.html non trouvé${NC}"
    echo "    Les fichiers Angular compilés ne sont pas au bon endroit"
    exit 1
fi
echo ""

# 6. Tester HTTP 200 pour la page dashboard
echo -e "${YELLOW}[6/8]${NC} Test HTTP (port 4200)..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:4200/dashboard 2>/dev/null || echo "000")
if [ "$HTTP_CODE" = "200" ]; then
    echo -e "${GREEN}✅ HTTP 200 OK - Fichiers servis correctement${NC}"
elif [ "$HTTP_CODE" = "304" ]; then
    echo -e "${RED}❌ HTTP 304 Not Modified - Problème de cache${NC}"
    echo "    Solution: Vider le cache du navigateur (Ctrl+Shift+Delete)"
    exit 1
elif [ "$HTTP_CODE" = "000" ]; then
    echo -e "${RED}❌ Connection refused (frontend non accessible sur port 4200)${NC}"
    echo "    Vérifiez que docker-compose up est exécuté et que le frontend est prêt"
    exit 1
else
    echo -e "${RED}❌ HTTP ${HTTP_CODE} - Réponse inattendue${NC}"
    exit 1
fi
echo ""

# 7. Vérifier Keycloak
echo -e "${YELLOW}[7/8]${NC} Vérification de Keycloak (port 9090)..."
KEYCLOAK_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:9090 2>/dev/null || echo "000")
if [ "$KEYCLOAK_CODE" = "200" ] || [ "$KEYCLOAK_CODE" = "302" ]; then
    echo -e "${GREEN}✅ Keycloak est accessible sur port 9090${NC}"
else
    echo -e "${YELLOW}⚠️  Keycloak non accessible (code ${KEYCLOAK_CODE})${NC}"
    echo "    Vérifiez que le service keycloak est démarré"
fi
echo ""

# 8. Vérifier les logs Nginx
echo -e "${YELLOW}[8/8]${NC} Vérification des logs Nginx..."
NGINX_ERRORS=$(docker logs frontend 2>&1 | grep -c "error" || echo "0")
if [ "$NGINX_ERRORS" = "0" ]; then
    echo -e "${GREEN}✅ Pas d'erreurs dans les logs Nginx${NC}"
else
    echo -e "${YELLOW}⚠️  ${NGINX_ERRORS} erreurs détectées dans les logs${NC}"
    echo "    Logs récents:"
    docker logs frontend --tail 10
fi
echo ""

# Résumé
echo "================================================"
echo -e "${GREEN}✅ DIAG COMPLET - Tout semble fonctionner!${NC}"
echo "================================================"
echo ""
echo "🌐 Accès utilisateur:"
echo "   Frontend: http://localhost:4200"
echo "   Keycloak Admin: http://localhost:9090/auth/admin"
echo "   Gateway: http://localhost:9091"
echo ""
echo "📝 Prochaines étapes:"
echo "   1. Ouvrez http://localhost:4200 dans le navigateur"
echo "   2. Vous devriez être redirigé vers Keycloak login"
echo "   3. Après login, dashboard devrait s'afficher"
echo "   4. Vérifiez la console du navigateur (F12) pour les erreurs"
echo ""

