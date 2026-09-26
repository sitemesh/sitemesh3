#!/usr/bin/env bash
#
# Boots each example on each of its containers and checks that pages come back
# decorated (or deliberately undecorated). CLAUDE.md asks for exactly this, on
# Tomcat and Jetty, whenever response buffering, dispatch or content-type
# handling changes; CI runs it too.
#
# Usage:   examples/smoke-test.sh [example...]
#          examples: hellowebapp springboot struts javalin micronaut (default: all)
# Needs:   bash, curl, lsof. Micronaut needs a JDK 25 toolchain (Gradle provisions one).
# Logs:    build/smoke-test/<run>.log
#
set -u
cd "$(dirname "$0")/.."

LOG_DIR=build/smoke-test
STARTUP_TIMEOUT=${SMOKE_STARTUP_TIMEOUT:-300}
mkdir -p "$LOG_DIR"

failures=0
run=""
port=""
gradle_pid=""

fail() {
    echo "  FAIL $*"
    failures=$((failures + 1))
}

# start <run-name> <port> <ready-path> <gradle args...>
start() {
    run=$1 port=$2
    local ready=$3
    shift 3
    echo "== $run"
    if lsof -ti "tcp:$port" -sTCP:LISTEN >/dev/null 2>&1; then
        fail "$run: port $port is already in use"
        return 1
    fi
    # Gretty stops when "any key" is pressed, and stdin at EOF counts, so feed
    # Gradle a FIFO that stays open until stop() closes it.
    local fifo="$LOG_DIR/$run.stdin"
    rm -f "$fifo" && mkfifo "$fifo"
    ./gradlew --no-configuration-cache "$@" < "$fifo" > "$LOG_DIR/$run.log" 2>&1 &
    gradle_pid=$!
    exec 3> "$fifo"

    local waited=0 code
    while [ "$waited" -lt "$STARTUP_TIMEOUT" ]; do
        # Only a 2xx/3xx counts: Tomcat answers 404 until the app is deployed.
        code=$(curl -s -o /dev/null -w '%{http_code}' "http://localhost:$port$ready")
        case "$code" in 2*|3*) return 0 ;; esac
        if ! kill -0 "$gradle_pid" 2>/dev/null; then
            fail "$run: Gradle exited before the app started (see $LOG_DIR/$run.log)"
            stop
            return 1
        fi
        sleep 2
        waited=$((waited + 2))
    done
    fail "$run: not ready after ${STARTUP_TIMEOUT}s (see $LOG_DIR/$run.log)"
    stop
    return 1
}

stop() {
    local pid
    for pid in $(lsof -ti "tcp:$port" -sTCP:LISTEN 2>/dev/null); do
        kill "$pid" 2>/dev/null
    done
    exec 3>&-
    kill "$gradle_pid" 2>/dev/null
    wait "$gradle_pid" 2>/dev/null
    local waited=0
    while lsof -ti "tcp:$port" -sTCP:LISTEN >/dev/null 2>&1 && [ "$waited" -lt 30 ]; do
        sleep 1
        waited=$((waited + 1))
    done
    rm -f "$LOG_DIR/$run.stdin"
}

# check <path> <status> <expectation>
#   "text"           the body must contain text (e.g. a decorator's <title> prefix)
#   "!text"          the body must not contain text
#   "type:mime/type" the Content-Type must start with mime/type
check() {
    local path=$1 status=$2 expect=$3
    local body headers code
    body=$(mktemp) headers=$(mktemp)
    curl -s -D "$headers" -o "$body" "http://localhost:$port$path"
    code=$(head -1 "$headers" | awk '{print $2}')
    if [ "$code" != "$status" ]; then
        fail "$run $path: status $code, expected $status"
    else
        case "$expect" in
            type:*)
                grep -qi "^content-type: ${expect#type:}" "$headers" \
                    || fail "$run $path: content type is not ${expect#type:}" ;;
            !*)
                ! grep -qF -- "${expect#!}" "$body" \
                    || fail "$run $path: unexpectedly contains '${expect#!}'" ;;
            *)
                grep -qF -- "$expect" "$body" \
                    || fail "$run $path: does not contain '$expect'" ;;
        esac
    fi
    rm -f "$body" "$headers"
}

hellowebapp() {
    local c
    for c in tomcat jetty; do
        start "hellowebapp-$c" 18080 /index.html ":examples:hellowebapp:${c}Run" -PhttpPort=18080 || continue
        check /index.html 200 "<title>SiteMesh Example: Hello World</title>"
        check /simple.jsp 200 "<title>SiteMesh Example:"
        check /demo.jsp 200 "<title>SiteMesh Example: Hello World (Dynamic)</title>"
        check /Pretty/looking.jsp 200 "<title>Bootstrap Demo</title>"
        check /assets/logo.png 200 "type:image/png"
        stop
    done
}

springboot() {
    local c mode args
    for c in tomcat jetty; do
        for mode in view-resolver filter; do
            args=(":examples:springboot:bootRun" "-Pcontainer=$c")
            [ "$mode" = filter ] && args+=("--args=--sitemesh.integration=filter")
            start "springboot-$c-$mode" 8080 /index.html "${args[@]}" || continue
            check /greeting 200 "<title>SiteMesh Example: JSP</title>"
            check /greeting/th 200 "<title>SiteMesh Example: Thymeleaf</title>"
            check /greeting/ftl 200 "<title>SiteMesh Example: Freemarker</title>"
            check "/greeting/th?name=W%C3%B6rld" 200 "Wörld"
            check /assets/logo.png 200 "type:image/png"
            if [ "$mode" = filter ]; then
                check /index.html 200 "<title>SiteMesh Example: Static Web Page Example</title>"
            else
                # The view-resolver integration decorates views only, not static files.
                check /index.html 200 "!<title>SiteMesh Example:"
            fi
            stop
        done
    done
}

struts() {
    local c
    for c in tomcat jetty; do
        start "struts-$c" 18080 /index.html ":examples:struts:${c}Run" -PhttpPort=18080 || continue
        check /hello.action 200 "<title>SiteMesh Struts Example: Hello World</title>"
        check /alternative.action 200 "<title>Alternative: Alternative decorator</title>"
        stop
    done
}

javalin() {
    start javalin 7070 / ":examples:javalin:run" || return
    check / 200 "<title>SiteMesh Example: Hello Scott</title>"
    stop
}

micronaut() {
    start micronaut 8080 / ":examples:micronaut:run" || return
    check / 200 "<title>SiteMesh Micronaut Example: Hello World</title>"
    check /meta 200 "<title>Alternative: Meta-tag override</title>"
    stop
}

examples=("$@")
[ ${#examples[@]} -eq 0 ] && examples=(hellowebapp springboot struts javalin micronaut)
for example in "${examples[@]}"; do
    case "$example" in
        hellowebapp|springboot|struts|javalin|micronaut) "$example" ;;
        *) echo "Unknown example: $example"; exit 2 ;;
    esac
done

if [ "$failures" -gt 0 ]; then
    echo "$failures check(s) failed"
    exit 1
fi
echo "All example checks passed"
