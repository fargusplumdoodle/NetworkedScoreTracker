//package ra.sekhnet.lab7;
package ra.sekhnet;
import java.io.*;
import java.net.ServerSocket;
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

    private Socket submit_life_socket;
    private DataInputStream submit_is = null;
    private DataOutputStream submit_os = null;

    private String player_name;
    private boolean disconnected = false;


    public NSAClient(String name, int port) {
        this.player_name = name;
        this.port = port;

        try {
            // creating socket and input/output streams
            c = new Socket(host, port);

            join_game();

            print("JOINED GAME");

            start_game();

        } catch (java.io.IOException e) {
            System.out.println("Error in socket");
            disconnected = true;
        }

    }

    private void join_game() throws java.io.IOException{
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
        // 3.
        recv_expect_main("READY");

        // 4.
        send_main(player_name);

        // 5.
        recv_expect_main("OK");

    }

    private void start_game() throws java.io.IOException {
        /*
        Protocol:
            For each client:
                1. client listens for the START_GAME signal
                2. client says OK
            3. Server sends port for secondary line of communication
            4. secondary line of communication established
            5. Once each client has responded with OK
            6. Server sends initial life total
         */
        // 1.
        print("Waiting for start game signal");

        recv_expect_main("START_GAME");
        print("start game");

        // 2.
        print("sending ok");
        send_main("OK");

        // 3.
        secondary_port = Integer.parseInt(recv_main());

        print("Secondary port: " + secondary_port);

        // 4.
        establish_secondary_line_of_communication();

        print("GOT THIS FAR");
    }

    private void establish_secondary_line_of_communication() throws java.io.IOException{
        /*
        Here we create a new socket for recieving life from the client so
        we dont get messages from the wrong protocol

        Protocol:
            1. server connects to client
            2. client says "NEW_LINE"
            3. server says "BOOYAH"
         */
        // 0. Create server socket for game to connect too
        print("create socket");
        ServerSocket serverSocket = new ServerSocket(secondary_port);

        // 1.
        print("waiting for connection on second socket");
        submit_life_socket = serverSocket.accept();

        // 2.
        print("sending new line signal on new line");
        send_submit_life("NEW_LINE");

        // 3.
        recv_expect_submit("BOOYAH");
    }

    private void send_submit_life(String msg) throws java.io.IOException{
        if (! this.disconnected) {
            try {
                // creating data output stream
                submit_os = new DataOutputStream(submit_life_socket.getOutputStream());

                // sending msg in UTF-8
                submit_os.writeUTF(msg);

            } catch (java.io.UnsupportedEncodingException e) {

                // java made me put this here for some reason
                System.out.println("Unsupported encoding " + e.toString());
                this.disconnected = true;
            }
        } else {
            this.print("Disconnected, not sending message");
        }

    }

    private void send_main(String msg) throws java.io.IOException{
        if (! this.disconnected) {
            try {
                // creating data output stream
                os = new DataOutputStream(c.getOutputStream());

                // sending msg in UTF-8
                os.writeUTF(msg);

            } catch (java.io.UnsupportedEncodingException e) {
                // java made me put this here for some reason
                System.out.println("Unsupported encoding " + e.toString());
                this.disconnected = true;
            }
        } else {
            this.print("Disconnected, not sending message");
        }

    }

    private void recv_expect_submit(String expected_msg) throws java.io.IOException{
        if (! this.disconnected) {
            submit_is = new DataInputStream(
                    new BufferedInputStream(submit_life_socket.getInputStream()));

            byte[] bytes_msg = submit_is.readNBytes(expected_msg.length());

            String msg = new String(bytes_msg, "UTF-8");

            if (!expected_msg.equals(msg)) {
                System.out.println("Error expecting '" + expected_msg + "' got '" + msg + "'");
                disconnected = true;
            }
        } else {
            this.print("Disconnected, not receiving message");
        }
    }

    private void recv_expect_main(String expected_msg) throws java.io.IOException{
        if (! this.disconnected) {
            is = new DataInputStream(
                    new BufferedInputStream(c.getInputStream()));

            byte[] bytes_msg = is.readNBytes(expected_msg.length());

            String msg = new String(bytes_msg, "UTF-8");

            if (!expected_msg.equals(msg)) {
                System.out.println("Error expecting '" + expected_msg + "' got '" + msg + "'");
                disconnected = true;
            }
        } else {
            this.print("Disconnected, not receiving message");
        }
    }

    private String recv_main() throws java.io.IOException{
        if (! this.disconnected) {

            is = new DataInputStream(new BufferedInputStream(c.getInputStream()));

            // waiting to ensure all bytes are received
            // this can be tweaked if we end up not getting all of the message
            try {
                Thread.sleep(100);
            } catch (java.lang.InterruptedException e) {
                print(e.toString());
            }

            int available_bytes = is.available();

            // reading all available bytes
            byte[] bytes_msg = is.readNBytes(available_bytes);

            String msg = new String(bytes_msg, "UTF-8");

            return msg;

        } else {
            this.print("Disconnected, not receiving message");
            return "";
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
