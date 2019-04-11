//package ra.sekhnet.lab7;
package ra.sekhnet.networkedscoretracker;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
NSA Client class!

This is for connecting to the NetworkScoreApo server class through sockets.

It operates on 2 lines of communication (sockets)

1. handles all the connecting protocols and recieves life updates from the server
2. sends new life totals to the server.

 */
public class NSAClient extends Thread{

    //  ----  NETWORKING  ----

    // Secondary port of for life updates, it is initialized here but will be set to whatever the
    // server sends us
    private int port = 8989;
    private int secondary_port = 9897;

    private String host = "localhost";

    // declaring main socket and input/output streams
    private Socket c;
    private DataInputStream is = null;
    private DataOutputStream os = null;

    // declaring secondary socket and input/output streams
    private Socket submit_life_socket;
    private DataInputStream submit_is = null;
    private DataOutputStream submit_os = null;

    // for the app to see the status of the NSAClient object
    public boolean disconnected = false;
    public int STATE = 0;
    /*
    States:
        0 = joining game/pre joining game
        1 = waiting for game to start
        2 = in game!
     */


    //  ---- GAME INFO ----

    // Player information
    private String life;
    private String player_name;

    public ArrayList<String> players;
    public ArrayList<String> lifes;

    private  boolean newLife = false;


    // debugging output
    private boolean verbose = true;

    public NSAClient(String name, String host, int port) {
        this.player_name = name;
        this.host = host;
        this.port = port;
    }

    // For accessing status of object
    public int getSTATE(){
        return STATE;
    }

    public void setSTATE(int state){
        STATE = state;
    }

    public void setLife(int life) {
        // User input is INT, we display the life as a string
        this.life = Integer.toString(life);

        // we set new life to true, so the in_game function will know to send it to the server
        newLife = true;
    }

