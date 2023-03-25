import java.net.*;
import java.security.*;

import java.util.*;

// import javax.lang.model.util.ElementScanner14;
// import javax.sound.sampled.BooleanControl;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import java.nio.charset.StandardCharsets;

public class Server {

    
    private static final Object lock = new Object();
    private static final Object lockPrepare = new Object();
    private static final Object lockCommit = new Object();
    
    private static  int SERVER_PORT ;
    private static final int BUFFER_SIZE = 1024;
    private static boolean leader=false;
    private static int round=1;
    private static Queue<String> queue = new LinkedList<>();
    private static Map<String, Integer> clientsRequests = new HashMap<>();
    private static Map<String, List<String>> clientsChain = new HashMap<>();
    
    private static Map<String, Integer> consensusValuePrepare = new HashMap<>();
    private static Map<String, Integer> consensusValue = new HashMap<>();
    private static List<String> receivedIds = new ArrayList<>();
    private static Map<String,PublicKey> publicKeys= new HashMap<>();
    private static int nounce=1000;
    private static Map<String, List<String>> portsPrepare = new HashMap<>();
    private static Map<String, List<String>> portsCommit = new HashMap<>();

    
    
    
    private static PrivateKey privateKey;
    
    
    
    private static int messageId=0;
    private static int consensus_instance=1;
    private static boolean consensus_started=false;
    private static int nServers;
    private static int faults=1;
    private static int byznatineQuorum;
    private static String[] ports;
    private static int lowestPort;


    public static void main(String[] args) throws Exception {
        
        nServers=Integer.parseInt(args[0]);
        byznatineQuorum=2*faults+1;
        SERVER_PORT=Integer.parseInt(args[1]);
        lowestPort=Integer.parseInt(args[2]);
         ports = new String[args.length-2];        
        // Load RSA keys from files

        for(int i=2;i< args.length;i++){
            
            PublicKey pubKey;
            pubKey=loadPublicKeyFromFile(args[i]+"Pub.key");
            publicKeys.put(args[i],pubKey);
            
             
            

            if(args[1].equals(args[i])){
                privateKey=loadPrivateKeyFromFile(args[1]+"Priv.key");
            }
            ports[i-2]=args[i];
            
            
        }
        
        if(lowestPort==SERVER_PORT){
            leader=true;
            System.out.println("I am the leader server.");
        } 

        

        // Create a DatagramSocket
        DatagramSocket socket = new DatagramSocket(SERVER_PORT);
               
        
        
        while(true){
            
            
            byte[] data = new byte[BUFFER_SIZE];
            DatagramPacket receivePacket = new DatagramPacket(data, data.length);
            
            
            
            socket.receive(receivePacket);
            Thread thread = new Thread(new Runnable()  {
                public void run()  {
                    try{
                        
                        process(receivePacket,socket);
                        
                        
                    }catch(Exception e){
                        System.out.println("erro");
                        e.printStackTrace();
                    }
                    
                }
            });
            thread.start();
            //lala(receivePacket, socket);    
            // Receive the packet from the client
            
            
            
            
        }
        // Close the socket
        //socket.close();
        
        
    }

