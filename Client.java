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
import java.nio.charset.StandardCharsets;

public class Client {

    private static final String PUBLIC_KEY_FILE = "clientPub.key";
    private static final String PRIVATE_KEY_FILE = "clientPriv.key";
    
    private static final int SERVER_PORT = 1234;
    private static final int BUFFER_SIZE = 1024;
    private static String clientName;
    private static int seqNumber = 0;
    private static PublicKey publicKey;
    private static PrivateKey privateKey;

    public static void main(String[] args) throws Exception {
        // Load RSA keys from files
        clientName=args[0];
        Scanner myObj = new Scanner(System.in); 
         publicKey = loadPublicKeyFromFile(PUBLIC_KEY_FILE);
         privateKey = loadPrivateKeyFromFile(PRIVATE_KEY_FILE);
        

        // Create a DatagramSocket
        DatagramSocket socket = new DatagramSocket();

        while(true){
            System.out.println("Type something to server");
            String message ="Client_"+ clientName + '_' +  (seqNumber++) + '_' + myObj.nextLine();
            byte[] data = sign(message,privateKey);
            
            
            
            InetAddress serverAddress = InetAddress.getByName("localhost");
            DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, SERVER_PORT);

        // Send the packet to the server
            socket.send(packet);

        }
        // Create a message to be signed
        


        
        
/* 
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
        PrivateKey rsaPrivateKey = keyFactory.generatePrivate(spec);

        return rsaPrivateKey;
    }


    private static byte[] sign(String message, PrivateKey privateKey) throws Exception{
        byte[] messageBytes = message.getBytes();
        Signature dsaForSign = Signature.getInstance("SHA1withRSA");
        dsaForSign.initSign(privateKey);
        dsaForSign.update(messageBytes);
        byte[] signature = dsaForSign.sign();
        
        
        
        String encodedString = Base64.getEncoder().encodeToString(signature);
        
        signature=encodedString.getBytes();
        
        byte[] data = new byte[messageBytes.length + signature.length];
        System.arraycopy(messageBytes, 0, data, 0, messageBytes.length);
        System.arraycopy(signature, 0, data, messageBytes.length, signature.length);

        return data;
    }


    private static String verifySign(byte[] data) throws Exception{
        
        byte[] messageBytes = new byte[data.length-684];
        byte[] signature = new byte[684];

        System.arraycopy(data, 0, messageBytes, 0, data.length-684);
        System.arraycopy(data, data.length-684, signature, 0, 684);
        Signature rsaForVerify = Signature.getInstance("SHA1withRSA");
        rsaForVerify.initVerify(publicKey);
        rsaForVerify.update(messageBytes);

        String sig = new String(signature);
        byte[] decodedBytes = Base64.getDecoder().decode(sig);
        
        
        boolean verifies = rsaForVerify.verify(decodedBytes);
        
        
        String str = new String(messageBytes, StandardCharsets.UTF_8);
        System.out.println("Received message: "+str);
        
        System.out.println("Signature verifies: " + verifies);

        return str;
    }
      


}






