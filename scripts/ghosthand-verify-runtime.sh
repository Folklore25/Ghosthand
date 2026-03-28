#!/usr/bin/env bash
set -euo pipefail

PKG="com.folklore25.ghosthand"
SERVICE="${PKG}/.GhosthandForegroundService"
ACTIVITY="${PKG}/.MainActivity"
CORE_SERVICE="${PKG}/${PKG}.GhostCoreAccessibilityService"
PORT="5583"
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
LAST_OUTPUT=""
LAST_LABEL=""
MODE_NAME=""
FIRST_FAILURE=""
RESULT_LINES=()

record_result() {
  local status="$1"
  local label="$2"
  RESULT_LINES+=("${status}|${label}")
}

begin_mode() {
  MODE_NAME="$1"
  FIRST_FAILURE=""
  RESULT_LINES=()
}

print_summary() {
  local mode_label="${MODE_NAME:-adhoc}"
  echo "== summary: ${mode_label} =="
  for line in "${RESULT_LINES[@]:-}"; do
    [[ -n "$line" ]] || continue
    local status="${line%%|*}"
    local label="${line#*|}"
    echo "${status}: ${label}"
  done
  if [[ -n "${FIRST_FAILURE}" ]]; then
    echo "First failing step: ${FIRST_FAILURE}"
  else
    echo "First failing step: none"
  fi
  echo
}

finish_mode() {
  local status="${1:-0}"
  print_summary
  return "$status"
}

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

