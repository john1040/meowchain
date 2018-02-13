import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Wallet {
    public PrivateKey privateKey;
    public PublicKey publicKey;
    public HashMap<String, TransactionOutput> UTXOs; //only UXTOs owned by this wallet

    public Wallet(){
        UTXOs = new HashMap<>();
        generateKeyPair();
    }

    public void generateKeyPair(){
        try{
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDSA", "BC");
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            ECGenParameterSpec ecSpec = new ECGenParameterSpec("prime192v1");
            //initiallize generator and generate key pair
            keyGen.initialize(ecSpec, random);
            KeyPair keyPair = keyGen.generateKeyPair();
            // set public and private keys from keyPair
            privateKey = keyPair.getPrivate();
            publicKey = keyPair.getPublic();
        }catch(Exception e){
            throw new RuntimeException();
        }
    }

    //returns balance and stores the UTXO's owned by this wallet in this.UTXOs
    public float getBalance(){
        float total = 0;
        for(Map.Entry<String, TransactionOutput> item: MeowChain.UTXOs.entrySet()){
            TransactionOutput UTXO = item.getValue();
            if(UTXO.isMine(publicKey)){ //if output belongs to me ( if coins belong to me )
                UTXOs.put(UTXO.id, UTXO); //add it to our list of unspent transactions.
                total += UTXO.value;
            }
        }
        return total;
    }

    //Generates and returns a new transaction from this wallet
    public Transaction sendFunds(PublicKey _recipient, float value){
        if(getBalance()<value){
            System.out.println("#Not Enough funds to send transaction. Transaction Discarded.");
            return null;
        }
        //create arraylist of inputs
        ArrayList<TransactionInput> inputs = new ArrayList<>();

        float total = 0;
        for(Map.Entry<String, TransactionOutput> item: MeowChain.UTXOs.entrySet()){
            TransactionOutput UTXO = item.getValue();
            total += UTXO.value;
            inputs.add(new TransactionInput(UTXO.id));
            if(total > value) break;

        }

        Transaction newTransaction = new Transaction(publicKey, _recipient,value, inputs);
        newTransaction.generateSignature(privateKey);

        for(TransactionInput input: inputs){
            UTXOs.remove(input.transactionOutputID);
        }

        return newTransaction;
    }
}
