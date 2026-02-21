import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class MaxFeeTxHandler {

    private UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current UTXOPool is {@code utxoPool}.
     * This makes a defensive copy of {@code utxoPool}.
     */
    public MaxFeeTxHandler(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * Standard transaction validation logic.
     */
    public boolean isValidTx(Transaction tx) {
        HashSet<UTXO> usedUtxos = new HashSet<>();
        double inputSum = 0.0;
        double outputSum = 0.0;

        for (int i = 0; i < tx.numInputs(); i++) {
            Transaction.Input in = tx.getInput(i);
            UTXO utxo = new UTXO(in.prevTxHash, in.outputIndex);

            // (1) Check if UTXO is in the pool
            if (!utxoPool.contains(utxo)) return false;
            // (3) Check for double-spending within the transaction
            if (usedUtxos.contains(utxo)) return false;
            usedUtxos.add(utxo);

            Transaction.Output prevOutput = utxoPool.getTxOutput(utxo);
            // (2) Verify the digital signature
            if (!prevOutput.address.verifySignature(tx.getRawDataToSign(i), in.signature)) {
                return false;
            }
            inputSum += prevOutput.value;
        }

        for (Transaction.Output out : tx.getOutputs()) {
            // (4) Output values must be non-negative
            if (out.value < 0) return false;
            outputSum += out.value;
        }
        // (5) Input sum >= output sum
        return inputSum >= outputSum;
    }


    // Calculates the transaction fee: (Sum of Inputs - Sum of Outputs).
    private double calculateTxFee(Transaction tx, UTXOPool pool) {
        double inputSum = 0;
        for (Transaction.Input in : tx.getInputs()) {
            UTXO utxo = new UTXO(in.prevTxHash, in.outputIndex);
            if (pool.contains(utxo)) {
                inputSum += pool.getTxOutput(utxo).value;
            }
        }
        double outputSum = 0;
        for (Transaction.Output out : tx.getOutputs()) {
            outputSum += out.value;
        }
        return inputSum - outputSum;
    }

    //Handles each epoch by attempting to find a set of transactions 
    //that maximizes total fees using an iterative randomized approach.

    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        Set<Transaction> bestSet = new LinkedHashSet<>();
        double maxTotalFee = -1.0;

        // Try multiple iterations with different processing orders to find the maximum fee set
        for (int iteration = 0; iteration < 200; iteration++) {
            UTXOPool tempPool = new UTXOPool(this.utxoPool);
            List<Transaction> candidates = new ArrayList<>(Arrays.asList(possibleTxs));
            
            if (iteration > 0) {
                // Shuffle to explore different combinations
                Collections.shuffle(candidates);
            } else {
                // First pass: Greedy approach by fee amount
                final UTXOPool initialPool = tempPool;
                candidates.sort((a, b) -> Double.compare(calculateTxFee(b, initialPool), calculateTxFee(a, initialPool)));
            }

            Set<Transaction> currentSet = new LinkedHashSet<>();
            double currentTotalFee = 0;
            boolean progress = true;

            while (progress) {
                progress = false;
                for (int i = 0; i < candidates.size(); i++) {
                    Transaction tx = candidates.get(i);
                    if (tx != null && isValidWithPool(tx, tempPool)) {
                        currentTotalFee += calculateTxFee(tx, tempPool);
                        applyTxToPool(tx, tempPool);
                        currentSet.add(tx);
                        candidates.set(i, null);
                        progress = true;
                    }
                }
            }

            if (currentTotalFee > maxTotalFee) {
                maxTotalFee = currentTotalFee;
                bestSet = currentSet;
            }
        }

        // Apply the best set found to the permanent UTXO pool
        for (Transaction tx : bestSet) {
            applyTxToPool(tx, this.utxoPool);
        }

        return bestSet.toArray(new Transaction[bestSet.size()]);
    }

    private boolean isValidWithPool(Transaction tx, UTXOPool pool) {
        HashSet<UTXO> usedUtxos = new HashSet<>();
        double inSum = 0;
        double outSum = 0;

        for (int i = 0; i < tx.numInputs(); i++) {
            Transaction.Input in = tx.getInput(i);
            UTXO utxo = new UTXO(in.prevTxHash, in.outputIndex);
            if (!pool.contains(utxo) || usedUtxos.contains(utxo)) return false;
            Transaction.Output out = pool.getTxOutput(utxo);
            if (!out.address.verifySignature(tx.getRawDataToSign(i), in.signature)) return false;
            usedUtxos.add(utxo);
            inSum += out.value;
        }
        for (Transaction.Output out : tx.getOutputs()) {
            if (out.value < 0) return false;
            outSum += out.value;
        }
        return inSum >= outSum;
    }

    private void applyTxToPool(Transaction tx, UTXOPool pool) {
        for (Transaction.Input in : tx.getInputs()) {
            pool.removeUTXO(new UTXO(in.prevTxHash, in.outputIndex));
        }
        for (int i = 0; i < tx.numOutputs(); i++) {
            pool.addUTXO(new UTXO(tx.getHash(), i), tx.getOutput(i));
        }
    }
}