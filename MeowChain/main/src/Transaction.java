import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;

public class Transaction {
    public String transactionID; //this is also the hash of the transaction
    public PublicKey sender; // sender's address/public key
    public PublicKey recipient; // recipient's address/public key
    public float value;
    public byte[] signature; // to prevent anybody from spending funds in our wallet

    public ArrayList<TransactionInput> inputs = new ArrayList<TransactionInput>();
    public ArrayList<TransactionOutput> outputs = new ArrayList<TransactionOutput>();

    private static int sequence = 0; // a rough count of amt of transaction generated

    public Transaction(PublicKey from, PublicKey to, float value, ArrayList<TransactionInput> inputs){
        this.sender = from;
        this.recipient = to;
        this.value = value;
        this.inputs = inputs;
    }

    //this calculates the transaction hash (which will be used as its ID
    private String calculateHash(){
        sequence++;
        return StringUtil.applySha256(StringUtil.getStringFromKey(sender) +
                StringUtil.getStringFromKey(recipient) +
                Float.toString(value) +
                sequence);
    }

    //signs all the data we dont wish to be tampered with
    public void generateSignature(PrivateKey privateKey){
        String data = StringUtil.getStringFromKey(sender)+StringUtil.getStringFromKey(recipient)+Float.toString(value);
        signature = StringUtil.applyECDSASig(privateKey,data);
    }

    //verifies the data we signed hasny been tampered with
    public boolean verifySignature(){
        String data = StringUtil.getStringFromKey(sender)+StringUtil.getStringFromKey(recipient)+Float.toString(value);
        return StringUtil.verifyECDSASig(sender,data,signature);
    }

    //returns true if new transaction could be created
    public boolean processTransaction(){
        if(verifySignature()==false){
            System.out.println("#transaction signature failed to verify");
            return false;
        }

        //gather transaction inputs to make sure they're unspent
        for(TransactionInput i: inputs){
            i.UTXO = MeowChain.UTXOs.get(i.transactionOutputID);
        }

        //check if transaction is valid
        if(getInputsValue()<MeowChain.minimumTransaction){
            System.out.println("#transaction inputs to small:" + getInputsValue());
            return false;
        }

        //generate transaction outputs
        float leftOver = getInputsValue() - value; //getvalue of inputs the the leftover change
        transactionID = calculateHash();
        outputs.add(new TransactionOutput(this.recipient, value, transactionID)); //send value to recipient
        outputs.add(new TransactionOutput(this.sender, leftOver,transactionID)); //send the leftover 'change' back to sender

        //add outputs to unspent list
        for(TransactionOutput o: outputs){
            MeowChain.UTXOs.put(o.id, o);
        }

        //remove transaction inputs from UTXO lists as spent
        for(TransactionInput i: inputs){
            if(i.UTXO == null) continue; //if transaction cant be found, skip it
            MeowChain.UTXOs.remove(i.UTXO.id);
        }

        return true;
    }

    //returns sum of inputs (UTXOs) values
    public float getInputsValue(){
        float total = 0;
        for(TransactionInput i: inputs){
            if(i.UTXO==null) continue; //if transaction cant be found, skip it
            total += i.UTXO.value;
        }
        return total;
    }

    //returns sum of outputs
    public float getOutputsValue(){
        float total = 0;
        for(TransactionOutput o: outputs){
            total += o.value;
        }
        return total;
    }


}
