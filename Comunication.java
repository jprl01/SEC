import java.util.*;
import java.net.*;

import java.util.regex.PatternSyntaxException;


public class Comunication {
    static int NServers;
    static int messageId=0;
    static int nounce=1000;
    static int SERVER_PORT;
    private static final Object lock = new Object();

    //for client-side
    private static Map<String, List<String>> portsAcks = new HashMap<>();
    private static int neededResponses=0;
    private static int neededResponsesForNack=0;

    private static int quorum;

    public static void sendMessage(String message, String port, int id,String nounceR) throws Exception{
        int messageNounce;
        //int id;
        synchronized (lock) {
            if(Integer.parseInt(nounceR)!=-1){
                messageNounce=Integer.parseInt(nounceR);
            }else{
                messageNounce=nounce;            
                nounce++;
            }
            
            
        }
        System.out.println("sending to "+port);
        boolean responseReceived=false;
        
        DatagramSocket socket = new DatagramSocket();
        int timeout=5000;
        message= String.valueOf(SERVER_PORT)+"_"+String.valueOf(messageNounce)+"_"+String.valueOf(id)+"_"+message;
        //System.out.println("message to send "+message);
        byte[] messageBytes= Signer.sign(message);
        //System.out.println("\n\nsgined message "+new String(messageBytes));
        InetAddress serverAddress = InetAddress.getByName("localhost");
        DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length, serverAddress, Integer.parseInt(port));
        
        // Send the packet to the server
        socket.setSoTimeout(timeout);
        
        
        while (!responseReceived) {
            // Send the packet to the server
            socket.send(packet);
            
            
            
            
            // Create a packet to receive the response from the server
            byte[] receiveData = new byte[65000];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

            try {
                
                // Wait for the response from the server
                socket.receive(receivePacket);
                
                
                String response = new String(receivePacket.getData(), 0, receivePacket.getLength());               
                
                

                String[] tokens;
                
                tokens= response.split("_");
                
                //response to PRE-PREPARE = PREPARE
                
                
                
                if(tokens.length>3 && (tokens[3].equals("PREPARE") || tokens[3].equals("COMMIT")) ){
                    
                    //System.out.println("recebi prepare");                    
                    responseReceived = true;

                    Thread thread = new Thread(new Runnable()  {
                        public void run()  {
        
                            try{
                                Server.process(receivePacket);
                                
                            }catch(Exception e){
                                System.out.println("erro");
                                e.printStackTrace();
                            }
                            
                        }
                    });
                    thread.start();
                    
                }else{
                    response=Signer.verifySign(response.getBytes());
                }
                    

                //verify freshness
                System.out.println("Received response from Server port: "+tokens[0]);

                
                if(Integer.parseInt(tokens[1])!=messageNounce){
                    System.out.println("Trying to corrupt the message");
                    continue;
                }
                else{
                    System.out.println("Response Ok");                   
                }                   
                
                responseReceived = true;
            } catch (SocketTimeoutException e) {
                // If a timeout occurs, retry sending the message
                System.out.println("Timeout occurred, retrying... "+message);
                
                
            }catch(PatternSyntaxException e){
                System.out.println("Message format is incorret. Message will be ignored.");
                return;
            }
        }
        
        if (!responseReceived) {
            System.out.println("No response received ");
        }
        socket.close();
        
            
         
    }

    public static void broadcast(String message, String[] ports,Boolean send, Boolean leaderSent,String leaderPort,String nounce) throws Exception{
        int i=0;
        int id;
        synchronized (lock) {
            
            id=messageId;
            Server.setBroadcastId(id);
            messageId++;
        }
        for (String port : ports) {
            System.out.println("port "+i);
            if(i==NServers){
                break;
            }
            i++;
            if(send && port.equals(String.valueOf(SERVER_PORT))){
                continue;
            }
            if(leaderSent && port.equals(String.valueOf(Server.getLowestPort()))){
                //System.out.println("testar broadcast prepare");
                port=leaderPort;
            }


            final String arg = port;

            
            Thread thread = new Thread(new Runnable()  {
                public void run()  {

                    try{
                        
                        sendMessage(message,arg,id,nounce);
                        
                    }catch(Exception e){
                        System.out.println("erro");
                        e.printStackTrace();
                    }
                    
                }
            });
            thread.start();
            
            
            
        }
    }

    public static void sendMessageClient(String message, String port) throws Exception{
        int mult=1;
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
                

                if(noDupliactedPort){
                    //System.out.println("nounce: "+tokens[1]+" expected: "+messageNounce+" port: "+socket.getLocalPort());
                    
                    System.out.println("Nounce recebido: " + tokens[1]);
                    System.out.println(("Nounce esperado: " + messageNounce));

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
                        else if(tokens[2].equals("NACK")){
                            neededResponsesForNack++;
                            if(neededResponsesForNack>=quorum){
                                System.out.println("Command "+message+ " was not applied");
                                neededResponsesForNack=0;
                            }
                            System.out.println("Response NOk");                            
                        }
                    }                    
                    responseReceived = true;
                    
                }
                System.out.println("ACKS received: " + portsAcks);
                if(tokens.length == 5){
                    System.out.print("You have the following value in your account: " + tokens[4] + "\n");
                }


            } catch (SocketTimeoutException e) {
                // If a timeout occurs, retry sending the message
                mult++;
                socket.setSoTimeout(timeout*mult);
                System.out.println("Timeout occurred, retrying... to "+port+" message: "+message);
                
                
                
            }
        }
        
        if (!responseReceived) {
            System.out.println("No response received ");
        }
        socket.close();
        
            
        
    }

    public static void broadcastClient(String message, String[] ports) throws Exception{
        int i=0;
        for (String port : ports) {
            if(i==NServers)
                break;
            

            final String arg = port;
            Thread thread = new Thread(new Runnable()  {
                public void run()  {
                    try{
                        System.out.println("sending to "+arg);
                        sendMessageClient(message,arg);
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

    

    public static void setServerPort(int serverPort) {
        SERVER_PORT = serverPort;
    }

    public static void setNServers(int nServers) {
        NServers = nServers;
    }

    public static void setQuorum(int quor){
        quorum=quor;
    }

}