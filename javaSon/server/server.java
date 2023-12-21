// -*- coding: utf-8 -*-


package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class server {
    
    private ServerSocket serverSocket;

    private ArrayList<ClientHandler> clientHandlers;

    public server(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;   
        this.clientHandlers = new ArrayList<>();
    }

    public void startServer(){
        try {
            while(!serverSocket.isClosed()){
                
                Socket socket = serverSocket.accept();
                System.out.println("New client connected");
                ClientHandler clientHandler = new ClientHandler(socket, clientHandlers);

                Thread thread = new Thread(clientHandler);
                thread.start();

            }
        }
        catch (IOException e) {

            e.printStackTrace();

        }
    }

    public void closeServerSocket() {

        try{
            if (serverSocket != null){
                serverSocket.close();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) throws IOException {
        
        ServerSocket serverSocket = new ServerSocket(1234);
        server server = new server(serverSocket);
        server.startServer();

    }

}
