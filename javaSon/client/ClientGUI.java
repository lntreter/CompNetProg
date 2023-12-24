// -*- coding: utf-8 -*-

package client;

import javax.swing.*;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;

public class ClientGUI extends JFrame {
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String username;

    private JTextPane chatArea;
    private StyledDocument chatAreaDoc;
    private Style style;
    private JTextField messageField;
    //private JTextArea chatArea;
    

    public ClientGUI(Socket socket, String username) {
        this.socket = socket;
        this.username = username;

        try {
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        } catch (IOException e) {
            closeEverything();
        }

        initializeGUI();
        startCheckingForHeader();
        
    }
    private void initializeGUI() {
        setTitle("Chat Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 400); // Increase the size of the frame
        setLayout(new BorderLayout());

        chatArea = new JTextPane();
        chatAreaDoc = chatArea.getStyledDocument();
        style = chatArea.addStyle("Bold", null);
        JScrollPane scrollPane = new JScrollPane(chatArea);
        add(scrollPane, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BorderLayout());

        messageField = new JTextField();
        inputPanel.add(messageField, BorderLayout.CENTER);

        JButton sendButton = new JButton("Send");
        sendButton.setEnabled(false); // Initially disable the Send button
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
        inputPanel.add(sendButton, BorderLayout.EAST);

        add(inputPanel, BorderLayout.SOUTH);

        // Create a new JPanel for the Connect button
        JPanel connectPanel = new JPanel();
        connectPanel.setLayout(new BoxLayout(connectPanel, BoxLayout.LINE_AXIS));
        JButton connectButton = new JButton("Connect");

        // Create a JLabel for the colored dot
        JLabel statusDot = new JLabel("\u2022"); // Unicode for a dot
        statusDot.setForeground(Color.RED); // Initially red
        statusDot.setFont(new Font("Default", Font.BOLD, 24)); // Increase the font size to make the dot bigger
        connectPanel.add(statusDot);

        // Add some space between the dot and the Connect button
        connectPanel.add(Box.createRigidArea(new Dimension(10, 0))); // Add 10px of space

        connectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendUserName();
                connectButton.setEnabled(false); // Disable the Connect button after clicking it
                sendButton.setEnabled(true); // Enable the Send button when Connect button is clicked
                statusDot.setForeground(Color.GREEN); // Change the dot color to green
                appendToChatArea("Connected" + "\n");
            }
        });
        connectPanel.add(connectButton);

        // Add another button
        JButton fileTransferButton = new JButton("File Transfer");

        fileTransferButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                int result = fileChooser.showOpenDialog(ClientGUI.this);
    
                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    sendFile(selectedFile);
                }
            }
        });
        
        connectPanel.add(Box.createRigidArea(new Dimension(10, 0))); // Add 10px of space between buttons
        connectPanel.add(fileTransferButton);

        // Add some space between the Connect button and the chatArea
        connectPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        add(connectPanel, BorderLayout.NORTH);
    }


    private void sendFile(File file) {
        try {
            // Başlık oluştur
            String header = "MessageType: FileTransfer|FileName: " + file.getName() + "|FileSize: " + file.length();
    
            // Başlık ve dosyayı gönder
            OutputStream outputStream = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(outputStream, true);
            writer.println(header);
    
            FileInputStream fileInputStream = new FileInputStream(file);
            byte[] buffer = new byte[102400];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.flush();
    
            // Kapat
            fileInputStream.close();
            System.out.println("File sent: " + file.getName());
            appendToChatArea("File sent: " + file.getName(), true);
        } catch (IOException ex) {

            ex.printStackTrace();

        }
    }

    private void startCheckingForHeader(){
        new Thread(new Runnable() {
            @Override
            public void run(){
                String header;
                System.out.println("Listening for Header...");
                try {

                    while (socket.isConnected()) {
                        header = bufferedReader.readLine();
                        String headerParts = header.split("\\|")[0];
                        System.out.println("Header: " + headerParts);
                        if (headerParts.equals("MessageType: FileTransfer")) {
                            startlisteningForFiles(header);
                        } else {
                            System.out.println("ZAAAA");
                            startListeningForMessages(header);
                        }
                    }
                    
                } catch (IOException e) {
                    closeEverything();
                }
            }
        }).start();
    }

    private void startlisteningForFiles(String header) {
        try {
                        
                
                System.out.println(header + "Listening Files..");

                String[] headerLines = header.split("\\|");

                long fileSize = headerLines[2].split(": ")[1] != null ? Long.parseLong(headerLines[2].split(": ")[1]) : 0;
                        
                FileOutputStream fileOutputStream = new FileOutputStream("received_" + headerLines[1].split(": ")[1]);

                InputStream in = socket.getInputStream();
                // gelen dosyayı oku
                byte[] buffer = new byte[102400];
                int bytesRead;

                while (/*in.available() > 0 && (fileSize > 0 && */(bytesRead = in.read(buffer, 0, (int) Math.min(buffer.length, fileSize))) != -1) {
                    System.out.println("Dosya alınıyor: " + bytesRead);
                    fileOutputStream.write(buffer, 0, bytesRead);
                    fileSize -= bytesRead;
                    break;

                    
                }

                fileOutputStream.close();
                System.out.println("File received: " + headerLines[1].split(": ")[1]);
                appendToChatArea("File received: " + headerLines[1].split(": ")[1], true);

            

        } catch (Exception e) {
            closeEverything();
        }
    }

    private void startListeningForMessages(String messageFromGroupChat) {
        try {
            
            if (!messageFromGroupChat.startsWith("MessageType: FileTransfer")) {
                
                System.out.println("ZAAAAAAA");
                System.out.println("Received message: " + messageFromGroupChat);
                appendToChatArea(messageFromGroupChat, false);

            } else {
                
            }
        
        } catch (Exception e) {
            closeEverything();
        }
    }
    

    private void sendUserName() {
        try {
            bufferedWriter.write(username);
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            closeEverything();
        }
    }

    private void sendMessage() {
        try {

            String header = "MessageType: Message|";

            String owString = messageField.getText();
            String messageToSend = username + ": " + messageField.getText();
            bufferedWriter.write(header + messageToSend);
            bufferedWriter.newLine();
            bufferedWriter.flush();
            System.out.println("Message sent: " + messageToSend);
            messageField.setText("");
            appendToChatArea(owString, true);
        } catch (IOException e) {
            closeEverything();
        }
    } 

    public void appendToChatArea(String message) {
        System.out.println("Appending to chat area: " + message);
        SwingUtilities.invokeLater(() -> {
            StyleConstants.setBold(style, true);
            StyleConstants.setForeground(style, Color.blue);
            try {
                chatAreaDoc.insertString(chatAreaDoc.getLength(), message + "\n", style);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    
    public void appendToChatArea(String message, boolean isClientMessage) {
        SwingUtilities.invokeLater(() -> {
            if (isClientMessage) {

                StyleConstants.setBold(style, false);
                StyleConstants.setForeground(style, Color.BLUE);

                try {
                    chatAreaDoc.insertString(chatAreaDoc.getLength(), message + "\n", style);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                StyleConstants.setBold(style, true);
                StyleConstants.setForeground(style, Color.green);

                try {
                    chatAreaDoc.insertString(chatAreaDoc.getLength(), message + "\n", style);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void closeEverything() {
        try {
            if (socket != null) {
                socket.close();
            }
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    String username = JOptionPane.showInputDialog("Enter your username for the group chat:");
                    Socket socket = new Socket("192.46.232.242", 1234);
                    //Socket socket = new Socket("localhost", 1234);
                    ClientGUI clientGUI = new ClientGUI(socket, username);
                    clientGUI.setVisible(true);
                    clientGUI.appendToChatArea("Welcome to the group chat, " + username + "!");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
