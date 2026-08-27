The metrics calculator provides a command-line interface (CLI) that allows users to calculate classification metrics as well as aggregate the results from multiple inputs.

## Running the CLI

To run the CLI, use the following command:

```bash
java -jar metrics-cli.jar <command> [options]
```

Each command has specific options and input files required to perform the desired calculation.

The process exits with status `0` on success and a non-zero status on failure &ndash; `1` when a command could not complete (a missing input file, an invalid option value, an unreadable result file) and `2` for a usage error such as an unknown subcommand. Scripts can therefore branch on the exit status instead of parsing the output.

## Commands

### 1. **Classification Metrics Command**

This command calculates classification metrics for a single classification task.

**Command:**
```
classification
```

**Options:**
- `-c, --classification <file>`: The file containing the classified items.
- `-g, --ground-truth <file>`: The file containing the ground truth items.
- `--header`: (Optional) Indicates that the input files have a header.
- `-s, --sum <Int>`: (Optional) The sum of the confusion matrix.
- `-b, --beta <beta>`: (Optional) Betas of additional F-beta scores. Repeatable (`-b 0.5 -b 2`) or comma-separated (`-b 0.5,2`). The F1-score is always calculated.
- `-o, --output <file>`: (Optional) The output file to store the results.


**Example Usage:**
```bash
java -jar metrics-cli.jar classification -c classified.txt -g ground_truth.txt --header -s 20 -b 0.5,2 -o result.json
```

This command reads the classification and ground truth data, computes metrics like precision, recall, F1-score and the requested F-beta scores, and saves the result to `result.json` if specified:

```json
{
  "truePositives" : [ "A", "B" ],
  "falsePositives" : [ "C", "D", "E" ],
  "falseNegatives" : [ ],
  "trueNegatives" : 15,
  "precision" : 0.4,
  "recall" : 1.0,
  "f1" : 0.5714285714285715,
  "fbetaScores" : {
    "0.5" : 0.45454545454545453,
    "1.0" : 0.5714285714285715,
    "2.0" : 0.7692307692307692
  },
  "accuracy" : 0.85,
  "specificity" : 0.8333333333333334,
  "phiCoefficient" : 0.5773502691896257,
  "phiCoefficientMax" : 0.5773502691896257,
  "phiOverPhiMax" : 1.0,
  "confusionMatrix" : {
    "truePositives" : 2,
    "falsePositives" : 3,
    "falseNegatives" : 0,
    "trueNegatives" : 15
  }
}
```

### 2. **Aggregation of Classification Metrics**

This command aggregates multiple classification results into one, calculating the macro, the weighted and the micro average.

**Command:**
```
aggCl
```

**Options:**
- `-d, --directory <directory>`: The directory with the result json files.
- `-o, --output <file>`: (Optional) The output file to store the results.

There is no `-b` option here: the F-beta scores to aggregate are taken from the result files. Result files written by version 0.2.x (which only contain `f1`) are still readable.

The files are read in alphabetical order by file name, so `singleResults` and `weights` are in a reproducible order regardless of platform.

Every non-hidden file in the directory has to be a classification result. One that is not &ndash; a stray note, or the `-o` output of an earlier run written back into the input directory &ndash; is reported by name and the command exits with status `1`, rather than aggregating over a silently smaller set of results. Hidden files such as `.DS_Store` are skipped, so the directory does not have to be cleaned up first.

**Example Usage:**
```bash
java -jar metrics-cli.jar aggCl -d classifiedDir/ -o aggregated_result.json
```

The output contains the aggregated single results once, the weights, and the three aggregations by name (abbreviated here &ndash; `singleResults` holds the full input results):

```json
{
  "singleResults" : [ "..." ],
  "weights" : [ 2, 3 ],
  "macroAverage" : {
    "type" : "MACRO_AVERAGE",
    "confusionMatrix" : {
      "truePositives" : 3,
      "falsePositives" : 3,
      "falseNegatives" : 2,
      "trueNegatives" : 32
    },
    "precision" : 0.7,
    "recall" : 0.6666666666666666,
    "f1" : 0.5357142857142858,
    "fbetaScores" : {
      "0.5" : 0.5844155844155844,
      "1.0" : 0.5357142857142858,
      "2.0" : 0.5769230769230769
    },
    "accuracy" : 0.875,
    "specificity" : 0.9166666666666667,
    "phiCoefficient" : 0.5617344752311879,
    "phiCoefficientMax" : 0.5617344752311879,
    "phiOverPhiMax" : 1.0
  },
  "weightedAverage" : { "type" : "WEIGHTED_AVERAGE", "precision" : 0.76, "recall" : 0.6, "f1" : 0.5285714285714287 },
  "microAverage" : { "type" : "MICRO_AVERAGE", "precision" : 0.5, "recall" : 0.6, "f1" : 0.5454545454545454 },
  "confusionMatrix" : {
    "truePositives" : 3,
    "falsePositives" : 3,
    "falseNegatives" : 2,
    "trueNegatives" : 32
  },
  "betas" : [ 0.5, 1.0, 2.0 ]
}
```
