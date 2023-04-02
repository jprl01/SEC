import java.util.HashMap;
import java.util.Map;

public class ServerManagement {
    private Map<String, Account> accounts = new HashMap<String, Account>();
    
    public void createAccount(String clientName, String initialValue){
        Account newAccount = new Account("clientPublicKey", clientName, initialValue);
        accounts.put("clientPublicKey", newAccount);
    }

    public int checkBalance(String publicKey){
        
        return accounts.get("clientPublicKey").getValue();
    }
}
