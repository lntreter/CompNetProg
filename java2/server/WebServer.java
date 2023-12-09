import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebServer {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(3000); // 8080 portunu dinle
        ExecutorService executorService = Executors.newFixedThreadPool(10); // 10 iş parçacığı

        try{
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected");
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            executorService.shutdown();
            serverSocket.close();
        }
            
    }
}

class ClientHandler implements Runnable {
    private final Socket clientSocket;

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try {
            // Gelen isteği oku
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String requestLine = reader.readLine();
            String[] requestTokens = requestLine.split(" ");
            String method = requestTokens[0];
            String path = requestTokens[1];

            // İstek türüne göre işleme yap
            if (method.equals("GET")) {
                if (path.equals("/")) {
                    sendStaticResponse(clientSocket, "Hello, World!");
                } else if (path.equals("/dynamic")) {
                    sendDynamicResponse(clientSocket);
                } else {
                    sendStaticResponse(clientSocket, "Not Found");
                }
            } else {
                sendStaticResponse(clientSocket, "Method Not Allowed");
            }

            reader.close();
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendStaticResponse(Socket clientSocket, String content) throws IOException {
    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
    out.println("HTTP/1.1 200 OK");
    out.println("Content-Type: text/plain");
    out.println("Access-Control-Allow-Origin: *"); // CORS header
    out.println();
    out.println(content);
    out.close();
}

private void sendDynamicResponse(Socket clientSocket) throws IOException {
    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
    out.println("HTTP/1.1 200 OK");
    out.println("Content-Type: text/html");
    out.println("Access-Control-Allow-Origin: *"); // CORS header
    out.println();
    out.println("<html><body><h1>Hello from Dynamic Content</h1></body></html>");
    out.close();
}
}