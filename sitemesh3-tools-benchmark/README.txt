These microbenchmarks use the Japex tool.
https://japex.dev.java.net/

In a nutshell:
- A 'Driver' class defines the code to be executed in the benchmark.
- The configuration for each micro-benchmark suite lives in a config file
  in src/benchmark/config. This consists of the drivers to use and
  the input data.
- The Japex tool runs the benchmarks and outputs the results.

TODO: How to run this from Maven?
