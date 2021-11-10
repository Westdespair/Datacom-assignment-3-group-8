package no.ntnu.datakomm.chat;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class TCPClient {
    private PrintWriter toServer;
    private BufferedReader fromServer;
    private Socket connection;


    // Hint: if you want to store a message for the last error, store it here
    private String lastError = null;

    private final List<ChatListener> listeners = new LinkedList<>();

    /**
     * Connect to a chat server.
     *
     * @param host host name or IP address of the chat server
     * @param port TCP port of the chat server
     * @return True on success, false otherwise
     */
    public boolean connect(String host, int port) {
        boolean connected = false;
        try {
            this.connection = new Socket(host, port);
            this.fromServer = new BufferedReader(new InputStreamReader(this.connection.getInputStream()));
            this.toServer = new PrintWriter(this.connection.getOutputStream(), true);
            connected = true;

        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException i) {
            i.printStackTrace();
        } catch (SecurityException s) {
            s.printStackTrace();
        }
        return connected;
    }

    /**
     * Close the socket. This method must be synchronized, because several
     * threads may try to call it. For example: When "Disconnect" button is
     * pressed in the GUI thread, the connection will get closed. Meanwhile, the
     * background thread trying to read server's response will get error in the
     * input stream and may try to call this method when the socket is already
     * in the process of being closed. with "synchronized" keyword we make sure
     * that no two threads call this method in parallel.
     */
    public synchronized void disconnect() {
        if (isConnectionActive()) {
            try {
                toServer = null;
                fromServer = null;
                connection.close();
                connection = null;
                onDisconnect();
            } catch (IOException i){
                //Will throw socketException
                i.printStackTrace();
            }
        }
    }

    /**
     * @return true if the connection is active (opened), false if not.
     */
    public boolean isConnectionActive() {
        return connection != null;
    }

    /**
     * Send a command to server.
     *
     * @param cmd A command. It should include the command word and optional attributes, according to the protocol.
     * @return true on success, false otherwise
     */
    private boolean sendCommand(String cmd) {
        if(isConnectionActive()){
            try{
                toServer.println(cmd);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("The connection was closed");
        }
        return false;
    }

    /**
     * Send a public message to all the recipients.
     *
     * @param message Message to send
     * @return true if message sent, false on error
     */
    public boolean sendPublicMessage(String message) {
        try {
            sendCommand("msg " + message);
            //toServer.println(message);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Hint: update lastError if you want to store the reason for the error.
        return false;
    }

    /**
     * Send a login request to the chat server.
     *
     * @param username Username to use
     */
    public void tryLogin(String username) {
        try {
            sendCommand("login " + username);
            toServer.println(username);
            refreshUserList();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Send a request for latest user list to the server. To get the new users,
     * clear your current user list and use events in the listener.
     */
    public void refreshUserList() {
        sendCommand("users");
        // Hint: Use Wireshark and the provided chat client reference app to find out what commands the
        // client and server exchange for user listing.
    }

    /**
     * Send a private message to a single recipient.
     *
     * @param recipient username of the chat user who should receive the message
     * @param message   Message to send
     * @return true if message sent, false on error
     */
    public boolean sendPrivateMessage(String recipient, String message) {
        if (isConnectionActive()) {
            try {
                sendCommand("privmsg " + recipient + " " + message);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
            return false;
    }


    /**
     * Send a request for the list of commands that server supports.
     */
    public void askSupportedCommands() {
        try {
            sendCommand("help");
            toServer.println();

        }catch (Exception e){
            e.printStackTrace();

        }
        // TODO Step 8: Implement this method
        // Hint: Reuse sendCommand() method
    }


    /**
     * Wait for chat server's response
     *
     * @return one line of text (one command) received from the server
     */
    private String waitServerResponse() {
        String response = null;
        try {
           response = fromServer.readLine();
           if (response == null) {
               disconnect();
           }

        } catch (Exception e){
            e.printStackTrace();
            disconnect();
    }
        // with the stream and hence the socket. Probably a good idea to close the socket in that case.
        return response;
    }

    /**
     * Get the last error message
     *
     * @return Error message or "" if there has been no error
     */
    public String getLastError() {
        if (lastError != null) {
            return lastError;
        } else {
            return "";
        }
    }

    /**
     * Start listening for incoming commands from the server in a new CPU thread.
     */
    public void startListenThread() {
        // Call parseIncomingCommands() in the new thread.
        Thread t = new Thread(() -> {
            parseIncomingCommands();
        });
        t.start();
    }

    /**
     * Read incoming messages one by one, generate events for the listeners. A loop that runs until
     * the connection is closed.
     */
    private void parseIncomingCommands() {
        while (isConnectionActive()) {
            String response = waitServerResponse();
            String regex = " ";
            String command = response.split(regex)[0];

            System.out.println(command);
            switch (command) {
                case "loginok":
                    onLoginResult(true, "Log in OK!");
                    break;

                case "loginerr":
                    onLoginResult(false,"Log in error");
                    break;

                case "users":
                    onUsersList(filterCommandAndGetStringList(response));
                    break;

                case "msg":
                    onMsgReceived(false, response.split(regex)[1],response.split(regex, 3)[2]);
                    break;

                case "privmsg":
                    onMsgReceived(true, response.split(regex)[1], response.split(regex, 3)[2]);
                    break;

                default:
                    break;
            }
            // Hint: Reuse waitServerResponse() method
            // Hint: Have a switch-case (or other way) to check what type of response is received from the server
            // and act on it.
            // Hint: In Step 3 you need to handle only login-related responses.
            // Hint: In Step 3 reuse onLoginResult() method
            // Hint: In Step 5 reuse onUserList() method
            // TODO Step 7: add support for incoming message errors (type: msgerr)
            // TODO Step 7: add support for incoming command errors (type: cmderr)
            // Hint for Step 7: call corresponding onXXX() methods which will notify all the listeners
            // TODO Step 8: add support for incoming supported command list (type: supported)

        }
    }

    /**
     * Register a new listener for events (login result, incoming message, etc)
     *
     * @param listener
     */
    public void addListener(ChatListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Unregister an event listener
     *
     * @param listener
     */
    public void removeListener(ChatListener listener) {
        listeners.remove(listener);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////////////
    // The following methods are all event-notificators - notify all the listeners about a specific event.
    // By "event" here we mean "information received from the chat server".
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Notify listeners that login operation is complete (either with success or
     * failure)
     *
     * @param success When true, login successful. When false, it failed
     * @param errMsg  Error message if any
     */
    private void onLoginResult(boolean success, String errMsg) {
        for (ChatListener l : listeners) {
            l.onLoginResult(success, errMsg);
        }
    }

    /**
     * Notify listeners that socket was closed by the remote end (server or
     * Internet error)
     */
    private void onDisconnect() {
        for (ChatListener l : listeners) {
            l.onDisconnect();
        }
        // Hint: all the onXXX() methods will be similar to onLoginResult()
    }

    /**
     * Notify listeners that server sent us a list of currently connected users
     *
     * @param users List with usernames
     */
    private void onUsersList(String[] users) {
        for (ChatListener l : listeners) {
            l.onUserList(users);
        }
    }

    /**
     * Notify listeners that a message is received from the server
     *
     * @param priv   When true, this is a private message
     * @param sender Username of the sender
     * @param text   Message text
     */
    private void onMsgReceived(boolean priv, String sender, String text) {
        for (ChatListener l : listeners) {
            l.onMessageReceived(new TextMessage(sender, priv, text));
        }
    }

    /**
     * Notify listeners that our message was not delivered
     *
     * @param errMsg Error description returned by the server
     */
    private void onMsgError(String errMsg) {
        // TODO Step 7: Implement this method
    }

    /**
     * Notify listeners that command was not understood by the server.
     *
     * @param errMsg Error message
     */
    private void onCmdError(String errMsg) {
        // TODO Step 7: Implement this method
    }

    /**
     * Notify listeners that a help response (supported commands) was received
     * from the server
     *
     * @param commands Commands supported by the server
     */
    private void onSupported(String[] commands) {


        // TODO Step 8: Implement this method
    }

    /**
     * @param stringList The list of strings.
     * @param regex The string that will be between array indices.
     * @return resultString
     */
    private String getStringListAsString(String[] stringList, String regex) {
       StringBuilder resultString = null;
        for (String stringPiece: stringList) {
            resultString.append(regex).append(stringPiece);
        }

        return resultString.toString();

    }
    /**
     *
     * @param string The string to be filtered. The first word separated by spaces is removed.
     * @return stringList An array with the filtered strings.
     */
    private String[] filterCommandAndGetStringList(String string) {
        String[] stringList = string.split(" ");
        ArrayList<String> stringArrayList = new ArrayList<String>(Arrays.asList(stringList));
        //Trims the first "word" from the array
        stringArrayList.remove(0);
        stringList = stringArrayList.toArray(new String[0]);
        return stringList;
    }



}
