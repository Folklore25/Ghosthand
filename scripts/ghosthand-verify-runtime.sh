#!/usr/bin/env bash
set -euo pipefail

PKG="com.folklore25.ghosthand"
SERVICE="${PKG}/.GhosthandForegroundService"
ACTIVITY="${PKG}/.MainActivity"
CORE_SERVICE="${PKG}/${PKG}.GhostCoreAccessibilityService"
REFERENCE_SERVICE="com.orb.eye/.OrbAccessibilityService"
PORT="5583"
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"

http_request() {
  local method="$1"
  local path="$2"
  local body="${3-}"
  local content_length="${#body}"

  if [[ -n "$body" ]]; then
    adb shell "printf '${method} ${path} HTTP/1.1\r\nHost: 127.0.0.1\r\nContent-Type: application/json\r\nContent-Length: ${content_length}\r\nConnection: close\r\n\r\n%s' '$body' | toybox nc 127.0.0.1 ${PORT}"
  else
    adb shell "printf '${method} ${path} HTTP/1.1\r\nHost: 127.0.0.1\r\nConnection: close\r\n\r\n' | toybox nc 127.0.0.1 ${PORT}"
  fi
}

restore_runtime() {
  adb shell su -c "am start-foreground-service -n ${SERVICE}"
  adb shell su -c "settings put secure enabled_accessibility_services ${REFERENCE_SERVICE}"
  adb shell su -c "settings put secure accessibility_enabled 1"
  sleep 1
  adb shell su -c "settings put secure enabled_accessibility_services '${REFERENCE_SERVICE}:${CORE_SERVICE}'"
  adb shell am start -n "${ACTIVITY}"
}

install_current_build() {
  if [[ ! -f "${APK_PATH}" ]]; then
    echo "APK not found at ${APK_PATH}"
    echo "Build it first with: ./gradlew :app:assembleDebug"
    exit 1
  fi

  local size
  size="$(stat -f %z "${APK_PATH}")"
  cat "${APK_PATH}" | adb shell su -c "pm install -r -S ${size}"
}

smoke_check() {
  echo "== ping =="
  http_request GET /ping
  echo
  echo "== foreground =="
  http_request GET /foreground
  echo
  echo "== commands =="
  http_request GET /commands | head -c 1200
  echo
  echo
}

wait_home_check() {
  adb shell su -c 'am start -a android.settings.SETTINGS'
  sleep 1
  adb shell '
    rm -f /data/local/tmp/ghosthand-wait.out /data/local/tmp/ghosthand-home.out
    (
      printf "GET /wait?timeout=5000 HTTP/1.1\r\nHost: 127.0.0.1\r\nConnection: close\r\n\r\n" |
        toybox nc 127.0.0.1 '"${PORT}"' > /data/local/tmp/ghosthand-wait.out
    ) &
    sleep 1
    printf "POST /home HTTP/1.1\r\nHost: 127.0.0.1\r\nContent-Length: 0\r\nConnection: close\r\n\r\n" |
      toybox nc 127.0.0.1 '"${PORT}"' > /data/local/tmp/ghosthand-home.out
    sleep 6
    echo "== home =="
    cat /data/local/tmp/ghosthand-home.out
    echo
    echo "== wait =="
    cat /data/local/tmp/ghosthand-wait.out
    echo
  '
}

usage() {
  cat <<'EOF'
Usage:
  scripts/ghosthand-verify-runtime.sh install-current-build
  scripts/ghosthand-verify-runtime.sh restore-runtime
  scripts/ghosthand-verify-runtime.sh smoke
  scripts/ghosthand-verify-runtime.sh wait-home
  scripts/ghosthand-verify-runtime.sh all

Notes:
  - `install-current-build` streams the local debug APK into `pm install -r -S`
  - Uses adb plus su -c to restore the foreground service and accessibility binding
  - Intended for repeatable device-shell verification on the target phone
EOF
}

main() {
  local command="${1-}"
  case "$command" in
    install-current-build)
      install_current_build
      ;;
    restore-runtime)
      restore_runtime
      ;;
    smoke)
      smoke_check
      ;;
    wait-home)
      wait_home_check
      ;;
    all)
      install_current_build
      restore_runtime
      smoke_check
      wait_home_check
      ;;
    *)
      usage
      exit 1
      ;;
  esac
}

main "${1-}"
