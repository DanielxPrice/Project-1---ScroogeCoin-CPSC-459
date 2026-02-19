# ScroogeCoin Transaction Handler

## Overview

This project implements the core transaction validation logic for a simplified cryptocurrency system called **ScroogeCoin**. The central authority, Scrooge, receives proposed transactions and processes them to maintain a valid ledger of unspent transaction outputs (UTXOs).

The objective of this assignment is to implement the transaction handler that validates transactions and updates the ledger according to the specified rules.

Only `TxHandler.java` was modified as required by the assignment.

---

## Files Included

- `Transaction.java` – Defines the transaction structure and hashing logic
- `UTXO.java` – Represents an unspent transaction output
- `UTXOPool.java` – Maintains the current set of valid UTXOs
- `rsa.jar` – Provides RSA signature verification functionality
- `TxHandler.java` – Implements transaction validation and block handling logic

---

## Transaction Validation Rules

A transaction is considered valid if:

1. All outputs claimed by its inputs exist in the current UTXO pool.
2. Each input signature is valid and verified against the referenced output's public key.
3. No UTXO is claimed more than once within the same transaction.
4. All output values are non-negative.
5. The total input value is greater than or equal to the total output value.

---

## `handleTxs()` Behavior

The `handleTxs()` method:

- Accepts an unordered array of proposed transactions.
- Selects a mutually valid subset of maximal size.
- Updates the internal UTXO pool by:
  - Removing spent outputs.
  - Adding newly created outputs.
- Supports transactions that depend on other transactions within the same block.
- Preserves ledger state across multiple calls.

The returned set is **maximal**, meaning no additional transaction can be added without violating validity constraints.

---

## Compilation Instructions (Windows)

To compile:

```bash
javac -cp ".;rsa.jar" *.java
```

To run the provided test file (if available):

```bash
java -cp ".;rsa.jar" TestTxHandler
```

---

## Notes

- Compiled `.class` files are ignored via `.gitignore`.
- No modifications were made to provided infrastructure files.
- The implementation passes all provided autograder tests.

---