    private static void process(DatagramPacket receivePacket,DatagramSocket socket) throws Exception{
        String[] tokens;
        String command;
        DatagramPacket sendPacket;
        int clientPort = receivePacket.getPort();

            
        InetAddress clientAddress = receivePacket.getAddress();    
        String receivedMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
        
        String str = verifySign(receivedMessage.getBytes());
        tokens= str.split("_");
        
        if(tokens[1].equals("NACK")){
            String response = String.valueOf(SERVER_PORT)+"_"+str;
                    
            byte[] sendData = sign(response);
            sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
            socket.send(sendPacket);
            return;
        }
        
        
        System.out.println("%%%%%%%%%%%%%%%%");
        System.out.println(receivedMessage);
        System.out.println("%%%%%%%%%%%%%%%%");
        if(tokens[1].equals("Client")){
            int idRequest=Integer.parseInt(tokens[3]);
            if(receivedIds.contains(tokens[2]+"_"+tokens[3])){
                System.out.println("duplicated message");
                return;
                
            }   
            synchronized(lock){
                if(consensus_started){  

                    //verify if order is correct
                    if(leader){
                        System.out.println("Incrementar requests2");
                        if(clientsRequests.get(tokens[2])==idRequest){
                            System.out.println(" comando certo");
                            clientsRequests.put(tokens[2],idRequest+1);
                        }
                        else{
                            System.out.println(" comando errado");
                            return;
                        }
                            
                        queue.add(str);
                    }
                    

                    
                    

                    receivedIds.add(tokens[2]+"_"+tokens[3]);
                    String response = String.valueOf(SERVER_PORT)+"_"+tokens[0]+"_ACK_" + consensus_instance;

                    System.out.println("\n\n\n\nMessage sent to client with ACK: " + response);

                    byte[] sendData = sign(response);
                    sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
                    socket.send(sendPacket);
                    return;
                }
                    
                consensus_started=true;
            }
            
            
            String response;
            
                        
                
            response = String.valueOf(SERVER_PORT)+"_"+tokens[0]+"_ACK_" + consensus_instance;                   
            //receivedIds.add(tokens[2]+"_"+tokens[3]);
                
                        
            
            
            
            
            if(leader){
                
                
                command=str.substring(tokens[0].length()+tokens[1].length()+2);
                
                if(!clientsRequests.containsKey(tokens[2])){
                    
                    if(idRequest==0){
                        System.out.println(" comando certo");
                        clientsRequests.put(tokens[2],1);
                    }                            
                    else{
                        System.out.println(" comando errado");
                        consensus_started=false;
                        return;
                    }
                        
                }
                else if(clientsRequests.get(tokens[2])==idRequest){
                    System.out.println("comando certo");
                    clientsRequests.put(tokens[2],idRequest+1);
                }
                else{
                    System.out.println(" comando errado");
                    consensus_started=false;
                    return;
                }
                    



                Thread thread = new Thread(new Runnable()  {
                    public void run()  {
                        try{
                            
                            consensus(command,ports);
                            
                            
                        }catch(Exception e){
                            System.out.println("erro");
                            e.printStackTrace();
                        }
                        
                    }
                });
                thread.start();
                
            }

            receivedIds.add(tokens[2]+"_"+tokens[3]);

            System.out.println("\n\n\n\nMessage sent to client with ACK: " + response);

            byte[] sendData = sign(response);
            sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
            socket.send(sendPacket);
            

            
            
        }else{

            if(receivedIds.contains(tokens[0]+"_"+tokens[2])){
                System.out.println("duplicated message");
                return;
                
            }
            else{
                receivedIds.add(tokens[0]+"_"+tokens[2]);
            }
            //System.out.println("Received from port: "+tokens[0]);
            command=str.substring(tokens[0].length()+tokens[1].length()+tokens[2].length()+3);
            
            
            String senderPort = tokens[0];
            
            String response = String.valueOf(SERVER_PORT)+"_"+tokens[1]+"_ACK";
            byte[] sendData = sign(response);
            sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
            socket.send(sendPacket);

            Thread thread = new Thread(new Runnable()  {
                public void run()  {
                    try{
                        boolean leaderSent=false;
                        if(lowestPort==Integer.parseInt(tokens[0]))
                            leaderSent=true;
                        System.out.println("analysing command "+command);
                        analyse_command(command,ports,leaderSent, senderPort);
                        
                    }catch(Exception e){
                        System.out.println("erro");
                        e.printStackTrace();
                    }
                    
                }
            });
            thread.start();
            

            
        }

    }

