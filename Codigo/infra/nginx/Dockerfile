FROM node:24.15.0-alpine3.23 AS frontend-build

WORKDIR /workspace/frontend

COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --ignore-scripts

COPY frontend/ ./
RUN npm run build

FROM nginx:1.30.4-alpine3.24

COPY infra/nginx/nginx.conf /etc/nginx/nginx.conf
COPY --from=frontend-build /workspace/frontend/dist /usr/share/nginx/html
