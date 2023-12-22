// -*- coding: utf-8 -*-

package server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;

public class ClientHandler implements Runnable{
    
    private ArrayList<ClientHandler> clientHandlers;
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String clientUsername;

    public ClientHandler(Socket socket, ArrayList<ClientHandler> clientHandlers) {

        try {
            this.socket = socket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.clientUsername = bufferedReader.readLine();
            this.clientHandlers = clientHandlers;
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
                    receiveFile(messageFromClient);
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


    
    // try {
    //     messageFromClient = bufferedReader.readLine();

    //     // Eğer dosya gönderilmişse
    //     if (messageFromClient.startsWith("MessageType: FileTransfer")) {
    //         receiveFile(messageFromClient);
    //     } else {
    //         broadcastMessage(messageFromClient);
    //     }
    // } catch (IOException e) {
    //     closeEverything(socket, bufferedReader, bufferedWriter);
    //     break;
    // }

    // private void receiveFile(String header) {
    //     try {
    //         // Başlık bilgilerini parçala
    //         String[] headerLines = header.split("\n");
    //         String fileName = headerLines[1].split(": ")[1];
    //         long fileSize = Long.parseLong(headerLines[2].split(": ")[1]);

    //         // Dosyayı al
    //         FileOutputStream fileOutputStream = new FileOutputStream("received_" + fileName);
    //         byte[] buffer = new byte[1024];
    //         int bytesRead;
    //         while (fileSize > 0 && (bytesRead = bufferedReader.read(buffer, 0, (int) Math.min(buffer.length, fileSize))) != -1) {
    //             fileOutputStream.write(buffer, 0, bytesRead);
    //             fileSize -= bytesRead;
    //         }

    //         // Kapat
    //         fileOutputStream.close();
    //         System.out.println("File received: " + fileName);
    //     } catch (IOException ex) {
    //         ex.printStackTrace();
    //     }
    // }

    private static String getValueByKey(String[] headerParts, String key) {
        for (String part : headerParts) {
            String[] keyValue = part.split(":");
            if (keyValue.length == 2 && keyValue[0].trim().equals(key)) {
                return keyValue[1].trim();
            }
        }
        return null;
    }
    

    private void receiveFile(String header) {
        try {
            // Split header information

            
            
            String[] headerLines = header.split("\\|");

            System.err.println(headerLines[0] + " " + headerLines.length);

            String fileName = getValueByKey(headerLines, "FileName");

            long fileSize =  getValueByKey(headerLines, "FileSize") != null ? Long.parseLong(getValueByKey(headerLines, "FileSize")) : 0;

            // Receive file
            FileOutputStream fileOutputStream = new FileOutputStream("received_" + fileName);
            InputStream in = socket.getInputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while (fileSize > 0 && (bytesRead = in.read(buffer, 0, (int) Math.min(buffer.length, fileSize))) != -1) {
                fileOutputStream.write(buffer, 0, bytesRead);
                fileSize -= bytesRead;
            }

            // Close
            fileOutputStream.close();
            System.out.println("File received: " + fileName);
        } catch (IOException ex) {
            ex.printStackTrace();
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
