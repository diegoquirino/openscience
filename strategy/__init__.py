import os
import faiss
import torch
from data.utils import get_key
from .edit_classifier_strategy import EditClassifierStrategy, OUTPUT_DF_COLUMN_NAMES
from .distance_function_classifiers import (LevenshteinClassifier, NGramClassifier, JaccardClassifier,
                                            CosineClassifier, SorensenDiceClassifier)
from .llm_factory import LLMFactory
from .llm_classifier import LLMClassifierStrategy
from .prompt_cot_tot_classifier import PromptCoTToTClassifier
from .naive_rag_classifier import NaiveRAGClassifier

os.environ['OPENAI_API_KEY'] = get_key('OPENAI_API_KEY', 'api_keys')
os.environ['MISTRAL_API_KEY'] = get_key('MISTRAL_API_KEY', 'api_keys')
os.environ['TOGETHER_API_KEY'] = get_key('TOGETHER_API_KEY', 'api_keys')
os.environ['HUGGINGFACEHUB_API_TOKEN'] = get_key('HUGGINGFACEHUB_API_TOKEN', 'api_keys')
os.environ['ANTHROPIC_API_KEY'] = get_key('ANTHROPIC_API_KEY', 'api_keys')
os.environ['GOOGLE_API_KEY'] = get_key('GOOGLE_API_KEY', 'api_keys')
os.environ['GOOGLE_PROJECT_ID'] = get_key('GOOGLE_PROJECT_ID', 'api_keys')
os.environ['GOOGLE_PROJECT_LOCATION'] = get_key('GOOGLE_PROJECT_LOCATION', 'api_keys')
os.environ['LANGCHAIN_API_KEY'] = get_key('LANGCHAIN_API_KEY', 'api_keys')
os.environ['LANGCHAIN_TRACING_V2'] = get_key('LANGCHAIN_TRACING_V2', 'api_keys')
os.environ['LANGCHAIN_ENDPOINT'] = get_key('LANGCHAIN_ENDPOINT', 'api_keys')
os.environ['LANGCHAIN_PROJECT'] = get_key('LANGCHAIN_PROJECT', 'api_keys')
print('API Keys configured.')

os.environ['MODELS_PATH'] = get_key('MODELS_PATH')
os.environ['COT_TOT_PROMPT_PATH'] = get_key('COT_TOT_PROMPT_PATH')
os.environ['RAG_QUESTION_PATH'] = get_key('RAG_QUESTION_PATH')
os.environ['RAG_CONTEXT_FILES_PATH'] = get_key('RAG_CONTEXT_FILES_PATH')
os.environ['RAG_RETRIEVER_QUERY_PATH'] = get_key('RAG_RETRIEVER_QUERY_PATH')
os.environ['CLARET_DOCS_BASE_URL'] = get_key('CLARET_DOCS_BASE_URL')
print('Strategies configured.')

# Check if CUDA (GPU) is available
if torch.cuda.is_available():
    print("CUDA available: Loading FAISS with GPU support.")
    # Initialize FAISS to use GPU
    res = faiss.StandardGpuResources()  # GPU resources initialization
    # Example: Create an index using GPU
    d = 128  # Dimension of vectors
    index = faiss.GpuIndexFlatL2(res, d)  # Use GPU-based index
else:
    print("CUDA not available: Loading FAISS with CPU support.")
    # Fallback to CPU-based index
    d = 128  # Dimension of vectors
    index = faiss.IndexFlatL2(d)  # Use CPU-based index

# You can now use the `index` for your FAISS operations
