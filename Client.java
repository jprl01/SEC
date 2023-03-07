import java.net.*;
import java.security.*;
import javax.crypto.*;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Scanner; 

public class Client {

    private static final String PUBLIC_KEY_FILE = "clientPub.key";
    private static final String PRIVATE_KEY_FILE = "clientPriv.key";
    private static final String AES_KEY_FILE = "aes.key";
    private static final int SERVER_PORT = 1234;
    private static final int BUFFER_SIZE = 1024;

    public static void main(String[] args) throws Exception {
        // Load RSA keys from files
        Scanner myObj = new Scanner(System.in); 
        PublicKey publicKey = loadPublicKeyFromFile(PUBLIC_KEY_FILE);
        PrivateKey privateKey = loadPrivateKeyFromFile(PRIVATE_KEY_FILE);
        SecretKey aesKey = loadAesKeyFromFile(AES_KEY_FILE);

        // Create a DatagramSocket
        DatagramSocket socket = new DatagramSocket();

        while(true){
            System.out.println("Type something to server");
            String message = myObj.nextLine();
            byte[] messageBytes = aesCypher(message,aesKey);
            InetAddress serverAddress = InetAddress.getByName("localhost");
            DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length, serverAddress, SERVER_PORT);

        // Send the packet to the server
            socket.send(packet);

        }
        // Create a message to be signed
        


        
        /*Signature dsaForSign = Signature.getInstance("SHA1withDSA");
        dsaForSign.initSign(privateKey);
        dsaForSign.update(messageBytes);
        byte[] signature = dsaForSign.sign();

        Signature dsaForVerify = Signature.getInstance("SHA1withDSA");
        dsaForVerify.initVerify(publicKey);
        dsaForVerify.update(messageBytes);
        boolean verifies = dsaForVerify.verify(signature);
        System.out.println("Signature verifies: " + verifies);*/

        // Create a DatagramPacket containing the message and the server address/port
        
        // Close the socket
        //socket.close();
    }

    private static PublicKey loadPublicKeyFromFile(String fileName) throws Exception {
        byte[] keyBytes = Files.readAllBytes(Paths.get(fileName));
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(spec);
    }

    private static PrivateKey loadPrivateKeyFromFile(String fileName) throws Exception {
        byte[] keyBytes = Files.readAllBytes(Paths.get(fileName));
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(spec);
    }

    private static SecretKey loadAesKeyFromFile(String fileName) throws Exception {
        File keyFile = new File(fileName);
        byte[] keyBytes = new byte[(int) keyFile.length()];
        try (DataInputStream dis = new DataInputStream(new FileInputStream(keyFile))) {
            dis.readFully(keyBytes);
        }
        SecretKey aesKey = new SecretKeySpec(keyBytes, "AES");
        return aesKey;
    }

    private static byte[] aesCypher(String message, SecretKey aesKey) throws Exception {
        aesKey.getEncoded();
        Cipher aesCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        aesCipher.init(Cipher.ENCRYPT_MODE, aesKey);
        byte[] cleartext = message.getBytes();
        // Cifar cleartext
        byte[] ciphertext = aesCipher.doFinal(cleartext);
        return ciphertext;
    }

    private static void aesDecypher( SecretKey aesKey, byte[] ciphertext) throws Exception {
        aesKey.getEncoded();
        Cipher aesCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        aesCipher.init(Cipher.DECRYPT_MODE, aesKey);
        // Decifrar criptograma
        byte[] cleartext1 = aesCipher.doFinal(ciphertext);

        String s = new String(cleartext1, "UTF-8"); // convert bytes to string
        System.out.println(s);
    }

}






