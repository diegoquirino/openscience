## Claret notation rules for use case specifications

### 2.1 Consecutive steps cannot be performed both the same actor or system.

*Rule: Consecutive steps (at least two) cannot be performed both the same actor or system action into basic, alternative or exception flow.*

**Violation Example:**
```
basic { 
  step 1 emailUser 'opens the login screen'
  step 2 emailUser 'fills out the fields'
  step 3 system 'displays the login form'
  step 4 system 'displays the incorrectly filled fields with a red border'
}
```

**Correction:**
```
basic {
  step 1 emailUser 'opens the login screen, and fills out the fields'
  step 2 system 'displays the login form, and the incorrectly filled fields with a red border'
}
```

### 2.2. Separation by commas: preConditions, steps and postConditions

*Rule: If there are multiple preconditions or postconditions, or multiple actions performed by the same actor within a step, they must be separated by commas.*

**Violation Examples:**
```
preCondition 'User is in homepage screen.
              There is an active network connection.'
```
```
basic { 
  step 1 emailUser 'opens the login screen.
                    fills out the fields.'           
  step 2 system 'displays the login form.
                 displays the incorrectly filled fields with a red border.'
}
```
```
postCondition 'User is successfully logged.
               User is redirected to dashboard screen.'
```

**Corrections:**
```
preCondition 'User is in homepage screen, and there is an active network connection.'
```
```
basic { 
  step 1 emailUser 'opens the login screen, and fills out the fields.'           
  step 2 system 'displays the login form, and displays the incorrectly filled fields with a red border.'
}
```
```
postCondition 'User is successfully logged, and is redirected to dashboard screen.'
```

### 2.3. Declaring multiple actors

*Rule: If there are multiple actors, each must be declared on a separate line.*

**Violation Example:**
```
actor users 'Email User, System Administrator'
```

**Correction:**
```
actor emailUser 'Email User'
actor adminUser 'System Administrator'
```

### 2.4. Last step in a basic, alternative or exception flow

*Rule: The last step in a flow must always be a system action, not an actor’s action. The only exception is when the last step involves an actor’s action followed by a 'bs' mark to return to the basic flow.*

**Violation Examples:**
```
basic { 
  step 1 emailUser 'fills out the fields and displays the incorrectly filled fields with a red border.'
}
```
```
alternative 1 'Username is predicted' {
  step 1 emailUser 'selects a suggested username, types the password, and clicks on the submit button'
}
```
```
exception 1 'User does not exist in database' {
  step 1 emailUser 'alerts that user does not exist'
}
```

**Corrections:**
```
basic { 
  step 1 emailUser 'fills out the fields.'           
  step 2 system 'displays the incorrectly filled fields with a red border.'
}
```
```
alternative 1 'Username is predicted' {
  step 1 emailUser 'selects a suggested username, types the password, and clicks on the submit button' bs 2
}
```
```
exception 1 'User does not exist in database' {
  step 1 system 'alerts that user does not exist'
}
```

### 2.5. Alternative and Exception flows cannot have another alternative or exception flows references inside of them

*Rule: The 'ef' and 'af' marks (references to another alternative or exception flows) cannot be used at the end of any step within alternative or exception flows. If the case, they can reference a step in the basic flow ('bs' mark)*

**Violation Examples:**
```
exception 2 'Incorrect username/password combination' {
  step 1 system 'alerts that the username and/or password are incorrect' ef[1]
}
```
```
alternative 1 'Nome de usuario e preenchido automaticamente pelo navegador' {
  step 1 userEval 'seleciona um nome de usuario sugerido, digita a senha e clica no botao entrar' af[2]
}
```

**Corrections:**
```
exception 2 'Incorrect username/password combination' {
  step 1 system 'alerts that the username and/or password are incorrect' bs 3
}
```
```
alternative 1 'Nome de usuario e preenchido automaticamente pelo navegador' {
  step 1 userEval 'seleciona um nome de usuario sugerido, digita a senha e clica no botao entrar' bs 4
}
```

### 2.6. Return to basic flow from alternative or exception flows

*Rule: In alternative and exception flows, if the last step involves a 'bs' mark to return to the basic flow, the referenced step in the basic flow must be performed by the system, if origin step is performed by an actor; or, by an actor, if origin step is performed by the system.*

