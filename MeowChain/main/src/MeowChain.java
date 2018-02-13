import java.security.Security;
import java.util.ArrayList;
import java.util.HashMap;

public class MeowChain {
    public static ArrayList<Block> blockchain = new ArrayList<>();
    public static HashMap<String, TransactionOutput> UTXOs = new HashMap<>();

    public static int difficulty = 3;
    public static float minimumTransaction = 0.1f;
    public static Wallet walletA;
    public static Wallet walletB;
    public static Transaction genesisTransaction;

    public static void main(String[] args){
        //setup bouncy castle as a security Provider
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        //create new wallets
        walletA = new Wallet();
        walletB = new Wallet();
        Wallet coinbase = new Wallet();

        //create genesis transaction, which sends 100 MeowCoin walletA;
        genesisTransaction = new Transaction(coinbase.publicKey, walletA.publicKey, 100f, null);
        genesisTransaction.generateSignature(coinbase.privateKey);
        genesisTransaction.transactionID = "0";
        genesisTransaction.outputs.add(new TransactionOutput(genesisTransaction.recipient, genesisTransaction.value, genesisTransaction.transactionID));
        UTXOs.put(genesisTransaction.outputs.get(0).id, genesisTransaction.outputs.get(0));

        System.out.println("creating and mining genesis block...");
        Block genesis = new Block("0");
        genesis.addTransaction(genesisTransaction);
        addBlock(genesis);
        System.out.println("here");


        //testing
        Block block1 = new Block(genesis.hash);
        System.out.println("\nWalletA's balance is: " + walletA.getBalance());
        System.out.println("\nWalletA is Attempting to send funds (40) to WalletB...");
        block1.addTransaction(walletA.sendFunds(walletB.publicKey, 40f));
        addBlock(block1);
        System.out.println("\nWalletA's balance is: " + walletA.getBalance());
        System.out.println("WalletB's balance is: " + walletB.getBalance());

        Block block2 = new Block(block1.hash);
        System.out.println("\nWalletA Attempting to send more funds (1000) than it has...");
        block2.addTransaction(walletA.sendFunds(walletB.publicKey, 1000f));
        addBlock(block2);
        System.out.println("\nWalletA's balance is: " + walletA.getBalance());
        System.out.println("WalletB's balance is: " + walletB.getBalance());

        Block block3 = new Block(block2.hash);
        System.out.println("\nWalletB is Attempting to send funds (20) to WalletA...");
        block3.addTransaction(walletB.sendFunds( walletA.publicKey, 20));
        System.out.println("\nWalletA's balance is: " + walletA.getBalance());
        System.out.println("WalletB's balance is: " + walletB.getBalance());

        isChainValid();

    }

    public static Boolean isChainValid(){
        Block currBlock;
        Block prevBlock;
        String hashTarget = new String(new char[difficulty]).replace('\0', '0');
        HashMap<String, TransactionOutput> tempUTXOs = new HashMap<>();
        tempUTXOs.put(genesisTransaction.outputs.get(0).id, genesisTransaction.outputs.get(0));

        //loop through blockchain to check hahshes
        for(int i = 1; i < blockchain.size(); i++){
            currBlock = blockchain.get(i);
            prevBlock = blockchain.get(i-1);


            if(!currBlock.hash.equals(currBlock.calculateHash())){
                System.out.println("current hash not equal");
                return false;
            }
            if(!prevBlock.hash.equals(currBlock.previousHash)){
                System.out.println("Previous hashes not equal");
                return false;
            }
            // check if hash is solved
            if(!currBlock.hash.substring(0, difficulty).equals(hashTarget)){
                System.out.println("this block hasnt been mined");
                return false;
            }

            //loop through blockchain transactions
            TransactionOutput tempOutput;
            for(int j=0; j<currBlock.transactions.size(); j++){
                Transaction currentTransaction = currBlock.transactions.get(j);

                if(!currentTransaction.verifySignature()){
                    System.out.println("#Signature on Transaction(" + j + ") is Invalid");
                    return false;
                }
                if(currentTransaction.getInputsValue() != currentTransaction.getOutputsValue()){
                    System.out.println("#Inputs are note equal to outputs on Transaction(" + j + ")");
                    return false;
                }

                for(TransactionInput input: currentTransaction.inputs){
                    tempOutput = tempUTXOs.get(input.transactionOutputID);

                    if(tempOutput==null){
                        System.out.println("#Referenced input on Transaction(" + j + ") is Missing");
                        return false;
                    }

                    if(input.UTXO.value!=tempOutput.value){
                        System.out.println("#Referenced input Transaction(" + j + ") value is Invalid");
                        return false;
                    }

                    tempUTXOs.remove(input.transactionOutputID);
                }

                for(TransactionOutput output: currentTransaction.outputs){
                    tempUTXOs.put(output.id, output);
                }

                if(currentTransaction.outputs.get(0).recipient != currentTransaction.recipient){
                    System.out.println("#Transaction(" + j + ") output reciepient is not who it should be");
                    return false;
                }

                if(currentTransaction.outputs.get(1).recipient != currentTransaction.sender){
                    System.out.println("#Transaction(" + j + ") output 'change' is not sender.");
                    return false;
                }
            }
        }
        System.out.println("Blockchain is valid");
        return true;
    }

    public static void addBlock(Block newBlock){
        newBlock.mineBlock(difficulty);
        blockchain.add(newBlock);
    }
}
