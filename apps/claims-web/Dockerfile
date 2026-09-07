# Build stage
FROM node:20-alpine AS build
WORKDIR /build
# OIDC authority baked at build time: the compose nginx proxies /realms on the
# same origin, so the issuer matches what Keycloak puts in tokens.
ARG VITE_OIDC_AUTHORITY=http://localhost:8081/realms/acme-claims
ENV VITE_OIDC_AUTHORITY=$VITE_OIDC_AUTHORITY
COPY package.json ./
RUN npm install --no-audit --no-fund
COPY . .
RUN npm run build

# Runtime stage: nginx serving the SPA and proxying /api, /realms, /genai
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /build/dist /usr/share/nginx/html
EXPOSE 80