expect_contains() {
  local response="$1"
  local needle="$2"
  local message="$3"
  if [[ "$response" != *"$needle"* ]]; then
    local last_index=$(( ${#RESULT_LINES[@]} - 1 ))
    if (( last_index >= 0 )) && [[ "${RESULT_LINES[$last_index]#*|}" == "${LAST_LABEL}" ]]; then
      RESULT_LINES[$last_index]="FAIL|${LAST_LABEL}"
    else
      record_result "FAIL" "${LAST_LABEL}"
    fi
    if [[ -z "${FIRST_FAILURE}" ]]; then
      FIRST_FAILURE="${LAST_LABEL}"
    fi
    echo "$message"
    return 1
  fi
}

expect_grep() {
  local response="$1"
  local needle="$2"
  local message="$3"
  if ! printf '%s' "$response" | grep -Fq "$needle"; then
    local last_index=$(( ${#RESULT_LINES[@]} - 1 ))
    if (( last_index >= 0 )) && [[ "${RESULT_LINES[$last_index]#*|}" == "${LAST_LABEL}" ]]; then
      RESULT_LINES[$last_index]="FAIL|${LAST_LABEL}"
    else
      record_result "FAIL" "${LAST_LABEL}"
    fi
    if [[ -z "${FIRST_FAILURE}" ]]; then
      FIRST_FAILURE="${LAST_LABEL}"
    fi
    echo "$message"
    return 1
  fi
}

dump_ui_xml() {
  adb shell uiautomator dump /sdcard/ghosthand-ui.xml >/dev/null 2>&1 || true
  adb shell cat /sdcard/ghosthand-ui.xml | sed -n '/^<?xml/,$p'
}

tap_ui_resource() {
  local resource_id="$1"
  local xml
  xml="$(dump_ui_xml | tr '\n' ' ')"

  local bounds
  bounds="$(printf '%s' "$xml" | grep -o "resource-id=\"${resource_id}\"[^>]*bounds=\"\\[[0-9]*,[0-9]*\\]\\[[0-9]*,[0-9]*\\]\"" | head -n 1 | sed -E 's/.*bounds=\"\[([0-9]+),([0-9]+)\]\[([0-9]+),([0-9]+)\]\"/\1 \2 \3 \4/')"
  if [[ -z "$bounds" ]]; then
    echo "tap-ui-resource failed: ${resource_id} not found in current UI dump"
    return 1
  fi

  local left top right bottom center_x center_y
  read -r left top right bottom <<<"$bounds"
  center_x=$(((left + right) / 2))
  center_y=$(((top + bottom) / 2))
  adb shell input tap "$center_x" "$center_y"
}

wait_for_http_ready() {
  local timeout_seconds="${1:-8}"
  local attempt=0

  while (( attempt < timeout_seconds )); do
    local response=""
    response="$(http_request GET /ping 2>/dev/null || true)"
    if [[ "$response" == *'"service":"ghosthand"'* ]]; then
      printf '%s\n' "$response"
      return 0
    fi
    sleep 1
    attempt=$((attempt + 1))
  done

  echo "wait-for-http-ready failed: /ping did not become available within ${timeout_seconds}s"
  return 1
}

extract_json_int() {
  local response="$1"
  local field="$2"
  printf '%s' "$response" | sed -n "s/.*\"${field}\":\([0-9][0-9]*\).*/\1/p" | head -n 1
}

run_step() {
  local label="$1"
  shift

  echo "== ${label} =="
  if LAST_OUTPUT="$("$@" 2>&1)"; then
    LAST_LABEL="$label"
    record_result "PASS" "$label"
    printf '%s\n' "$LAST_OUTPUT"
    echo
    echo "PASS: ${label}"
    echo
    return 0
  fi

  LAST_LABEL="$label"
  record_result "FAIL" "$label"
  if [[ -z "${FIRST_FAILURE}" ]]; then
    FIRST_FAILURE="$label"
  fi
  printf '%s\n' "$LAST_OUTPUT"
  echo
  echo "FAIL: ${label}"
  echo "First failing step: ${label}"
  echo
  return 1
}

restore_runtime() {
  local current_services
  current_services="$(adb shell su -c 'settings get secure enabled_accessibility_services' | tr -d '\r')"
  if [[ "$current_services" == "null" ]]; then
    current_services=""
  fi

  local merged_services="${CORE_SERVICE}"
  if [[ -n "$current_services" ]]; then
    if [[ ":${current_services}:" == *":${CORE_SERVICE}:"* ]]; then
      merged_services="${current_services}"
    else
      merged_services="${current_services}:${CORE_SERVICE}"
    fi
  fi

  adb shell su -c "am start-foreground-service -n ${SERVICE}"
  adb shell su -c "settings put secure accessibility_enabled 1"
  sleep 1
  adb shell su -c "settings put secure enabled_accessibility_services '${merged_services}'"
  adb shell am start -n "${ACTIVITY}"
  sleep 1

  if wait_for_http_ready 2 >/dev/null 2>&1; then
    return 0
  fi

  tap_ui_resource "${PKG}:id/startServiceButton"
  wait_for_http_ready 8 >/dev/null
}

install_current_build() {
  if [[ ! -f "${APK_PATH}" ]]; then
    echo "APK not found at ${APK_PATH}"
    echo "Build it first with: ./gradlew :app:assembleDebug"
    return 1
  fi

  local size
  size="$(stat -f %z "${APK_PATH}")"
  cat "${APK_PATH}" | adb shell su -c "pm install -r -S ${size}"
}

commands_preview() {
  http_request GET /commands | head -c 1200
}

screenshot_preview() {
  http_request GET /screenshot | head -c 1200
}

smoke_check() {
  run_step "ping" http_request GET /ping || return 1
  run_step "foreground" http_request GET /foreground || return 1
  run_step "commands" commands_preview || return 1
}

commands_schema_check() {
  local response
  response="$(http_request GET /commands)"

  echo "== commands schema check =="
  echo "$response" | head -c 1400
  echo
  echo

  expect_contains "$response" '"schemaVersion":"1.2"' "commands-schema-check failed: missing schemaVersion 1.2" || return 1
  expect_contains "$response" '"selectorAliases"' "commands-schema-check failed: missing selectorAliases" || return 1
  expect_contains "$response" '"selectorStrategies"' "commands-schema-check failed: missing selectorStrategies" || return 1
  expect_contains "$response" '"selectorSupport"' "commands-schema-check failed: missing selectorSupport" || return 1
  expect_contains "$response" '"focusRequirement"' "commands-schema-check failed: missing focusRequirement" || return 1
  expect_contains "$response" '"delayedAcceptance"' "commands-schema-check failed: missing delayedAcceptance" || return 1
  expect_contains "$response" '"stability"' "commands-schema-check failed: missing stability" || return 1
  expect_grep "$response" '"id":"click"' "commands-schema-check failed: missing click command" || return 1
  expect_grep "$response" '"id":"wait_ui_change"' "commands-schema-check failed: missing wait_ui_change command" || return 1
  expect_grep "$response" '"id":"wait_condition"' "commands-schema-check failed: missing wait_condition command" || return 1
}

tree_find_click_check() {
  adb shell am start -n "${ACTIVITY}"
  sleep 1

  run_step "tree package matches foreground" http_request GET /tree || return 1
  expect_contains "$LAST_OUTPUT" '"packageName":"com.folklore25.ghosthand"' "tree-find-click failed: /tree packageName did not match Ghosthand" || return 1

  run_step "find startServiceButton" http_request POST /find '{"id":"com.folklore25.ghosthand:id/startServiceButton"}' || return 1
  expect_contains "$LAST_OUTPUT" '"found":true' "tree-find-click failed: /find did not locate startServiceButton" || return 1

  run_step "click startServiceButton" http_request POST /click '{"id":"com.folklore25.ghosthand:id/startServiceButton"}' || return 1
  expect_contains "$LAST_OUTPUT" '"performed":true' "tree-find-click failed: /click did not perform" || return 1
}

focused_input_check() {
  adb shell su -c 'am start -a android.settings.SETTINGS'
  sleep 1

  run_step "find search field" http_request POST /find '{"id":"android:id/input"}' || return 1
  expect_contains "$LAST_OUTPUT" '"found":true' "focused-input failed: /find did not locate the Settings search field" || return 1

  local center_x center_y
  center_x="$(extract_json_int "$LAST_OUTPUT" "centerX")"
  center_y="$(extract_json_int "$LAST_OUTPUT" "centerY")"
  if [[ -z "$center_x" || -z "$center_y" ]]; then
    echo "focused-input failed: could not extract tap coordinates from /find response"
    return 1
  fi

  run_step "tap search field" http_request POST /tap "{\"x\":${center_x},\"y\":${center_y}}" || return 1
  expect_contains "$LAST_OUTPUT" '"performed":true' "focused-input failed: search field tap did not perform" || return 1
  sleep 1

  run_step "focused" http_request GET /focused || return 1
  expect_contains "$LAST_OUTPUT" '"available":true' "focused-input failed: /focused did not expose a focused node" || return 1
  sleep 1

  run_step "input" http_request POST /input '{"text":"wifi"}' || return 1
  expect_contains "$LAST_OUTPUT" '"performed":true' "focused-input failed: /input did not perform" || return 1
}

selector_click_check() {
  adb shell am start -n "${ACTIVITY}"
  sleep 1

  run_step "selector click by text" http_request POST /click '{"text":"启动运行时"}' || return 1
  expect_contains "$LAST_OUTPUT" '"performed":true' "selector-click failed: text selector did not perform" || return 1

  adb shell am start -n "${ACTIVITY}"
  sleep 1

  run_step "selector click by resource id" http_request POST /click '{"id":"com.folklore25.ghosthand:id/startServiceButton"}' || return 1
  expect_contains "$LAST_OUTPUT" '"performed":true' "selector-click failed: id selector did not perform" || return 1

  adb shell su -c 'am start -a android.settings.SETTINGS'
  sleep 1

  run_step "prepare desc target" http_request POST /input '{"text":"wifi"}' || return 1
  expect_contains "$LAST_OUTPUT" '"performed":true' "selector-click failed: could not prepare content-description target" || return 1

  run_step "selector click by content description" http_request POST /click '{"desc":"清空"}' || return 1
  expect_contains "$LAST_OUTPUT" '"performed":true' "selector-click failed: content-description selector did not perform" || return 1
}

swipe_check() {
  adb shell su -c 'am start -a android.settings.SETTINGS'
  sleep 1
  run_step "swipe delayed acceptance" http_request POST /swipe '{"from":{"x":540,"y":1800},"to":{"x":540,"y":900},"durationMs":300}' || return 1
  expect_contains "$LAST_OUTPUT" '"performed":true' "swipe-check failed: /swipe did not perform" || return 1
}

screenshot_check() {
  run_step "screenshot" screenshot_preview || return 1
  if [[ "$LAST_OUTPUT" == *'"format":"png"'* ]]; then
    return 0
  fi
  expect_contains "$LAST_OUTPUT" '"image":"data:image\/png;base64,' "screenshot-check failed: response did not expose PNG image data" || return 1
}

notify_check() {
  run_step "notify post" http_request POST /notify '{"title":"Ghosthand","text":"notify test"}' || return 1
  if [[ "$LAST_OUTPUT" == *'"performed":true'* ]]; then
    :
  else
    expect_contains "$LAST_OUTPUT" '"posted":true' "notify-check failed: /notify POST did not report a posted notification" || return 1
  fi
  sleep 1

  run_step "notify read" http_request GET /notify || return 1
  expect_contains "$LAST_OUTPUT" 'notify test' "notify-check failed: /notify GET did not return the posted notification" || return 1
}

clipboard_check() {
  adb shell am start -n "${ACTIVITY}"
  sleep 1
  run_step "clipboard write" http_request POST /clipboard '{"text":"ghosthand verify"}' || return 1
  expect_contains "$LAST_OUTPUT" '"written":true' "clipboard-check failed: /clipboard write did not succeed" || return 1
  sleep 1

  run_step "clipboard read" http_request GET /clipboard || return 1
  expect_contains "$LAST_OUTPUT" 'ghosthand verify' "clipboard-check failed: /clipboard read did not return the written value" || return 1
}

wait_home_check() {
  adb shell su -c 'am start -a android.settings.SETTINGS'
  sleep 1

  local combined
  combined="$(adb shell '
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
  ')"
  printf '%s\n' "$combined"
  expect_contains "$combined" '"performed":true' "wait-home failed: /home did not perform" || return 1
  expect_contains "$combined" '"changed":true' "wait-home failed: /wait did not observe UI change" || return 1
}

core_check() {
  run_step "restore-runtime" restore_runtime || return 1
  smoke_check || return 1
  run_step "commands schema check" commands_schema_check || return 1
  run_step "screen" http_request GET /screen || return 1
  expect_contains "$LAST_OUTPUT" '"centerX"' "core failed: /screen did not return action-ready geometry" || return 1
  tree_find_click_check || return 1
  focused_input_check || return 1
  selector_click_check || return 1
  clipboard_check || return 1
  swipe_check || return 1
}

full_check() {
  run_step "install-current-build" install_current_build || return 1
  core_check || return 1
  run_step "screenshot-check" screenshot_check || return 1
  run_step "notify-check" notify_check || return 1
  run_step "wait-home" wait_home_check || return 1
}

usage() {
  cat <<'EOF'
Usage:
  scripts/ghosthand-verify-runtime.sh install-current-build
  scripts/ghosthand-verify-runtime.sh restore-runtime
  scripts/ghosthand-verify-runtime.sh smoke
  scripts/ghosthand-verify-runtime.sh core
  scripts/ghosthand-verify-runtime.sh full
  scripts/ghosthand-verify-runtime.sh focused-input
  scripts/ghosthand-verify-runtime.sh tree-find-click
  scripts/ghosthand-verify-runtime.sh selector-click
  scripts/ghosthand-verify-runtime.sh screenshot-check
  scripts/ghosthand-verify-runtime.sh notify-check
  scripts/ghosthand-verify-runtime.sh commands-schema-check
  scripts/ghosthand-verify-runtime.sh wait-home
  scripts/ghosthand-verify-runtime.sh all

Modes:
  - smoke: runtime up + foreground + commands preview
  - core: restore runtime, validate contract, screen, tree/find/click, focused input, selector click, clipboard, swipe
  - full: install current build, then run core + screenshot + notify + wait

Notes:
  - Uses adb plus su -c to install and restore runtime on the target device
  - Stops at the first narrow failing step and prints a summary matrix
  - Intended for repeatable device-shell verification on the target phone
EOF
}

main() {
  local command="${1-}"
  local status=0
  begin_mode "${command:-help}"
  case "$command" in
    install-current-build)
      run_step "install-current-build" install_current_build || status=$?
      ;;
    restore-runtime)
      run_step "restore-runtime" restore_runtime || status=$?
      ;;
    smoke)
      smoke_check || status=$?
      ;;
    core)
      core_check || status=$?
      ;;
    full)
      full_check || status=$?
      ;;
    focused-input)
      run_step "focused-input chain" focused_input_check || status=$?
      ;;
    tree-find-click)
      run_step "tree-find-click chain" tree_find_click_check || status=$?
      ;;
    selector-click)
      run_step "selector-click variants" selector_click_check || status=$?
      ;;
    screenshot-check)
      run_step "screenshot-check" screenshot_check || status=$?
      ;;
    notify-check)
      run_step "notify-check" notify_check || status=$?
      ;;
    commands-schema-check)
      run_step "commands schema check" commands_schema_check || status=$?
      ;;
    wait-home)
      run_step "wait-home" wait_home_check || status=$?
      ;;
    all)
      MODE_NAME="full"
      full_check || status=$?
      ;;
    *)
      usage
      status=1
      ;;
  esac
  finish_mode "$status"
}

main "${1-}"
