import java.net.*;
import java.security.*;

import java.util.*;
import java.util.regex.PatternSyntaxException;
import java.io.IOException;



public class Server {

    private static final int BLOCK_SIZE=3;
    private static final Object lock = new Object();
    private static final Object lockServers = new Object();
    private static final Object lockPrepare = new Object();
    private static final Object lockCommit = new Object();
    
    private static  int SERVER_PORT ;
    private static final int BUFFER_SIZE = 1024;
    private static boolean leader=false;
    private static int round=1;
    private static Queue<String> queue = new LinkedList<>();
    private static Map<String, Integer> idRequests = new HashMap<>();
    private static Map<String, List<String>> clientsChain = new HashMap<>();
    private static Map<String,String> clientsSource = new HashMap<>();
    
    private static Map<String, Integer> consensusValuePrepare = new HashMap<>();
    private static Map<String, Integer> consensusValue = new HashMap<>();
    
    
    private static int nounce=1000;  

    private static Map<String, List<String>> portsPrepare = new HashMap<>();
    private static Map<String, List<String>> portsCommit = new HashMap<>();

    
    
    
    private static int messageId=0;
    private static int consensus_instance=0;
    private static boolean consensus_started=false;
    private static int nServers;
    private static int faults;
    private static int byznatineQuorum;
    private static String[] ports;
    private static int lowestPort;
    //private static Signer signer= null;


