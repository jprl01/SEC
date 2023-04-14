import java.net.*;


import java.util.*;
import java.util.regex.PatternSyntaxException;
import java.io.IOException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.nio.ByteBuffer;



public class Server {
    private static final String red = "\u001B[31m";
    private static final int BLOCK_SIZE=2;
    private static final int FEE=2;

    private static final Object lock = new Object();
    private static final Object lockServers = new Object();
    private static final Object lockPrepare = new Object();
    private static final Object lockCommit = new Object();
    
    private static  int SERVER_PORT ;
    private static final int BUFFER_SIZE = 65000;
    private static boolean leader=false;
    private static int round=1;
    private static Queue<String> queue = new LinkedList<>();
    private static Map<String, Integer> idRequests = new HashMap<>();
    
    private static Map<String,String> clientsSource = new HashMap<>();
    
    private static Map<String, Integer> consensusValuePrepare = new HashMap<>();
    private static Map<String, Integer> consensusValue = new HashMap<>();
    
    private static Map<String,Account> systemAccounts = new HashMap<>();
    private static MerkleTree merkleTree=null;
    //private static Map<String,String> snapshots = new HashMap<>();
    

    private static Map<String, List<String>> portsPrepare = new HashMap<>();
    private static Map<String, List<String>> portsCommit = new HashMap<>();

    private static String[] sourcePrep =new String[3];
    static int ind=0;
    
    
    
    
    private static int consensus_instance=0;
    private static boolean consensus_started=false;
    private static int nServers;
    private static int faults;
    private static int byznatineQuorum;
    private static String[] ports;
    private static int lowestPort;
    private static DatagramSocket serverSocket;
    


