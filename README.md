# Text-Generation LLMs integration in TCM with MBT, Experiment#01

Experiment #1: **Integrating** Text-Generation Large Language Models (LLMs) *in* Test Case Maintenance (TCM) **with** Model-Based Testing (MBT) in the ***RGP-Diarias and GTI-Competencias Systems of the Court of Justice of the State of Paraíba (TJPB), Brazil***

## Contact

* *Researcher:* Carlos Diego Quirino Lima
* *E-mails:* diego.quirino@tjpb.jus.br or diego.quirino@copin.ufcg.edu.br

# Project Configurations

This repository contains three main scripts that must be executed in the following order to perform data collection, processing, and generation of analysis graphs and summaries. Below are detailed instructions on how to configure and run each script.

## Directory Structure

The expected project structure is as follows:

```
/data
/strategy
/analysis
.gitignore
LICENSE
README.md
main_data_fetch.py
main.py
main_analysis_plot_and_summary.py
```

## Global Variables and Configurations

### 1. Script: `main_data_fetch.py`

This script is responsible for fetching data from a GitHub repository. It uses the `GitHubDiffsFinder` class to find software version differences and `GitHubTagAffectedTestCasesTracker` to track the test cases affected by those changes.

#### Global Variables:

- **SOFTWARE**: Name of the software to be analyzed. Default is `'gti-competencias'`.
- **UC_PREFIX_PATTERN**: Regex pattern for use case prefixes. Default is `r'RF\d+'`.

#### Environment Variables:

- **GITHUB_API_KEY**: This script requires a GitHub API key to access the repository and collect data. The user must configure this key as an environment variable named `GITHUB_API_KEY` before running the script.

To set the environment variable:

- On Linux/macOS:
  ```bash
  export GITHUB_API_KEY=<your_api_key>
  ```

- On Windows (Command Prompt):
  ```cmd
  set GITHUB_API_KEY=<your_api_key>
  ```

#### Configuration File:

- **`software_conf_file_path`**: Path to the software configuration file, which should be inside the `data` folder. The file name follows the format `<SOFTWARE>.conf`. The content of this file must provide information about the software versions and the test case reduction strategy.

#### What should be changed by the user:
- Change the `SOFTWARE` variable to the desired software name, if different from the default.
- Ensure that the corresponding configuration file exists in the `/data` folder.
- Set the `GITHUB_API_KEY` environment variable.

#### Execution:
```bash
python main_data_fetch.py
```

### 2. Script: `main.py`

This script processes models and change classification strategies using different APIs and local strategies (e.g., ChatGPT, Gemini).

#### Global Variables:

- **MODELS**: List of models to be used for classification. Example: `['gpt-3.5-turbo', 'gemini-1', 'ollama-2']`.
- **SOFTWARE**: Name of the software to be analyzed, should be the same as used in the previous script.
- **MAX_WORKERS**: Maximum number of concurrent threads for parallel execution. It is recommended to set a value between 2 and 4 depending on your environment.

#### Environment Variables:

- **OPENAI_API_KEY**: Required to interact with OpenAI's GPT models.
- **GOOGLEAI_API_KEY**: Required to interact with Google's Gemini models.
  
To set the environment variables:

- On Linux/macOS:
  ```bash
  export OPENAI_API_KEY=<your_openai_api_key>
  export GOOGLEAI_API_KEY=<your_googleai_api_key>
  ```

- On Windows (Command Prompt):
  ```cmd
  set OPENAI_API_KEY=<your_openai_api_key>
  set GOOGLEAI_API_KEY=<your_googleai_api_key>
  ```

#### Prerequisites:

- **Ollama**: Ensure that Ollama is installed and running locally to use the `ollama` models. Instructions for installing and running Ollama can be found on the [Ollama documentation](https://ollama.com/docs).

#### What should be changed by the user:
- **MODELS**: Define the list of models to be used. For example, to use ChatGPT and Gemini, set something like `MODELS = ['gpt-3.5-turbo', 'gemini-1']`.
- **SOFTWARE**: Define the software name according to the previous script and the corresponding configuration file.
- **MAX_WORKERS**: Set the number of threads according to your environment’s capacity. A common value would be `MAX_WORKERS = 4`.
- Ensure that the environment variables `OPENAI_API_KEY` and `GOOGLEAI_API_KEY` are properly set.
- Ensure Ollama is installed and running locally.

#### Execution:
```bash
python main.py
```

### 3. Script: `main_analysis_plot_and_summary.py`

This script is responsible for generating graphs and summaries of the data analysis. It uses plotting and analysis functions from the processed data.

#### Global Variables:

- **ANALYSIS_RESULTS_PATH**: Path where the analysis results will be saved.
- **CURRENT_USE_CASES_EDIT_CLASSIFICATIONS_PATH**: Relative path to the use case edit classification files.
- **SOFTWARE**: Name of the software being analyzed, the same as used in the previous scripts.

#### What should be changed by the user:
- Ensure that the directories defined in `ANALYSIS_RESULTS_PATH` exist and have write permissions.
- Verify that the value of `SOFTWARE` is correct, consistent with the previous scripts.

#### Execution:
```bash
python main_analysis_plot_and_summary.py
```

## Final Considerations

- Ensure that all necessary packages are installed, especially those listed in the `requirements.txt` file.
- The global variables in the scripts are configured with default values that may need adjustment depending on your environment and the software being analyzed.
- For greater accuracy, review the software configuration file and adjust parameters as necessary.