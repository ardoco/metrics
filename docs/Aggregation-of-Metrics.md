In addition to calculating individual metrics for a classification task, the system supports the **aggregation** of results across multiple classifications. Aggregation methods allow users to compute overall metrics that represent the combined performance of several tasks.

## Aggregation Types

The following **Aggregation Types** are supported:

1. **Macro Average**: Computes the average of the metrics of each single result, giving equal weight to each.
   - **Use Case**: Useful when all tasks are equally important, regardless of how many instances belong to each task.

2. **Weighted Average**: The average is computed with weights, by default proportional to the size of the gold standard of each task (`TP + FN`).
   - **Use Case**: Useful when certain tasks are more important and should contribute more to the overall metrics.

3. **Micro Average**: Pools the true positives, false positives, false negatives and true negatives across all tasks, then computes the metrics from those pooled counts.
   - **Use Case**: Useful when tasks have an uneven number of instances, and you want to prioritise overall accuracy over individual task performance.

## The Aggregation Result

`calculateAverages` returns a single **`ClassificationAggregationResult`**. The three aggregations are reachable by name, so there is no list to filter:

```kotlin
val aggregation = calculator.calculateAverages(results)

aggregation.macroAverage.f1
aggregation.weightedAverage.precision
aggregation.microAverage.fbetaScores[2.0]

// or by type, if the type is only known at runtime
aggregation[AggregationType.MICRO_AVERAGE].recall
// or all three at once, in the order macro, weighted, micro
aggregation.asList().forEach { it.prettyPrint() }
```

Each of the three is an **`AggregatedClassificationResult`** carrying its `type`, the pooled `confusionMatrix` and the aggregated metrics:

- **Precision**
- **Recall**
- **F1-Score** and all aggregated **F-beta scores** (`fbetaScores`)
- **Accuracy (if available)**
- **Specificity (if available)**
- **Phi Coefficient (if available)**
- **Phi Coefficient Max (if available)**
- **Phi Over Phi Max (if available)**

The metrics requiring true negatives are only available if a confusion matrix sum was provided. All aggregated results must agree on this: aggregating results where some have a confusion matrix sum and others do not is rejected.

## What the Aggregation Keeps

The aggregated single results and the weights are held **once** by the `ClassificationAggregationResult` rather than once per aggregation type. Everything that can be derived from them is provided on demand instead of being stored:

| Member | Meaning |
| --- | --- |
| `singleResults` | The results that were aggregated, with their element sets intact |
| `weights` | The weights used for `weightedAverage`, in the order of `singleResults` |
| `confusionMatrix` | TP/FP/FN/TN pooled over all single results &ndash; the basis of the micro average |
| `betas` | The betas of the F-beta scores present in every aggregation |
| `truePositives()`, `falsePositives()`, `falseNegatives()` | The unions of the classified elements across all single results |
| `spread(metric)` | The min, max, mean and standard deviation of one metric across the single results; `null` if that metric is unavailable |
| `fbetaSpread(beta)` | The same for the F-beta score of the given beta |

`asList()` and the union and spread accessors are deliberately **functions** rather than properties, so that they do not end up duplicated in the serialized JSON.

## Aggregating F-beta scores

Macro and weighted averages are the (weighted) mean of the F-beta scores of the single results. The micro average is recalculated from the pooled confusion matrix. These are genuinely different numbers, and neither is the F-beta score of the averaged precision and recall &ndash; do not compute it that way.

By default the betas to aggregate are the union of the betas of the given results, so the aggregation covers everything that was calculated. You may also pass an explicit set of betas, including betas that no single result stored: a per-result F-beta score is always exactly recoverable from that result's own precision and recall.

An **aggregation itself** cannot answer for a beta it was not built with, because averaged precision and recall are not enough to reconstruct an averaged F-beta score. `fbetaOrNull` therefore returns `null` for such a beta rather than a plausible-looking wrong value.

**Example:**
If you perform multiple classification tasks and want a single precision or recall score, the **macro average** treats each task equally, the **weighted average** accounts for the number of instances in each task, and the **micro average** treats every classified instance equally regardless of which task it came from.
