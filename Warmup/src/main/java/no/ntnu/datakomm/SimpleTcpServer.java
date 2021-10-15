package no.ntnu.datakomm;

import javax.sound.sampled.Port;
import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Locale;

/**
 * A Simple TCP server, used as a warm-up exercise for assignment A4.
 */
public class SimpleTcpServer {
    private static final int PORT = 1301;

    public static void main(String[] args) {
        SimpleTcpServer server = new SimpleTcpServer();
        log("Simple TCP server starting");
        server.run();
        log("ERROR: the server should never go out of the run() method! After handling one client");
    }

    public void run() {
        try{
        ServerSocket welcomeSocket = new ServerSocket(PORT);
        System.out.println("Server started on port " + PORT);
        Socket clientSocket = welcomeSocket.accept();

        InputStreamReader reader = new InputStreamReader(clientSocket.getInputStream());
        BufferedReader bufferedReader = new BufferedReader(reader);

        String clientInput = bufferedReader.readLine();
        System.out.println("Client sent: " + clientInput);

        clientSocket.close();

        welcomeSocket.close();

        } catch (IOException e){

        }
        // TODO - implement the logic of the server, according to the protocol.
        // Take a look at the tutorial to understand the basic blocks: creating a listening socket,
        // accepting the next client connection, sending and receiving messages and closing the connection
    }

    /**
     * Log a message to the system console.
     *
     * @param message The message to be logged (printed).
     */
    private static void log(String message) {
        System.out.println(message);
    }

   /** public boolean binOfCode(){
        String clientSentence, capitalizedSentence;
        ServerSocket welcomeSocket = new ServerSocket(3101);
        while(true){
            Socket connectionSocket = welcomeSocket.accept();
            BufferedReader inFromClient = new BufferedReader(
                    new InputStreamReader(connectionSocket.getInputStream()));
            PrintWriter outToClient = new PrintWriter(
                    connectionSocket.getOutputStream(), true);
            clientSentence = inFromClient.readLine();
            capitalizedSentence = clientSentence.toUpperCase(Locale.ROOT);
            outToClient.println(capitalizedSentence);
        } catch(IOException e){

        }

    }
    */
}
