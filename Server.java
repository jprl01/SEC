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

    private static String PUBLIC_KEY_FILE = "clientPub.key";
    private static String PRIVATE_KEY_FILE = "clientPriv.key";
    private static String SERVER1_PUBLIC_KEY_FILE ;
    private static String SERVER1_PRIVATE_KEY_FILE;
    private static String SERVER2_PUBLIC_KEY_FILE ;
    private static String SERVER2_PRIVATE_KEY_FILE;
    private static String SERVER3_PUBLIC_KEY_FILE ;
    private static String SERVER3_PRIVATE_KEY_FILE;
    private static String SERVER4_PUBLIC_KEY_FILE ;
    private static String SERVER4_PRIVATE_KEY_FILE;
    
    private static  int SERVER_PORT ;
    private static final int BUFFER_SIZE = 1024;
    private static boolean leader=false;
    private static int round=1;
    private static Map<String, Integer> clientsRequests = new HashMap<>();
    private static int nounce=1000;
    private static int messagesId=0;
    private static List<Integer> receivedIds = new ArrayList<>();
    private static final int timeout = 5000; // 5 seconds
    private static final int maxRetries = 10;
    private static PublicKey publicKey;
    private static PrivateKey privateKey;
    
    private static int quorum=0;
    private static int consensus_instance=1;


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

            
            
            String receivedMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
            String str = verifySign(receivedMessage.getBytes());
            String[] tokens= str.split("_");
            
            if(tokens[0].equals("Client")){
                if(leader){
                    String command=str.substring(7);
                    consensus(command,ports);
                }
                
                
            }else{

                String command=str.substring(tokens[0].length()+1);
                
                
                InetAddress clientAddress = receivePacket.getAddress();
                int clientPort = receivePacket.getPort();
                String response = tokens[0]+"_"+tokens[1]+"_ACK";
                byte[] sendData = sign(response);
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
                socket.send(sendPacket);

                analyse_command(command,ports);
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

    private static void analyse_command(String command,String ports[]) throws Exception{
        String[] tokens= command.split("_");
        
        if(tokens[0].equals("PRE-PREPARE")){
            command=command.substring(12);
            
            String prepare="PREPARE_"+command;
            System.out.println("Broadcasting");
            broadcast(prepare,ports);
        }
        else if(tokens[0].equals("PREPARE")){
            command=command.substring(8);
            String commit="COMMIT_"+command;
            System.out.println("Broadcasting");
            broadcast(commit,ports);
        }
        else if(tokens[0].equals("COMMIT")){
            //command=command.substring(8);
            //String commit="COMMIT_"+command;
            //broadcast(commit,ports);
        }
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

    

    public static void start( String message,String[] ports) throws Exception{
        
        if(leader){
            String start ="PRE-PREPARE_"+String.valueOf(consensus_instance)+"_"+ String.valueOf(round)+"_"+message;
            
            broadcast(start, ports);
            
            Thread.sleep(5000);
            System.out.println(quorum);
            if(quorum>=2){
                System.out.println("Majority received");
            }

            
        }
        
    }

    public static void broadcast(String message, String[] ports) throws Exception{
        
        for (String port : ports) {
            
            if(SERVER_PORT!= Integer.parseInt(port)){

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

    private static void consensus (String message, String[] ports) throws Exception{
        start(message,ports);

        
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
        int messageId=messagesId;
        messagesId++;
        System.out.println("MESSAGE ID:" + messageId);
        boolean responseReceived=false;
        
        DatagramSocket socket = new DatagramSocket();
        int timeout=5000;
        message= String.valueOf(messageNounce)+"_"+String.valueOf(messageId)+"_"+message;
        byte[] messageBytes= sign(message);
        InetAddress serverAddress = InetAddress.getByName("localhost");
        DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length, serverAddress, Integer.parseInt(port));
        
        // Send the packet to the server
        socket.setSoTimeout(timeout);
        
        
        while (!responseReceived) {
            // Send the packet to the server
            socket.send(packet);
            
            // just for test, remove later
            System.out.println("MESSAGE SENT: " + message);
            
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
                if(Integer.parseInt(tokens[0])!=messageNounce){
                    System.out.println("Trying to corrupt the message");
                }
                else{
                    //verify if is duplicate
                    if(receivedIds.contains(Integer.parseInt(tokens[1]))){
                        System.out.println("Duplicate message");
                    }
                    else{
                        receivedIds.add(Integer.parseInt(tokens[1]));
                        if(tokens[2].equals("ACK")){
                            System.out.println("Response Ok");
                            
                            quorum++;
                        }
                    }
                }
                    
                
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








 