    private static void analyse_command(String command,String ports[], boolean leaderSent, String senderPort) throws Exception{
        System.out.println("\n\n####################");
        System.out.println(command);
        System.out.println("Sender port: " + senderPort);
        System.out.println("####################\n\n");
        String[] tokens= command.split("_");
        
        if(tokens[0].equals("PRE-PREPARE") && tokens[1].equals(String.valueOf(consensus_instance)) && leaderSent){
            
            command=command.substring(12);
            
            String prepare="PREPARE_"+command;
            
            System.out.println("Broadcasting PREPARE");
            broadcast(prepare,ports);
            
            
        }
        else if(tokens[0].equals("PREPARE") && tokens[1].equals(String.valueOf(consensus_instance))){
            boolean broadcast=false;
            command=command.substring(8);
            String commit="COMMIT_"+command;
            int requests;
            boolean noDupliactedPort = false;

            synchronized(lockPrepare){

                if(portsPrepare.containsKey(tokens[1]))
                {
                    List<String> listPortsPrepare = portsPrepare.get(tokens[1]);
                    if(listPortsPrepare.contains(senderPort)){
                        System.out.println("The Server " + senderPort + " has already sent a PREPARE message for consensus instance " + tokens[1]);
                    }
                    else{
                        portsPrepare.get(tokens[1]).add(senderPort);
                        noDupliactedPort = true;
                    }
                }
                else{
                    portsPrepare.put(tokens[1], new ArrayList<>());
                    portsPrepare.get(tokens[1]).add(senderPort);
                    noDupliactedPort = true;
                }

                if(noDupliactedPort){
                    if(!consensusValuePrepare.containsKey(tokens[3]+"_"+tokens[4]+"_"+tokens[5])){
                        requests=1;
                    }else{
                        requests=consensusValuePrepare.get(tokens[3]+"_"+tokens[4]+"_"+tokens[5])+1;
                        
                    }
                    consensusValuePrepare.put(tokens[3]+"_"+tokens[4]+"_"+tokens[5],requests);
                    
                    if(consensusValuePrepare.get(tokens[3]+"_"+tokens[4]+"_"+tokens[5])>=byznatineQuorum){
                        consensusValuePrepare.put(tokens[3]+"_"+tokens[4]+"_"+tokens[5],0);
                        System.out.println("Broadcasting COMMIT");
                        broadcast=true;  
                    }
                }
            }
            if(broadcast)
                broadcast(commit, ports);
            
            
        }
        else if(tokens[0].equals("COMMIT") && tokens[1].equals(String.valueOf(consensus_instance))){
            boolean decide=false;
            command=command.substring(7);
            int requests;
            boolean noDupliactedPort = false;

            synchronized(lockCommit){

                if(portsCommit.containsKey(tokens[1]))
                {
                    List<String> listPortsCommit = portsCommit.get(tokens[1]);
                    if(listPortsCommit.contains(senderPort)){
                        System.out.println("The Server " + senderPort + " has already sent a COMMIT message for consensus instance " + tokens[1]);
                    }
                    else{
                        portsCommit.get(tokens[1]).add(senderPort);
                        noDupliactedPort = true;
                    }
                }
                else{
                    portsCommit.put(tokens[1], new ArrayList<>());
                    portsCommit.get(tokens[1]).add(senderPort);
                    noDupliactedPort = true;
                }

                if(noDupliactedPort){
                    if(!consensusValue.containsKey(tokens[3]+"_"+tokens[4]+"_"+tokens[5])){
                        requests=1;
                    }else{
                        requests=consensusValue.get(tokens[3]+"_"+tokens[4]+"_"+tokens[5])+1;
                        
                    }
                    consensusValue.put(tokens[3]+"_"+tokens[4]+"_"+tokens[5],requests); 
                    
                    
                    System.out.println("commits received "+consensusValue.get(tokens[3]+"_"+tokens[4]+"_"+tokens[5]));
                    if(consensusValue.get(tokens[3]+"_"+tokens[4]+"_"+tokens[5])>=byznatineQuorum){
                        consensusValue.put(tokens[3]+"_"+tokens[4]+"_"+tokens[5],0);
                        System.out.println("Deciding COMMIT");
                        decide=true;
                        
                    }
                }


            }
            if(decide)
                decide(command);
            
            
        }
    }

