# syntax=docker/dockerfile:1.7

FROM docker.io/library/node:22-alpine AS builder

WORKDIR /app

COPY package.json package-lock.json ./
RUN --mount=type=secret,id=npmrc,target=/root/.npmrc npm ci

COPY angular.json tsconfig.json ./
COPY projects ./projects

RUN npm run build -- --configuration production

FROM docker.io/library/nginx:1.27-alpine AS runtime

COPY docker/nginx.conf /etc/nginx/nginx.conf
COPY docker/entrypoint.sh /entrypoint.sh
COPY --from=builder /app/dist/multi-module-app/browser /usr/share/nginx/html

RUN chmod +x /entrypoint.sh

USER nginx

EXPOSE 8080

ENTRYPOINT ["/entrypoint.sh"]
CMD ["nginx", "-c", "/etc/nginx/nginx.conf", "-g", "daemon off;"]
