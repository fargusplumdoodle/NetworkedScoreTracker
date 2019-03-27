package ra.sekhnet.lab7;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class SocketClient extends Thread {
    // attributes
    private int port = 8979;
    private int secondary_port = 9897;
    private int packet_len = 1024;
    private String host = "localhost";
    private Socket c;
    private ObjectOutputStream os;
    private ObjectInputStream is;
    private String player_name;
    private boolean disconnected = false;


    public SocketClient(String name) {
        player_name = name;

    }
    private void join_game() {
       /*
        Protocol:
            1. server listens
            2. client connects through tcp
            3. server says READY
            4. clients send the name of the player ( e.g: "Fargus" )
            5. server says OK

        The server adds the clients connection to list of connections

        The client then waits for the game to start
        */

        // 2.
        try {
            // creating socket and input/output streams
            c = new Socket(host, port);
            os = new ObjectOutputStream(c.getOutputStream());
            is = new ObjectInputStream(c.getInputStream());
        } catch (java.io.IOException e) {
            System.out.println("Error in socket");
            disconnected = true;
        }
        // 3.
        recv_expect_main("READY");

        System.out.println("WE GOT THIS FAR!");

    }

    private void recv_expect_main(String expected_msg) {
        try {
            String msg = is.readUTF();

            if (!expected_msg.equals(msg)) {
                System.out.println("Error expecting '" + expected_msg + "' got '" + msg + "'");
                disconnected = true;
            }
        } catch (java.io.IOException e) {
            disconnected = true;
        }
    }
    public void run() {
        //TODO: finish this part yo
        try {
            // running
        } catch (Exception e) {
            // caught exception
        }
    }
}
