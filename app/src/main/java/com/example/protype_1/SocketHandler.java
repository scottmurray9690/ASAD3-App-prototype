package com.example.protype_1;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * SocketHandler
 * Helper class that allows for socket connections to be maintained over multiple activities
 */
public class SocketHandler {
    private static Socket socket;
    private IOException error;
    // sets up the socket, closing the old one if necessary
    public void newSocket() throws IOException {
        if(socket != null){
            socket.close();
        }
        socket = new Socket();
        error = null;
    }

    public Socket getSocket() {
        return socket;
    }
    // Start the connection
    public void connectTo(InetAddress hostAddress, int port) throws IOException {
        socket.connect(new InetSocketAddress(hostAddress,port),1000);
    }
    public IOException getError() {
        return error;
    }

}
