import java.net.*;


import java.util.*;
import java.util.regex.PatternSyntaxException;
import java.io.IOException;



public class Server {

    private static final int BLOCK_SIZE=1;
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
    
    

    private static Map<String, List<String>> portsPrepare = new HashMap<>();
    private static Map<String, List<String>> portsCommit = new HashMap<>();

    private static String[] sourcePrep =new String[3];
    static int ind=0;
    private static int broadcastId;
    //private static boolean broadcast=false;
    
    
    private static int consensus_instance=0;
    private static boolean consensus_started=false;
    private static int nServers;
    private static int faults;
    private static int byznatineQuorum;
    private static String[] ports;
    private static int lowestPort;
    private static DatagramSocket serverSocket;
    //private static Signer signer= null;
    //private static Comunication comunication=null;


    public static void main(String[] args) throws Exception {
        //signer = new Signer();
        //comunication = new Comunication();
        
        nServers=Integer.parseInt(args[0]);

        // nServers >= 3 * faults + 1;
        faults = (nServers  - 1)/3;
        byznatineQuorum=2*faults+1;
        SERVER_PORT=Integer.parseInt(args[1]);
        lowestPort=Integer.parseInt(args[2]);
         ports = new String[args.length-2];        
        // Load RSA keys from files

        Comunication.setServerPort(SERVER_PORT);
        Comunication.setNServers(nServers);

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
        serverSocket = new DatagramSocket(SERVER_PORT);
               
        
        
        while(true){
            
            
            byte[] data = new byte[BUFFER_SIZE];
            DatagramPacket receivePacket = new DatagramPacket(data, data.length);
            
            
            
            serverSocket.receive(receivePacket);
            Thread thread = new Thread(new Runnable()  {
                public void run()  {
                    try{
                        
                        process(receivePacket);
                        
                        
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

    public static void process(DatagramPacket receivePacket) throws Exception{
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
            serverSocket.send(sendPacket);
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
                    

                    return;
                }
                if(leader){
                    consensus_started=true;
                }
                
                
            }
            
            
            
            
              
                      
            
            if(leader){
                
                
                //command=str.substring(tokens[0].length()+tokens[1].length()+2);
                
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

     
            
        }else{

            /*if(!tokens[4].equals(String.valueOf(consensus_instance))){
                return;
            }*/
            synchronized(lockServers){
                if(!processIdRequest(tokens[0], Integer.parseInt(tokens[2]))){
                    System.out.println("duplicated message");
                    return;
                }
            }
            command=str.substring(tokens[0].length()+tokens[1].length()+tokens[2].length()+3);
            
            
            String senderPort = tokens[0];
            
            if((!"PRE-PREPARE".equals(tokens[3]) /*|| ("PRE-PREPARE".equals(tokens[3]) && lowestPort==SERVER_PORT)*/) && !"PREPARE".equals(tokens[3])){
                String response = String.valueOf(SERVER_PORT)+"_"+tokens[1]+"_ACK";
                byte[] sendData = Signer.sign(response);
                sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
                serverSocket.send(sendPacket);
            }   
            

            Thread thread = new Thread(new Runnable()  {
                public void run()  {
                    try{
                        boolean leaderSent=false;
                        if(lowestPort==Integer.parseInt(tokens[0]))
                            leaderSent=true;
                        System.out.println("analysing command "+command);
                        analyse_command(command,ports,leaderSent, senderPort,receivePacket.getPort(),tokens[1]);
                        
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
                /*if(client.equals(String.valueOf(SERVER_PORT))){
                    idRequests.put(client,3);
                }else{
                    
                }*/
                idRequests.put(client,1);
                
            }                            
            else{
                System.out.println(" comando errado");
                
                return false;
            }
                
        }
        else if(idRequests.get(client)==idRequest ){
            System.out.println("comando certo");
            /*if(client.equals(String.valueOf(SERVER_PORT))){
                idRequests.put(client,idRequest+3);
            }else{
                
            }*/
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
                int i=1;
                String block;
                String request=queue.poll();
                String tokens[]=request.split("_");
                block=request.substring(tokens[0].length()+tokens[1].length()+2);
                
                while(!queue.isEmpty() ){
                    if(i==BLOCK_SIZE){
                        break;
                    }
                    request=queue.poll();
                    tokens=request.split("_");
                    block+=" "+request.substring(tokens[0].length()+tokens[1].length()+2);
                    i++;
                    
                    
                        
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

    

    private static void analyse_command(String command,String ports[], boolean leaderSent, String senderPort,int socketPort,String nounceR) throws Exception{
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
            Comunication.broadcast(prepare,ports,true,leaderSent,String.valueOf(socketPort),nounceR);
            
            
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
                    if(requests>byznatineQuorum){
                        System.out.println("prepare extra");
                        Comunication.sendMessage(commit, String.valueOf(socketPort), broadcastId, "-1");
                        return;
                    }
                    
                    System.out.println("indiceeeeeeeeeeeee "+ind);
                    sourcePrep[ind]=String.valueOf(socketPort);
                    ind++;
                    if(consensusValuePrepare.get(tokens[3]+"_"+tokens[4]+"_"+tokens[5])>=byznatineQuorum){
                        //consensusValuePrepare.put(tokens[3]+"_"+tokens[4]+"_"+tokens[5],0);
                        ind=0;
                        
                        System.out.println("Broadcasting COMMIT");
                        broadcast=true;  
                    }
                }
            }
            if(broadcast){
                Comunication.broadcast(commit, sourcePrep,false,false,null,"-1");
                
            }
                
            
            
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
            
            
        }else{
            System.out.println("Format not expected consensus"+consensus_instance);
        }
    }

    private static void decide(String command) throws Exception{
        
        
        
        
        parseCommand(command);
        //broadcast=false;
        consensus_instance++;
                
        commmandsQueue();

    }


    public static void commmandsQueue() throws Exception{
        if(queue.size()>=BLOCK_SIZE && leader){
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
            

            Comunication.broadcast(start, ports,true,false,null,"-1");
            
            
        }
        
    }

    
   

    private static void consensus (String message, String[] ports) throws Exception{
        start(message,ports);

        
    }

    private static void parseCommand (String command) throws Exception{
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
            serverSocket.send(sendPacket);
        }
        
        
        
        
        System.out.println("Map of lists: " + clientsChain);
    }

    public static void setBroadcastId(int id){
        broadcastId=id;
    }

    
}








 




