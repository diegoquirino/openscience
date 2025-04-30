# Claret Notation Explanation

The Claret notation provides a structured and standardized framework for documenting use case specifications, improving clarity, consistency, and facilitating effective communication among stakeholders. Additionally, it supports model-based testing by offering a detailed, systematic way to represent use case flows and their associated metadata. Below is a detailed explanation of the key components in Claret notation:

---

## Key Elements of Claret Notation

1. **systemName:**  
   Specifies the name of the system under discussion.  
   _Example:_ `systemName "OnlineShoppingPortal"`

2. **usecase:**  
   Defines a specific use case within the system. This serves as the primary context for the subsequent elements.  
   _Example:_ `usecase "UC001: PlaceOrder"`

3. **Metadata Fields (version, type, author, creation date):**  
   Provides additional information about the use case for tracking and documentation purposes:  
   - **version:** Tracks the iteration or revision of the use case.  
     _Example:_ `version "1.0"`  
   - **type:** Classifies the use case (e.g., functional, non-functional).  
     _Example:_ `type "Functional"`  
   - **author:** Identifies the creator of the use case.  
     _Example:_ `author "Jane Doe"`  
   - **creation date:** Specifies when the use case was documented.  
     _Example:_ `creation date "2024-12-30"`

4. **actor:**  
   Identifies the primary actor or user interacting with the system for the given use case.  
   _Example:_ `actor customer "Customer"`

5. **preCondition:**  
   Describes the conditions that must be satisfied before the use case can be executed.  
   _Example:_ `preCondition "User is logged into the system."`

6. **basic {...}:**  
   Defines the basic flow or primary scenario of the use case, representing the steps for normal execution.  
   _Example:_  
   ```claret
   basic {
       step 1 customer "selects items to purchase, and adds items to the shopping cart."
       step 2 system "calculates the total amount and displays the cart." 
       step 3 customer "proceeds to checkout."
       step 4 system "prompts user to enter shipping details."
   }
   ```

7. **step X:**  
   Describes a specific action performed by the actor or system, numbered sequentially (X = positive integer).  
   _Example:_ `step 1 customer "enters shipping details."`

8. **alternative X 'W' {...}:**  
   Defines an alternative flow (variation) of the main scenario, identified by a name ('W') and a number (X = positive integer).  
   _Example:_  
   ```claret
   alternative 3 "PaymentWithGiftCard" {
       step 1 customer "selects 'Pay with Gift Card.'"
       step 2 system "validates the gift card details."
   }
   ```

9. **af[X,Y,Z]:**  
   Points to one or multiple alternative flows (X, Y, Z = positive integers).  
   _Example:_ `step 1 customer "selects 'Pay in Cash'." af[2,3]` Refers to alternative flows 2 and 3.

10. **exception X 'W' {...}:**  
    Describes an exception flow, identifying how the system handles errors or deviations, with a name ('W') and a number (X = positive integer).  
    _Example:_  
    ```claret
    exception 4 "InvalidPaymentDetails" {
        step 1 system "displays error message 'Invalid Payment Details'".
    }
    ```

11. **ef[X,Y,Z]:**  
    Points to one or multiple exception flows (X, Y, Z = positive integers).  
    _Example:_ `step 4 system "validates the gift card details." ef[1,4]` Refers to exception flows 1 and 4.

12. **bs X:**  
    Indicates the next step (X) in the basic flow, where an alternative or exception flow resumes execution.  
    _Example:_ `step 1 customer "selects 'Pay with Credit Card.'" bs 4` Returns to step 4 in the basic flow.

13. **postCondition:**  
    Specifies the state or conditions that must hold true after the use case has been successfully executed.  
    _Example:_ `postCondition "Order is successfully placed, and confirmation email is sent."`

---

## Benefits of Claret Notation

- **Consistency:** Ensures a uniform way of documenting use cases across teams and projects.
- **Clarity:** Makes it easier to identify and understand the various flows, actors, and conditions.
- **Traceability:** Metadata fields help track revisions and identify contributors.
- **Testability:** Provides a detailed structure that can directly support model-based testing.

Claret notation bridges the gap between requirements documentation and implementation, enhancing the effectiveness of both communication and validation processes.
