import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.Socket;
import java.util.*;

public class ServerWorker extends Thread {
    private final Socket aClientSocket; // Client socket
    private Optional<String> aLogin; // user logged in if applicable
    private Server server;
    private Set<String> topicSet = new HashSet<>();

    public ServerWorker(Server server, Socket pClientSocket) {
        this.aClientSocket = pClientSocket;
        this.server = server;
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

                } else if(cmd.equalsIgnoreCase("msg")) {
                    String[] msgTokens = StringUtils.split(line, null, 3);
                    handleMessage(msgTokens);
                } else if(cmd.equalsIgnoreCase("join")) {
                    handleJoin(tokens);
                } else if(cmd.equalsIgnoreCase("leave")) {
                    handleLeave(tokens);
                } else {
                    String msg = "Unknown command: " + cmd + "\n";
                    outputStream.write(msg.getBytes());
                }
            }


        }
        client.close();
    }

    private void handleLeave(String[] tokens) {
        if(tokens.length>1) {
            this.topicSet.remove(tokens[1]);
        }
    }

    private void handleJoin(String[] tokens) {
        if(tokens.length > 1) {
            String topic = tokens[1];

            topicSet.add(topic);
        }
    }

    // Format: msg <name> <message>
    private void handleMessage(String[] tokens) throws IOException {
        String sendTo  = tokens[1];
        String body = tokens[2];

        boolean isTopic = sendTo.charAt(0) == '#';

        List<ServerWorker> serverWorkers = server.getServerWorkers();
        String msg = "Msg From: " + this.aLogin.get() + ": " + body + "\n";

        for(ServerWorker s : serverWorkers) {
            if(isTopic) {
                if(s.isMemberOfTopic(sendTo)) {

                    s.send("Topic :" + sendTo + " " + msg);
                }

            } else if(s.getLogin().equalsIgnoreCase(sendTo)) {
                s.send(msg);
            }
        }
    }

    private void handleQuit() throws IOException {
        server.removeWorker(this);
        for(ServerWorker s : server.getServerWorkers()) {
            if(s.equals(this)) continue;
            s.send("offline " + this.getLogin());
            System.out.println(this.getLogin() + " has logged off.");
        }
        aClientSocket.close();
    }

    private void handleLogin(OutputStream outputStream, @NotNull String[] tokens) throws IOException {
        if(tokens.length == 3) {
            String login = tokens[1];
            String password = tokens[2];

            if((login.equals("admin") && password.equals("admin")) ||(login.equals("bruh")&& password.equals("bruh"))) {
                String msg = "Login successful\n";
                outputStream.write(msg.getBytes());
                aLogin = Optional.of(login);
                System.out.println("User successfully logged in:" + aLogin.get());

                List<ServerWorker> serverWorkers = server.getServerWorkers();
                String onlineMessage = "online " + aLogin.get() + "\n";
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
                        userMessage = "online " + s.getLogin() + "\n";
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

    public boolean isMemberOfTopic(String topic) {
        return topicSet.contains(topic);
    }
    public String getLogin() {
        return aLogin.get();
    }

    private void send(String msg) throws IOException {
        if(msg != null) {
            OutputStream outputStream = aClientSocket.getOutputStream();
            String finalMessage =  msg + "\n";
            outputStream.write(finalMessage.getBytes());
        }
    }


}
