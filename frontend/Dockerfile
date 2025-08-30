# Multi-stage build: Test stage
FROM node:18-alpine AS test
WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm ci

# Copy source code
COPY . .

# Run tests in CI mode with coverage
RUN npm run test:ci

# Production stage: Serve static files
FROM nginx:alpine AS production

# Copy static files from test stage
COPY --from=test /app /usr/share/nginx/html

# Copy nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]