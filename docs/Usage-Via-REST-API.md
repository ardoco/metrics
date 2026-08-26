The metrics calculator provides a REST API that allows users to calculate classification metrics by sending requests with their data. The API is built using Spring Boot and offers endpoints for calculating metrics for a single project as well as aggregating results across projects.

## Base URL

By default, the API runs on port **8080**, and all endpoints are accessible under the base URL:

```
http://localhost:8080/api
```

## API Documentation via Swagger

The REST API provides a **Swagger UI** that allows you to easily explore and test the API endpoints. Swagger generates interactive API documentation and can be accessed from a web browser.

**Swagger URL:**
```
http://localhost:8080/swagger-ui/index.html
```

Through Swagger, you can:
- View all available endpoints.
- See detailed descriptions of the request and response formats.
- Test API calls directly from the browser.

## Endpoints

### 1. Classification Metrics

You can calculate classification metrics by sending data to the classification API.

**Endpoint:**
```
POST /classification-metrics
```

**Request Body Example:**

```json
{
  "classification": ["A", "B", "C", "D", "E"],
  "groundTruth": ["A", "B"],
  "confusionMatrixSum": 20,
  "betas": [0.5, 2.0]
}
```

`confusionMatrixSum` and `betas` are optional. Without a confusion matrix sum the metrics that need true negatives are `null`. Without `betas` only the F1-score is calculated; beta 1.0 is always included and every beta has to be finite and greater than 0.

**Response Example:**

```json
{
  "truePositives": ["A", "B"],
  "falsePositives": ["C", "D", "E"],
  "falseNegatives": [],
  "trueNegatives": 15,
  "precision": 0.4,
  "recall": 1.0,
  "f1": 0.5714285714285715,
  "fbetaScores": {
    "0.5": 0.45454545454545453,
    "1.0": 0.5714285714285715,
    "2.0": 0.7692307692307692
  },
  "accuracy": 0.85,
  "specificity": 0.8333333333333334,
  "phiCoefficient": 0.5773502691896257,
  "phiCoefficientMax": 0.5773502691896257,
  "phiOverPhiMax": 1.0,
  "confusionMatrix": {
    "truePositives": 2,
    "falsePositives": 3,
    "falseNegatives": 0,
    "trueNegatives": 15
  }
}
```

#### Aggregation of Classification Metrics

You can also aggregate multiple classification results into one by sending the following request:

**Endpoint:**
```
POST /classification-metrics/average
```

**Request Body Example:**

```json
{
  "classificationMetricsRequests": [
    {
      "classification": ["A", "B", "C", "D", "E"],
      "groundTruth": ["A", "B"],
      "confusionMatrixSum": 20
    },
    {
      "classification": ["F"],
      "groundTruth": ["F", "G", "H"],
      "confusionMatrixSum": 20
    }
  ],
  "weights": [2, 3],
  "betas": [0.5, 2.0]
}
```

`weights` and `betas` are optional. Without weights the size of the gold standard of each request is used; there has to be exactly one weight per request. The betas apply to all requests and therefore have to be given on the **request level** &ndash; a `betas` field inside a single `classificationMetricsRequests` entry is rejected rather than silently ignored.

**Response Example:**

The response is the aggregation itself, so the three averages are reachable by name instead of having to be filtered out of an array. `singleResults` holds the full input results and is abbreviated here.

```json
{
  "singleResults": ["..."],
  "weights": [2, 3],
  "macroAverage": {
    "type": "MACRO_AVERAGE",
    "confusionMatrix": {
      "truePositives": 3,
      "falsePositives": 3,
      "falseNegatives": 2,
      "trueNegatives": 32
    },
    "precision": 0.7,
    "recall": 0.6666666666666666,
    "f1": 0.5357142857142858,
    "fbetaScores": {
      "0.5": 0.5844155844155844,
      "1.0": 0.5357142857142858,
      "2.0": 0.5769230769230769
    },
    "accuracy": 0.875,
    "specificity": 0.9166666666666667,
    "phiCoefficient": 0.5617344752311879,
    "phiCoefficientMax": 0.5617344752311879,
    "phiOverPhiMax": 1.0
  },
  "weightedAverage": {
    "type": "WEIGHTED_AVERAGE",
    "precision": 0.76,
    "recall": 0.6,
    "f1": 0.5285714285714287,
    "fbetaScores": {
      "0.5": 0.6103896103896104,
      "1.0": 0.5285714285714287,
      "2.0": 0.5384615384615384
    }
  },
  "microAverage": {
    "type": "MICRO_AVERAGE",
    "precision": 0.5,
    "recall": 0.6,
    "f1": 0.5454545454545454,
    "fbetaScores": {
      "0.5": 0.5172413793103449,
      "1.0": 0.5454545454545454,
      "2.0": 0.5769230769230769
    }
  },
  "confusionMatrix": {
    "truePositives": 3,
    "falsePositives": 3,
    "falseNegatives": 2,
    "trueNegatives": 32
  },
  "betas": [0.5, 1.0, 2.0]
}
```

See [Aggregation of Metrics](Aggregation-of-Metrics) for what the three averages mean and how F-beta scores are aggregated.

### 2. Error Responses

Invalid input is answered with **400 Bad Request** and the reason as the body, for example:

- a beta that is not a finite number greater than 0,
- a `weights` array whose length does not match the number of requests,
- a `confusionMatrixSum` smaller than the number of classified and expected elements,
- a mix of requests with and without a `confusionMatrixSum` on the `/average` endpoint,
- `betas` given inside a single request on the `/average` endpoint.
