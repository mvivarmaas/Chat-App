import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.net.Socket;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class ServerWorker extends Thread {
    private final Socket aClientSocket; // Client socket
    private Optional<String> aLogin; // user logged in if applicable
    private Server server;

    // Boilerplate
    public ServerWorker(Server server, Socket pClientSocket) {
        this.aClientSocket = pClientSocket;
        this.server = server;
    }

    public String getLogin() {
        return aLogin.get();
    }

    public void run() {
        try {
            handleClientSocket(aClientSocket);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void handleClientSocket(Socket client) throws IOException, InterruptedException {
        OutputStream outputStream = client.getOutputStream();
        InputStream inputStream = client.getInputStream();

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while( (line = reader.readLine()) != null ) {
            String[] tokens = StringUtils.split(line);
            if(tokens != null && tokens.length > 0) {
                String cmd = tokens[0];
                if (cmd.equalsIgnoreCase("quit")) {
                    handleQuit();
                    break;
                } else if(cmd.equalsIgnoreCase("login")) {
                    handleLogin(outputStream, tokens);
                } else {
                    String msg = "Unknown command: " + cmd + "\n";
                    outputStream.write(msg.getBytes());
                }
            }


        }
        client.close();
    }

    private void handleQuit() throws IOException {
        for(ServerWorker s : server.getServerWorkers()) {
            if(s.equals(this)) continue;
            s.send(this.getLogin() + " has logged off.");
            System.out.println(this.getLogin() + " has logged off.");
        }
        aClientSocket.close();
    }

    private void handleLogin(OutputStream outputStream, String[] tokens) throws IOException {
        if(tokens.length == 3) {
            String login = tokens[1];
            String password = tokens[2];

            if((login.equals("admin") && password.equals("admin")) ||(login.equals("bruh")&& password.equals("bruh"))) {
                String msg = "Login successful\n";
                outputStream.write(msg.getBytes());
                aLogin = Optional.of(login);
                System.out.println("User successfully logged in:" + aLogin.get());

                List<ServerWorker> serverWorkers = server.getServerWorkers();
                String onlineMessage = aLogin.get() + " is online.\n";
                // Broadcast user is online and show who is online
                for(ServerWorker s : serverWorkers) {
                    if(s.equals(this)) continue;

                    s.send(onlineMessage);
                }

                // Show who is online
                for(ServerWorker s : serverWorkers) {
                    if(s.equals(this)) continue;

                    String userMessage;
                    try {
                        userMessage = s.getLogin() + " is online.";
                    } catch(NullPointerException e) {
                        continue;
                    }
                        this.send(userMessage);
                }

            } else {
                String msg = "Login not successful\n";
                outputStream.write(msg.getBytes());
            }
        }
    }

    private void send(String msg) throws IOException {
        if(msg != null) {
            OutputStream outputStream = aClientSocket.getOutputStream();
            String finalMessage = new Date() + " New Message: " + msg + "\n";
            outputStream.write(finalMessage.getBytes());
        }
    }
}
