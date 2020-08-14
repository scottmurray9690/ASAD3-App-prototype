package com.example.protype_1;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public class SocketHandler {
    private static Socket socket;
    private IOException error;
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
    public void connectTo(InetAddress hostAddress, int port) throws IOException {
        socket.connect(new InetSocketAddress(hostAddress,port),1000);
    }
    public IOException getError() {
        return error;
    }

}
