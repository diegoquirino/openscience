import os
import bs4
import copy
import json

# from langchain_core.vectorstores import InMemoryVectorStore
from langchain_core.prompts import PromptTemplate
from langchain_community.document_loaders import WebBaseLoader
from langchain_text_splitters import RecursiveCharacterTextSplitter
# from langchain_core.embeddings import DeterministicFakeEmbedding
from langchain_huggingface import HuggingFaceEmbeddings
from langchain_community.vectorstores import FAISS
from langchain.schema import Document

from strategy import LLMClassifierStrategy
from data import WebScraper
from data.utils import log, extract_json_answer, add_text_file_contents_to_rag_docs, format_docs
from time import time, sleep


class NaiveRAGClassifier(LLMClassifierStrategy):
    def __init__(self, llm, prompt_path, context_path, retriever_query_path):
        super().__init__(llm)
        if not prompt_path or not context_path:
            raise ValueError('Context path and Origin file are required.')
        self.prompt_path = os.path.normpath(prompt_path)
        self.retriever_query_path = os.path.normpath(retriever_query_path)
        self.context_path = os.path.normpath(context_path)
        self.web_scraper_base_url = os.getenv('CLARET_DOCS_BASE_URL')
        self.context_rag_docs = self._init_context_rag_docs()
        self.origin_file_path = None

    def set_origin_file_path(self, origin_file_path):
        self.origin_file_path = os.path.normpath(origin_file_path)

    def _initialize_faiss_vectorstore(self, documents, model_name="sentence-transformers/all-MiniLM-L6-v2"):
        log(f"Creating FAISS VectorStore with HuggingFace Embeddings model: {model_name}")
        embedding = HuggingFaceEmbeddings(model_name=model_name)
        vectorstore = FAISS.from_documents(documents=documents, embedding=embedding)
        return vectorstore

    def _initialize_text_splitter(self):
        text_splitter = RecursiveCharacterTextSplitter(
            chunk_size=512,
            chunk_overlap=128,
            add_start_index=True,
            separators=["\n\n", "\n", ". ", "! ", "? ", ", "]
        )
        log(f"Recursive Character Text Splitter initialized.")
        return text_splitter

    def _init_context_rag_docs(self):
        # Initialize documents with the web scraped ones
        scraper = WebScraper(self.web_scraper_base_url)
        web_paths = scraper.get_all_urls()
        web_loader = WebBaseLoader(
            web_paths=web_paths,
            bs_kwargs=dict(
                parse_only=bs4.SoupStrainer('body')
            ),
        )
        log(f'Adding webpages: {web_paths}')
        web_docs = [
            json.dumps({"metadata": doc.metadata, "page_content": doc.page_content})
            for doc in web_loader.load()
        ]
        # Include the local context (rules) documents
        context_rag_docs = add_text_file_contents_to_rag_docs(os.path.join(os.getcwd(), self.context_path), web_docs)
        # Initialize split and vectorstore with context documents
        # text_splitter = self._initialize_text_splitter()
        # splits = text_splitter.split_documents(rag_docs)
        # vectorstore = self._initialize_faiss_vectorstore(splits)
        log('Context Naive RAG documents web-scraped from the web and/or retrieved from textual rules.')
        return context_rag_docs

    def classify(self, origin: str, target: str) -> str:
        log("=================================================")
        # Start timing the classification process
        start_time = time()

        # Load the content of the origin file
        try:
            with open(self.origin_file_path, 'r', encoding='utf-8') as file:
                origin_file_content = file.read()
            log(f'Loaded content from origin file: {self.origin_file_path}')
        except Exception as e:
            origin_file_content = ""
            log(f'File \'{self.origin_file_path}\' does not exist: {e}')
        # Add the origin file content to the context
        new_document = Document(metadata={"source": f"{self.origin_file_path}"}, page_content=origin_file_content)
        context_with_origin_file_docs = copy.deepcopy(self.context_rag_docs)
        context_with_origin_file_docs.extend([
            json.dumps({"metadata": doc.metadata, "page_content": doc.page_content}) for doc in [new_document]
        ])
        context_with_origin_file_docs = [json.loads(doc) for doc in context_with_origin_file_docs]
        context_with_origin_file_docs = [Document(metadata=doc["metadata"], page_content=doc["page_content"]) for doc in context_with_origin_file_docs]
        text_splitter = self._initialize_text_splitter()
        documents = text_splitter.split_documents(context_with_origin_file_docs)

        # Prompt the LLM with the origin and target
        with open(self.prompt_path, 'r', encoding='utf-8') as f:
            prompt = f.read()
        use_case_change_classification_prompt = PromptTemplate(
            input_variables=["context", "origin", "target"],
            template=prompt
        )

        # Initialize the vectorstore with the context documents
        local_vectorstore = self._initialize_faiss_vectorstore(documents)
        log(f'Context with origin file content added to vectorstore: {self.origin_file_path}')
        # Retrieve the vectorstore retriever with relevant documents for the prompt
        with open(self.retriever_query_path, 'r', encoding='utf-8') as f:
            retriever_query = f.read()
        retriever = local_vectorstore.as_retriever()
        retrieved_docs = retriever.invoke(retriever_query)
        content_dict = {}
        # Retry mechanism
        for attempt in range(3):
            # Retrieve context to prompt template, converting into messages
            messages = use_case_change_classification_prompt.invoke({
                "context": retrieved_docs,
                "origin": origin,
                "target": target
            }).to_messages()

            # Run the chain
            response = self.llm.invoke(messages)

            # Log the classification result
            content_dict = extract_json_answer(response.content)
            # End timing the classification process
            end_time = time()
            elapsed_time_ms = int((end_time - start_time) * 1000)
            content_dict['elapsed_time_ms'] = elapsed_time_ms
            log(f'Origin: [[{origin}]]\nTarget: [[{target}]]\nComplete Response: [[{response}]]\nClassification result: {content_dict}')
            if content_dict.get('edit_classification') in ['HIGH', 'LOW']:
                break
            sleep(30)
            log(f'Attempt {attempt + 1} failed to get valid NAIVE RAG edit classification. Retrying...')

        # Append the classification result to the DataFrame
        self.df.loc[len(self.df)] = content_dict
        return self.df