    private static void decide(String command) throws Exception{
        
        
        
        
        parseCommand(command);
        
        consensus_instance++;
                
        commmandsQueue();

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
        PublicKey publicKey;
        byte[] messageBytes = new byte[data.length-684];
        byte[] signature = new byte[684];

        System.arraycopy(data, 0, messageBytes, 0, data.length-684);
        System.arraycopy(data, data.length-684, signature, 0, 684);

        String str = new String(messageBytes, StandardCharsets.UTF_8);
        System.out.println("Received message: "+str);

        String[] tokens= str.split("_");
        if(tokens[1].equals("Client"))
            publicKey=publicKeys.get(tokens[2]);
        else
            publicKey=publicKeys.get(tokens[0]);

        Signature rsaForVerify = Signature.getInstance("SHA1withRSA");
        rsaForVerify.initVerify(publicKey);
        rsaForVerify.update(messageBytes);

        String sig = new String(signature);
        byte[] decodedBytes = Base64.getDecoder().decode(sig);
        
        
        boolean verifies = rsaForVerify.verify(decodedBytes);
        
        
        
        
        System.out.println("Signature verifies: " + verifies+"\n");

        if(!verifies){
            return tokens[0]+"_NACK";
        }
        return str;
    }   
    public static void commmandsQueue() throws Exception{
        if(!queue.isEmpty()){
            System.out.println("There are commands to run");
            
            String str=queue.remove();
            
            String[]tokens= str.split("_");
            String command=str.substring(tokens[0].length()+tokens[1].length()+2);
            
            if(leader){
                Thread thread = new Thread(new Runnable()  {
                    public void run()  {
                        try{
                            //System.out.println("consensus "+command);
                            consensus(command,ports);
                            //System.out.println("consensus "+command);
                            
                        }catch(Exception e){
                            System.out.println("erro");
                            e.printStackTrace();
                        }
                        
                    }
                });
                thread.start();
            }
            
            
        }
        else{
            System.out.println("Nothing to update");
            consensus_started=false;
        }
    }
    

    public static void start( String message,String[] ports) throws Exception{
        
        if(leader){
            String start ="PRE-PREPARE_"+String.valueOf(consensus_instance)+"_"+ String.valueOf(round)+"_"+message;
            

            broadcast(start, ports);
            
            
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
        //2_1_Joao_1_adeus
        String[] tokens= command.split("_");
        
        if(clientsChain.containsKey(tokens[2])){
            clientsChain.get(tokens[2]).add(tokens[4]);
            
        }else{
            
            clientsChain.put(tokens[2], new ArrayList<>());
            clientsChain.get(tokens[2]).add(tokens[4]);
            
        }
        System.out.println("Map of lists: " + clientsChain);
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
        message= String.valueOf(SERVER_PORT)+"_"+String.valueOf(messageNounce)+"_"+String.valueOf(messageId++)+"_"+message;
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
                System.out.println("Received response from Server port: "+tokens[0]);

                
                if(Integer.parseInt(tokens[1])!=messageNounce){
                    System.out.println("Trying to corrupt the message");
                }
                else{
                    if(tokens[2].equals("ACK")){
                        //System.out.println("Response Ok");
                        
                        
                    }
                    
                }
                    
                
                responseReceived = true;
            } catch (Exception e) {
                // If a timeout occurs, retry sending the message
                System.out.println("Timeout occurred, retrying...");
                
                
            }
        }
        
        if (!responseReceived) {
            System.out.println("No response received ");
        }
        socket.close();
        
            
         
    }
}








 




