import com.sun.istack.internal.NotNull;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ChatClient {

    private final String aServerName;
    private final int aServerPort;
    private Socket socket;
    private OutputStream serverOut;
    private InputStream serverIn;
    private BufferedReader bufferedIn;
    private List<UserStatusListener> userStatusListeners = new ArrayList<>();

    public ChatClient(String pServerName, int pServerPort) {
        this.aServerName = pServerName;
        this.aServerPort = pServerPort;
    }

    public static void main(String[] args) {
        try {
            ChatClient client = new ChatClient("localhost", 1312);
            client.addUserStatusListener(new UserStatusListener() {
                @Override
                public void online(String login) {
                    System.out.println("ONLINE:" + login);
                }

                @Override
                public void offline(String login) {
                    System.out.println("OFFLINE:" + login);
                }
            });
            if (!client.connect()) {
                System.err.println("Connection failed");
            }
            System.out.println("Connection successful");
            if(client.login("admin", "admin")) {
                System.out.println("Login successful");
            } else {
                System.err.println("Login failed");
            }

        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public void addUserStatusListener(@NotNull UserStatusListener listener) {
        userStatusListeners.add(listener);
    }

    public void removeUserStatusListener(@NotNull UserStatusListener listener) {
        userStatusListeners.remove(listener);
    }

    private boolean login(String login, String password) throws IOException {
        String cmd = "login " + login + " " + password + "\n";
        serverOut.write(cmd.getBytes());
        String response = bufferedIn.readLine();
        System.out.println("Response line: " + response);

        if(response.equalsIgnoreCase("Login successful")) {
            startMessageReader();
            return true;
        } else {
            return false;
        }
    }

    private void startMessageReader() {
        Thread t = new Thread() {
            @Override
            public void run() {
                readMessageLoop();
            }
        };
        t.start();
    }

    private void readMessageLoop() {
        try {
            String line;
            while ((line = bufferedIn.readLine()) != null) {
                String[] tokens = StringUtils.split(line);
                if(tokens != null && tokens.length > 0) {
                    String cmd = tokens[0];
                    if(cmd.equalsIgnoreCase("online")) {
                        handleOnline(tokens);
                    } else if(cmd.equalsIgnoreCase("offline")) {
                        handleOffline(tokens);
                    }
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
            try {
                socket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void handleOffline(String[] tokens) {
        String login = tokens[1];
        for(UserStatusListener listener : userStatusListeners) {
            listener.offline(login);
        }
    }

    private void handleOnline(String[] tokens) {
        String login = tokens[1];
        for(UserStatusListener listener : userStatusListeners) {
            listener.online(login);
        }
    }

    private boolean connect() {
        try {
             this.socket = new Socket(aServerName, aServerPort);
             System.out.println("Client port is: " + socket.getLocalPort());
             this.serverOut = this.socket.getOutputStream();
             this.serverIn = this.socket.getInputStream();
             this.bufferedIn = new BufferedReader(new InputStreamReader(serverIn));
             return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
