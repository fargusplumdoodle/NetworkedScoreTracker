//package ra.sekhnet.lab7;
package ra.sekhnet;
import java.io.*;
import java.net.Socket;

public class NSAClient extends Thread {
    // attributes
    private int port = 8979;
    private int secondary_port = 9897;
    private int packet_len = 1024;
    private String host = "localhost";
    private boolean verbose = true;
    private Socket c;
    private DataInputStream is = null;
    private DataOutputStream os = null;

    private String player_name;
    private boolean disconnected = false;


    public NSAClient(String name, int port) {
        this.player_name = name;
        this.port = port;
        this.join_game();

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
        } catch (java.io.IOException e) {
            System.out.println("Error in socket");
            disconnected = true;
        }
        // 3.
        recv_expect_main("READY");

        // 4.
        send_main(player_name);

        // 5.
        recv_expect_main("OK");

        this.print("WOOOO WE JOINED THE GAME!!");

    }


    private void send_main(String msg) {
        try {
            byte[] bytes_msg = msg.getBytes("UTF-8");

            os = new DataOutputStream(c.getOutputStream());

            os.writeUTF(msg);

        } catch (java.io.UnsupportedEncodingException e) {
            System.out.println("Unsupported encoding " + e.toString());
        } catch (java.io.IOException e) {
            System.out.println("IO Exception: " + e.toString());
        }

    }

    private void recv_expect_main(String expected_msg) {
        try {
            is = new DataInputStream(
                    new BufferedInputStream(c.getInputStream()));

            byte[] bytes_msg = is.readNBytes(expected_msg.length());

            String msg = new String(bytes_msg, "UTF-8");
            if (!expected_msg.equals(msg)) {
                System.out.println("Error expecting '" + expected_msg + "' got '" + msg + "'");
                disconnected = true;
            }
        } catch (java.io.IOException e) {
            System.out.println(e.toString());
            disconnected = true;
        }
    }

    private void print(String msg) {
        // only prints if verbose is on
        if (this.verbose) {
            System.out.println(msg);
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
