import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.regex.PatternSyntaxException;

public class Server {
    
    private static final int BLOCK_SIZE=4;
    private static final int FEE=1;
    private static final int SNAPSHOT=1;
    private static final int BUFFER_SIZE = 65000;
    private static final Object lock = new Object();
    private static final Object lockServers = new Object();
    private static final Object lockPrepare = new Object();
    private static final Object lockCommit = new Object();
    
    private static int SERVER_PORT;
    private static int consensus_instance=0;
    private static int round=1;
    private static int nServers;
    private static int faults;
    private static int byznatineQuorum;
    private static int lowestPort;
    private static boolean leader=false;
    private static boolean consensus_started=false;
    private static String[] ports;
    private static Queue<String> queue = new LinkedList<>();
    private static Map<String, Integer> idRequests = new HashMap<>();
    private static Map<String,String> clientsSource = new HashMap<>();
    private static Map<String, Integer> consensusValuePrepare = new HashMap<>();
    private static Map<String, Integer> consensusValue = new HashMap<>();
    private static Map<String,Account> systemAccounts = new HashMap<>();
    private static MerkleTree merkleTree=null;
    private static Map<String, List<String>> portsPrepare = new HashMap<>();
    private static Map<String, List<String>> portsCommit = new HashMap<>();
    private static DatagramSocket serverSocket;

