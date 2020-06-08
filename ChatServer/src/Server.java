import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server extends Thread {
    int serverPort;
    private List<ServerWorker> serverWorkers = new ArrayList<>();

    public Server(int serverPort) {
        this.serverPort = serverPort;
    }

    public List<ServerWorker> getServerWorkers() {
        return serverWorkers;
    }

    public void run() {
        try {
            // Localhost socket on port number
            ServerSocket serverSocket = new ServerSocket(serverPort);

            // infinite loop that accepts client socket connections
            while(true) {

                // Tries to accept client
                System.out.println("About to accept client");
                Socket client = serverSocket.accept();
                System.out.println("Client accepted from: " + client);

                // New thread
                ServerWorker worker = new ServerWorker(this, client);
                serverWorkers.add(worker);
                worker.start();

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
