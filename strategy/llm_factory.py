from langchain_anthropic import ChatAnthropic
from langchain_google_genai import ChatGoogleGenerativeAI
from langchain_huggingface import ChatHuggingFace, HuggingFaceEndpoint
from langchain_openai import ChatOpenAI
from langchain_ollama import ChatOllama
from langchain_together import ChatTogether

class LLMFactory:
    def __init__(self, provider, model):
        self.provider = provider
        self.model = model

    def get_llm(self):
        if self.provider.lower() == "openai":
            return ChatOpenAI(model=self.model)
        elif self.provider.lower() == "ollama":
            return ChatOllama(model=self.model)
        elif self.provider.lower() == "together":
            return ChatTogether(model=self.model)
        elif self.provider.lower() == "anthropic":
            return ChatAnthropic(model=self.model)
        elif self.provider.lower() == "huggingface":
            if self.model.startswith("http"):
                return ChatHuggingFace(llm=HuggingFaceEndpoint(endpoint_url=self.model, task="text-generation"))
            return ChatHuggingFace(llm=HuggingFaceEndpoint(repo_id=self.model, task="text-generation"))
        elif self.provider.lower() == "google":
            return ChatGoogleGenerativeAI(model=self.model)
        else:
            raise ValueError(f"Unknown provider: {self.provider}")
