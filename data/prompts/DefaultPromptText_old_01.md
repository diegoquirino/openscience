You are evaluating changes in a use case in the context of specification-based regression testing. Below are the descriptions of the original use case and the modified (target) use case. Based on these descriptions, determine if the change:

1. Creates new test cases.
2. Reuses existing test cases (only textual adjustments or minor modifications, without semantic changes).
3. Makes existing test cases obsolete (significant semantic changes that invalidate existing test cases).

### Excerpt or Original Use Case:
"[origin]"

### Excerpt or Modified (Target) Use Case:
"[target]"

### Evaluation Questions:
1. Do the changes in the use case introduce new features, requirements, steps, exception flows or alternative flows that were not present in the original use case? (Yes/No)
2. Are the changes merely textual, typo, synonyms, punctuation or formatting, without altering the logic or behavior that were present in the original use case (semantically equivalent)? (Yes/No)
3. Are the changes just detailing, reducing, reordering or renaming of the checks that need to be made, usually related only to data presentation (semantically equivalent)? (Yes/No)
4. Do the changes in the use case remove or deprecate features, requirements, steps, exception flow or alternative flow, or promote significant alterations in the logic or behavior that were present in the original use case? (Yes/No)

### Mandatory Rules:
- If the original use case is '', 'nan', 'null', or 'empty' and the target use case is not, classify the change as 'NEW'.
- If the target use case is '', 'nan', 'null', or 'empty' and the original use case is not, classify the change as 'OBSOLETE'.

### Edit Classification (decision):
- If the answer to question 1 is 'Yes', classify the change as 'NEW'.
- If the answer to question 2 or 3 is 'Yes', classify the change as 'REUSABLE'.
- If the answer to question 4 is 'Yes', classify the change as 'OBSOLETE'.

Please, in the output, return a python valid dictionary (answer must begin with '{' and ends with '}', don't use code blocks ```python, or ' or " into the answer) into the format below, with your final edit classification and, if possible, a brief justification for your decision rationale:
{
    'edit_classification': 'NEW' or 'REUSABLE' or 'OBSOLETE',
    'decision_rationale': 'the brief justification for your decision'
}
