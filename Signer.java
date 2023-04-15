import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;
import java.nio.file.InvalidPathException;


public class Signer {

    static PrivateKey privatekey = null;
    static Map<String,PublicKey> publicKeys= new HashMap<>();

    static PublicKey loadPublicKeyFromFile(String name, boolean save) throws Exception {
        PublicKey pubk=null;
        
        try{
            String fileName = name+"Pub.key";
            byte[] keyBytes = Files.readAllBytes(Paths.get(fileName));
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            pubk = keyFactory.generatePublic(spec);

            if(save)
                publicKeys.put(name, pubk);
            
        }catch(IOException | SecurityException |InvalidPathException e){
            // e.printStackTrace();
            System.out.println("The client " + name + " is not known by the system.");
        }
        return pubk;
        
        
        
    }

    static void loadPrivateKeyFromFile(String name) throws Exception {
        try{
            String fileName = name+"Priv.key";
            byte[] keyBytes = Files.readAllBytes(Paths.get(fileName));
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey rsaPrivateKey = keyFactory.generatePrivate(spec);

            privatekey = rsaPrivateKey;
        }catch(IOException | SecurityException  |InvalidPathException e){
            e.printStackTrace();
        }
        
    }

    static byte[] sign(String message) throws Exception{
        
        byte[] messageBytes = message.getBytes();
        Signature dsaForSign = Signature.getInstance("SHA1withRSA");
        dsaForSign.initSign(privatekey);
        dsaForSign.update(messageBytes);
        byte[] signature = dsaForSign.sign();

        messageBytes = (message+'\n').getBytes();

        

        String encodedString = Base64.getEncoder().encodeToString(signature);

        signature=encodedString.getBytes();

        

        byte[] data = new byte[messageBytes.length + signature.length];
        System.arraycopy(messageBytes, 0, data, 0, messageBytes.length);
        System.arraycopy(signature, 0, data, messageBytes.length, signature.length);

        
        return data;
    }

    static String verifySign(byte[] data) throws Exception{
        PublicKey publicKey;
        int separatorIndex = indexOf(data, (byte)'\n');
        
        byte[] messageBytes = new byte[separatorIndex];
        byte[] signature = new byte[data.length-separatorIndex-1];

        System.arraycopy(data, 0, messageBytes, 0, separatorIndex);
        System.arraycopy(data, separatorIndex+1, signature, 0, data.length-separatorIndex-1);

        String str = new String(messageBytes, StandardCharsets.UTF_8);

        

        String[] tokens= str.split("_");
        if(tokens[1].equals("Client"))
            publicKey=publicKeys.get(tokens[2]);
        else
            publicKey=publicKeys.get(tokens[0]);

        Signature rsaForVerify = Signature.getInstance("SHA1withRSA");
        rsaForVerify.initVerify(publicKey);
        rsaForVerify.update(messageBytes);

        String sig = new String(signature);
        byte[] decodedBytes = Base64.getDecoder().decode(sig);


        boolean verifies = rsaForVerify.verify(decodedBytes);





        if(!verifies){
            return tokens[0]+"_NACK";
        }
        return str;
    }

    private static int indexOf(byte[] array, byte value) {
        

        for (int i = array.length - 1; i >= 0; i--) {
            if (array[i] == value) {
                return i;
                
            }
        }
        return -1;
    }

    

    public static PrivateKey getPrivatekey() {
        return privatekey;
    }

    public static void setPrivatekey(PrivateKey privKey) {
        privatekey = privKey;
    }

    public static PublicKey getPublicKey(String name){
        return publicKeys.get(name);
    }
}

