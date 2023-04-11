import java.net.*;


import java.util.*;
import java.security.PublicKey;

import java.util.Scanner;
import java.util.regex.PatternSyntaxException;


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
    

    public static void main(String[] args) throws Exception {
        nServers=Integer.parseInt(args[1]);
        faults = (nServers  - 1)/3; 
        quorum=faults+1;
        clientName=args[0];
        
        String[] ports = new String[args.length-2];
        for(int i=2;i< args.length;i++){
            Signer.loadPublicKeyFromFile(args[i],true);   
            ports[i-2]=args[i];

            if(clientName.equals(args[i])){
                
                Signer.loadPrivateKeyFromFile(args[i]);
            }
        }
        
        Scanner myObj = new Scanner(System.in); 
         
         
        
        // Create a DatagramSocket
        //DatagramSocket socket = new DatagramSocket();
        
        System.out.println("Welcome " + args[0] + "!\n");
        while(true){
            String message;
            System.out.println("\nPlease, request an available option:\n");
            System.out.print("CreateAccount_PubKey_initial Balance.\n");
            System.out.print("CheckBalance_PubKey.\n");
            System.out.print("Transfer_SourcePubKey_DestinationPubKey_Amount.\n");
            System.out.print("Exit.\n");
            String command= myObj.nextLine();

            String request=parseCommand(command);
            if(request==null){
               continue;
            }
            message ="Client_"+ clientName + '_' +  (messageId++) + '_' + request;
            Thread thread = new Thread(new Runnable()  {
                public void run()  {
                    try{
                        
                        
                        broadcast(message,ports);
                        
                    }catch(Exception e){
                        System.out.println("erro");
                        e.printStackTrace();
                    }
                    
                }
            });
            thread.start();
            
            
            

        // Send the packet to the server
        }
        
        


        
    }

    private static String parseCommand(String command) throws Exception{
        String[] tokens;
        try{
            tokens= command.split("_");
        }
        catch(PatternSyntaxException e){
            System.out.println("Message format is incorret. Message will be ignored.");
            return null;
        }
        if(tokens[0].equals("CreateAccount")){
            if(tokens.length!=3){
                System.out.println("CreateAccount needs 3 arguments");
                return null;
            }
                
            PublicKey pubAccountKey=Signer.loadPublicKeyFromFile(tokens[1],false);
            String publicKeyString = Base64.getEncoder().encodeToString(pubAccountKey.getEncoded());
            //initial balance
            if(Integer.parseInt(tokens[2])<=0){
                System.out.println("CreateAccount needs a positive initial balance");
                return null;
            }
            return tokens[0]+"_"+publicKeyString+"_"+tokens[2];
           

        }else if(tokens[0].equals("Transfer")){
            if(tokens.length!=4){
                System.out.println("Transfer needs 4 arguments");
                return null;
            }
            PublicKey pubSourceKey=Signer.loadPublicKeyFromFile(tokens[1],false);
            PublicKey pubDestKey=Signer.loadPublicKeyFromFile(tokens[2],false);

            String publicSourceString = Base64.getEncoder().encodeToString(pubSourceKey.getEncoded());
            String publicDestString = Base64.getEncoder().encodeToString(pubDestKey.getEncoded());

            if(Integer.parseInt(tokens[3])<=0){
                System.out.println("Transfer needs a positive ammount to be transfered");
                return null;
            }

            return tokens[0]+"_"+publicSourceString+"_"+publicDestString+"_"+tokens[3] + "_"+tokens[2];
                
            
        }else if(tokens[0].equals("CheckBalance")){
            if(tokens.length!=2){
                System.out.println("CheckBalance needs 2 arguments");
                return null;
            }

            PublicKey pubAccountKey=Signer.loadPublicKeyFromFile(tokens[1],false);
            String publicKeyString = Base64.getEncoder().encodeToString(pubAccountKey.getEncoded());

            return tokens[0]+"_"+publicKeyString;
            
        }else{
            System.out.println("Unknown command");
            return null;
        }

        //return command;
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
        System.out.println("server port nounce: "+messageNounce+" port: "+socket.getLocalPort());
        int timeout=5000;
        message= String.valueOf(messageNounce)+"_"+message;
        byte[] messageBytes= Signer.sign(message);
        //messageBytes=Signer.sign(new String(String.valueOf(messageNounce).getBytes()+"_".getBytes()+message.getBytes()));
        //String ola =new String(messageBytes);
        //messageBytes=ola.getBytes();
        InetAddress serverAddress = InetAddress.getByName("localhost");
        DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length, serverAddress, Integer.parseInt(port));
        
        // Send the packet to the server
        socket.setSoTimeout(timeout);
        
        
        while (!responseReceived) {
            // Send the packet to the server
            socket.send(packet);
            //socket.send(packet);
            
            
            // Create a packet to receive the response from the server
            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

            try {
                
                // Wait for the response from the server
                socket.receive(receivePacket);
                
                // Print the response from the server
                String response = new String(receivePacket.getData(), 0, receivePacket.getLength());
                
                
                response=Signer.verifySign(response.getBytes());
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
                synchronized(lock){
                    if(tokens[2].equals("ACK")){

                        if(portsAcks.containsKey(tokens[3])){
                            //vSystem.out.println("HERE");
                            List<String> acksReceived = portsAcks.get(tokens[3]);
                            // tokens[0] = server port
                            if(acksReceived.contains(tokens[0])){
                                System.out.println("The Server " + tokens[0] + " has already sent an ACK for request " + tokens[3]);
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
                    }
                }
                

                if(noDupliactedPort){
                    //System.out.println("nounce: "+tokens[1]+" expected: "+messageNounce+" port: "+socket.getLocalPort());
                    if(Integer.parseInt(tokens[1])!=messageNounce){
                        System.out.println("Trying to corrupt the message");
                        return;
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
                if(tokens.length == 5){
                    System.out.print("You have the following value in your account: " + tokens[4] + "\n");
                }


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






