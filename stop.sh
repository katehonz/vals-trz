#!/bin/bash
DIR="$(cd "$(dirname "$0")" && pwd)"
PIDS="$DIR/.pids"

if [ ! -f "$PIDS" ]; then
  echo "Нищо не е стартирано."
  exit 0
fi

read BACK_PID FRONT_PID < "$PIDS"

kill $FRONT_PID 2>/dev/null && echo "[ok] Frontend спрян" || echo "[--] Frontend вече не работи"
kill $BACK_PID 2>/dev/null && echo "[ok] Backend спрян" || echo "[--] Backend вече не работи"

rm -f "$PIDS"
echo "Готово."
