import java.util.HashMap;
import java.util.Map;

public class ServerManagement {
    private Map<byte[], Account> accounts = new HashMap<byte[], Account>();
    
    public void createAccount(String clientName, String initialValue, byte[] clientPublicKey){
        Account newAccount = new Account(clientPublicKey, clientName, initialValue);
        accounts.put(clientPublicKey, newAccount);
    }

    public int checkBalance(byte[]  clientPublicKey){
        
        return accounts.get(clientPublicKey).getValue();
    }
}