    public static void main(String[] args) throws Exception {
        //signer = new Signer();
        nServers=Integer.parseInt(args[0]);

        // nServers >= 3 * faults + 1;
        faults = (nServers  - 1)/3;
        byznatineQuorum=2*faults+1;
        SERVER_PORT=Integer.parseInt(args[1]);
        lowestPort=Integer.parseInt(args[2]);
         ports = new String[args.length-2];        
        // Load RSA keys from files

        for(int i=2;i< args.length;i++){
            Signer.loadPublicKeyFromFile(args[i]);           
             
            

            if(args[1].equals(args[i])){
                Signer.loadPrivateKeyFromFile(args[1]);
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
        
        String str = Signer.verifySign(receivedMessage.getBytes());
        //System.out.println("olaaa "+str);
        try{
            tokens= str.split("_");
        }
        catch(PatternSyntaxException e){
            System.out.println("Message format is incorret. Message will be ignored.");
            return;
        }
        
        if(tokens[1].equals("NACK")){
            String response = String.valueOf(SERVER_PORT)+"_"+str;
                    
            byte[] sendData = Signer.sign(response);
            sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
            socket.send(sendPacket);
            return;
        }
        
        
        System.out.println("%%%%%%%%%%%%%%%%");
        System.out.println(str);
        System.out.println("%%%%%%%%%%%%%%%%");
        if(tokens[1].equals("Client")){
            //clientsSource.put(tokens[2],clientAddress.getHostAddress()+"_"+clientPort);
            int idRequest=Integer.parseInt(tokens[3]);
            clientsSource.put(tokens[2]+idRequest,clientAddress.getHostAddress()+"_"+clientPort+"_"+tokens[0]);
            System.out.println("port para enviar: "+clientPort+"pedido id: "+tokens[3]);
                      
            synchronized(lock){
                if(!processIdRequest(tokens[2], idRequest)){
                    System.out.println(" comando errado");
                    return;
                }else{
                    System.out.println(" comando certo");
                    
                }

                if(consensus_started){  

                    //verify if order is correct
                    if(leader){
                                               
                            
                        queue.add(str);
                    }
                    

                    //receivedIds.add(tokens[2]+"_"+tokens[3]);
                    //expectedId++;

                    /*String response = String.valueOf(SERVER_PORT)+"_"+tokens[0]+"_ACK_" + idRequest;

                    System.out.println("\n\n\n\nMessage sent to client with ACK: " + response);

                    byte[] sendData = sign(response);
                    sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
                    socket.send(sendPacket);*/

                    return;
                }
                if(leader){
                    consensus_started=true;
                }
                
                
            }
            
            
            
            
            //String response = String.valueOf(SERVER_PORT)+"_"+tokens[0]+"_ACK_" + tokens[3];      
                      
            
            if(leader){
                
                
                command=str.substring(tokens[0].length()+tokens[1].length()+2);
                
                queue.add(str);

                System.out.println("queue "+queue.size());
                if(queue.size()==BLOCK_SIZE){
                    //consensus_started=true;
                    sendBlock();
                }
                else{
                    consensus_started=false;
                }
                
                
            }


            //expectedId++;
            /*
            System.out.println("\n\n\n\nMessage sent to client with ACK: " + response);
            boolean received=false;
            byte[] sendData = sign(response);
            sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
            socket.send(sendPacket);
            sendConfirmation(sendPacket,response);*/       
            
        }else{
            synchronized(lockServers){
                if(!processIdRequest(tokens[0], Integer.parseInt(tokens[2]))){
                    System.out.println("duplicated message");
                    return;
                }
            }
            command=str.substring(tokens[0].length()+tokens[1].length()+tokens[2].length()+3);
            
            
            String senderPort = tokens[0];
            
            String response = String.valueOf(SERVER_PORT)+"_"+tokens[1]+"_ACK";
            byte[] sendData = Signer.sign(response);
            sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
            socket.send(sendPacket);

            Thread thread = new Thread(new Runnable()  {
                public void run()  {
                    try{
                        boolean leaderSent=false;
                        if(lowestPort==Integer.parseInt(tokens[0]))
                            leaderSent=true;
                        System.out.println("analysing command "+command);
                        analyse_command(command,ports,leaderSent, senderPort,socket);
                        
                    }catch(Exception e){
                        System.out.println("erro");
                        e.printStackTrace();
                    }
                    
                }
            });
            thread.start();
            

            
        }

    }
    private static boolean processIdRequest(String client, int idRequest){
        
        if(!idRequests.containsKey(client)){
                    
            if(idRequest==0){
                System.out.println(" comando certo");
                idRequests.put(client,1);
            }                            
            else{
                System.out.println(" comando errado");
                
                return false;
            }
                
        }
        else if(idRequests.get(client)==idRequest){
            System.out.println("comando certo");
            idRequests.put(client,idRequest+1);
        }
        else{
            System.out.println(" comando errado");
            
            return false;
        }

        return true;
    }

    private static void sendBlock() throws Exception{
        
        Thread thread = new Thread(new Runnable()  {
            
            public void run()  {
                String block;
                String request=queue.poll();
                String tokens[]=request.split("_");
                block=request.substring(tokens[0].length()+tokens[1].length()+2);
                
                while(!queue.isEmpty()){
                    
                    request=queue.poll();
                    tokens=request.split("_");
                    block+=" "+request.substring(tokens[0].length()+tokens[1].length()+2);
                }
                try{
                    
                    consensus(block,ports);
                    
                    
                }catch(Exception e){
                    System.out.println("erro");
                    e.printStackTrace();
                }
                
            }
        });
        thread.start();
    }

    private static void sendConfirmation(DatagramPacket sendPacket,String response) throws Exception{
        
        DatagramSocket confSocket = new DatagramSocket();
            confSocket.setSoTimeout(5000);
            Thread thread = new Thread(new Runnable()  {
                public void run()  {
                    boolean received=false;
                    while(!received){
                        try{
                            
                            
                            confSocket.send(sendPacket);
                            
            
                            byte[] confirmationData = new byte[1024];
                            DatagramPacket confirmationPacket = new DatagramPacket(confirmationData, confirmationData.length);           
                            System.out.println("Enviar resposta: "+response);
                            confSocket.receive(confirmationPacket);
                            

                            String confirmationMessage = new String(confirmationPacket.getData(), 0, confirmationPacket.getLength());
                            System.out.print("confirmation "+confirmationMessage);
                            String conf = Signer.verifySign(confirmationMessage.getBytes());
                            received=true;
                                                                 
                        }catch(SocketTimeoutException e){
                            System.out.println("trying again...");
                        }catch(IOException e){
                            System.out.println("erro..");
                        }catch(Exception e){
                            System.out.println("erro..");
                        }
                    }
                    
                }
            });
            thread.start(); 
    }

    private static void analyse_command(String command,String ports[], boolean leaderSent, String senderPort,DatagramSocket socket) throws Exception{
        System.out.println("\n\n####################");
        System.out.println(command);
        System.out.println("Sender port: " + senderPort);
        System.out.println("####################\n\n");

        // String[] tokens= command.split("_");
        String[] tokens;
        try{
            tokens= command.split("_");
        }
        catch(PatternSyntaxException e){
            System.out.println("Message format is incorret. Message will be ignored.");
            return;
        }
        
        
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
                decide(command,socket);
            
            
        }else{
            System.out.println("Format not expected consensus"+consensus_instance);
        }
    }

    private static void decide(String command,DatagramSocket socket) throws Exception{
        
        
        
        
        parseCommand(command,socket);
        
        consensus_instance++;
                
        commmandsQueue();

    }


    public static void commmandsQueue() throws Exception{
        if(queue.size()==BLOCK_SIZE && leader){
            System.out.println("There are BLOCKS to run");
            sendBlock();
            
            
            
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
        int id;
        synchronized (lock) {
            
            id=messageId;
            
            messageId++;
        }
        for (String port : ports) {
            System.out.println("port "+i);
            if(i==nServers){
                break;
            }
            i++;
            /*if(Integer.parseInt(port)==SERVER_PORT){
                continue;
            }*/
                
            

            final String arg = port;
            Thread thread = new Thread(new Runnable()  {
                public void run()  {

                    try{
                        System.out.println("sending to "+arg);
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
   

    private static void consensus (String message, String[] ports) throws Exception{
        start(message,ports);

        
    }

    private static void parseCommand (String command,DatagramSocket socket) throws Exception{
        //2_1_Joao_1_adeus
        
        System.out.println("deciding command "+command);
        String[] tokens;
        
        try{
            tokens= command.split("_");
        }
        catch(PatternSyntaxException e){
            System.out.println("Message format is incorret. Message will be ignored.");
            return;
        }
        command=command.substring(tokens[0].length()+tokens[1].length() +2);

        String transactions[]=command.split(" ");

        for(int i=0;i<BLOCK_SIZE;i++){
            try{
                
                tokens= transactions[i].split("_");
            }
            catch(PatternSyntaxException e){
                System.out.println("Message format is incorret. Message will be ignored.");
                continue;
            }

            String client=tokens[0];
            String exec=tokens[2];
            String idRequest=tokens[1];

            if(clientsChain.containsKey(client)){
                clientsChain.get(client).add(exec);
                
            }else{
                
                clientsChain.put(client, new ArrayList<>());
                clientsChain.get(client).add(exec);
                
            }
            
            String clientSource[]=clientsSource.get(client+idRequest).split("_");
            String response = String.valueOf(SERVER_PORT)+"_"+clientSource[2]+"_ACK_" + idRequest;

            System.out.println("\n\n\n\nMessage sent to client with ACK: " + response);
            
            byte[] sendData = Signer.sign(response);
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,InetAddress.getByName(clientSource[0]), Integer.parseInt(clientSource[1]));
            socket.send(sendPacket);
        }
        
        
        
        
        System.out.println("Map of lists: " + clientsChain);
    }

    private static void sendMessage(String message, String port, int id) throws Exception{
        int messageNounce;
        //int id;
        synchronized (lock) {
            messageNounce=nounce;            
            nounce++;
            
        }
        
        boolean responseReceived=false;
        
        DatagramSocket socket = new DatagramSocket();
        int timeout=5000;
        message= String.valueOf(SERVER_PORT)+"_"+String.valueOf(messageNounce)+"_"+String.valueOf(id)+"_"+message;
        byte[] messageBytes= Signer.sign(message);
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
            } catch (SocketTimeoutException e) {
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








 




