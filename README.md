# Exploring the Use of LLMs to Reduce the Discard of MBT Tests Project

This project explores the use of Large Language Models (LLMs) to reduce the discard of Model-Based Testing (MBT) tests. It includes scripts for fetching data from GitHub, classifying text edits, and analyzing the results.

## Requirements
- Python (v &ge; 3.12) and Pip (v &ge; 25.0.0) (`python.exe -m pip install --upgrade pip`)
- Install dependencies via `pip install -r requirements.txt`
- Ensure the environment variables are correctly set (or that \`app.conf\` is properly configured and read)

> **Important Note:**  
> This project requires the installation of [FAISS](https://github.com/facebookresearch/faiss) to enable high-performance similarity search functionalities. Depending on your hardware configuration, please install one of the following:
> 
> - **For CPU-only environments:**  
>   Install the `faiss-cpu` package:
>   ```bash
>   pip install faiss-cpu
>   ```
> 
> - **For GPU-enabled environments:**  
>   Install the `faiss-gpu` package:
>   ```bash
>   pip install faiss-gpu
>   ```
> 
> **Technical Details:**  
> - FAISS is a library developed by Facebook Research optimized for fast similarity search on large-scale vector datasets.
> - **Requirements:** Ensure that your Python version and other dependencies are compatible with FAISS. For detailed installation instructions and configuration options, please refer to the [official documentation](https://github.com/facebookresearch/faiss/blob/main/INSTALL.md).
> - The choice between `faiss-cpu` and `faiss-gpu` depends on your available hardware:  
>   - Use `faiss-cpu` for systems without GPU support or for initial testing.
>   - Use `faiss-gpu` to leverage GPU acceleration for significantly faster processing on large datasets.


## Script 1 (optional, this source contains all diff files) - Main Data Fetch

### Overview
The script `main_data_fetch.py` is responsible for fetching GitHub diffs and tracking affected test cases. It leverages the functionality provided by `data/__init__.py` to handle environment-dependent configurations.

### Environment Variables
A few environment variables must be set so that the data processing can run correctly. These variables include (but are not necessarily limited to):
- \`GITHUB_TOKEN\`: Your GitHub access token.
- \`GITHUB_REPO\`: The repository name to fetch data from.
- \`GITHUB_OWNER\`: The owner or organization of the repository.
  
These values should be loaded from \`app.conf\`. You can see the file \`app.conf.example\` to understand how they need to be configured and how they are then imported in \`data/__init__.py\`.

### How to Run
1. In your terminal, navigate to the project directory.
2. Make sure that your environment variables are properly set in \`app.conf\` (or in your system environment).
3. Run the script:
   ```bash
   python main_data_fetch.py
   ```
4. The script will process any GitHub diffs (if configured) and track test cases affected by the retrieved changes.

## Script 2 (optional, results are in `data/sw_{software-name}/use_cases_edit_classifications`) - Main Edit Classification

### Overview
The script `main_edit_classification.py` classifies text edits across different strategies. It may use a Large Language Model \(LLM\) or distance-based measures. Results are stored as CSV files for further analysis.

### Environment Variables
Several environment variables must be defined and loaded from `app.conf` (see `app.conf.example`). They are referenced in `strategy/__init__.py` and may include:
- `COT_TOT_PROMPT_PATH`
- `RAG_QUESTION_PATH`
- `RAG_CONTEXT_FILES_PATH`
- `RAG_RETRIEVER_QUERY_PATH`
- `MODELS_PATH`

These provide the paths needed for prompt-based classification, retrieval-augmented generation, and model configuration.

### How to Run
1. Confirm that `app.conf` contains the correct environment variables listed above.
2. Navigate to the project directory in your terminal.
3. Execute:
   ```bash
   python main_edit_classification.py
   ````
4. The script will read the necessary data and classify changes by the specified strategy.

## Script 3 - Main Analysis Plot and Summary

### Overview
The script `main_analysis_plot_and_summary.py` is designed to load and analyze classification results, compute various metrics, and generate summary outputs. It processes classification result files, performs confusion matrix analysis, test case analysis, and summarizes the results in multiple formats including CSV, XLSX, and LaTeX.

### How to Run
1. Ensure all required environment variables are defined in `app.conf`. Example, if you want to analyze the results of the `data\sw_gti-competencias\use_cases_edit_classifications\final_gti_competencias` folder, set the `ANALYSIS_PATH=final_gti_competencias` and `SOFTWARE=gti-competencias`.
2. Confirm they match those in `app.conf.example`.
3. Inside the project directory, run:
   ```bash
   python main_analysis_plot_and_summary.py
   ```
4. The script will process the classification results and generate the necessary outputs (plots, and latex-format tables).

### Required Environment Variables

These variables are referenced in `analysis/__init__.py` and must be defined in your app.conf file:  
- `ANALYSIS_PATH`: Path used for the analysis results.

## Notes
- Ensure that all required variables are set before running.
- Double-check \`app.conf\` to confirm that all necessary environment variables are set.
- For more details on configuring environment variables, refer to \`app.conf.example\`.

