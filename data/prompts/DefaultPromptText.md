**Given the original and updated snippets of a use case in CLARET notation, classify the change as high-impact or low-impact.**

# Claret Notation explanation:

The Claret notation provides a structured and standardized way to capture use case specifications, making it easier to communicate requirements, and perform model-based testing:
- systemName: it specifies the name of the system
- usecase: it defines a use case with.
- version, type, author, and creation date: they provide metadata about the use case.
- actor: it defines an actor for the use case.
- preCondition: it specifies a precondition for the use case.
- basic {...}: it defines the basic flow of the use case.
- step X: it describes an action performed by the actor or by the system of number 'X' (positive integer from 1).
- alternative X 'W' {...}: it defines an alternative flow with the name 'W' of number 'X' (positive integer from 1).
- af[X,Y,Z]: it points to one or various alternatives flows of numbers X, Y and Z (positive integers).
- exception X 'W' {...}: it defines an exception flow with the name 'W' of number 'X' (positive integer from 1).
- ef[X,Y,Z]: it points to one or various exception flows of numbers X, Y and Z (positive integers).
- bs X: it refers to the next step 'X' in the basic flow, where the alternative/exception flow continues executing.
- postCondition: it specifies the post-condition for the use case.

# Original snippet:

> [origin]

# Updated snippet:

> [target]

# Classification:

## High-impact

> It changes the system expected behavior (semantic edit).
> The changes introduce new ones or delete: requirement, feature, business model entity, step, exception flow (ef) or alternative flow (af).

## Low-impact

> It does not change the system behavior (syntactic edit).
> The changes are merely textual (update): detailing, typo, synonyms, punctuation, formatting, reducing, reordering or renaming.

# Output

Provide a plain-text answer, and provide only one valid JSON string, in the format below:

```json
{
    "edit_classification": "HIGH" or "LOW",
    "decision_rationale": "the brief justification for your decision"
}
```