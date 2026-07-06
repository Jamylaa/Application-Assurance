# Stage 1: Build stage avec Node.js
FROM node:20 AS build

WORKDIR /app

# Configurer npm pour éviter les problèmes de proxy et réseau
RUN npm config set registry https://registry.npmjs.org/ \
    && npm config set fetch-retries 5 \
    && npm config set fetch-retry-mintimeout 20000 \
    && npm config set fetch-retry-maxtimeout 120000 \
    && npm config set strict-ssl false

# Copier les fichiers package.json et package-lock.json
COPY package*.json ./

# Installer les dépendances
RUN npm install --legacy-peer-deps --network-timeout=180000

# Copier les sources
COPY . .

# Build l'application Angular pour la production
# Augmenter la mémoire pour éviter les erreurs OOM avec Angular 18
ENV NODE_OPTIONS="--max-old-space-size=4096"
RUN npm run build --configuration=production

# Stage 2: Production stage avec Nginx
FROM nginx:alpine

# Copier les fichiers build - vérifier si le dossier browser existe ou non
# Angular 17+ avec nouveau builder crée /dist/frontend/browser/
# Si le build échoue, on peut avoir /dist/frontend/ directement
RUN mkdir -p /usr/share/nginx/html
COPY --from=build /app/dist/frontend/ /usr/share/nginx/html/
# Si le sous-dossier browser existe, déplacer son contenu
RUN if [ -d /usr/share/nginx/html/browser ]; then mv /usr/share/nginx/html/browser/* /usr/share/nginx/html/ && rmdir /usr/share/nginx/html/browser; fi

# Copier la configuration nginx personnalisée
COPY nginx.conf /etc/nginx/nginx.conf

# Exposer le port 80 (standard pour Nginx)
EXPOSE 80

# Démarrer Nginx
CMD ["nginx", "-g", "daemon off;"]
