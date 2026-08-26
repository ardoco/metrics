The classification metrics calculator is responsible for computing various classification performance metrics based on input classifications and ground truth data.

## Input

1. **Classification**: A set of classified elements.
2. **Ground Truth**: A set representing the actual classification labels for comparison.
3. **String Provider Function (optional)**: A function that converts classification and ground truth elements into string representations for comparison purposes.
4. **Confusion Matrix Sum (optional)**: The sum of the confusion matrix values (true positives, false positives, etc.). Some metrics may not be calculated if this is not provided. It must be at least the number of classified and expected elements, otherwise the number of true negatives would be negative.
5. **Betas (optional)**: The betas of the [F-beta scores](#f-beta-scores-optional) to calculate. The F1-score (beta 1.0) is always calculated.

:warning: Classification result entries have to match entries in the ground truth (equals) 

## Supported Metrics

The system calculates a variety of standard classification metrics:

1. **Precision**: Measures the accuracy of the positive predictions.

   $$\text{Precision} = \frac{TP}{TP + FP}$$

   Where:
   - \( TP \) is the number of true positives.
   - \( FP \) is the number of false positives.

2. **Recall**: Also known as sensitivity, recall measures the ability to find all positive instances.

   $$\text{Recall} = \frac{TP}{TP + FN}$$

   Where:
   - \( FN \) is the number of false negatives.

3. **F1-Score**: A harmonic mean of precision and recall, providing a single score that balances both concerns.

   $$F1 = 2 \times \frac{\text{Precision} \times \text{Recall}}{\text{Precision} + \text{Recall}}$$

### F-beta scores (optional)

The **F-beta score** generalises the F1-score by weighting recall relative to precision. `beta < 1` weighs precision higher, `beta > 1` weighs recall higher, and `beta = 1` is exactly the F1-score.

$$F_\beta = (1 + \beta^2) \times \frac{\text{Precision} \times \text{Recall}}{\beta^2 \times \text{Precision} + \text{Recall}}$$

Every result exposes all calculated scores as a map keyed by beta (`fbetaScores`), and beta 1.0 is always present so `f1` is always available. Betas must be finite and greater than 0. A score for a beta that was not requested can still be obtained from a single result &ndash; it is recalculated from that result's precision and recall, which gives the exact same value.

:warning: For an **aggregation** this is not possible: see [Aggregation of Metrics](Aggregation-of-Metrics) for why the betas to aggregate have to be chosen up front.

4. **Accuracy (optional)**: Measures the proportion of correctly predicted instances (if true negatives are provided).

   $$\text{Accuracy} = \frac{TP + TN}{TP + TN + FP + FN}$$

5. **Specificity (optional)**: Also called true negative rate, it measures the proportion of actual negatives that are correctly identified.

   $$\text{Specificity} = \frac{TN}{TN + FP}$$

6. **Phi Coefficient (optional)**: A measure of the degree of association between two binary variables.

   $$\Phi = \frac{TP \times TN - FP \times FN}{\sqrt{(TP + FP)(TP + FN)(TN + FP)(TN + FN)}}$$

7. **Phi Coefficient Max (optional)**: The maximum possible value for the phi coefficient.

8. **Phi Over Phi Max (optional)**: The ratio of the phi coefficient to its maximum possible value.

## Confusion Matrix

Every result also exposes a `confusionMatrix`, i.e. the number of true positives, false positives, false negatives and &ndash; if a confusion matrix sum was provided &ndash; true negatives. For a single result these are the sizes of the corresponding element sets; for an aggregation they are the counts pooled over all aggregated results.

For a single result, and for the micro average of an aggregation, the metrics are exactly the metrics of that matrix. For the macro and the weighted average they are not &ndash; see [Aggregation of Metrics](Aggregation-of-Metrics).

Note that precision returns `1.0` when nothing was classified (`TP + FP = 0`) and recall returns `1.0` when the ground truth is empty (`TP + FN = 0`), because in those cases nothing was classified wrongly respectively nothing was missed. Specificity and accuracy follow the same convention for an empty confusion matrix.

No metric is ever `NaN` or infinite. That matters beyond taste: JSON has no literal for those values, so they would be serialized as the *strings* `"NaN"` and `"Infinity"` and break the `number` type that the REST schema declares.

Each result includes a human-readable format that logs the computed metrics for ease of debugging and verification.


