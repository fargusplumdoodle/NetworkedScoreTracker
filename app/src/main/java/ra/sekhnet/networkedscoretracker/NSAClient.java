//package ra.sekhnet.lab7;
package ra.sekhnet;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NSAClient extends Thread {
    // attributes
    private int port = 8979;
    private int secondary_port = 9897;

    private String life;

    private String host = "localhost";
    private boolean verbose = true;

    private Socket c;
    private DataInputStream is = null;
    private DataOutputStream os = null;

    private Socket submit_life_socket;
    private DataInputStream submit_is = null;
    private DataOutputStream submit_os = null;

    private String player_name;
    public boolean disconnected = false;

    public ArrayList<String> players;
    public ArrayList<String> lifes;


    public NSAClient(String name, int port) {
        this.player_name = name;
        this.port = port;

        try {
            // creating socket and input/output streams
            c = new Socket(host, port);

            // joining game
            join_game();

            print("JOINED GAME");

            start_game();

        } catch (java.io.IOException e) {
            System.out.println("Error in socket");
            disconnected = true;
        }

    }

    private void get_player_life_totals_from_json(String json) throws java.io.IOException{
        // Dont look at this function, trust it works
        // i was very tired when writing, I have proved it works in my test senario
        // with data coming from the Python server

        ArrayList<String> playerLifeTMP = new ArrayList<String>();

        // validation
        if (json.charAt(0) != '{') {
            throw new java.io.IOException("Error invalid start character in JSON");
        } else if (json.charAt(json.length() - 1) != '}') {
            throw new java.io.IOException("Error invalid end character in JSON");
        }

        // removing curly braces
        json = json.substring(1, json.length() -1);

        int curPlayer = 0;

        // RESETING PLAYERS AND LIFETOTALS
        players = new ArrayList<String>();
        lifes = new ArrayList<String>();

        // getting number of players
        for (int i = 0; i < json.length(); i++) {
            // each player will be seperated by a comma
            if (json.charAt(i) == ','){
                playerLifeTMP.add(json.substring(curPlayer, i).replaceAll("\\s+", ""));
                curPlayer = i + 1;
            }
        }
        playerLifeTMP.add(json.substring(curPlayer, json.length()).replaceAll("\\s+", ""));

        Pattern pattern = Pattern.compile("\"(\\w+)\":\"(\\w+)\"");


        for( String x : playerLifeTMP ) {
            Matcher matcher = pattern.matcher(x);
            if (matcher.find()) {
                players.add(matcher.group(1));
                lifes.add(matcher.group(2));
            } else {
                throw new IOException("Invalid input from life update: " + json);
            }
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

        recv_expect_main("START_GAME");

        // 2.
        send_main("OK");

        // 3.
        secondary_port = Integer.parseInt(recv_main());

        // 4.
        establish_secondary_line_of_communication();

        // The game begins
        in_game();
    }

    private void in_game() throws java.io.IOException{
        // starting thread
        start();

        while (true) {
            // get new life from user, this will be different in app
            Scanner scn = new Scanner(System.in);
            print("Enter new life: ");

            life = scn.nextLine();

            submit_life();
        }

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

            // waiting to ensure all bytes are received
            // this can be tweaked if we end up not getting all of the message
            try {
                Thread.sleep(100);
            } catch (java.lang.InterruptedException e) {
                print(e.toString());
            }

            int available_bytes = is.available();

            // reading all available bytes
            byte[] bytes_msg = new byte[available_bytes];
            for (int i = 0; i < available_bytes; i++) {
                bytes_msg[i] = is.readByte();
            }

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

            // waiting to ensure all bytes are received
            // this can be tweaked if we end up not getting all of the message
            try {
                Thread.sleep(100);
            } catch (java.lang.InterruptedException e) {
                print(e.toString());
            }

            int available_bytes = is.available();

            // reading all available bytes
            byte[] bytes_msg = new byte[available_bytes];
            for (int i = 0; i < available_bytes; i++) {
                bytes_msg[i] = is.readByte();
            }
            String msg = new String(bytes_msg, "UTF-8");

            if (!expected_msg.equals(msg)) {
                System.out.println("Error expecting '" + expected_msg + "' got '" + msg + "'");
                disconnected = true;
            }
        } else {
            this.print("Disconnected, not receiving message");
        }
    }

    private void submit_life() throws java.io.IOException{
        /*
        This protocol runs on the submit life socket

        Protocol:
            1. listen for new life from client
            2. client says NEW_LIFE
            3. server says READY
            4. client sends life as a positive integer
            5. server says OK
            6. server sends new life to each player
         */

        // 2.
        send_submit_life("NEW_LIFE");

        // 3.
        recv_expect_submit("READY");

        // 4.
        send_submit_life(life);

        // 5.
        recv_expect_submit("OK");

    }

    private void life_update() throws java.io.IOException {
        /*
        Protocol:
            1. client listens for life update
            2. server says LIFE_UPDATE
            3. client says OK
            4. send each client each players life total in JSON
                - example:
                    {
                        "Isaac": 20,
                        "Liyani": 4,
                        "Dylan": 99
                    }
            5. each player responds with OK
            6. client goes to step 1.
         */
        // 2.
        recv_expect_main("LIFE_UPDATE");

        // 3.
        send_main("OK");

        // 4.
        String msg = recv_main();
        print("Recieved message in life update: " + msg);
        get_player_life_totals_from_json(msg);

        // 5.
        send_main("OK");


        // displaying player lives

        print("New Life totals:");
        for (int i = 0; i < players.size(); i++) {
            print("    " + players.get(i) + ": " + lifes.get(i));
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
            byte[] bytes_msg = new byte[available_bytes];
            for (int i = 0; i < available_bytes; i++) {
                bytes_msg[i] = is.readByte();
            }

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
        while (true) {
            try {
                life_update();
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
        }
    }
}
