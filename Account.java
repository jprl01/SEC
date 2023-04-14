import java.security.PublicKey;

public class Account {
    
        private PublicKey publicKey;
        private String clientName;
        private int value;
        private byte[] accountHash;

        public Account(PublicKey publicKey, String clientName, String value) {
            this.publicKey = publicKey;
            this.clientName = clientName;
            this.value = Integer.parseInt(value);
        }


        public PublicKey getPublicKey() {
            return publicKey;
        }

        public void setPublicKey(PublicKey publicKey) {
            this.publicKey = publicKey;
        }

         
        public String getClientName() {
            return clientName;
        }

        public void setClientName(String clientName) {
            this.clientName = clientName;
        }

        
        
        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        public void setAccountHash(byte[] hash){
            this.accountHash=hash;
        }

        public byte[] getAccountHash(){
            return this.accountHash;
        }
        
}