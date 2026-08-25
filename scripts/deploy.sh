#!/bin/bash
# Acme Claims Platform - Deployment Script
# Supports all four environments: dev, qa, uat, prod

set -e

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
ENV=${1:-dev}
VERSION=${2:-latest}

# Environment-specific configurations
declare -A ENV_CONFIGS=(
    ["dev"]="server:dev-server.acme.internal|port:8080|db:dev-db.acme.internal"
    ["qa"]="server:qa-server.acme.internal|port:8080|db:qa-db.acme.internal"
    ["uat"]="server:uat-server.acme.internal|port:8080|db:uat-db.acme.internal"
    ["prod"]="server:prod-server.acme.internal|port:8080|db:prod-db.acme.internal"
)

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

validate_environment() {
    if [[ ! "${!ENV_CONFIGS[@]}" =~ "$ENV" ]]; then
        log_error "Invalid environment: $ENV"
        echo "Valid environments: ${!ENV_CONFIGS[@]}"
        exit 1
    fi
}

parse_config() {
    local config="${ENV_CONFIGS[$ENV]}"
    SERVER=$(echo "$config" | cut -d'|' -f1 | cut -d':' -f2)
    PORT=$(echo "$config" | cut -d'|' -f2 | cut -d':' -f2)
    DB=$(echo "$config" | cut -d'|' -f3 | cut -d':' -f2)
}

pre_deployment_checks() {
    log_info "Running pre-deployment checks..."
    
    # Check if backend JAR exists
    if [[ ! -f "$PROJECT_ROOT/apps/backend/target/*.jar" ]]; then
        log_warn "Backend JAR not found. Building..."
        cd "$PROJECT_ROOT/apps/backend"
        mvn clean package -DskipTests
        cd "$PROJECT_ROOT"
    fi
    
    # Check if frontend dist exists
    if [[ ! -d "$PROJECT_ROOT/apps/frontend/dist" ]]; then
        log_warn "Frontend dist not found. Building..."
        cd "$PROJECT_ROOT/apps/frontend"
        npm ci && npm run build
        cd "$PROJECT_ROOT"
    fi
    
    log_info "Pre-deployment checks passed."
}

deploy_backend() {
    log_info "Deploying backend to $ENV environment..."
    
    # Find the latest JAR
    JAR_FILE=$(ls -t "$PROJECT_ROOT/apps/backend/target/"*.jar 2>/dev/null | head -1)
    
    if [[ -z "$JAR_FILE" ]]; then
        log_error "No JAR file found in target directory"
        exit 1
    fi
    
    # Deploy to server
    scp "$JAR_FILE" "$SERVER:/opt/acme-claims/backend.jar"
    ssh "$SERVER" "systemctl restart acme-claims-backend"
    
    log_info "Backend deployment completed."
}

deploy_frontend() {
    log_info "Deploying frontend to $ENV environment..."
    
    # Deploy static files
    scp -r "$PROJECT_ROOT/apps/frontend/dist/"* "$SERVER:/var/www/acme-claims/"
    
    log_info "Frontend deployment completed."
}

run_migrations() {
    log_info "Running database migrations..."
    
    cd "$PROJECT_ROOT/data/migration"
    ./migrate.sh --environment "$ENV"
    
    log_info "Database migrations completed."
}

health_check() {
    log_info "Running health check..."
    
    local max_attempts=30
    local attempt=1
    
    while [[ $attempt -le $max_attempts ]]; do
        if curl -sf "http://$SERVER:$PORT/api/v1/actuator/health" > /dev/null; then
            log_info "Health check passed!"
            return 0
        fi
        
        log_warn "Health check attempt $attempt/$max_attempts failed. Retrying in 5 seconds..."
        sleep 5
        ((attempt++))
    done
    
    log_error "Health check failed after $max_attempts attempts"
    return 1
}

rollback() {
    log_warn "Initiating rollback..."
    
    ssh "$SERVER" "systemctl stop acme-claims-backend"
    ssh "$SERVER" "cp /opt/acme-claims/backend.jar.backup /opt/acme-claims/backend.jar"
    ssh "$SERVER" "systemctl start acme-claims-backend"
    
    log_info "Rollback completed."
}

main() {
    log_info "Starting deployment to $ENV environment (version: $VERSION)"
    
    validate_environment
    parse_config
    pre_deployment_checks
    
    # Backup current version
    ssh "$SERVER" "cp /opt/acme-claims/backend.jar /opt/acme-claims/backend.jar.backup" 2>/dev/null || true
    
    # Run migrations
    run_migrations
    
    # Deploy components
    deploy_backend
    deploy_frontend
    
    # Health check
    if ! health_check; then
        rollback
        exit 1
    fi
    
    log_info "Deployment to $ENV completed successfully!"
}

# Trap errors
trap 'log_error "Deployment failed!"; exit 1' ERR

main "$@"
