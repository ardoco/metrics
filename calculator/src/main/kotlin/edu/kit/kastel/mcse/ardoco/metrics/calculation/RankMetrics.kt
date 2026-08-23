package edu.kit.kastel.mcse.ardoco.metrics.calculation

/**
 * Calculates the average precision for a given ordered list of ranked results and ground truth.
 * @param similarityList The ranked results for a query as a list of sorted lists (most relevant artifacts first). Each list represents one query of a source artifact.
 * @param groundTruth The ground truth for the query.
 * @return The average precision for the query and whether the gold standard includes a link for this query.
 */
fun calculateAP(
    similarityList: List<String>,
    groundTruth: Set<String>
): Pair<Double, Boolean> {
    var relevantLinksAtK = 0
    var ap = 0.0

    // Calculate precision at each k
    for ((index, id) in similarityList.withIndex()) {
        val relevant = if (groundTruth.contains(id)) 1 else 0
        relevantLinksAtK += relevant
        val precisionAtK: Double = relevantLinksAtK.toDouble() / (index + 1)
        ap += precisionAtK * relevant
    }

    ap = if (relevantLinksAtK == 0) 0.0 else ap / relevantLinksAtK

    return ap to (relevantLinksAtK != 0)
}

/**
 * Calculates the mean average precision for a given set of ranked results and ground truth.
 * We assume that the rankedResults lists contain all possible target artifacts!
 *
 * @param rankedResults The ranked results for each query as a list of sorted lists (most relevant artifacts first). Each list represents one query of a source artifact.
 * @param groundTruth The ground truth for the queries.
 * @return The mean average precision for the queries.
 */
fun calculateMAP(
    rankedResults: Collection<List<String>>,
    groundTruth: Set<String>
): Double {
    var map = 0.0
    var numberOfGroundTruthSourceArtifacts = 0
    for (rankedResult in rankedResults) {
        val (ap, inGroundTruth) = calculateAP(rankedResult, groundTruth)
        if (inGroundTruth) numberOfGroundTruthSourceArtifacts++
        map += ap
    }

    return map / numberOfGroundTruthSourceArtifacts
}

/**
 * Calculates the lag for a given set of ranked results and ground truth.
 *
 * @param rankedResults The ranked results for each query.
 * @param groundTruth The ground truth for the queries.
 * @return The lag for the queries.
 */
fun calculateLAG(
    rankedResults: List<List<String>>,
    groundTruth: Set<String>
): Double {
    var totalRelevantLinks = 0
    var lagSum = 0
    for (rankedResult in rankedResults) {
        var relevantLinksAtIndex = 0
        for ((index, id) in rankedResult.withIndex()) {
            if (groundTruth.contains(id)) {
                relevantLinksAtIndex += 1
                totalRelevantLinks += 1
                lagSum += (index + 1) - relevantLinksAtIndex
            }
        }
    }
    return lagSum.toDouble() / totalRelevantLinks
}

/**
 * Calculates the area under curve (AUC) of the receiver operating characteristic (ROC) for a given set of ranked results, their respective relevances, and ground truth.
 *
 * @param rankedResults The ranked results for each query.
 * @param rankedRelevances The respective relevance values of the ranked results for each query.
 * @param groundTruth The ground truth for the queries.
 */
fun calculateAUC(
    rankedResults: List<List<String>>,
    rankedRelevances: List<List<Double>>,
    groundTruth: Set<String>,
    biggerIsMoreSimilar: Boolean
): Double {
    require(rankedResults.size == rankedRelevances.size) {
        "Results and relevance lists must have the same size."
    }

    val flattenedRelevances = mutableListOf<Double>()
    val flattenedTPLabels = mutableListOf<Boolean>()

    for ((results, relevances) in rankedResults.zip(rankedRelevances)) {
        require(results.size == relevances.size) {
            "Each result list and its corresponding relevance list must have the same size."
        }

        flattenedRelevances += relevances
        flattenedTPLabels += results.map { groundTruth.contains(it) }
    }

    return calculateAUC(calculateROC(flattenedRelevances, flattenedTPLabels, biggerIsMoreSimilar))
}

/**
 * Calculates the ROC (Receiver Operating Characteristic) curve for a set of relevance scores and their corresponding
 * true positive labels.
 *
 * The ROC curve is a graphical plot that illustrates the diagnostic ability of a binary classifier system as its
 * discrimination threshold is varied. This function returns the ROC points as a list of [fpr, tpr] pairs where:
 * - FPR: False Positive Rate
 * - TPR: True Positive Rate
 *
 * @param relevances A list of relevance scores (predicted probabilities or confidence scores) for each instance.
 *                   Higher scores indicate higher likelihood of being a positive instance.
 * @param isTPLabels A list of boolean values indicating whether each instance is a true positive (`true`) or not (`false`).
 *                   The size of this list must match the size of the `relevances` list.
 * @return A list of double arrays where each array contains two elements:
 *         - The first element is the False Positive Rate (FPR)
 *         - The second element is the True Positive Rate (TPR)
 *         These points can be used to plot the ROC curve.
 * @throws IllegalArgumentException if the sizes of `relevances` and `isTPLabels` do not match.
 */
fun calculateROC(
    relevances: List<Double>,
    isTPLabels: List<Boolean>,
    biggerIsMoreSimilar: Boolean
): List<DoubleArray> {
    require(relevances.size == isTPLabels.size) { "Relevances and labels must have the same length" }

    // Create a list of pairs (relevance, isTPLabel) and sort it by relevance in descending order
    val relevanceIsTPList: MutableList<Pair<Double, Boolean>> = relevances.zip(isTPLabels).toMutableList()
    if (biggerIsMoreSimilar) relevanceIsTPList.sortByDescending { it.first } else relevanceIsTPList.sortBy { it.first }

    // Initialize variables for TPR and FPR
    val totalPositives = isTPLabels.count { it }
    val totalNegatives = isTPLabels.size - totalPositives

    var truePositives = 0
    var falsePositives = 0

    val rocPoints: MutableList<DoubleArray> = ArrayList()
    rocPoints.add(doubleArrayOf(0.0, 0.0)) // Start at (0, 0)

    for (pair in relevanceIsTPList) {
        if (pair.second) {
            truePositives++
        } else {
            falsePositives++
        }

        val tpr = truePositives.toDouble() / totalPositives
        val fpr = falsePositives.toDouble() / totalNegatives

        rocPoints.add(doubleArrayOf(fpr, tpr))
    }

    rocPoints.add(doubleArrayOf(1.0, 1.0)) // End at (1, 1)
    return rocPoints
}

/**
 * Calculates the area under curve (AUC) of the receiver operating characteristic (ROC) for a given set of ROC curve points.
 *
 * @param rocPoints A List of points representing the ROC curve
 */
fun calculateAUC(rocPoints: List<DoubleArray>): Double {
    var auc = 0.0

    for (i in 1 until rocPoints.size) {
        val x1 = rocPoints[i - 1][0]
        val y1 = rocPoints[i - 1][1]
        val x2 = rocPoints[i][0]
        val y2 = rocPoints[i][1]

        // Calculate the area of the trapezoid
        auc += (x2 - x1) * (y1 + y2) / 2.0
    }

    return auc
}
