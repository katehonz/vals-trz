#!/bin/bash
DIR="$(cd "$(dirname "$0")" && pwd)"
PIDS="$DIR/.pids"

if [ -f "$PIDS" ]; then
  echo "Вече е стартирано. Спри първо с ./stop.sh"
  exit 1
fi

echo "=== vals-trz ==="

# ArangoDB check & auto-start
if ! curl -s http://localhost:8529/_api/version > /dev/null 2>&1; then
  echo "[..] ArangoDB не е достъпна, опитвам стартиране..."
  if docker ps -a --format '{{.Names}}' | grep -q '^arangodb$'; then
    docker start arangodb > /dev/null 2>&1
  else
    echo "[!] Няма контейнер 'arangodb'. Създай го първо:"
    echo "    docker run -d --name arangodb -p 8529:8529 -e ARANGO_ROOT_PASSWORD=root arangodb:3.12"
    exit 1
  fi
  # Wait for ArangoDB to become ready
  for i in $(seq 1 15); do
    if curl -s http://localhost:8529/_api/version > /dev/null 2>&1; then
      break
    fi
    sleep 1
  done
  if ! curl -s http://localhost:8529/_api/version > /dev/null 2>&1; then
    echo "[!] ArangoDB не успя да стартира навреме."
    exit 1
  fi
fi
echo "[ok] ArangoDB"

# Backend
cd "$DIR/backend"
mvn spring-boot:run -q > "$DIR/backend.log" 2>&1 &
BACK_PID=$!
echo "[..] Backend стартира (PID $BACK_PID) ..."

# Wait for backend
for i in $(seq 1 30); do
  if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo "[ok] Backend :8080"
    break
  fi
  if ! kill -0 $BACK_PID 2>/dev/null; then
    echo "[!!] Backend умря. Виж backend.log"
    exit 1
  fi
  sleep 2
done

# Frontend
cd "$DIR/frontend"
npx vite --host > "$DIR/frontend.log" 2>&1 &
FRONT_PID=$!
sleep 2
echo "[ok] Frontend :5173"

# Save PIDs
echo "$BACK_PID $FRONT_PID" > "$PIDS"

echo ""
echo "  Frontend:  http://localhost:5173"
echo "  Backend:   http://localhost:8080"
echo "  ArangoDB:  http://localhost:8529"
echo ""
echo "  Логове: backend.log, frontend.log"
echo "  Спиране: ./stop.sh"
