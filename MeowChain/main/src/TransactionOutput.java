import java.security.PublicKey;

public class TransactionOutput {
    public String id;
    public PublicKey recipient;
    public float value;
    public String parentTransactionID;

    public TransactionOutput(PublicKey recipient, float value, String parentTransactionID){
        this.recipient = recipient;
        this.value = value;
        this.parentTransactionID = parentTransactionID;
        this.id = StringUtil.applySha256(StringUtil.getStringFromKey(recipient)+Float.toString(value)+parentTransactionID);

    }

    //check if coin belongs to you
    public boolean isMine(PublicKey publicKey){
        return (publicKey==recipient);
    }
}
