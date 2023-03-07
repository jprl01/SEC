import java.lang.ProcessBuilder;
import java.lang.Process;
import java.io.IOException;
import java.lang.InterruptedException;
import java.io.File;
import java.io.*;
import java.awt.*;


public class PuppetMaster{
    public static void main(String[] args) throws Exception {
        // Launch a new terminal window and execute a command in it
        String[] ports ={"1234","1235","1236","1237"};
        String portsS=" 1234 1235 1236 1237";
        String command = "java Server";
        String command2 = "java Client";

        String[] terminalCommand = {"cmd.exe", "/c", "start", "cmd.exe", "/k",command+" "+ports[0]+portsS};
        String[] terminalCommand2 = {"cmd.exe", "/c", "start", "cmd.exe", "/k",command+" "+ports[1]+portsS};
        String[] terminalCommand3 = {"cmd.exe", "/c", "start", "cmd.exe", "/k",command+" "+ports[2]+portsS};
        String[] terminalCommand4 = {"cmd.exe", "/c", "start", "cmd.exe", "/k",command+" "+ports[3]+portsS};
        String[] terminalCommand5 = {"cmd.exe", "/c", "start", "cmd.exe", "/k",command2+" "+portsS};
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