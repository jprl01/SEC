import java.lang.ProcessBuilder;
import java.lang.Process;

import java.lang.InterruptedException;



public class StrongCheckBalanceByzantinePuppetMaster{
    public static void main(String[] args) throws Exception {
        // Launch a new terminal window and execute a command in it
        String[] ports ={"1234","1235","1236","1237"};
        String portsS=" 1234 1235 1236 1237";
        String clients=" Joao Catarina Joaquim Manuel";
        String command = "java Server";
        String byzatineCommand = "java StrongCheckBalanceByzantineServer";
        String commandClient1 = "java Client Joao";
        String commandClient2 = "java Client Catarina";
        String commandClient3 = "java Client Joaquim";
        String commandClient4 = "java Client Manuel";

        String[] terminalCommand = {"cmd.exe", "/c", "start", "cmd.exe", "/k",byzatineCommand+" 4 "+ports[0]+portsS+clients};
        String[] terminalCommand2 = {"cmd.exe", "/c", "start", "cmd.exe", "/k",command+" 4 "+ports[1]+portsS+clients};
        String[] terminalCommand3 = {"cmd.exe", "/c", "start", "cmd.exe", "/k",command+" 4 "+ports[2]+portsS+clients};
        String[] terminalCommand4 = {"cmd.exe", "/c", "start", "cmd.exe", "/k",command+" 4 "+ports[3]+portsS+clients};
        String[] terminalCommand5 = {"cmd.exe", "/c", "start", "cmd.exe", "/k",commandClient1+" 4 "+portsS+clients};
        String[] terminalCommand6 = {"cmd.exe", "/c", "start", "cmd.exe", "/k",commandClient2+" 4 "+portsS+clients};
        String[] terminalCommand7 = {"cmd.exe", "/c", "start", "cmd.exe", "/k",commandClient3+" 4 "+portsS+clients};
        String[] terminalCommand8 = {"cmd.exe", "/c", "start", "cmd.exe", "/k",commandClient4+" 4 "+portsS+clients};

        Process process1 = new ProcessBuilder(terminalCommand).start();
        Process process2 = new ProcessBuilder(terminalCommand2).start();
        Process process3 = new ProcessBuilder(terminalCommand3).start();
        Process process4 = new ProcessBuilder(terminalCommand4).start();
        Process process5 = new ProcessBuilder(terminalCommand5).start();
        Process process6 = new ProcessBuilder(terminalCommand6).start();
        Process process7 = new ProcessBuilder(terminalCommand7).start();
        Process process8 = new ProcessBuilder(terminalCommand8).start();

        // Wait for the process to finish
        try {
            int exitCode = process1.waitFor();
            System.out.println("Process exited with code " + exitCode);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            int exitCode = process2.waitFor();
            System.out.println("Process exited with code " + exitCode);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            int exitCode = process3.waitFor();
            System.out.println("Process exited with code " + exitCode);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            int exitCode = process4.waitFor();
            System.out.println("Process exited with code " + exitCode);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            int exitCode = process5.waitFor();
            System.out.println("Process exited with code " + exitCode);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            int exitCode = process6.waitFor();
            System.out.println("Process exited with code " + exitCode);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            int exitCode = process7.waitFor();
            System.out.println("Process exited with code " + exitCode);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            int exitCode = process8.waitFor();
            System.out.println("Process exited with code " + exitCode);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}