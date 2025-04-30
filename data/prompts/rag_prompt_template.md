# Task: Use Case Change Classification

## Objective:
Use the following pieces of **context** to answer the **question** below.

## Context: 
{context} 

## Question: 
Given the **original** and **updated** snippets inputs of a use case in **CLARET notation**, classify the change as **high-impact** or **low-impact**, answering in the correct **output format**.

---

### Input:

#### Original Snippet:
> {origin}

#### Updated Snippet:
> {target}

---

### Output Format:
Provide a valid JSON string response, with fields:
- "editClassification" (choose "HIGH" or "LOW"); and,
- "decisionRationale" (provide a concise justification for your decision).