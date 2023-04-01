import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class Signer {

    static PrivateKey privatekey = null;
    static Map<String,PublicKey> publicKeys= new HashMap<>();

    static void loadPublicKeyFromFile(String name) throws Exception {
        String fileName = name+"Pub.key";
        byte[] keyBytes = Files.readAllBytes(Paths.get(fileName));
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey pubk = keyFactory.generatePublic(spec);

        publicKeys.put(name, pubk);
    }

    static void loadPrivateKeyFromFile(String name) throws Exception {
        String fileName = name+"Priv.key";
        byte[] keyBytes = Files.readAllBytes(Paths.get(fileName));
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PrivateKey rsaPrivateKey = keyFactory.generatePrivate(spec);

        privatekey = rsaPrivateKey;
    }

    static byte[] sign(String message) throws Exception{
        
        byte[] messageBytes = message.getBytes();
        Signature dsaForSign = Signature.getInstance("SHA1withRSA");
        dsaForSign.initSign(privatekey);
        dsaForSign.update(messageBytes);
        byte[] signature = dsaForSign.sign();

        messageBytes = (message+'\n').getBytes();

        //System.out.println("tamanho "+ messageBytes.length);

        String encodedString = Base64.getEncoder().encodeToString(signature);

        signature=encodedString.getBytes();

        String sig = new String(signature);
        //System.out.println("sig "+signature);

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
        System.out.println("Received message: "+str);

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




        System.out.println("Signature verifies: " + verifies+"\n");

        if(!verifies){
            return tokens[0]+"_NACK";
        }
        return str;
    }

    private static int indexOf(byte[] array, byte value) {
        for (int i = 0; i < array.length; i++) {
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
}

