package Tests;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
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
import java.util.Scanner;

public class DuplicateMessageClient {

    
    private static int nounce=1000;
    private static Map<String,PublicKey> publicKeys= new HashMap<>();
    
    
    private static int messageId=0;
    private static String clientName;
    
    private static int nServers;
    
    private static PrivateKey privateKey;
    private static final Object lock = new Object();
    private static int neededResponses=0;
    private static int faults=1;
    private static int quorum;
    public static void main(String[] args) throws Exception {
        quorum=faults+1;
        clientName=args[0];
        nServers=Integer.parseInt(args[1]);
        String[] ports = new String[args.length-2];
        for(int i=2;i< args.length;i++){
            PublicKey pubKey;
            pubKey=loadPublicKeyFromFile(args[i]+"Pub.key");
            publicKeys.put(args[i],pubKey);
            ports[i-2]=args[i];

            if(clientName.equals(args[i])){
                
                privateKey = loadPrivateKeyFromFile(args[i]+"Priv.key");
            }
        }
        
        Scanner myObj = new Scanner(System.in); 
         
         
        

        // Create a DatagramSocket
        DatagramSocket socket = new DatagramSocket();
        
        while(true){
            String message;
            System.out.println("Type something to server");
            
            message ="Client_"+ clientName + '_' +  (messageId++) + '_' + myObj.nextLine();
            
            
            broadcast(message,ports);
            

        // Send the packet to the server
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
        PublicKey publicKey;
        byte[] messageBytes = new byte[data.length-684];
        byte[] signature = new byte[684];

        System.arraycopy(data, 0, messageBytes, 0, data.length-684);
        System.arraycopy(data, data.length-684, signature, 0, 684);

        String str = new String(messageBytes, StandardCharsets.UTF_8);
        System.out.println("Received message: "+str);

        String[] tokens= str.split("_");
        publicKey=publicKeys.get(tokens[0]);

        Signature rsaForVerify = Signature.getInstance("SHA1withRSA");
        rsaForVerify.initVerify(publicKey);
        rsaForVerify.update(messageBytes);

        String sig = new String(signature);
        byte[] decodedBytes = Base64.getDecoder().decode(sig);
        
        
        boolean verifies = rsaForVerify.verify(decodedBytes);
        
        
        
        
        System.out.println("Signature verifies: " + verifies);

        return str;
    }

    public static void broadcast(String message, String[] ports) throws Exception{
        int i=0;
        for (String port : ports) {
            if(i==nServers)
                break;
            

            final String arg = port;
            Thread thread = new Thread(new Runnable()  {
                public void run()  {
                    try{
                        System.out.println("sending to "+arg);
                        sendMessage(message,arg);
                    }catch(Exception e){
                        System.out.println("erro");
                        e.printStackTrace();
                    }
                    
                }
            });
            thread.start();
            i++;
            
            
            
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
                        neededResponses++;
                        if(neededResponses>=quorum){
                            System.out.println("Command "+message+ " was applied");
                            neededResponses=0;
                        }
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

        //This if block is exclusive to this class implementation in order to test the duplicate Message that might occur
        if (port.equals("1234")){
            System.out.println("\nSENDING DUPLICATE TO 1234\n");
            socket.send(packet);
        }
        socket.close();
        
    }
      


}