    private void get_player_life_totals_from_json(String json) throws java.io.IOException{
        /*
         So it turns out Java doesn't have a built in JSON parser, so I have to write one myself.
         Things like this are part of why this project didn't work out. I didn't anticipate this.

         Parameters:
            String json: The JSON provided by the server in a life update

          Returns:
            Nothing. But it does set the lifetotals attributes
         */
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

    public void join_game() throws java.io.IOException{
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
        // connecting to game
        try {
            c = new Socket(host, port);
        } catch (java.io.IOException e) {
            disconnected = true;
            e.printStackTrace();
        }

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

        print("STARTGAME 1 " + disconnected);
        recv_expect_main("START_GAME");

        // 2.
        /*
        RIGHT HERE is where it stops executing.

        Every time the log says:
            "I/Choreographer: Skipped 88 frames!  The application may be doing too much work on its main thread."

         It is clear that it skips past the following lines and then hangs in a receive function.

         STARTGAME 2 <disconnected> is never printed to the screen.

         This could be an issue with the amount of power my laptop has. Unfortunatley there is
         no other way for me to test this. The Mac Mini has likely less power than my laptop.

         */
        print("STARTGAME 2 " + disconnected);
        send_main("OK");
        print("SENDING ALOT OF OKS");

        for (int i = 0; i < 5; i++) {
            print("Waiting i: " + Integer.toString(i));
            try {
                Thread.sleep(100);
            } catch (java.lang.InterruptedException e ){
                continue;
            }
        }

        // 3.
        print("STARTGAME 3 " + disconnected);
        // POSSIBLY HERE IS WHERE THE CODE BEGINS EXECUTING AGAIN
        secondary_port = Integer.parseInt(recv_main());

        // 4.
        establish_secondary_line_of_communication();

        // The game begins
        in_game();
    }

    private void in_game() throws java.io.IOException{
        /*
        Waits for the setLife() function to be ran.

        Once we have a new life we send it to the server.
         */

        // starting thread
        this.start();

        while (true) {
            // Waiting for change to life
            while (! newLife) {
                try {
                    // no changes to life, we wait for 10 milliseconds
                    Thread.sleep(10);
                } catch (java.lang.InterruptedException e) {
                    // Java made me do this
                    continue;
                }
            }
            // setting newLife to false and sending the new life to the server!
            newLife = false;
            submit_life();
        }

    }

    private void establish_secondary_line_of_communication() throws java.io.IOException{
        /*
        Here we create a new socket for recieving life from the client so
        we dont get messages from the wrong protocol

        Protocol:
            1. client connects to server
            2. server says "NEW_LINE"
            3. client says "BOOYAH"
         */
        // 0. Create server socket for game to connect too
        print("connecting to server on " + secondary_port);
        // ServerSocket serverSocket = new ServerSocket(secondary_port);

        // 1.
        print("waiting for connection on second socket");
        try {
            submit_life_socket = new Socket(host, secondary_port);
        } catch (java.io.IOException e) {
            disconnected = true;
            e.printStackTrace();
        }

        // 2.
        print("sending new line signal on new line");
        recv_expect_submit("NEW_LINE");

        // 3.
        send_submit_life("BOOYAH");
    }

    private void OLD_establish_secondary_line_of_communication() throws java.io.IOException{
        /*
        This is prefered as it is the way the protocols where origionally written. However in
        testing I am unable to connect directly to the emulator from my laptop.

        Here we create a new socket for recieving life from the client so
        we dont get messages from the wrong protocol

        Protocol:
            1. server connects to client
            2. client says "NEW_LINE"
            3. server says "BOOYAH"
         */
        // 0. Create server socket for game to connect too
        print("create socket on " + secondary_port);
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
        /*
        Sends sends the life total on the submit life line.
         */
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
        /*
        This sends data on the main line
         */
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
        /*
        This waits for bytes in the Data input stream and reads them one by one, testing against
        the expected message.

        If the message it found matches the one we were expecting we exit and dont read the next
        bytes as the server could be part of the next protocol.

        Raises error if we dont recieve the right thing.
         */
        submit_is = new DataInputStream(new BufferedInputStream(c.getInputStream()));

        int bytes_recvd = 0;
        int expected_bytes = expected_msg.length();
        String msg = "";

        // waiting to ensure all bytes are received
        // this can be tweaked if we end up not getting all of the message
        while(is.available() != 0) {
            try {
                Thread.sleep(500);
            } catch (java.lang.InterruptedException e) {
                print(e.toString());
            }
            System.out.println("waiting submit is available: " + submit_is.available());
        }

        while ( bytes_recvd <= expected_bytes) {
            if (submit_is.available() == 0){

                msg += (char) submit_is.readByte();
                bytes_recvd += 1;

                // checking if we have received expected message
                if (msg.equals(expected_msg)) {
                    return;
                }

            } else {
                if (! msg.equals(expected_msg)) {
                    disconnected = true;
                    System.out.println("Error, no bytes avaliable");
                    return;
                }
            }
        }

        System.out.println("end");
    }

    private void recv_expect_main(String expected_msg) throws java.io.IOException {
        /*
        This waits for bytes in the Data input stream and reads them one by one, testing against
        the expected message.

        If the message it found matches the one we were expecting we exit and dont read the next
        bytes as the server could be part of the next protocol.

        Raises error if we dont recieve the right thing.
         */
        is = new DataInputStream(new BufferedInputStream(c.getInputStream()));

        int bytes_recvd = 0;
        int expected_bytes = expected_msg.length();
        String msg = "";

        // waiting to ensure all bytes are received
        // this can be tweaked if we end up not getting all of the message
        while(is.available() == 0) {
        //for (int i = 0; i < 10; i++){
            try {
                Thread.sleep(500);
            } catch (java.lang.InterruptedException e) {
                print(e.toString());
            }
            System.out.println("waiting main is available: " + is.available());
        }

        while ( bytes_recvd <= expected_bytes) {
            if (is.available() != 0){

                msg += (char) is.readByte();
                bytes_recvd += 1;

                // checking if we have received expected message
                if (msg.equals(expected_msg)) {
                    return;
                }

            } else {
                if (! msg.equals(expected_msg)) {
                    disconnected = true;
                    System.out.println("Error, no bytes avaliable");
                    return;
                }
            }
        }

        System.out.println("end");

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
        /*
        Just recieves any old message on the main line. Currently does not time out.
         */
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
        // this method behaves differently depending on the state

        // state = 0 means we need to join game
        if (this.STATE == 0) {
            try {
                this.join_game();
                // once join game has completed we set the state to 1
                STATE = 1;
            } catch (java.io.IOException e) {
                disconnected = true;
                e.printStackTrace();
            }
        }

        if (this.STATE == 1) {
            // starting game
            try {
                start_game();
                STATE = 2;
            } catch (java.io.IOException e) {
                disconnected = true;
                e.printStackTrace();
            }
        }
        print("MADE IT HERE!!!!!!!!!!!!!!!!!");
//        while (true) {
//            if (STATE == 1) {
//                try {
//                    life_update();
//                } catch (java.io.IOException e) {
//                    disconnected = true;
//                    e.printStackTrace();
//                }
//            }
//        }
    }
}
