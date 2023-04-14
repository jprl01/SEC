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
    private static Map<String,List<Integer>> readsValues = new HashMap<>();
    private static Map<String,Integer> responsesReceived =new HashMap<>();
    //private static int neededResponses=0;
    private static String[] portsS;
    private static int quorum;
    private static int Byzantinequorum;

    public static void sendMessage(String message, String port, int id) throws Exception{
        int messageNounce;
        //int id;
        synchronized (lock) {
            
            messageNounce=nounce;            
            nounce++;
            
            
            
        }
        System.out.println("sending to "+port);
        boolean responseReceived=false;
        
        DatagramSocket socket = new DatagramSocket();
        int timeout=5000;
        message= String.valueOf(SERVER_PORT)+"_"+String.valueOf(messageNounce)+"_"+String.valueOf(id)+"_"+message;
        
        byte[] messageBytes= Signer.sign(message);
        
        InetAddress serverAddress = InetAddress.getByName("localhost");
        DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length, serverAddress, Integer.parseInt(port));
        
       
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
                
                
                
                
                
                
                response=Signer.verifySign(response.getBytes());
                
                    

                //verify freshness
                System.out.println("Received response from Server port: "+tokens[0]);

                
                if(Integer.parseInt(tokens[1])!=messageNounce){
                    System.out.println("Trying to corrupt the message");
                    return;
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
                socket.close();
                return;
            }catch(SocketException e){                
                e.printStackTrace();
                socket.close();
                return;
            }
        }
        
        if (!responseReceived) {
            System.out.println("No response received ");
        }
        socket.close();
        
            
         
    }

    public static void broadcast(String message, String[] ports) throws Exception{
        int i=0;
        int id;
        synchronized (lock) {
            
            id=messageId;
            
            messageId++;
        }
        for (String port : ports) {
            System.out.println("port "+i);
            if(i==NServers){
                break;
            }
            i++;
            
            


            final String arg = port;

            
            Thread thread = new Thread(new Runnable()  {
                public void run()  {

                    try{
                        
                        sendMessage(message,arg,id);
                        
                    }catch(Exception e){
                        System.out.println("erro");
                        e.printStackTrace();
                    }
                    
                }
            });
            thread.start();
            
            
            
        }
    }

    public static void sendMessageClient(String message, String port,int CheckBalance) throws Exception{
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
        String sendMessage= String.valueOf(messageNounce)+"_"+message;
        byte[] messageBytes= Signer.sign(sendMessage);
        
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
                    //each server can only send one response
                    if(portsAcks.containsKey(tokens[3])){
                        
                        List<String> acksReceived = portsAcks.get(tokens[3]);
                        
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
                    
                    if(Integer.parseInt(tokens[1])!=messageNounce){
                        System.out.println("Trying to corrupt the message");
                        return;
                    }
                    else{
                        if(tokens[2].equals("ACK")){

                            if(!responsesReceived.containsKey(tokens[3])){
                                responsesReceived.put(tokens[3],1);
                            }else{
                                responsesReceived.put(tokens[3],responsesReceived.get(tokens[3])+1);
                            }
                            
                            //strongPhase1
                            if(CheckBalance==1){
                                
                                if(!readsValues.containsKey(tokens[3])){
                                    readsValues.put(tokens[3],new ArrayList<>());
                                }
                                readsValues.get(tokens[3]).add(Integer.parseInt(tokens[4]));

                                //check if all received values are the same
                                if(responsesReceived.get(tokens[3])>=Byzantinequorum){
                                    responsesReceived.put(tokens[3],0);
                                    int value=-1;
                                    int auxValue=-1;
                                    boolean allSame=true;
                                    for(int values: readsValues.get(tokens[3])){
                                        
                                        if(value==-1){
                                            value=values;
                                            auxValue=values;
                                            continue;
                                        }
                                        else if(auxValue!=-1 && values!=auxValue){
                                            allSame=false;
                                            break;
                                        }
                                        auxValue=values;
                                    }
                                    if(allSame){
                                        System.out.println(message);
                                        System.out.print("You have the following value in your account: " + tokens[4] + "\n");
                                    }else{
                                        //if some value is different ask for consensus

                                        int id =Client.incMessageId();
                                        String tokens2[] =message.split("_");
                                        String phase2= tokens2[0]+"_"+tokens2[1]+"_"+id+"_StrongCheckBalancePhase2_"+tokens2[4];

                                        Thread thread = new Thread(new Runnable()  {
                                            public void run()  {
                                                try{
                                                    broadcastClient(phase2, portsS, 2);       
                                                    
                                                    
                                                }catch(Exception e){
                                                    System.out.println("erro");
                                                    e.printStackTrace();
                                                }
                                                
                                            }
                                        });
                                        thread.start();
                                        

                                    }
                                }

                            }else if(CheckBalance==2){
                                if(responsesReceived.get(tokens[3])>=Byzantinequorum){
                                    responsesReceived.put(tokens[3],0);
                                    System.out.print("You have the following value in your account: " + tokens[4] + "\n");
                                }
                                
                                //System.out.println("hello");
                            }else{
                                if(responsesReceived.get(tokens[3])>=quorum){
                                    System.out.println("Command "+message+ " was applied");
                                    responsesReceived.put(tokens[3],0);
                                }
                            }
                            
                            System.out.println("Response Ok");                            
                        }
                    }                    
                    responseReceived = true;
                    
                }
                System.out.println("ACKS received: " + portsAcks);
                


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

    public static void broadcastClient(String message, String[] ports, int CheckBalance) throws Exception{
        int i=0;
        for (String port : ports) {
            if(i==NServers)
                break;
            

            final String arg = port;
            Thread thread = new Thread(new Runnable()  {
                public void run()  {
                    try{
                        System.out.println("sending to "+arg);
                        sendMessageClient(message,arg,CheckBalance);
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

    public static void setByzantineQuorum(int quor){
        Byzantinequorum=quor;
    }

    public static void setPorts(String[] ports){
        portsS=ports;
    }

}