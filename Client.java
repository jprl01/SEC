import java.net.*;
import java.security.*;

import java.util.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import java.util.Scanner;
import java.util.regex.PatternSyntaxException;
import java.nio.charset.StandardCharsets;

public class Client {

    
    private static int nounce=1000;
    
    private static int messageId=0;
    private static String clientName;
    
    private static int nServers;
    
    private static final Object lock = new Object();
    private static int neededResponses=0;
    private static int faults=1;
    private static int quorum;
    private static Map<String, List<String>> portsAcks = new HashMap<>();
    private static Signer signer = null;
    public static void main(String[] args) throws Exception {
        signer = new Signer();
        quorum=faults+1;
        clientName=args[0];
        nServers=Integer.parseInt(args[1]);
        String[] ports = new String[args.length-2];
        for(int i=2;i< args.length;i++){
            signer.loadPublicKeyFromFile(args[i]);
            ports[i-2]=args[i];

            if(clientName.equals(args[i])){
                signer.loadPrivateKeyFromFile(args[i]);
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
        byte[] messageBytes= signer.sign(message);
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
                
                
                response=signer.verifySign(response.getBytes());
                // String[] tokens= response.split("_");
                String[] tokens;
                try{
                    tokens= response.split("_");
                }
                catch(PatternSyntaxException e){
                    System.out.println("Message format is incorret. Message will be ignored.");
                    return;
                }

                //verify freshness
                System.out.println("SERVER MESSAGE: " + response);
                System.out.println("\n\n\nConsensus instance:" + tokens[3]);

                boolean noDupliactedPort = false;

                // tokens[3] = consensus instance
                if(portsAcks.containsKey(tokens[3])){
                    //vSystem.out.println("HERE");
                    List<String> acksReceived = portsAcks.get(tokens[3]);
                    // tokens[0] = server port
                    if(acksReceived.contains(tokens[0])){
                        System.out.println("The Server " + tokens[0] + " has already sent an ACK for consensus instance " + tokens[3]);
                    }
                    else{
                        portsAcks.get(tokens[3]).add(tokens[0]);
                        noDupliactedPort = true;
                    }
                    
                }else{
                    portsAcks.put(tokens[3], new ArrayList<>());
                    portsAcks.get(tokens[3]).add(tokens[0]);
                    noDupliactedPort = true;                    
                }

                if(noDupliactedPort){
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
                    /*int confPort = receivePacket.getPort();           
                    InetAddress confAddress = receivePacket.getAddress();
                    System.out.println("bahhhhhhhhh");
                    byte[] confirmation=sign(Integer.parseInt(tokens[1])+"_ACK");
                    DatagramPacket confpacket = new DatagramPacket(confirmation, confirmation.length, confAddress, confPort);
                    socket.send(confpacket);*/
                }
                System.out.println("ACKS received: " + portsAcks);


            } catch (SocketTimeoutException e) {
                // If a timeout occurs, retry sending the message
                System.out.println("Timeout occurred, retrying... to "+port+" message: "+message);
                
                
                
            }
        }
        
        if (!responseReceived) {
            System.out.println("No response received ");
        }
        socket.close();
        
            
        
    }
      


}






