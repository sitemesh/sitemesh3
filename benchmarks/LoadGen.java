/*
 *    Copyright 2009-2026 SiteMesh authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/** Tiny fixed-concurrency load generator: prints throughput and latency percentiles. */
public class LoadGen {
    public static void main(String[] args) throws Exception {
        String url = args[0];
        int warmup = Integer.parseInt(args[1]);
        int requests = Integer.parseInt(args[2]);
        int threads = Integer.parseInt(args[3]);

        HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().build();

        boolean expectDecorated = args.length < 5 || !"nocheck".equals(args[4]);
        // sanity: the response must actually be decorated (unless baseline)
        String body = client.send(req, HttpResponse.BodyHandlers.ofString()).body();
        if (expectDecorated && (!body.contains("DECORATOR-MARKER") || !body.contains("BODY-MARKER"))) {
            System.err.println("NOT DECORATED:\n" + body);
            System.exit(2);
        }

        run(client, req, warmup, threads, null);
        long[] latencies = new long[requests];
        long start = System.nanoTime();
        run(client, req, requests, threads, latencies);
        long totalNanos = System.nanoTime() - start;

        Arrays.sort(latencies);
        double seconds = totalNanos / 1e9;
        System.out.printf("requests=%d threads=%d seconds=%.2f throughput=%.0f req/s%n",
                requests, threads, seconds, requests / seconds);
        System.out.printf("latency_us p50=%d p90=%d p99=%d max=%d%n",
                latencies[requests / 2] / 1000, latencies[(int) (requests * 0.9)] / 1000,
                latencies[(int) (requests * 0.99)] / 1000, latencies[requests - 1] / 1000);
    }

    private static void run(HttpClient client, HttpRequest req, int total, int threads, long[] latencies)
            throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        AtomicInteger next = new AtomicInteger();
        CountDownLatch done = new CountDownLatch(threads);
        for (int t = 0; t < threads; t++) {
            pool.execute(() -> {
                try {
                    int i;
                    while ((i = next.getAndIncrement()) < total) {
                        long begin = System.nanoTime();
                        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                        long elapsed = System.nanoTime() - begin;
                        if (resp.statusCode() != 200 || resp.body().length() < 50) {
                            System.err.println("bad response: " + resp.statusCode());
                            System.exit(3);
                        }
                        if (latencies != null) {
                            latencies[i] = elapsed;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(4);
                } finally {
                    done.countDown();
                }
            });
        }
        done.await();
        pool.shutdown();
    }
}
