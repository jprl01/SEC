
import java.net.*;

import java.util.regex.PatternSyntaxException;


public class Comunication {
    static int NServers;
    static int messageId=0;
    static int nounce=1000;
    static int SERVER_PORT;
    private static final Object lock = new Object();
    

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
                //System.out.println("lalalalal "+response);
                //response to PRE-PREPARE = PREPARE
                
                //provisorio este true
                
                if(tokens.length>3 && (tokens[3].equals("PREPARE") || tokens[3].equals("COMMIT")) ){
                    //System.out.println("lalalalal "+response);
                    System.out.println("recebi prepare");                    
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
                    if(tokens[2].equals("ACK")){
                        
                        
                        
                    }
                    
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
            //dont send to himself
            //if(Integer.parseInt(port)==SERVER_PORT && !send){
            //   continue;
            //}
            //ta hardcoded dar fix
            if(leaderSent && port.equals(String.valueOf(1234))){
                System.out.println("testar broadcast prepare");
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

    

    public static void setServerPort(int serverPort) {
        SERVER_PORT = serverPort;
    }

    public static void setNServers(int nServers) {
        NServers = nServers;
    }

}