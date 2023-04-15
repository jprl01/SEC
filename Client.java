


import java.util.*;
import java.security.PublicKey;

import java.util.Scanner;
import java.util.regex.PatternSyntaxException;


public class Client {

    
    private static int messageId=0;
    private static String clientName;
    
    private static int nServers;
        
    private static int faults=1;   
    private static boolean accountCreated=false;   

    public static void main(String[] args) throws Exception {
        nServers=Integer.parseInt(args[1]);
        faults = (nServers  - 1)/3; 
        
        Comunication.setQuorum(faults+1);
        Comunication.setByzantineQuorum(2*faults+1);
        Comunication.setNServers(nServers);
        
        clientName=args[0];
        
        String[] ports = new String[args.length-2];
        for(int i=2;i< args.length;i++){
            Signer.loadPublicKeyFromFile(args[i],true);   
            ports[i-2]=args[i];

            if(clientName.equals(args[i])){
                
                Signer.loadPrivateKeyFromFile(args[i]);
            }
        }
        
        Comunication.setPorts(ports);
        Scanner myObj = new Scanner(System.in); 
         
         
        
        // Create a DatagramSocket
        //DatagramSocket socket = new DatagramSocket();
        
        
        while(true){
            String message;
            if(!accountCreated){
                System.out.println("\nPlease, create an account:\n");
                System.out.print("CreateAccount_PublicKey_InitialBalance.\n");
                System.out.print("Exit.\n\n");
                accountCreated = true;
            }
            else{
                System.out.println("\nPlease, choose an option:\n");
                System.out.print("CheckBalance_PublicKey.\n");
                System.out.print("Transfer_SourcePublicKey_DestinationPublicKey_Ammount.\n");
                System.out.print("Exit.\n\n");
            }

            String command= myObj.nextLine();

            String request=parseCommand(command);
            if(request==null){
               continue;
            }
            message ="Client_"+ clientName + '_' +  (messageId++) + '_' + request;
            Thread thread = new Thread(new Runnable()  {
                public void run()  {
                    try{
                        
                        if(request.split("_")[0].equals("StrongCheckBalancePhase1")){
                            
                            Comunication.broadcastClient(message,ports,1);
                        }else{
                            Comunication.broadcastClient(message,ports,0);
                        }
                        
                        
                    }catch(InterruptedException e){                        
                        e.printStackTrace();
                    }catch(Exception e){
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

        //create format= CreateAccount + file to load the key + amount
        if(tokens[0].equals("CreateAccount")){
            if(tokens.length!=3){
                System.out.println("\nCreateAccount needs 3 arguments");
                return null;
            }
            String ammount =tokens[2];
            PublicKey pubAccountKey=Signer.loadPublicKeyFromFile(tokens[1],false);

            if(pubAccountKey == null){
                System.out.println("Please, enter a valid client name.");

                return null;
            }
            String publicKeyString = Base64.getEncoder().encodeToString(pubAccountKey.getEncoded());

            //initial balance
            if(Integer.parseInt(tokens[2])<=0){
                System.out.println("\nCreateAccount needs a positive initial balance");
                return null;
            }
            return tokens[0]+"_"+publicKeyString+"_"+ammount;
           

        }else if(tokens[0].equals("Transfer")){
            if(tokens.length!=4){
                System.out.println("\nTransfer needs 4 arguments");
                return null;
            }
            String ammount=tokens[3];

            PublicKey pubSourceKey=Signer.loadPublicKeyFromFile(tokens[1],false);
            PublicKey pubDestKey=Signer.loadPublicKeyFromFile(tokens[2],false);

            String publicSourceString = Base64.getEncoder().encodeToString(pubSourceKey.getEncoded());
            String publicDestString = Base64.getEncoder().encodeToString(pubDestKey.getEncoded());

            if(Integer.parseInt(tokens[3])<=0){
                System.out.println("\nTransfer needs a positive ammount to be transfered");
                return null;
            }

            return tokens[0]+"_"+publicSourceString+"_"+publicDestString+"_"+ammount;
                
            
        }else if(tokens[0].equals("StrongCheckBalance")){
            if(tokens.length!=2){
                System.out.println("\nCheckBalance needs 2 arguments");
                return null;
            }

            PublicKey pubAccountKey=Signer.loadPublicKeyFromFile(tokens[1],false);
            String publicKeyString = Base64.getEncoder().encodeToString(pubAccountKey.getEncoded());

            return tokens[0]+"Phase1_"+publicKeyString;
            
        }else if(tokens[0].equals("Exit")){
            System.exit(0);
            return null;
            
        }else{
            System.out.println("\nUnknown command");
            return null;
        }

        //return command;
    }
    public static int incMessageId(){
        
        return messageId++;
    }
    
    

       

    

    
      


}






