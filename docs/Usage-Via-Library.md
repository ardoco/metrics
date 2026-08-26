The project can be integrated into your own system as a library to calculate classification metrics. Follow the steps below to use the metrics calculator within your code.

## 1. Adding the Dependency

To use this project as a Maven dependency, you need to include the following dependency in your `pom.xml` file:

```xml
<dependency>
    <groupId>io.github.ardoco</groupId>
    <artifactId>metrics</artifactId>
    <version>${revision}</version>
</dependency>
```

Make sure to replace `${revision}` with the appropriate version number of the library. You can find the version from the repository or Maven Central.

### Optional: Snapshot Repository

If you are using a snapshot version of the library (like `0.3.0-SNAPSHOT`), you will need to include the **snapshot repository** configuration in your `pom.xml` file. This enables Maven to fetch the latest snapshot build:

```xml
<repositories>
    <repository>
        <id>mavenSnapshot</id>
        <url>https://central.sonatype.com/repository/maven-snapshots/</url>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </repository>
</repositories>
```

## 2. Importing the Metrics Calculator

Once the library is included, you can import and use the **ClassificationMetricsCalculator** in your project.

<details>
<summary>Kotlin example</summary>

```kotlin
import edu.kit.kastel.mcse.ardoco.metrics.ClassificationMetricsCalculator
import edu.kit.kastel.mcse.ardoco.metrics.result.SingleClassificationResult

fun main() {
    val classification = setOf("A", "B", "C")
    val groundTruth = setOf("A", "C", "D")

    // Use the ClassificationMetricsCalculator to calculate metrics
    val calculator = ClassificationMetricsCalculator.Instance
    val result: SingleClassificationResult<String> = calculator.calculateMetrics(
        classification = classification,
        groundTruth = groundTruth,
        confusionMatrixSum = null
    )

    result.prettyPrint()  // Logs precision, recall, F1 score, etc.
}
```

</details>
<details open>
<summary>Java example</summary>

```java
import edu.kit.kastel.mcse.ardoco.metrics.ClassificationMetricsCalculator;
import edu.kit.kastel.mcse.ardoco.metrics.result.SingleClassificationResult;

import java.util.Set;

public class ClassificationExample {
    public static void main(String[] args) {
        Set<String> classification = Set.of("A", "B", "C");
        Set<String> groundTruth = Set.of("A", "C", "D");

        // Use the ClassificationMetricsCalculator to calculate metrics
        ClassificationMetricsCalculator calculator = ClassificationMetricsCalculator.getInstance();
        SingleClassificationResult<String> result = calculator.calculateMetrics(
                classification,
                groundTruth,
                null  // Confusion matrix sum (optional)
        );

        // Print the result, which includes precision, recall, F1 score, etc.
        result.prettyPrint();
    }
}
```
</details>

## 3. F-beta Scores

Every `calculateMetrics` overload has a variant that takes the betas of the [F-beta scores](Classification-Metrics) to calculate. The F1-score is always included, duplicates are dropped and the scores are keyed by beta in ascending order. Betas have to be finite and greater than 0.

<details>
<summary>Kotlin example</summary>

```kotlin
val result = calculator.calculateMetrics(classification, groundTruth, 20, listOf(0.5, 2.0))

result.f1                    // always available
result.fbetaScores           // {0.5=..., 1.0=..., 2.0=...}
result.fbeta(3.0)            // not requested, but recalculated exactly from precision and recall
result.fbetaOrNull(3.0)      // null, because it was not requested
result.confusionMatrix       // the counts the metrics were derived from
```

</details>
<details open>
<summary>Java example</summary>

```java
SingleClassificationResult<String> result =
        calculator.calculateMetrics(classification, groundTruth, 20, List.of(0.5, 2.0));

result.getF1();                 // always available
result.getFbetaScores();        // {0.5=..., 1.0=..., 2.0=...}
result.fbeta(3.0);              // not requested, but recalculated exactly from precision and recall
result.fbetaOrNull(3.0);        // null, because it was not requested
result.getConfusionMatrix();    // the counts the metrics were derived from
```
</details>

## 4. Customizing the Calculations