**Basic flow example**
```
basic {
  step 1 emailUser 'launches the login screen'
  step 2 system 'presents a form with username and password fields and a submit button'
  step 3 emailUser 'fills out the fields and clicks on the submit button' af[1]
  step 4 system 'displays a success message' ef[1,2]
}
```

**Violation Example Alternative flow:**
Step 1 in alternative flow 1 is performed by an actor, it can not return to step 3 in basic flow because it is also performed by an actor.
```
alternative 1 'Username is predicted' {
  step 1 emailUser 'selects a suggested username, types the password, and clicks on the submit button' bs 3
}
```

**Correction of alternative flow:**
Now, step 1 in alternative flow is correctly pointing to step 4 in basic flow that is performed by the system.
```
alternative 1 'Username is predicted' {
  step 1 emailUser 'selects a suggested username, types the password, and clicks on the submit button' bs 4
}
```

**Violation Example of exception flow):**
Step 1 in exception flow 2 is performed by the system, it can not return to step 4 in basic flow because it is also performed by the system.
```
exception 2 'Incorrect username/password combination' {
  step 1 system 'alerts that the username and/or password are incorrect' bs 4
}
```

**Correction of exception flow:**
Now, step 1 in exception flow is correctly pointing to step 3 in basic flow that is performed by an actor.
```
exception 2 'Incorrect username/password combination' {
  step 1 system 'alerts that the username and/or password are incorrect' bs 3
}
```

### 2.7. Return to basic flow only can be called from alternative and exception flow last step

*Rule: The 'bs' mark can only be used in the last step of an alternative or exception flow.*

**Violation Examples:**
```
alternative 1 'Username is predicted' {
  step 1 emailUser 'waits the system to present the listbox with names, and selects one of them' bs 4
  step 2 system 'presents a form with username filled by the choose done by the user'
}
```
```
exception 2 'Incorrect username/password combination' {
  step 1 system 'shows the form with username and password fields, and a submit button'
  step 2 emailUser 'fills the form with valid data, and submits the form' bs 3
  step 3 system 'alerts that username and/or password are incorrect' 
}
```

**Corrections:**
```
alternative 1 'Username is predicted' {
  step 1 emailUser 'waits the system to present the listbox with names, and selects one of them' bs 4
}
```
```
exception 2 'Incorrect username/password combination' {
  step 1 system 'shows the form with username and password fields, and a submit button'
  step 2 emailUser 'fills the form with valid data, and submits the form' 
  step 3 system 'alerts that username and/or password are incorrect' bs 3
}
```

### 2.8. Alternative flows must only be referenced in a step performed by an actor into the basic flow

*Rule The 'af' mark (reference to an alternative flow) can only be used in a step performed by an actor into the basic flow.*

**Violation Example:**
```
basic {
  step 1 emailUser 'launches the login screen'
  step 2 system 'presents a form with username and password fields and a submit button'
  step 3 emailUser 'fills out the fields and clicks on the submit button'
  step 4 system 'displays a success message' af[1,2]
}
```

**Correction:**
```
basic {
  step 1 emailUser 'launches the login screen'
  step 2 system 'presents a form with username and password fields and a submit button'
  step 3 emailUser 'fills out the fields and clicks on the submit button' af[1,2]
  step 4 system 'displays a success message' 
}
```

### 2.9. Exceptions flows must only be referenced in a step performed by the system into the basic flow

*Rule The 'ef' mark (reference to an exception flow) can only be used in a step performed by the system into the basic flow.*

 **Violation Example:**
```
basic {
  step 1 emailUser 'launches the login screen'
  step 2 system 'presents a form with username and password fields and a submit button'
  step 3 emailUser 'fills out the fields and clicks on the submit button' ef[1,2]
  step 4 system 'displays a success message' 
}
```

**Correction:**
```
basic {
  step 1 emailUser 'launches the login screen'
  step 2 system 'presents a form with username and password fields and a submit button'
  step 3 emailUser 'fills out the fields and clicks on the submit button'
  step 4 system 'displays a success message' ef[1,2]
}
```
