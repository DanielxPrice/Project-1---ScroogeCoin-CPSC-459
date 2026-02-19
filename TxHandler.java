import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Iterator;

public class TxHandler {

	private UTXOPool utxoPool;
	/* Creates a public ledger whose current UTXOPool (collection of unspent 
	 * transaction outputs) is utxoPool. This should make a defensive copy of 
	 * utxoPool by using the UTXOPool(UTXOPool uPool) constructor.
	 */
	public TxHandler(UTXOPool utxoPool) {
		// IMPLEMENT THIS
		this.utxoPool = new UTXOPool(utxoPool);
	}

	/* Returns true if 
	 * (1) all outputs claimed by tx are in the current UTXO pool, 
	 * (2) the signatures on each input of tx are valid, 
	 * (3) no UTXO is claimed multiple times by tx, 
	 * (4) all of tx’s output values are non-negative, and
	 * (5) the sum of tx’s input values is greater than or equal to the sum of   
	        its output values;
	   and false otherwise.
	 */

	public boolean isValidTx(Transaction tx) {

        HashSet<UTXO> usedUtxos = new HashSet<>();
        double inputSum = 0.0;
        double outputSum = 0.0;

        // Check inputs
        for (int i = 0; i < tx.numInputs(); i++) {
            Transaction.Input in = tx.getInput(i);

            UTXO utxo = new UTXO(in.prevTxHash, in.outputIndex);

            // (1) must exist in current pool
            if (!utxoPool.contains(utxo)) {
                return false;
            }

            // (3) no double claim
            if (usedUtxos.contains(utxo)) {
                return false;
            }
            usedUtxos.add(utxo);

            Transaction.Output prevOutput = utxoPool.getTxOutput(utxo);

            // (2) signature must be valid
            byte[] message = tx.getRawDataToSign(i);
            if (!prevOutput.address.verifySignature(message, in.signature)) {
                return false;
            }

            inputSum += prevOutput.value;
        }

        // Check outputs
        for (int i = 0; i < tx.numOutputs(); i++) {
            Transaction.Output out = tx.getOutput(i);

            // (4) no negative outputs
            if (out.value < 0) {
                return false;
            }

            outputSum += out.value;
        }

        // (5) input sum must be >= output sum
        if (inputSum < outputSum) {
            return false;
        }

        return true;
    }

	/* Handles each epoch by receiving an unordered array of proposed 
	 * transactions, checking each transaction for correctness, 
	 * returning a mutually valid array of accepted transactions, 
	 * and updating the current UTXO pool as appropriate.
	 */
	public Transaction[] handleTxs(Transaction[] possibleTxs) {

        List<Transaction> remaining = new ArrayList<>(Arrays.asList(possibleTxs));
        List<Transaction> accepted = new ArrayList<>();

        boolean progress = true;

        // Keep trying to accept transactions until no more can be added
        while (progress) {
            progress = false;

            Iterator<Transaction> it = remaining.iterator();

            while (it.hasNext()) {
                Transaction tx = it.next();

                if (isValidTx(tx)) {

                    // Remove spent UTXOs
                    for (int i = 0; i < tx.numInputs(); i++) {
                        Transaction.Input in = tx.getInput(i);
                        UTXO spent = new UTXO(in.prevTxHash, in.outputIndex);
                        utxoPool.removeUTXO(spent);
                    }

                    // Add new UTXOs created by this transaction
                    byte[] txHash = tx.getHash();
                    for (int i = 0; i < tx.numOutputs(); i++) {
                        UTXO newUtxo = new UTXO(txHash, i);
                        utxoPool.addUTXO(newUtxo, tx.getOutput(i));
                    }

                    accepted.add(tx);
                    it.remove();
                    progress = true;
                }
            }
        }

        return accepted.toArray(new Transaction[0]);
    }

} 
