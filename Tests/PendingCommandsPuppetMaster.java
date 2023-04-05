package Tests;
import java.lang.ProcessBuilder;
import java.lang.Process;

import java.lang.InterruptedException;



public class PendingCommandsPuppetMaster{
    public static void main(String[] args) throws Exception {
        // Launch a new terminal window and execute a command in it
        String[] ports ={"1234","1235","1236","1237"};
        String portsS=" 1234 1235 1236 1237";
        String clients=" Joao";
        String command = "java Server";
        String pendingCommands = "java PendingCommandsServer";
        String command2 = "java Client Joao";

        String[] terminalCommand = {"cmd.exe", "/c", "start", "cmd.exe", "/k",pendingCommands+" 4 "+ports[0]+portsS+clients};
        String[] terminalCommand2 = {"cmd.exe", "/c", "start", "cmd.exe", "/k",pendingCommands+" 4 "+ports[1]+portsS+clients};
        String[] terminalCommand3 = {"cmd.exe", "/c", "start", "cmd.exe", "/k",pendingCommands+" 4 "+ports[2]+portsS+clients};
        String[] terminalCommand4 = {"cmd.exe", "/c", "start", "cmd.exe", "/k",pendingCommands+" 4 "+ports[3]+portsS+clients};
        String[] terminalCommand5 = {"cmd.exe", "/c", "start", "cmd.exe", "/k",command2+" 4 "+portsS+clients};
        Process process1 = new ProcessBuilder(terminalCommand).start();
        Process process2 = new ProcessBuilder(terminalCommand2).start();
        Process process3 = new ProcessBuilder(terminalCommand3).start();
        Process process4 = new ProcessBuilder(terminalCommand4).start();
        Process process5 = new ProcessBuilder(terminalCommand5).start();

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

    }
}