// -*- coding: utf-8 -*-

package server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

public class ClientHandler implements Runnable{
    
    private ArrayList<ClientHandler> clientHandlers;
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private OutputStream outputStream;
    private InputStream inputStream;
    private PrintWriter pw;
    private String clientUsername;

    public ClientHandler(Socket socket, ArrayList<ClientHandler> clientHandlers) {

        try {
            this.socket = socket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.clientUsername = bufferedReader.readLine();
            this.clientHandlers = clientHandlers;
            this.outputStream = socket.getOutputStream();
            this.inputStream = socket.getInputStream();
            this.pw = new PrintWriter(outputStream, true);
            clientHandlers.add(this);
            broadcastMessage("Server: " + clientUsername + " has joined the chat");
        }
        catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }

    }

    @Override
    public void run() {
        // TODO Auto-generated method stub
        
        String messageFromClient;

        while (socket.isConnected()) {
            try {
                messageFromClient = bufferedReader.readLine();
                
                System.out.println(messageFromClient);

                String[] headerParts = messageFromClient.split("\\|");
                String messageType = getValueByKey(headerParts, "MessageType");

                String message = messageFromClient.split("\\|")[1];

                System.out.println("A " + messageType);
    
                // Eğer dosya gönderilmişse
                if (messageType.equals("FileTransfer")) {
                    try {

                        System.out.println("Z" + messageType);
                    
                        broadcastFile(messageFromClient);
                        
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } else {
                    System.out.println("B " + messageType);
                    broadcastMessage(message);
                }
            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
                break;
            }
        }

    }


    
    private static String getValueByKey(String[] headerParts, String key) {
        for (String part : headerParts) {
            String[] keyValue = part.split(":");
            if (keyValue.length == 2 && keyValue[0].trim().equals(key)) {
                return keyValue[1].trim();
            }
        }
        return null;
    }
    
    public void broadcastFile(String messageFromClient){

        for (ClientHandler clientHandler : clientHandlers) {
            try {

                if(clientHandler == this) continue;

                System.out.println("GIRDI");

                clientHandler.pw.println(messageFromClient);

                
                int fileSize = Integer.parseInt( getValueByKey(messageFromClient.split("\\|"), "FileSize"));

                byte[] buffer = new byte[4096];
                int bytesRead;
                long totalBytesRead = 0;
                while (totalBytesRead < fileSize  && (bytesRead = this.inputStream.read(buffer)) != -1) {
                    clientHandler.outputStream.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                    System.out.println("Dosya gönderiliyor: " + bytesRead);
                }

                clientHandler.outputStream.flush();

                System.out.println("File received and sent to other clients: " + messageFromClient);
                
            } catch (Exception e) {
                
                closeEverything(socket, bufferedReader, bufferedWriter);

            }
        }

    }

    public void broadcastMessage(String messageToSend) {
        for (ClientHandler clientHandler : clientHandlers) {
            try {
                if (!clientHandler.clientUsername.equals(clientUsername)) {
                    clientHandler.bufferedWriter.write(messageToSend + "\n");
                    clientHandler.bufferedWriter.flush();
                }
            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
            }
        }
    }
    

    public void removeClientHandler() {
    
        clientHandlers.remove(this);
        broadcastMessage("Server: " + clientUsername + " has left the chat");
    
    }

    public void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {

        try {

            if (bufferedReader != null) {
                bufferedReader.close();
            }

            if (bufferedWriter != null) {
                bufferedWriter.close();
            }

            if (socket != null) {
                socket.close();
            }

            removeClientHandler();

        }
        catch (IOException e) {
            e.printStackTrace();

        }

    }


}
