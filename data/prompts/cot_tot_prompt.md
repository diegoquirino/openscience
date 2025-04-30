# Task: Use Case Change Classification

## Objective:
Given the **original** and **updated** snippets of a use case in **CLARET notation**, classify the change as **high-impact** or **low-impact**, following a logical reasoning process.

---

## Claret Notation Explanation:
The CLARET notation provides a structured and standardized way to capture use case specifications, enabling precise communication of requirements and facilitating model-based testing.

### Key Elements:
- **systemName:** Specifies the system's name.
- **usecase:** Defines a use case.
- **Metadata:** Includes version, type, author, and creation date.
- **actor:** Defines an actor for the use case.
- **preCondition:** Specifies a precondition for the use case.
- **basic {{…}}:** Defines the basic flow of the use case.
  - **step X:** Describes an action performed by the actor or system at step X (positive integer).
- **alternative X 'W' {{…}}:** Defines an alternative flow named 'W' at step X.
  - **af[X,Y,Z]:** Points to one or more alternative flows (X, Y, Z).
- **exception X 'W' {{…}}:** Defines an exception flow named 'W' at step X.
  - **ef[X,Y,Z]:** Points to one or more exception flows (X, Y, Z).
- **bs X:** Refers to step X in the basic flow where the alternative/exception flow continues.
- **postCondition:** Specifies the post-condition of the use case.

---

## Classification Guidelines:

### High-impact:
- **Definition:** Changes the system's expected behavior (semantic edit).
- **Examples:** 
  - Introduces or deletes a requirement, feature, business model entity, step, exception flow (ef), or alternative flow (af).

### Low-impact:
- **Definition:** Does not change the system's behavior (syntactic edit).
- **Examples:** 
  - Textual updates such as detailing, fixing typos, using synonyms, adjusting punctuation, formatting, usability, items added or removed from the screen (for display, review, or verification purposes), reducing, reordering, or renaming elements.

---

## Input:

### Original Snippet:
> {origin}

### Updated Snippet:
> {target}

---

## Output Format:
Provide a valid JSON string response, with fields:
- "editClassification" (choose "HIGH" or "LOW"); and,
- "decisionRationale" (provide a concise justification for your decision).