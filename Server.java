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

public class Server {

    private static final String PUBLIC_KEY_FILE = "clientPub.key";
    private static final String PRIVATE_KEY_FILE = "clientPriv.key";
    private static final String AES_KEY_FILE = "aes.key";
    private static  int SERVER_PORT ;
    private static final int BUFFER_SIZE = 1024;
    private static boolean leader=false;
    private static int round=1;

    public static void main(String[] args) throws Exception {
        SERVER_PORT=Integer.parseInt(args[0]);
        int lowestPort=Integer.parseInt(args[1]);
        String[] ports = new String[args.length-1];
        for(int i=1;i< args.length;i++){

            ports[i-1]=args[i];
        }
        if(lowestPort==SERVER_PORT){
            leader=true;
        } 

        
        // Load RSA keys from files
        PublicKey publicKey = loadPublicKeyFromFile(PUBLIC_KEY_FILE);
        PrivateKey privateKey = loadPrivateKeyFromFile(PRIVATE_KEY_FILE);
        SecretKey aesKey = loadAesKeyFromFile(AES_KEY_FILE);

        // Create a DatagramSocket
        DatagramSocket socket = new DatagramSocket(SERVER_PORT);
        
        
       

        //byte[] ciphertext = aesCypher("Hello",aesKey);
        //aesDecypher(aesKey,ciphertext);

        
        start(socket,ports,aesKey);
        while(true){
            byte[] data = new byte[BUFFER_SIZE];
            DatagramPacket receivePacket = new DatagramPacket(data, data.length);

            // Receive the packet from the client
            socket.receive(receivePacket);

            String receivedMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
            aesDecypher(aesKey,receivedMessage.getBytes());

            
            //System.out.println("Received from server: " + receivedMessage);
        }
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

    public static void start(DatagramSocket socket, String[] ports, SecretKey aesKey) throws Exception{
        Thread.sleep(1000);
        if(leader){
            String start ="PRE-PREPARE 1 "+ String.valueOf(round)+" ola";
            byte[] messageBytes = aesCypher(start,aesKey);
            for (String port : ports) {
                if(SERVER_PORT!= Integer.parseInt(port)){
                    InetAddress serverAddress = InetAddress.getByName("localhost");
                    DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length, serverAddress, Integer.parseInt(port));

                    // Send the packet to the server
                    socket.send(packet);
                }
                
            }
        }
        
    }

}





