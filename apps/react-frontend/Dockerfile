# Use pre-built image with Java 21 JDK and Node.js 22
FROM timbru31/java-node:21-jdk-22 AS builder

# Set working directory
WORKDIR /workspace

# Copy package files for dependency installation
COPY package*.json ./
COPY apps/react-frontend/package*.json ./apps/react-frontend/

# Install all dependencies (including dev dependencies needed for build)
RUN npm ci

# Copy source code
COPY ../.. .

# Build the frontend application
RUN npx nx build react-frontend

# The build output will be in apps/react-frontend/dist/
# Digital Ocean will use this for static site deployment

# Minimal export stage containing only the frontend build artifacts
FROM scratch AS export
COPY --from=builder /workspace/apps/react-frontend/dist/ .

