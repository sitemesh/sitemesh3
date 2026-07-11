# SiteMesh view-resolver throughput benchmark

A manual A/B harness measuring the end-to-end request cost of the Spring MVC
view-resolver integration: a minimal Spring Boot app serving one Thymeleaf
page decorated via `<meta name="decorator">` with a static decorator.
Decorator selection uses the meta tag deliberately — it behaves identically
across SiteMesh versions, so no version-specific configuration can skew a
comparison.

**This project is intentionally excluded from the root build and must never
run in CI.** Timing numbers from shared runners are meaningless; run it
manually, on an idle machine, and compare configurations against each other —
never treat the absolute numbers as capacity figures.

## Building the jars

From the repository root (one boot jar per configuration):

```bash
./gradlew -p benchmarks bootJar -PsitemeshVersion=3.3.0-M1 \
    && cp benchmarks/build/libs/sitemesh-benchmarks.jar benchmarks/sm-bench-m1.jar
./gradlew -p benchmarks bootJar -PsitemeshVersion=3.3.0-SNAPSHOT \
    && cp benchmarks/build/libs/sitemesh-benchmarks.jar benchmarks/sm-bench-snapshot.jar
./gradlew -p benchmarks bootJar -PwithSitemesh=false \
    && cp benchmarks/build/libs/sitemesh-benchmarks.jar benchmarks/sm-bench-baseline.jar
```

`-PsitemeshVersion` picks the SiteMesh artifacts (snapshots resolve from
`mavenLocal()`, so `./gradlew publishToMavenLocal` first — and **republish
after any code change**, or the snapshot jar bakes in stale bits);
`-PwithSitemesh=false` builds the undecorated baseline.

## Running

The app listens on port 8093 (`src/main/resources/application.properties`) —
make sure it is free.

```bash
cd benchmarks
javac LoadGen.java

java -jar sm-bench-snapshot.jar &
until curl -sf http://localhost:8093/bench > /dev/null; do sleep 1; done

java LoadGen http://localhost:8093/bench 5000 15000 8   # warmup pass (discard)
java LoadGen http://localhost:8093/bench 1000 15000 8   # measured pass (report)
kill %1
```

Same sequence for `sm-bench-m1.jar`. The baseline jar is undecorated, so its
LoadGen calls need the `nocheck` flag:

```bash
java LoadGen http://localhost:8093/bench 5000 15000 8 nocheck
java LoadGen http://localhost:8093/bench 1000 15000 8 nocheck
```

`LoadGen` args: `url warmup-requests measured-requests threads [nocheck]`.
It refuses to measure unless the response actually contains both the page and
decorator markers (pass `nocheck` only for the baseline jar) — a benchmark of
blank error pages is worse than no benchmark. If it prints `NOT DECORATED`
and exits, the run is invalid: check the server's console output for the
cause (e.g. a decorator dispatch error) before rerunning. On success it
prints throughput and p50/p90/p99/max latency.

Protocol notes:

- Repeat the full start → warm → measure → kill cycle at least twice per
  configuration in fresh JVMs; only trust deltas that reproduce with the same
  sign.
- Alternate configuration order between rounds to average out thermal and
  background drift.
- The page is deliberately tiny, which maximizes the visibility of per-request
  fixed costs; real applications dilute any delta measured here.

## Reference numbers

2026-07-11, Apple Silicon laptop, loopback, JDK 25, Boot 4.1.0, 8 threads,
15000 measured requests, warmed (second) rounds:

| Configuration | Throughput | p50 | p99 |
|---|---|---|---|
| No SiteMesh (baseline) | ~32,700 req/s | 222 µs | 547 µs |
| 3.3.0-M1 (wrap-all default) | ~23,700–24,000 req/s | 304–306 µs | 705–748 µs |
| 3.3.0-SNAPSHOT (delegate default) | ~22,900–23,000 req/s | 316 µs | 735–742 µs |

Interpretation: decoration itself costs ~85–95 µs median on this trivial page
in either version (buffering + parse + decorator dispatch dominate); the
delegate-mode default costs ~10 µs median over wrap-all (extra resolver hop,
decoratable-media-type check, duplicate cached lookups under
`ContentNegotiatingViewResolver`), with p99 unchanged. See the "Cost" note on
`SiteMeshDelegatingViewResolver` and the consultation-count test in
`SiteMeshDelegatingViewResolverTest` for the structural pin.

Three-way interleaved session, same day and machine (order M2 → SNAPSHOT →
M1, twice, fresh JVM per cycle):

| Round | 3.3.0-M2 | 3.3.0-SNAPSHOT | 3.3.0-M1 |
|---|---|---|---|
| a | 24,268 req/s / p50 296 µs | 24,254 req/s / p50 297 µs | 25,354 req/s / p50 279 µs |
| b | 24,449 req/s / p50 291 µs | 25,891 req/s / p50 279 µs | 25,324 req/s / p50 282 µs |

Interpretation: SNAPSHOT is at parity with M2 (its two rounds straddle M2 and
M1, so the delegate redesign costs nothing measurable against the version it
replaces); M1 measures ~3–4% faster than both consistently, i.e. that delta
arrived with M2's dispatch/status-handling work, not with the 3.3.0 redesign.
Single rounds can swing ~5% (see SNAPSHOT a vs b) — never conclude from one.
