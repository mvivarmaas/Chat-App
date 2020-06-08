

/**
 * @author Max Vivar-Maas
 *
 */
public class ServerMain {

    public static void main(String[] args) throws InterruptedException {

        // int containing port number
        int port = 1312;

        Server server = new Server(port);

        server.start();

    }




}