    public static void main(String[] args) throws Exception {
        
        
        nServers=Integer.parseInt(args[0]);

        // nServers >= 3 * faults + 1;
        faults = (nServers  - 1)/3;
        byznatineQuorum=2*faults+1;
        
        SERVER_PORT=Integer.parseInt(args[1]);
        lowestPort=Integer.parseInt(args[2]);
        ports = new String[args.length-2];        
        
        Comunication.setServerPort(SERVER_PORT);
        Comunication.setNServers(nServers);
        
        for(int i=2;i< args.length;i++){
            Signer.loadPublicKeyFromFile(args[i],true);
            if(args[1].equals(args[i])){
                Signer.loadPrivateKeyFromFile(args[1]);
            }
            ports[i-2]=args[i];
        }

        PublicKey leaderPublicKey = Signer.getPublicKey(String.valueOf(lowestPort));
        Account account = new Account(leaderPublicKey, "Leader", "0");
        systemAccounts.put("Leader",account);

        if(lowestPort==SERVER_PORT){
            leader=true;
            System.out.println("I am the leader server");
        } 
        System.out.println("I am server " + SERVER_PORT);

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
                            e.printStackTrace();
                        }
                    }
                });
                thread.start();
            }
        }catch(SocketException e){
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
        
        if(tokens[1].equals("NACK")){
            String response = String.valueOf(SERVER_PORT)+"_"+str;
                    
            byte[] sendData = Signer.sign(response);
            sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
            serverSocket.send(sendPacket);
            return;
        }

        if(tokens[1].equals("Client")){
            
            int idRequest=Integer.parseInt(tokens[3]);
            String clientName = tokens[2];
            clientsSource.put(tokens[2]+idRequest,clientAddress.getHostAddress()+"_"+clientPort+"_"+tokens[0]);
                      
            synchronized(lock){

                if(!processIdRequest(tokens[2], idRequest)){
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

                if(consensus_started){  
                    if(leader){
                        queue.add(receivedMessage);
                    }
                    return;
                }
                if(leader){
                    consensus_started=true;
                }
            }       

            if(tokens[4].equals("WeakCheckBalance")){

                Account account = systemAccounts.get(tokens[2]);
                byte[] hash=account.getAccountHash();

                MerkleTree.MerkleProof proof = merkleTree.getProof(hash);

                String proofEncoded=transformProof(account, proof);
                
                String clientSource[]=clientsSource.get(clientName + idRequest).split("_");
                String response = String.valueOf(SERVER_PORT)+"_"+clientSource[2]+"_ACK_" + idRequest+"_"+proofEncoded ;
                
                byte[] sendData = Signer.sign(response);
                sendPacket = new DatagramPacket(sendData, sendData.length,InetAddress.getByName(clientSource[0]), Integer.parseInt(clientSource[1]));
                serverSocket.send(sendPacket);

                consensus_started=false;
                return;
            }

            if(leader){
                
                queue.add(receivedMessage);

                if(queue.size()==BLOCK_SIZE){
                    sendBlock();
                }
                else{
                    consensus_started=false;
                }
            }
        }
        else{
            synchronized(lockServers){
                if(!processIdRequest(tokens[0], Integer.parseInt(tokens[2]))){
                    System.out.println("duplicated message");
                    return;
                }
            }

            command=str.substring(tokens[0].length()+tokens[1].length()+tokens[2].length()+3);
            
            String senderPort = tokens[0];
            
            if(tokens[3].equals("SNAPSHOT")){

                System.out.println("Snapshots being done");
            }
            
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
                        analyse_command(command,ports,leaderSent, senderPort,receivePacket.getPort());
                        
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                }
            });
            thread.start();
        }

    }
    public static String transformProof(Account account,MerkleTree.MerkleProof proof){
        String siblings="";
        String left="";
        int i=0;

        for(boolean isleft: proof.getLefts()){
            if(proof.getSiblingHashes()==null || i==proof.getSiblingHashes().length)
                break;
            
            if(isleft){
                left+="L-";
            }else{
                left+="R-";
            }
            
            i++;
        }
        if(proof.getSiblingHashes()!=null){
            for(byte[] sibling : proof.getSiblingHashes()){
                siblings+=Base64.getEncoder().encodeToString(sibling)+"-";
            }
        }
        else{
            siblings="-";
        }
        
        String proofEncoded=Base64.getEncoder().encodeToString(account.getAccountHash())+"_"+account.getValue()+"_"+
                                Base64.getEncoder().encodeToString(proof.getLeafHash())+
                                    "_"+ Base64.getEncoder().encodeToString(proof.getRootHash())+"_"+siblings+"_"+left;
        return proofEncoded;
    }

    private static boolean processIdRequest(String client, int idRequest){
        
        if(!idRequests.containsKey(client)){
                    
            if(idRequest==0){
                idRequests.put(client,1);
                
            }                            
            else{
                return false;
            }
                
        }
        else if(idRequests.get(client)==idRequest ){
            idRequests.put(client,idRequest+1);
        }
        else{
            return false;
        }

        return true;
    }

    private static void sendBlock() throws Exception{
        
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
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    private static void analyse_command(String command,String ports[], boolean leaderSent, String senderPort,int socketPort) throws Exception{

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

            for(int i=0;i<BLOCK_SIZE;i++){
                
                String str=Signer.verifySign(transactions[i].getBytes());

                //if not sent by client invalidate block
                if(str.split("_")[1].equals("NACK")){
                    String response = String.valueOf(SERVER_PORT)+"_"+str;
                    byte[] sendData = Signer.sign(response);
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("localhost"), socketPort);
                    serverSocket.send(sendPacket);

                    String[] tokensRequest;

                    try{
                        tokensRequest= transactions[i].split("_");
                    }
                    catch(PatternSyntaxException e){
                        System.out.println("Message format is incorret. Message will be ignored.");
                        return;
                    }
                    String[] clientSource;
                    if(clientsSource.containsKey(tokensRequest[2] + tokensRequest[3])){
                        clientSource = clientsSource.get(tokensRequest[2] + tokensRequest[3]).split("_");;
                        response = String.valueOf(SERVER_PORT)+"_"+clientSource[2]+"_NACK_" + tokensRequest[3];
                        sendData = Signer.sign(response);
                        sendPacket = new DatagramPacket(sendData, sendData.length,InetAddress.getByName(clientSource[0]), Integer.parseInt(clientSource[1]));
                        serverSocket.send(sendPacket);
                    }
                    return;
                }
            }

            String prepare="PREPARE_"+command;
            
            System.out.println("Broadcasting PREPARE");
            Comunication.broadcast(prepare,ports);
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
                    }
                    else{
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
            if(broadcast){
                Comunication.broadcast(commit, ports);
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


    public static void commmandsQueue() throws Exception{

        if(queue.size()>=BLOCK_SIZE && leader){
            sendBlock();
        }
        else{
            consensus_started=false;
        }
    }
    

    public static void start( String message,String[] ports) throws Exception{
        
        if(leader){
            String start ="PRE-PREPARE_"+String.valueOf(consensus_instance)+"_"+ String.valueOf(round)+"_"+message;
            Comunication.broadcast(start, ports);
        }
    }

    private static void consensus (String message, String[] ports) throws Exception{
        start(message,ports);
    }

    private static void parseCommand (String command) throws Exception{
        
        String state="_NACK_";
        System.out.println("Deciding block");
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
                        System.out.println("Command was executed");
                    }
                }
            }
            else if(type.equals("StrongCheckBalancePhase2")){
                
                byte[] publicKeyBytes = Base64.getDecoder().decode(tokens[5].split("\n")[0]);
                X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                PublicKey publicKey = keyFactory.generatePublic(keySpec);
                if(Signer.getPublicKey(client).equals(publicKey)){
                    if(systemAccounts.containsKey(client)){
                        
                       balance=systemAccounts.get(client).getValue();
                        
                        state="_ACK_";
                        System.out.println("Command was executed");
                    }
                }
            }
            else if(type.equals("Transfer")){
                String amountToTransfer=tokens[7].split("\n")[0];

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
                String destinationName = MerkleTree.getName(destinationPublicKey);

                if(Signer.getPublicKey(client).equals(sourcePublicKey)){
                    if(Integer.parseInt(amountToTransfer)>=0 && systemAccounts.containsKey(client) && systemAccounts.containsKey(destinationName)){
                        Account sourceAccount = systemAccounts.get(client);
                        Integer sourceAccountValue = sourceAccount.getValue();
                        if(sourceAccountValue >= (Integer.parseInt(amountToTransfer) + FEE)){
                            sourceAccount.setValue(sourceAccount.getValue()-Integer.parseInt(amountToTransfer) - FEE);
                            systemAccounts.replace(client, sourceAccount);

                            Account destinationAccount = systemAccounts.get(destinationName);
                            destinationAccount.setValue(destinationAccount.getValue()+Integer.parseInt(amountToTransfer));
                            systemAccounts.replace(destinationName, destinationAccount);

                            //paying fee to the leader
                            Account leaderAccount = systemAccounts.get("Leader");
                            leaderAccount.setValue(leaderAccount.getValue()+ FEE);
                            systemAccounts.replace("Leader", leaderAccount);

                            state="_ACK_";
                            System.out.println("Command was executed");
                        }
                    }
                }
            }
            
            response = String.valueOf(SERVER_PORT)+"_"+clientSource[2]+state + idRequest;
            if(balance!=-1){
                response +="_"+balance;
            }
            
            byte[] sendData = Signer.sign(response);
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,InetAddress.getByName(clientSource[0]), Integer.parseInt(clientSource[1]));
            serverSocket.send(sendPacket);
        }


        //make a snapshot
        if(consensus_instance%SNAPSHOT==0){
            
            merkleTree= new MerkleTree(systemAccounts);
            Comunication.broadcast("SNAPSHOT_"+consensus_instance,ports);
        }
    }

    
    public static int getLowestPort(){
        return lowestPort;
    }
}








 