The calculator also accepts a **string provider**, which lets you specify how the elements of your classification are converted to strings before they are compared:

```kotlin
val result = calculator.calculateMetrics(
    classification = myLinks,
    groundTruth = myGoldStandard,
    stringProvider = { it.id },
    confusionMatrixSum = 20,
    betas = listOf(2.0)
)
```

## 5. Aggregation of Results

`calculateAverages` aggregates multiple single results into one `ClassificationAggregationResult`, which exposes the macro, the weighted and the micro average by name. See [Aggregation of Metrics](Aggregation-of-Metrics) for the semantics.

<details>
<summary>Kotlin example</summary>

```kotlin
import edu.kit.kastel.mcse.ardoco.metrics.ClassificationMetricsCalculator
import edu.kit.kastel.mcse.ardoco.metrics.result.AggregationType
import edu.kit.kastel.mcse.ardoco.metrics.result.ClassificationMetric

fun main() {
    val calculator = ClassificationMetricsCalculator.Instance

    val first = calculator.calculateMetrics(setOf("A", "B", "C", "D", "E"), setOf("A", "B"), 20, listOf(0.5, 2.0))
    val second = calculator.calculateMetrics(setOf("F"), setOf("F", "G", "H"), 20, listOf(0.5, 2.0))

    // weights = null uses the size of the gold standard of each result, betas = null uses the betas of the results
    val aggregation = calculator.calculateAverages(listOf(first, second), null, null)

    aggregation.macroAverage.f1                          // 0.5357142857142858
    aggregation.weightedAverage.precision                // 0.76
    aggregation.microAverage.fbetaScores.getValue(2.0)   // 0.5769230769230769

    aggregation[AggregationType.MICRO_AVERAGE].recall    // by type, if only known at runtime
    aggregation.confusionMatrix                          // pooled TP/FP/FN/TN over all results
    aggregation.truePositives()                          // union of the true positives
    aggregation.spread(ClassificationMetric.PRECISION)   // min, max, mean and standard deviation
    aggregation.fbetaSpread(2.0)                         // the same for the F2 score

    aggregation.prettyPrint()                            // logs all three aggregations
}
```

</details>
<details open>
<summary>Java example</summary>

```java
import edu.kit.kastel.mcse.ardoco.metrics.ClassificationMetricsCalculator;
import edu.kit.kastel.mcse.ardoco.metrics.result.AggregationType;
import edu.kit.kastel.mcse.ardoco.metrics.result.ClassificationAggregationResult;
import edu.kit.kastel.mcse.ardoco.metrics.result.ClassificationMetric;
import edu.kit.kastel.mcse.ardoco.metrics.result.SingleClassificationResult;

import java.util.List;
import java.util.Set;

public class AggregationExample {
    public static void main(String[] args) {
        ClassificationMetricsCalculator calculator = ClassificationMetricsCalculator.getInstance();

        SingleClassificationResult<String> first =
                calculator.calculateMetrics(Set.of("A", "B", "C", "D", "E"), Set.of("A", "B"), 20, List.of(0.5, 2.0));
        SingleClassificationResult<String> second =
                calculator.calculateMetrics(Set.of("F"), Set.of("F", "G", "H"), 20, List.of(0.5, 2.0));

        // weights = null uses the size of the gold standard of each result, betas = null uses the betas of the results
        ClassificationAggregationResult<String> aggregation =
                calculator.calculateAverages(List.of(first, second), null, null);

        aggregation.getMacroAverage().getF1();                          // 0.5357142857142858
        aggregation.getWeightedAverage().getPrecision();                // 0.76
        aggregation.getMicroAverage().getFbetaScores().get(2.0);        // 0.5769230769230769

        aggregation.get(AggregationType.MICRO_AVERAGE).getRecall();     // by type, if only known at runtime
        aggregation.getConfusionMatrix();                               // pooled TP/FP/FN/TN over all results
        aggregation.truePositives();                                    // union of the true positives
        aggregation.spread(ClassificationMetric.PRECISION);             // min, max, mean and standard deviation
        aggregation.fbetaSpread(2.0);                                   // the same for the F2 score

        aggregation.prettyPrint();                                      // logs all three aggregations
    }
}
```
</details>
