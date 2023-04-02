public class Account {
    
        private String publicKey; 
        public String getPublicKey() {
            return publicKey;
        }

        public void setPublicKey(String publicKey) {
            this.publicKey = publicKey;
        }

        private String clientName; 
        public String getClientName() {
            return clientName;
        }

        public void setClientName(String clientName) {
            this.clientName = clientName;
        }

        private int value;
        
        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        public Account(String publicKey, String clientName, String value) {
            this.publicKey = publicKey;
            this.clientName = clientName;
            this.value = Integer.parseInt(value);
        }
}
