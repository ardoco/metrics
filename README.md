# ARDoCo: Metrics Calculator
Welcome to the **ARDoCo Metrics Calculator** project! This tool provides functionality to calculate and aggregate **classification metrics** for various machine learning tasks.

The [Wiki](https://github.com/ardoco/metrics/wiki) contains all the necessary information to use the **ARDoCo Metrics Calculator** via multiple interfaces, including a library, REST API, and command-line interface (CLI).

## Quickstart

To use this project as a Maven dependency, you need to include the following dependency in your `pom.xml` file:

```xml
<dependency>
	<groupId>io.github.ardoco</groupId>
	<artifactId>metrics</artifactId>
	<version>${revision}</version>
</dependency>
```

To use the CLI run the following command:

```shell
java -jar metrics-cli.jar -h
```

To use the REST API via Docker, start the server with the following command:
```shell
docker run -it -p 8080:8080 ghcr.io/ardoco/metrics:latest
```

## Breaking changes in 0.3.0

* **Rank metrics have been removed.** `RankMetricsCalculator`, the `rank` and `aggRnk` CLI subcommands and the `/rank-metrics` endpoints are gone. Their aggregation was incorrect (the aggregated AUC was a weighted sum rather than a weighted average) and untested.
* **`calculateAverages` returns an object instead of a list.** It now returns a `ClassificationAggregationResult` whose `macroAverage`, `weightedAverage` and `microAverage` are reachable by name, so callers no longer filter a list by `AggregationType`. The REST `/average` response is keyed by those names instead of being a `classificationResults` array.
* **F-beta scores are supported everywhere.** Every result carries a `fbetaScores` map next to `f1`, and the library, the CLI (`-b/--beta`) and the REST API accept the betas to calculate. The F1-score is always included.
* **Aggregations keep more data.** They report the pooled `confusionMatrix`, and the aggregated single results and weights are held once by the `ClassificationAggregationResult`, which also derives the element unions and the spread of a metric on demand.
