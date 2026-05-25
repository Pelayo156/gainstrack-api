#!/bin/bash

# ── GainsTrack API — Script de arranque ──────────────────
# Detiene y elimina el contenedor anterior si existe,
# luego levanta uno nuevo con la configuración del .env

SCRIPT_DIR="$(dirname "$0")"
CONTAINER_NAME="gainstrack-api"
IMAGE_NAME="gainstrack-api"
NETWORK="gainstrack-network"

echo "Deteniendo contenedor anterior..."
podman stop $CONTAINER_NAME 2>/dev/null
podman rm $CONTAINER_NAME 2>/dev/null

echo "Iniciando $CONTAINER_NAME..."
podman run -d \
  --name $CONTAINER_NAME \
  --network $NETWORK \
  --env-file "$SCRIPT_DIR/.env" \
  -p 8080:8080 \
  $IMAGE_NAME

echo "Verificando estado..."
podman logs --tail 20 $CONTAINER_NAME