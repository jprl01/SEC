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
import java.nio.charset.StandardCharsets;

public class Server {

    private static final String PUBLIC_KEY_FILE = "clientPub.key";
    private static final String PRIVATE_KEY_FILE = "clientPriv.key";
    
    private static  int SERVER_PORT ;
    private static final int BUFFER_SIZE = 1024;
    private static boolean leader=false;
    private static int round=1;
    private static Map<String, Integer> clientsRequests = new HashMap<>();
    private static int nounce=1000;
    private static final int timeout = 5000; // 5 seconds
    private static final int maxRetries = 10;
    private static PublicKey publicKey;
    private static PrivateKey privateKey;

    public static void main(String[] args) throws Exception {
        boolean ola=false;
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
        publicKey = loadPublicKeyFromFile(PUBLIC_KEY_FILE);
        privateKey = loadPrivateKeyFromFile(PRIVATE_KEY_FILE);
        

        // Create a DatagramSocket
        DatagramSocket socket = new DatagramSocket(SERVER_PORT);
        
        
       

        //byte[] ciphertext = aesCypher("Hello",aesKey);
        //aesDecypher(aesKey,ciphertext);

        
        //start(socket,ports);
        
        while(true){
            byte[] data = new byte[BUFFER_SIZE];
            DatagramPacket receivePacket = new DatagramPacket(data, data.length);

            // Receive the packet from the client
            socket.receive(receivePacket);

            
            System.out.println("huiiii");
            String receivedMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
            String command = verifySign(receivedMessage.getBytes());
            String[] tokens= receivedMessage.split("_");
            
            if(leader && tokens[0].equals("Client")){
                
                broadcast(command, ports);
            }else{
                
                InetAddress clientAddress = receivePacket.getAddress();
                int clientPort = receivePacket.getPort();
                String response = tokens[0]+"_recebi bro";
                byte[] sendData = sign(response);
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
                socket.send(sendPacket);
            }
            
            

            //parseCommand(command);
            //sendMessage(receivedMessage, "1235", socket);
            /*if(SERVER_PORT==1234 && ola==false){
                ola=true;
                broadcast(receivedMessage, ports);
            }else{*/
                
            
            
                
            
            

            
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

    private static String verifySign(byte[] data) throws Exception{
        
        byte[] messageBytes = new byte[data.length-512];
        byte[] signature = new byte[512];

        System.arraycopy(data, 0, messageBytes, 0, data.length-512);
        System.arraycopy(data, data.length-512, signature, 0, 512);
        Signature rsaForVerify = Signature.getInstance("SHA1withRSA");
        rsaForVerify.initVerify(publicKey);
        rsaForVerify.update(messageBytes);
        boolean verifies = rsaForVerify.verify(signature);
        
        
        String str = new String(messageBytes, StandardCharsets.UTF_8);
        System.out.println("Received message: "+str);
        
        System.out.println("Signature verifies: " + verifies);

        return str;
    }   

    

    public static void start( String[] ports) throws Exception{
        Thread.sleep(1000);
        if(leader){
            String start ="PRE-PREPARE_1_"+ String.valueOf(round)+" ola";
            
            broadcast(start, ports);
            
        }
        
    }

    public static void broadcast(String message, String[] ports) throws Exception{
        
        for (String port : ports) {
            
            if(SERVER_PORT!= Integer.parseInt(port)){

                final String arg = port;
                Thread thread = new Thread(new Runnable()  {
                    public void run()  {
                        try{
                            sendMessage(message,arg);
                        }catch(Exception e){
                            System.out.println("erro");
                        }
                        
                    }
                });
                thread.start();
                
            }
            
        }
    }

    private static byte[] sign(String message) throws Exception{
        byte[] messageBytes = message.getBytes();
        Signature dsaForSign = Signature.getInstance("SHA1withRSA");
        dsaForSign.initSign(privateKey);
        dsaForSign.update(messageBytes);
        byte[] signature = dsaForSign.sign();
        System.out.println(signature.length);
        byte[] data = new byte[messageBytes.length + signature.length];

        System.arraycopy(messageBytes, 0, data, 0, messageBytes.length);
        System.arraycopy(signature, 0, data, messageBytes.length, signature.length);

        return data;
    }

    private static void consensus ( String[] ports) throws Exception{
        start(ports);
    }

    private static void parseCommand (String command){
        String[] tokens= command.split("_");
        int requestId=Integer.parseInt(tokens[1]);
        if(clientsRequests.containsKey(tokens[0])){
            if(requestId==clientsRequests.get(tokens[0])){
                clientsRequests.put(tokens[0],requestId++);
            }
        }else{
            if(requestId==0){
                clientsRequests.put(tokens[0],requestId++);
            }
        }
    }

    private static void sendMessage(String message, String port) throws Exception{
        int messageNounce=nounce;
        nounce++;
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
                
                String[] tokens= response.split("_");
                verifySign(response.getBytes());
                //verify freshness
                if(Integer.parseInt(tokens[0])!=messageNounce){
                    System.out.println("Trying to corrupt the message");
                }
                else
                    nounce++;
                
                responseReceived = true;
            } catch (Exception e) {
                // If a timeout occurs, retry sending the message
                System.out.println("Timeout occurred, retrying...");
                e.printStackTrace();
                
            }
        }
        
        if (!responseReceived) {
            System.out.println("No response received after " + maxRetries + " retries.");
        }
        socket.close();
        
            
         
    }
}








 




