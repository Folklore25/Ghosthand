#!/usr/bin/env bash
set -euo pipefail

PKG="com.folklore25.ghosthand"
SERVICE="${PKG}/.service.runtime.GhosthandForegroundService"
ACTIVITY="${PKG}/.ui.main.MainActivity"
CORE_SERVICE="${PKG}/${PKG}.GhostCoreAccessibilityService"
PORT="5583"
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
SCREENSHOT_RESPONSE_FILE="/tmp/ghosthand-screenshot-response.txt"
PREVIEW_MIN_SHORT_EDGE=360
FORWARD_PORT=6553
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

forwarded_http_get() {
  local path="$1"
  adb forward "tcp:${FORWARD_PORT}" "tcp:${PORT}" >/dev/null
  local status=0
  curl -sS -D - "http://127.0.0.1:${FORWARD_PORT}${path}" || status=$?
  adb forward --remove "tcp:${FORWARD_PORT}" >/dev/null 2>&1 || true
  return $status
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

extract_ui_text_bounds() {
  local xml="$1"
  local text="$2"
  printf '%s' "$xml" |
    grep -o "text=\"${text}\"[^>]*bounds=\"\\[[0-9]*,[0-9]*\\]\\[[0-9]*,[0-9]*\\]\"" |
    head -n 1 |
    sed -E 's/.*bounds=\"\[([0-9]+),([0-9]+)\]\[([0-9]+),([0-9]+)\]\"/\1 \2 \3 \4/'
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

extract_json_string() {
  local response="$1"
  local field="$2"
  printf '%s' "$response" | sed -n "s/.*\"${field}\":\"\\([^\"]*\\)\".*/\\1/p" | head -n 1 | sed 's#\\/#/#g'
}

decoded_base64_byte_count() {
  local payload="$1"
  printf '%s' "$payload" | node -e '
let s = "";
process.stdin.on("data", d => s += d);
process.stdin.on("end", () => {
  if (!s.trim()) {
    process.stdout.write("0");
    return;
  }
  try {
    process.stdout.write(String(Buffer.from(s.trim(), "base64").length));
  } catch (_) {
    process.stdout.write("0");
  }
});
'
}

validate_screenshot_response() {
  local response="$1"
  local label="$2"
  local min_short_edge="${3:-0}"

  expect_contains "$response" '"image":"data:image\/png;base64,' "${label} failed: response did not expose PNG image data" || return 1

  local image_base64
  image_base64="$(extract_screenshot_base64 "$response")"
  if [[ -z "$image_base64" ]]; then
    echo "${label} failed: screenshot base64 payload was blank after the PNG prefix"
    return 1
  fi

  local decoded_bytes
  decoded_bytes="$(decoded_base64_byte_count "$image_base64")"
  if [[ -z "$decoded_bytes" || "$decoded_bytes" -le 0 ]]; then
    echo "${label} failed: screenshot payload did not decode into non-empty bytes"
    return 1
  fi

  local width height
  width="$(extract_json_int "$response" "width")"
  height="$(extract_json_int "$response" "height")"
  if [[ -z "$width" || -z "$height" || "$width" -le 0 || "$height" -le 0 ]]; then
    echo "${label} failed: screenshot dimensions were not positive"
    return 1
  fi

  if (( min_short_edge > 0 )); then
    local short_edge="$width"
    if (( height < short_edge )); then
      short_edge="$height"
    fi
    if (( short_edge < min_short_edge )); then
      echo "${label} failed: preview short edge ${short_edge}px was below the ${min_short_edge}px decision-usable floor"
      return 1
    fi
  fi
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
  cat "${APK_PATH}" | adb shell su -c "pm install -r -d -S ${size}"
}

commands_preview() {
  http_request GET /commands | head -c 1200
}

screenshot_preview() {
  http_request GET /screenshot | head -c 1200
}

capture_screenshot_response() {
  forwarded_http_get /screenshot | tee "${SCREENSHOT_RESPONSE_FILE}"
}

capture_endpoint_response() {
  local endpoint="$1"
  forwarded_http_get "$endpoint" | tee "${SCREENSHOT_RESPONSE_FILE}"
}

capture_screenshot_response_quiet() {
  forwarded_http_get /screenshot > "${SCREENSHOT_RESPONSE_FILE}"
  wc -c "${SCREENSHOT_RESPONSE_FILE}"
}

capture_endpoint_response_quiet() {
  local endpoint="$1"
  forwarded_http_get "$endpoint" > "${SCREENSHOT_RESPONSE_FILE}"
  wc -c "${SCREENSHOT_RESPONSE_FILE}"
}

extract_http_content_length() {
  local response="$1"
  printf '%s' "$response" | sed -n 's/^Content-Length: \([0-9][0-9]*\).*/\1/p' | head -n 1
}

extract_screenshot_base64() {
  local response="$1"
  printf '%s' "$response" | node -e '
let s = "";
process.stdin.on("data", d => s += d);
process.stdin.on("end", () => {
  const bodyStart = s.indexOf("{");
  if (bodyStart < 0) return;
  try {
    const obj = JSON.parse(s.slice(bodyStart));
    const image = obj?.data?.image || "";
    process.stdout.write(image.replace(/^data:image\/png;base64,/, ""));
  } catch (_) {}
});
'
}

extract_screenshot_base64_from_file() {
  local path="$1"
  node -e '
const fs = require("fs");
const path = process.argv[1];
const s = fs.readFileSync(path, "utf8");
const bodyStart = s.indexOf("{");
if (bodyStart < 0) process.exit(0);
try {
  const obj = JSON.parse(s.slice(bodyStart));
  const image = obj?.data?.image || "";
  process.stdout.write(image.replace(/^data:image\/png;base64,/, ""));
} catch (_) {}
' "$path"
}

validate_screenshot_response_file() {
  local path="$1"
  local label="$2"
  local min_short_edge="${3:-0}"
  local response
  response="$(cat "$path")"

  expect_contains "$response" '"image":"data:image\/png;base64,' "${label} failed: response did not expose PNG image data" || return 1

  local image_base64
  image_base64="$(extract_screenshot_base64_from_file "$path")"
  if [[ -z "$image_base64" ]]; then
    echo "${label} failed: screenshot base64 payload was blank after the PNG prefix"
    return 1
  fi

  local decoded_bytes
  decoded_bytes="$(decoded_base64_byte_count "$image_base64")"
  if [[ -z "$decoded_bytes" || "$decoded_bytes" -le 0 ]]; then
    echo "${label} failed: screenshot payload did not decode into non-empty bytes"
    return 1
  fi

  local width height
  width="$(extract_json_int "$response" "width")"
  height="$(extract_json_int "$response" "height")"
  if [[ -z "$width" || -z "$height" || "$width" -le 0 || "$height" -le 0 ]]; then
    echo "${label} failed: screenshot dimensions were not positive"
    return 1
  fi

  if (( min_short_edge > 0 )); then
    local short_edge="$width"
    if (( height < short_edge )); then
      short_edge="$height"
    fi
    if (( short_edge < min_short_edge )); then
      echo "${label} failed: preview short edge ${short_edge}px was below the ${min_short_edge}px decision-usable floor"
      return 1
    fi
  fi
}

hash_text_sha256() {
  local text="$1"
  printf '%s' "$text" | shasum -a 256 | awk '{print $1}'
}

hash_screenshot_file_sha256() {
  local path="$1"
  node -e '
const fs = require("fs");
const path = process.argv[1];
const s = fs.readFileSync(path, "utf8");
const bodyStart = s.indexOf("{");
if (bodyStart < 0) process.exit(0);
try {
  const obj = JSON.parse(s.slice(bodyStart));
  const image = obj?.data?.image || "";
  const payload = image.replace(/^data:image\/png;base64,/, "");
  process.stdout.write(payload);
} catch (_) {}
' "$path" | shasum -a 256 | awk '{print $1}'
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
  run_step "screenshot full" capture_screenshot_response_quiet || return 1
  validate_screenshot_response_file "${SCREENSHOT_RESPONSE_FILE}" "screenshot-check full capture" || return 1

  run_step "screen preview metadata" http_request GET /screen || return 1
  local preview_path
  preview_path="$(extract_json_string "$LAST_OUTPUT" "previewPath")"
  if [[ -z "$preview_path" ]]; then
    echo "screenshot-check failed: /screen did not publish previewPath"
    return 1
  fi

  sleep 1
  run_step "screenshot preview" capture_endpoint_response_quiet "$preview_path" || return 1
  validate_screenshot_response_file "${SCREENSHOT_RESPONSE_FILE}" "screenshot-check preview capture" "$PREVIEW_MIN_SHORT_EDGE" || return 1
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

scenario_settings_search_back() {
  run_step "restore-runtime" restore_runtime || return 1

  adb shell su -c 'am start -a android.settings.SETTINGS'
  sleep 1

  run_step "scenario foreground settings" http_request GET /foreground || return 1
  expect_contains "$LAST_OUTPUT" '"packageName":"com.android.settings"' "scenario failed: Settings was not foreground after launch" || return 1

  run_step "scenario find search field" http_request POST /find '{"id":"android:id/input"}' || return 1
  expect_contains "$LAST_OUTPUT" '"found":true' "scenario failed: search field was not found" || return 1

  local center_x center_y
  center_x="$(extract_json_int "$LAST_OUTPUT" "centerX")"
  center_y="$(extract_json_int "$LAST_OUTPUT" "centerY")"
  if [[ -z "$center_x" || -z "$center_y" ]]; then
    echo "scenario failed: could not extract search field coordinates"
    return 1
  fi

  run_step "scenario tap search field" http_request POST /tap "{\"x\":${center_x},\"y\":${center_y}}" || return 1
  expect_contains "$LAST_OUTPUT" '"performed":true' "scenario failed: tap on search field did not perform" || return 1
  sleep 1

  run_step "scenario focused field" http_request GET /focused || return 1
  expect_contains "$LAST_OUTPUT" '"available":true' "scenario failed: no focused field after tap" || return 1
  expect_contains "$LAST_OUTPUT" '"resourceId":"android:id\/input"' "scenario failed: focused field was not the Settings search input" || return 1

  run_step "scenario input text" http_request POST /input '{"text":"wifi"}' || return 1
  expect_contains "$LAST_OUTPUT" '"performed":true' "scenario failed: input did not perform" || return 1
  expect_contains "$LAST_OUTPUT" '"text":"wifi"' "scenario failed: input response did not reflect the requested text" || return 1

  run_step "scenario find result" http_request POST /find '{"text":"WLAN","clickable":true}' || return 1
  expect_contains "$LAST_OUTPUT" '"found":true' "scenario failed: Settings search result was not found" || return 1

  local combined
  combined="$(adb shell '
    rm -f /data/local/tmp/ghosthand-scenario-wait.out /data/local/tmp/ghosthand-scenario-click.out
    (
      printf "GET /wait?timeout=4000 HTTP/1.1\r\nHost: 127.0.0.1\r\nConnection: close\r\n\r\n" |
        toybox nc 127.0.0.1 '"${PORT}"' > /data/local/tmp/ghosthand-scenario-wait.out
    ) &
    sleep 1
    BODY="{\"text\":\"WLAN\",\"clickable\":true}"
    LEN=$(printf "%s" "$BODY" | wc -c | tr -d " ")
    printf "POST /click HTTP/1.1\r\nHost: 127.0.0.1\r\nContent-Type: application/json\r\nContent-Length: %s\r\nConnection: close\r\n\r\n%s" "$LEN" "$BODY" |
      toybox nc 127.0.0.1 '"${PORT}"' > /data/local/tmp/ghosthand-scenario-click.out
    sleep 5
    echo "== click =="
    cat /data/local/tmp/ghosthand-scenario-click.out
    echo
    echo "== wait =="
    cat /data/local/tmp/ghosthand-scenario-wait.out
    echo
  ')"
  printf '%s\n' "$combined"
  expect_contains "$combined" '"performed":true' "scenario failed: result click did not perform" || return 1
  expect_contains "$combined" '"changed":true' "scenario failed: /wait did not observe UI change after result click" || return 1

  run_step "scenario foreground after click" http_request GET /foreground || return 1
  expect_contains "$LAST_OUTPUT" '"packageName":"com.android.settings"' "scenario failed: foreground package drifted after back" || return 1

  combined="$(adb shell '
    rm -f /data/local/tmp/ghosthand-scenario-wait.out /data/local/tmp/ghosthand-scenario-back.out
    (
      printf "GET /wait?timeout=4000 HTTP/1.1\r\nHost: 127.0.0.1\r\nConnection: close\r\n\r\n" |
        toybox nc 127.0.0.1 '"${PORT}"' > /data/local/tmp/ghosthand-scenario-wait.out
    ) &
    sleep 1
    printf "POST /back HTTP/1.1\r\nHost: 127.0.0.1\r\nContent-Length: 0\r\nConnection: close\r\n\r\n" |
      toybox nc 127.0.0.1 '"${PORT}"' > /data/local/tmp/ghosthand-scenario-back.out
    sleep 5
    echo "== back =="
    cat /data/local/tmp/ghosthand-scenario-back.out
    echo
    echo "== wait =="
    cat /data/local/tmp/ghosthand-scenario-wait.out
    echo
  ')"
  printf '%s\n' "$combined"
  expect_contains "$combined" '"performed":true' "scenario failed: /back did not perform" || return 1
  expect_contains "$combined" '"changed":true' "scenario failed: /wait did not observe UI change after back" || return 1

  run_step "scenario foreground after back" http_request GET /foreground || return 1
  expect_contains "$LAST_OUTPUT" '"packageName":"com.android.settings"' "scenario failed: foreground package drifted after back" || return 1
}

scenario_settings_home_screenshot() {
  run_step "restore-runtime" restore_runtime || return 1
  local baseline_file="/tmp/ghosthand-scenario2-settings-response.txt"
  local home_file="/tmp/ghosthand-scenario2-home-response.txt"

  adb shell su -c 'am start -a android.settings.SETTINGS'
  sleep 1

  run_step "scenario2 foreground settings" http_request GET /foreground || return 1
  expect_contains "$LAST_OUTPUT" '"packageName":"com.android.settings"' "scenario2 failed: Settings was not foreground before baseline capture" || return 1

  run_step "scenario2 screenshot settings baseline" capture_screenshot_response || return 1
  expect_contains "$LAST_OUTPUT" '"image":"data:image\/png;base64,' "scenario2 failed: baseline screenshot did not expose PNG image data" || return 1
  local baseline_response_length
  baseline_response_length="$(extract_http_content_length "$LAST_OUTPUT")"
  if [[ -z "$baseline_response_length" || "$baseline_response_length" -lt 100000 ]]; then
    echo "scenario2 failed: baseline screenshot response size was unexpectedly small"
    return 1
  fi
  cp "${SCREENSHOT_RESPONSE_FILE}" "${baseline_file}"
  local baseline_hash
  baseline_hash="$(hash_screenshot_file_sha256 "${baseline_file}")"
  if [[ -z "$baseline_hash" ]]; then
    echo "scenario2 failed: could not hash baseline screenshot payload"
    return 1
  fi

  local combined
  combined="$(adb shell '
    rm -f /data/local/tmp/ghosthand-scenario2-wait.out /data/local/tmp/ghosthand-scenario2-home.out
    (
      printf "GET /wait?timeout=5000 HTTP/1.1\r\nHost: 127.0.0.1\r\nConnection: close\r\n\r\n" |
        toybox nc 127.0.0.1 '"${PORT}"' > /data/local/tmp/ghosthand-scenario2-wait.out
    ) &
    sleep 1
    printf "POST /home HTTP/1.1\r\nHost: 127.0.0.1\r\nContent-Length: 0\r\nConnection: close\r\n\r\n" |
      toybox nc 127.0.0.1 '"${PORT}"' > /data/local/tmp/ghosthand-scenario2-home.out
    sleep 6
    echo "== home =="
    cat /data/local/tmp/ghosthand-scenario2-home.out
    echo
    echo "== wait =="
    cat /data/local/tmp/ghosthand-scenario2-wait.out
    echo
  ')"
  printf '%s\n' "$combined"
  expect_contains "$combined" '"performed":true' "scenario2 failed: /home did not perform" || return 1
  expect_contains "$combined" '"packageName":"com.miui.home"' "scenario2 failed: /wait did not resolve to the launcher package" || return 1
  expect_contains "$combined" '"activity":"com.miui.home.launcher.Launcher"' "scenario2 failed: /wait did not resolve to the launcher activity" || return 1

  run_step "scenario2 foreground home" http_request GET /foreground || return 1
  expect_contains "$LAST_OUTPUT" '"packageName":"com.miui.home"' "scenario2 failed: launcher was not foreground after /home" || return 1
  expect_contains "$LAST_OUTPUT" '"activity":"com.miui.home.launcher.Launcher"' "scenario2 failed: launcher activity was not foreground after /home" || return 1

  run_step "scenario2 screenshot home" capture_screenshot_response || return 1
  expect_contains "$LAST_OUTPUT" '"image":"data:image\/png;base64,' "scenario2 failed: home screenshot did not expose PNG image data" || return 1
  local home_response_length
  home_response_length="$(extract_http_content_length "$LAST_OUTPUT")"
  if [[ -z "$home_response_length" || "$home_response_length" -lt 100000 ]]; then
    echo "scenario2 failed: home screenshot response size was unexpectedly small"
    return 1
  fi
  cp "${SCREENSHOT_RESPONSE_FILE}" "${home_file}"
  local home_hash
  home_hash="$(hash_screenshot_file_sha256 "${home_file}")"
  if [[ -z "$home_hash" ]]; then
    echo "scenario2 failed: could not hash home screenshot payload"
    return 1
  fi
  if [[ "$home_hash" == "$baseline_hash" ]]; then
    echo "scenario2 failed: home screenshot hash matched the baseline Settings screenshot"
    return 1
  fi
}

scenario_settings_clipboard_input() {
  run_step "restore-runtime" restore_runtime || return 1

  adb shell su -c 'am start -a android.settings.SETTINGS'
  sleep 1

  run_step "scenario3 foreground settings" http_request GET /foreground || return 1
  expect_contains "$LAST_OUTPUT" '"packageName":"com.android.settings"' "scenario3 failed: Settings was not foreground after launch" || return 1

  local clipboard_seed="ghosthand clip path"
  run_step "scenario3 clipboard write" http_request POST /clipboard "{\"text\":\"${clipboard_seed}\"}" || return 1
  expect_contains "$LAST_OUTPUT" '"written":true' "scenario3 failed: clipboard write did not succeed" || return 1

  run_step "scenario3 clipboard read" http_request GET /clipboard || return 1
  expect_contains "$LAST_OUTPUT" "\"text\":\"${clipboard_seed}\"" "scenario3 failed: clipboard read did not return the expected text" || return 1

  local clipboard_text
  clipboard_text="$(extract_json_string "$LAST_OUTPUT" "text")"
  if [[ -z "$clipboard_text" ]]; then
    echo "scenario3 failed: could not extract clipboard text from /clipboard response"
    return 1
  fi

  run_step "scenario3 find search field" http_request POST /find '{"id":"android:id/input"}' || return 1
  expect_contains "$LAST_OUTPUT" '"found":true' "scenario3 failed: Settings search field was not found" || return 1

  local center_x center_y
  center_x="$(extract_json_int "$LAST_OUTPUT" "centerX")"
  center_y="$(extract_json_int "$LAST_OUTPUT" "centerY")"
  if [[ -z "$center_x" || -z "$center_y" ]]; then
    echo "scenario3 failed: could not extract Settings search field coordinates"
    return 1
  fi

  run_step "scenario3 tap search field" http_request POST /tap "{\"x\":${center_x},\"y\":${center_y}}" || return 1
  expect_contains "$LAST_OUTPUT" '"performed":true' "scenario3 failed: tap on Settings search field did not perform" || return 1
  sleep 1

  run_step "scenario3 focused field before input" http_request GET /focused || return 1
  expect_contains "$LAST_OUTPUT" '"available":true' "scenario3 failed: no focused field after tapping the search field" || return 1
  expect_contains "$LAST_OUTPUT" '"resourceId":"android:id\/input"' "scenario3 failed: focused field was not the Settings search input" || return 1

  run_step "scenario3 input clipboard text" http_request POST /input "{\"text\":\"${clipboard_text}\"}" || return 1
  expect_contains "$LAST_OUTPUT" '"performed":true' "scenario3 failed: /input did not perform with clipboard-driven text" || return 1
  expect_contains "$LAST_OUTPUT" "\"text\":\"${clipboard_text}\"" "scenario3 failed: /input response did not reflect the clipboard-driven text" || return 1

  run_step "scenario3 focused field after input" http_request GET /focused || return 1
  expect_contains "$LAST_OUTPUT" '"available":true' "scenario3 failed: focused field disappeared after input" || return 1
  expect_contains "$LAST_OUTPUT" "\"text\":\"${clipboard_text}\"" "scenario3 failed: focused field text did not match the clipboard-driven text" || return 1
}

scenario_notification_navigation() {
  run_step "restore-runtime" restore_runtime || return 1

  run_step "scenario4 go home" http_request POST /home '{}' || return 1
  expect_contains "$LAST_OUTPUT" '"performed":true' "scenario4 failed: /home did not perform" || return 1
  sleep 1

  run_step "scenario4 foreground home" http_request GET /foreground || return 1
  expect_contains "$LAST_OUTPUT" '"packageName":"com.miui.home"' "scenario4 failed: launcher was not foreground before posting the notification" || return 1

  local notification_text="scenario4-nav-$(date +%s)"
  run_step "scenario4 notify post" http_request POST /notify "{\"title\":\"Ghosthand Scenario 04\",\"text\":\"${notification_text}\"}" || return 1
  if [[ "$LAST_OUTPUT" == *'"performed":true'* ]]; then
    :
  else
    expect_contains "$LAST_OUTPUT" '"posted":true' "scenario4 failed: notification post did not succeed" || return 1
  fi

  run_step "scenario4 notify read" http_request GET /notify || return 1
  expect_contains "$LAST_OUTPUT" "${notification_text}" "scenario4 failed: /notify did not return the scenario notification text" || return 1
  expect_contains "$LAST_OUTPUT" '"package":"com.folklore25.ghosthand"' "scenario4 failed: /notify entry package did not match Ghosthand" || return 1
  expect_contains "$LAST_OUTPUT" '"tag":"ghosthand_notify"' "scenario4 failed: /notify entry tag did not match the posted notification" || return 1

  adb shell cmd statusbar expand-notifications
  sleep 1

  run_step "scenario4 find notification" http_request POST /find "{\"text\":\"${notification_text}\",\"clickable\":true}" || return 1
  if [[ "$LAST_OUTPUT" != *'"found":true'* ]]; then
    local shade_xml aggregate_bounds left top right bottom aggregate_center_y
    shade_xml="$(dump_ui_xml | tr '\n' ' ')"
    aggregate_bounds="$(extract_ui_text_bounds "$shade_xml" "不重要通知")"
    if [[ -z "$aggregate_bounds" ]]; then
      echo "scenario4 failed: notification text was not found in the expanded notification shade"
      return 1
    fi
    read -r left top right bottom <<<"$aggregate_bounds"
    aggregate_center_y=$(((top + bottom) / 2))
    adb shell su -c "input tap 540 ${aggregate_center_y}"
    sleep 1

    run_step "scenario4 find notification after aggregate" http_request POST /find "{\"text\":\"${notification_text}\",\"clickable\":true}" || return 1
    expect_contains "$LAST_OUTPUT" '"found":true' "scenario4 failed: notification text was not found after opening the unimportant notifications bucket" || return 1
  fi

  local combined
  combined="$(adb shell '
    rm -f /data/local/tmp/ghosthand-scenario4-wait.out /data/local/tmp/ghosthand-scenario4-click.out
    (
      printf "GET /wait?timeout=5000 HTTP/1.1\r\nHost: 127.0.0.1\r\nConnection: close\r\n\r\n" |
        toybox nc 127.0.0.1 '"${PORT}"' > /data/local/tmp/ghosthand-scenario4-wait.out
    ) &
    sleep 1
    BODY="{\"text\":\"'"${notification_text}"'\",\"clickable\":true}"
    LEN=$(printf "%s" "$BODY" | wc -c | tr -d " ")
    printf "POST /click HTTP/1.1\r\nHost: 127.0.0.1\r\nContent-Type: application/json\r\nContent-Length: %s\r\nConnection: close\r\n\r\n%s" "$LEN" "$BODY" |
      toybox nc 127.0.0.1 '"${PORT}"' > /data/local/tmp/ghosthand-scenario4-click.out
    sleep 6
    echo "== click =="
    cat /data/local/tmp/ghosthand-scenario4-click.out
    echo
    echo "== wait =="
    cat /data/local/tmp/ghosthand-scenario4-wait.out
    echo
  ')"
  printf '%s\n' "$combined"
  expect_contains "$combined" '"performed":true' "scenario4 failed: clicking the notification did not perform" || return 1
  expect_contains "$combined" '"changed":true' "scenario4 failed: /wait did not observe a UI change after notification click" || return 1

  run_step "scenario4 foreground ghosthand" http_request GET /foreground || return 1
  expect_contains "$LAST_OUTPUT" '"packageName":"com.folklore25.ghosthand"' "scenario4 failed: Ghosthand was not foreground after notification click" || return 1
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
  scripts/ghosthand-verify-runtime.sh scenario-settings-search-back
  scripts/ghosthand-verify-runtime.sh scenario-settings-home-screenshot
  scripts/ghosthand-verify-runtime.sh scenario-settings-clipboard-input
  scripts/ghosthand-verify-runtime.sh scenario-notification-navigation
  scripts/ghosthand-verify-runtime.sh all

Modes:
  - smoke: runtime up + foreground + commands preview
  - core: restore runtime, validate contract, screen, tree/find/click, focused input, selector click, clipboard, swipe
  - full: install current build, then run core + screenshot + notify + wait
  - scenario-settings-search-back: Scenario 01, search inside Settings, enter Wi-Fi settings, then back with wait confirmation
  - scenario-settings-home-screenshot: Scenario 02, wait for home transition and confirm it with a screenshot digest change
  - scenario-settings-clipboard-input: Scenario 03, write/read clipboard text, focus the Settings search field, input the clipboard-driven value, and confirm the field content
  - scenario-notification-navigation: Scenario 04, post a unique Ghosthand notification, read it, open the shade, open the MIUI unimportant bucket if needed, click that exact notification, and confirm navigation back into Ghosthand

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
    scenario-settings-search-back)
      run_step "scenario-settings-search-back" scenario_settings_search_back || status=$?
      ;;
    scenario-settings-home-screenshot)
      run_step "scenario-settings-home-screenshot" scenario_settings_home_screenshot || status=$?
      ;;
    scenario-settings-clipboard-input)
      run_step "scenario-settings-clipboard-input" scenario_settings_clipboard_input || status=$?
      ;;
    scenario-notification-navigation)
      run_step "scenario-notification-navigation" scenario_notification_navigation || status=$?
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
