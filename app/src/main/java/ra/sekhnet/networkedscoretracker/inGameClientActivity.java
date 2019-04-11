package ra.sekhnet.networkedscoretracker;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import ra.sekhnet.networkedscoretracker.NSAClient;

import static java.lang.System.exit;

/*
The client activity for using the NSAClient class to connect to the NSAServer
 */
public class inGameClientActivity extends AppCompatActivity {

    private String playerName;

    // this is for the hosts index on the client arrays
    private int me;

    public String[] client_players;
    public String host_ip;
    public int[] client_health;

    // 10.0.2.2 is the host loopback address.
    private NSAClient c = new NSAClient("Manfish", "10.0.2.2", 8989);
    private boolean connected = false;

    private int START_HEALTH = 20;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_in_game_client);

        // making text input start minimized
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        // getting player name/host ip
        Intent intent = getIntent();
        playerName = intent.getStringExtra("PLAYER_NAME");
        host_ip = intent.getStringExtra("HOST_IP");

        // temporarily creating fake other players
        this.getPlayersTEMP();

        // initializing all players health
        client_health = new int[client_players.length];
        for (int i = 0; i < client_health.length; i++){

            // setting player to start health
            setPlayerHealth(client_players[i], START_HEALTH);

            // setting my index
            if (client_players[i].equals(playerName)){
                me = i;
                this.updateOwnHealth();
            }
        }

        // doing the first update of player info
        this.updatePlayerInfo();

        // displays a toast about the game
        this.printGameInfo();

        // attempting to connect to the server
        if (! this.connectToGame()) {
            // crashing if unsuccessful
            exit(-3);
        };
    }

    private boolean connectToGame() {
        /*
        This facilitates the NSAClients connecting to the server.

        Will return false if unsuccessful, when the protocol completes
        successfully the STATE will be 1
         */
        // starting join game activity
        c.setSTATE(0);
        c.start();

        while (c.getSTATE() != 1) {
            // waiting for game to connect
            try {
                Thread.sleep(100);
            } catch (java.lang.InterruptedException e ){
                break;
            }
        }

        System.out.println("CONNECTED!");
        Toast toast = Toast.makeText(getApplicationContext(), "CONNECTED, Waiting for game to start", Toast.LENGTH_SHORT);
        toast.show();


        return c.getSTATE() == 1;
    }

    private void getPlayersTEMP(){
        // Creates temporary players while the networking is configured
        client_players = new String[] {"Liyani", "Dylan", "Cody", playerName};
        client_health = new int[] {20, 20, 19, 20};

    }

    private void printGameInfo() {
            // Makes a toast that displays the IP that we are connected too
            String msg = "Connected to: " + host_ip;
            Toast toast = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT);
            toast.show();
    }

    public void loseGame(View view){
        // A shortcut for the user for when they lose
        setPlayerHealth(playerName, 0);
        updateOwnHealth();
        updatePlayerInfo();
    }

    public void submitHealth(View view){
        // Sends life to server

        EditText life = (EditText) findViewById(R.id.lifeTotalEditText);
        int newHealth = Integer.parseInt(life.getText().toString());

        setPlayerHealth(playerName, newHealth);

        // telling NSAClient we have new health
        c.setLife(newHealth);

        updateOwnHealth();
        updatePlayerInfo();
    }

    // The following methods are for the buttons that modify users life
    public void minusFive(View view){
        setPlayerHealth(playerName, client_health[me] - 5);
        updateOwnHealth();
    }

    public void minusOne(View view){
        setPlayerHealth(playerName, client_health[me] - 1);
        updateOwnHealth();
    }

    public void plusOne(View view){
        setPlayerHealth(playerName, client_health[me] + 1);
        updateOwnHealth();
    }

    public void plusFive(View view){
        setPlayerHealth(playerName, client_health[me] + 5);
        updateOwnHealth();
    }

    private void updateOwnHealth(){
        // This updates the health on the GUI of the user
        EditText ownHealth = (EditText) findViewById(R.id.lifeTotalEditText);
        ownHealth.setText(Integer.toString(getPlayerHealth(playerName)));
    }

    private int getPlayerHealth(String player){
        // returns the health of a specific player
        for (int i = 0; i < client_players.length; i++){
            if (client_players[i].equals(player)){
                return client_health[i];
            }
        }

        throw new Resources.NotFoundException("GET PLAYER HEALTH Error Player " + player + " not found");
    }

    private void setPlayerHealth(String player, int newHealth){
        // sets the health of a specific player
        boolean done = false;
        for (int i = 0; i < client_players.length; i++){
            if (client_players[i].equals(player)){
                client_health[i] = newHealth;
                done = true;
            }
        }
        if (!done) {
            throw new Resources.NotFoundException("SET PLAYER HEALTH Error Player " + player + " not found");
        }
    }

    private void updatePlayerInfo(){
        // redraws the listView with appropriate info
        String[] client_info = new String[client_players.length];

        for (int i = 0; i < client_players.length; i++){
            String info = client_players[i] + ": " + client_health[i];
            client_info[i] = info;
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1,
                client_info);

        ListView listView = (ListView) findViewById(R.id.inGamePlayerListView);
        listView.setAdapter(adapter);
    }
}
