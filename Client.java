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
    private static int nounce=1000;
    
    
    private static final int BUFFER_SIZE = 1024;
    private static int messageId=0;
    private static String clientName;
    private static int seqNumber = 0;
    private static PublicKey publicKey;
    private static PrivateKey privateKey;
    private static final Object lock = new Object();

    public static void main(String[] args) throws Exception {
        String[] ports = new String[args.length-1];
        for(int i=1;i< args.length;i++){

            ports[i-1]=args[i];
        }
        clientName=args[0];
        Scanner myObj = new Scanner(System.in); 
         publicKey = loadPublicKeyFromFile(PUBLIC_KEY_FILE);
         privateKey = loadPrivateKeyFromFile(PRIVATE_KEY_FILE);
        

        // Create a DatagramSocket
        DatagramSocket socket = new DatagramSocket();

        while(true){
            System.out.println("Type something to server");
            String message ="Client_"+ clientName + '_' +  (seqNumber++) + '_' + myObj.nextLine();
            broadcast(message,ports);
            

        // Send the packet to the server
            

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


    private static byte[] sign(String message) throws Exception{
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

    public static void broadcast(String message, String[] ports) throws Exception{
        
        for (String port : ports) {
            
            

            final String arg = port;
            Thread thread = new Thread(new Runnable()  {
                public void run()  {
                    try{
                        System.out.println("sending to "+arg);
                        sendMessage(message,arg);
                    }catch(Exception e){
                        System.out.println("erro");
                    }
                    
                }
            });
            thread.start();
            
            
            
        }
    }

    private static void sendMessage(String message, String port) throws Exception{
        int messageNounce;
        synchronized (lock) {
            messageNounce=nounce;
            nounce++;
        }
        
        boolean responseReceived=false;
        
        DatagramSocket socket = new DatagramSocket();
        int timeout=5000;
        message= String.valueOf(messageNounce)+"_"+message;
        byte[] messageBytes= sign(message);
        InetAddress serverAddress = InetAddress.getByName("localhost");
        DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length, serverAddress, Integer.parseInt(port));
        
        // Send the packet to the server
        socket.setSoTimeout(timeout);
        
        
        while (!responseReceived) {
            // Send the packet to the server
            socket.send(packet);
            
            
            // Create a packet to receive the response from the server
            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

            try {
                
                // Wait for the response from the server
                socket.receive(receivePacket);
                
                // Print the response from the server
                String response = new String(receivePacket.getData(), 0, receivePacket.getLength());
                
                
                response=verifySign(response.getBytes());
                String[] tokens= response.split("_");
                //verify freshness
                
                if(Integer.parseInt(tokens[1])!=messageNounce){
                    System.out.println("Trying to corrupt the message");
                }
                else{
                    if(tokens[2].equals("ACK")){
                        System.out.println("Response Ok");
                        
                        
                    }
                    
                }
                    
                
                responseReceived = true;
            } catch (Exception e) {
                // If a timeout occurs, retry sending the message
                System.out.println("Timeout occurred, retrying... to "+port);
                
                
            }
        }
        
        if (!responseReceived) {
            System.out.println("No response received ");
        }
        socket.close();
        
            
         
    }
      


}