    public static void main(String[] args) throws Exception {
        
        
        nServers=Integer.parseInt(args[0]);

        // nServers >= 3 * faults + 1;
        faults = (nServers  - 1)/3;
        byznatineQuorum=2*faults+1;
        
        SERVER_PORT=Integer.parseInt(args[1]);
        lowestPort=Integer.parseInt(args[2]);
        ports = new String[args.length-2];        
        
        System.out.println("My port is: " + SERVER_PORT);


        Comunication.setServerPort(SERVER_PORT);
        Comunication.setNServers(nServers);
        
        // Load RSA keys from files
        for(int i=2;i< args.length;i++){
            Signer.loadPublicKeyFromFile(args[i],true);
            if(args[1].equals(args[i])){
                Signer.loadPrivateKeyFromFile(args[1]);
            }
            ports[i-2]=args[i];
            
            
        }
        
        if(lowestPort==SERVER_PORT){
            leader=true;
            System.out.println("I am the leader server.");
        } 
        PublicKey leaderPublicKey = Signer.getPublicKey("1234");
        Account account= new Account(leaderPublicKey, "Leader", "0");
        systemAccounts.put("Leader",account);

        

        // Create a DatagramSocket
        serverSocket = new DatagramSocket(SERVER_PORT);
               
        
        try{
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
                    
                
                
            }
        }catch(SocketException e){
            System.out.println("error with socket");
            serverSocket.close();
        }
        
        
        
        
    }

    public static void process(DatagramPacket receivePacket) throws Exception{
        String[] tokens;
        String command;
        DatagramPacket sendPacket;
        int clientPort = receivePacket.getPort();

            
        InetAddress clientAddress = receivePacket.getAddress();    
        String receivedMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
        

        String str = Signer.verifySign(receivedMessage.getBytes());
        
        try{
            tokens= str.split("_");
        }
        catch(PatternSyntaxException e){
            System.out.println("Message format is incorret. Message will be ignored.");
            return;
        }
        
        //signature false
        if(tokens[1].equals("NACK")){
            String response = String.valueOf(SERVER_PORT)+"_"+str;
                    
            byte[] sendData = Signer.sign(response);
            sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
            serverSocket.send(sendPacket);
            return;
        }
        

        if(tokens[1].equals("Client")){
            
            //clientsSource.put(tokens[2],clientAddress.getHostAddress()+"_"+clientPort);
            int idRequest=Integer.parseInt(tokens[3]);
            String clientName = tokens[2];
            clientsSource.put(tokens[2]+idRequest,clientAddress.getHostAddress()+"_"+clientPort+"_"+tokens[0]);
            System.out.println("port para enviar: "+clientPort+"pedido id: "+tokens[3]);
                      
            synchronized(lock){

                //verify Id
                if(!processIdRequest(tokens[2], idRequest)){
                    System.out.println(" comando errado");
                    return;
                }else{
                    System.out.println(" comando certo");
                    
                }

                if(consensus_started){  

                System.out.println("1");
                    if(leader){
                       
                        queue.add(receivedMessage);
                        

                    }
                    return;
                }
                if(leader){
                    System.out.println("4");

                    consensus_started=true;
                }
                
                
            }       
            

            if(tokens[4].equals("WeakCheckBalance")){

                System.out.println("tokens "+tokens[2]);
                Account aliceAccount = systemAccounts.get(tokens[2]);
                byte[] aliceHash=aliceAccount.getAccountHash();

                

                MerkleTree.MerkleProof proof = merkleTree.getProof(aliceHash);

                boolean oi=MerkleTree.verifyProof( proof);

                System.out.println("proof "+oi);
                /*String proofEncoded=Base64.getEncoder().encodeToString(proof.getLeafHash())+
                                    Base64.getEncoder().encodeToString(proof.getSiblingHashes())+
                                    Base64.getEncoder().encodeToString(proof.getLefts().getBytes())+
                                    Base64.getEncoder().encodeToString(proof.getRootHash());*/
                String clientSource[]=clientsSource.get(clientName + idRequest).split("_");
                String response = String.valueOf(SERVER_PORT)+"_"+clientSource[2]+"_ACK_" + idRequest ;
                
                /* 
                Integer value = systemAccounts.get(tokens[2]).getValue();
                System.out.println("Client " + tokens[2] + " has this value in the account: " + value);         
    
                System.out.println("\n\n\n\nMessage sent to client with ACK: " + response);*/
                
                byte[] sendData = Signer.sign(response);
                sendPacket = new DatagramPacket(sendData, sendData.length,InetAddress.getByName(clientSource[0]), Integer.parseInt(clientSource[1]));
                serverSocket.send(sendPacket);
                return;
            }


            if(tokens[4].equals("StrongCheckBalancePhase1")){
                Integer value = systemAccounts.get(tokens[2]).getValue();
                String clientSource[]=clientsSource.get(clientName + idRequest).split("_");
                String response = String.valueOf(SERVER_PORT)+"_"+clientSource[2]+"_ACK_" + idRequest+"_"+ value;

                byte[] sendData = Signer.sign(response);
                sendPacket = new DatagramPacket(sendData, sendData.length,InetAddress.getByName(clientSource[0]), Integer.parseInt(clientSource[1]));
                serverSocket.send(sendPacket);
                consensus_started=false;
                return;
            }

            
            if(leader){
                
                System.out.println("\n\n\n\nAntes de adicionar à queue, mensagem é:\n");
                System.out.println(receivedMessage);  
                // receivedMessage = 1001_Client_Joao_0_CreateAccount_MIIC...
                
                
                
                queue.add(receivedMessage);
                

                System.out.println("queue "+queue.size());
                if(queue.size()==BLOCK_SIZE){
                    
                    sendBlock();
                }
                else{
                    consensus_started=false;
                }
            }
                
   
            
        }else{

            
            synchronized(lockServers){
                if(!processIdRequest(tokens[0], Integer.parseInt(tokens[2]))){
                    System.out.println("duplicated message");
                    return;
                }
            }
            command=str.substring(tokens[0].length()+tokens[1].length()+tokens[2].length()+3);
            
            
            String senderPort = tokens[0];
            
            //only send acks responding to commits
            
            String response = String.valueOf(SERVER_PORT)+"_"+tokens[1]+"_ACK";
            byte[] sendData = Signer.sign(response);
            sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
            serverSocket.send(sendPacket);
              
            

            Thread thread = new Thread(new Runnable()  {
                public void run()  {
                    try{
                        boolean leaderSent=false;
                        if(lowestPort==Integer.parseInt(tokens[0]))
                            leaderSent=true;
                        System.out.println("analysing command "+command.split("\n")[0]);
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
        
        //see if command is the expected:
        
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
        else if(idRequests.get(client)==idRequest ){
            System.out.println("comando certo");
            
            idRequests.put(client,idRequest+1);
            
            
        }
        else{
            System.out.println(" comando errado - id diferente do esperado");
            System.out.println("Id esperado e: " + idRequests.get(client));
            System.out.println("Id recebido e: " + idRequest);            
            return false;
        }

        return true;
    }

    private static void sendBlock() throws Exception{
        
        //append n commands (BLOCK_SIZE) to a block and start a consensus instance
        Thread thread = new Thread(new Runnable()  {
            
            public void run()  {
                try{
                    int i=1;
                    String block;
                    String request=queue.poll();
                    
                    block=request;

                    while(!queue.isEmpty() ){
                        if(i==BLOCK_SIZE){
                            break;
                        }
                        request=queue.poll();
                        
                        block+=" "+request;
                        i++;
                        
                        
                            
                    }
                
                    
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

            
            String block=command.substring(tokens[1].length()+tokens[2].length()+2);

            String transactions[]=block.split(" ");

            //verifying if commands in block are or not signed by the client
            for(int i=0;i<BLOCK_SIZE;i++){
                //System.out.println("\n\ntransaction "+transactions[i]);
                String str=Signer.verifySign(transactions[i].getBytes());

                //if not sent by client invalidate block
                if(str.split("_")[1].equals("NACK")){
                    String response = String.valueOf(SERVER_PORT)+"_"+str;
                    byte[] sendData = Signer.sign(response);
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("localhost"), socketPort);
                    serverSocket.send(sendPacket);
                    return;
                        
                }
                

            }

            String prepare="PREPARE_"+command;
            
            System.out.println("Broadcasting PREPARE");
            Comunication.broadcast(prepare,ports,nounceR);
            
            
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
                    
                    
                    System.out.println("indiceeeeeeeeeeeee "+ind);
                    
                    if(consensusValuePrepare.get(tokens[3]+"_"+tokens[4]+"_"+tokens[5])>=byznatineQuorum){
                        consensusValuePrepare.put(tokens[3]+"_"+tokens[4]+"_"+tokens[5],0);
                        
                        
                        System.out.println("Broadcasting COMMIT");
                        broadcast=true;  
                    }
                }
            }
            if(broadcast){
                Comunication.broadcast(commit, ports,nounceR);
                
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
            

            Comunication.broadcast(start, ports,"-1");
            
            
        }
        
    }

    
   

    private static void consensus (String message, String[] ports) throws Exception{
        start(message,ports);

        
    }

    private static void parseCommand (String command) throws Exception{
        //2_1_Joao_1_adeus
        
        String state="_NACK_";
        System.out.println("deciding block "+command);
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
            state="_NACK_";
            try{
                
                tokens= transactions[i].split("_");
                
            }
            catch(PatternSyntaxException e){
                System.out.println("Message format is incorret. Message will be ignored.");
                continue;
            }
            System.out.println("\n\n\n\n\n###########################");
            System.out.println(transactions[i]);
            System.out.println("###########################\n\n\n\n\n");

            String client=tokens[2];
            String type=tokens[4];
            String idRequest=tokens[3];
            String response;
            int balance=-1;
            
            String clientSource[]=clientsSource.get(client+idRequest).split("_");

            if(type.equals("CreateAccount") ){
                String initialBalance=tokens[6].split("\n")[0];

                byte[] publicKeyBytes = Base64.getDecoder().decode(tokens[5]);
                X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                PublicKey publicKey = keyFactory.generatePublic(keySpec);

                if(Signer.getPublicKey(client).equals(publicKey)){
                    if(Integer.parseInt(initialBalance)>=0 && !systemAccounts.containsKey(client)){
                        
                        Account account= new Account(publicKey,client,initialBalance);
                        systemAccounts.put(client,account);
                        
                        state="_ACK_";
                    }

                }

                System.out.println("account "+systemAccounts.get(client).getValue());
            }else if(type.equals("StrongCheckBalancePhase2")){
                
                //System.out.println("alalalla\n"+tokens[5]);
                byte[] publicKeyBytes = Base64.getDecoder().decode(tokens[5].split("\n")[0]);
                X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                PublicKey publicKey = keyFactory.generatePublic(keySpec);
                if(Signer.getPublicKey(client).equals(publicKey)){
                    if(systemAccounts.containsKey(client)){
                        
                       balance=systemAccounts.get(client).getValue();
                        
                        state="_ACK_";
                    }

                }


            }
            else if(type.equals("Transfer")){
                System.out.println("\n\n\n\n\n\n\n\n\n Entrou no transfer!\n\n\n\n\n");
                String amountToTransfer=tokens[7].split("\n")[0];
                System.out.println("Amount to transfer: " + amountToTransfer);


                // source client
                byte[] publicKeyBytes = Base64.getDecoder().decode(tokens[5]);
                X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                PublicKey sourcePublicKey = keyFactory.generatePublic(keySpec);

                //destination client
                publicKeyBytes = Base64.getDecoder().decode(tokens[6]);
                keySpec = new X509EncodedKeySpec(publicKeyBytes);
                keyFactory = KeyFactory.getInstance("RSA");
                PublicKey destinationPublicKey = keyFactory.generatePublic(keySpec);
                String destinationName = Signer.getKey(destinationPublicKey);
                System.out.println("Destination name: "+ destinationName);

                if(Signer.getPublicKey(client).equals(sourcePublicKey)){
                    System.out.println("1!");

                    if(Integer.parseInt(amountToTransfer)>=0 && systemAccounts.containsKey(client) && systemAccounts.containsKey(destinationName)){
                        System.out.println(" 2!");

                        Account sourceAccount = systemAccounts.get(client);
                        Integer sourceAccountValue = sourceAccount.getValue();

                        if(sourceAccountValue >= (Integer.parseInt(amountToTransfer) + FEE)){
                            System.out.println(" 3!");

                            sourceAccount.setValue(sourceAccount.getValue()-Integer.parseInt(amountToTransfer) - FEE);
                            System.out.print("Client " + client + " has in account " + sourceAccount.getValue());
                            systemAccounts.replace(client, sourceAccount);

                            Account destinationAccount = systemAccounts.get(destinationName);
                            destinationAccount.setValue(destinationAccount.getValue()+Integer.parseInt(amountToTransfer));
                            System.out.print("Client " + destinationName + " has in account " + destinationAccount.getValue());
                            systemAccounts.replace(destinationName, destinationAccount);

    
                            //paying fee to the leader
                            Account leaderAccount = systemAccounts.get("Leader");
                            leaderAccount.setValue(leaderAccount.getValue()+ FEE);
                            systemAccounts.replace("Leader", leaderAccount);

                            System.out.println("\n\n\n\n\n\n\n\nFez transferencia!\n\n\n\n\n");
                            state="_ACK_";

                        }


                    }

                }

                System.out.println("account "+systemAccounts.get(client).getValue());
            }
            
            response = String.valueOf(SERVER_PORT)+"_"+clientSource[2]+state + idRequest;
            if(balance!=-1){
                response +="_"+balance;
            }
            
            

            System.out.println("\n\n\n\nMessage sent to clienttt : " + response);
            
            byte[] sendData = Signer.sign(response);
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,InetAddress.getByName(clientSource[0]), Integer.parseInt(clientSource[1]));
            serverSocket.send(sendPacket);

            
            

        }


        //send signed state to other replicas
        merkleTree= new MerkleTree(systemAccounts);

        
        
        
        
        //System.out.println("Map of lists: " + clientsChain);
    }

    
    public static int getLowestPort(){
        return lowestPort;
    }

    
}








 




