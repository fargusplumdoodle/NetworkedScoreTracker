package ra.sekhnet.networkedscoretracker;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

public class inGameServerActivity extends AppCompatActivity {

    private String playerName;

    // this is for the hosts index on the client arrays
    private int me;

    public String[] client_players;
    public String[] client_ip;
    public int[] client_health;

    private int START_HEALTH = 20;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_in_game);

        // getting player name
        Intent intent = getIntent();
        playerName = intent.getStringExtra(MainActivity.PLAYER_NAME);

        // getting client info
        client_players = intent.getStringArrayExtra("CLIENT_PLAYER_LIST");
        client_ip = intent.getStringArrayExtra("CLIENT_IP_LIST");

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

        this.printPlayerInfo();
        this.updatePlayerInfo();
    }

    private void updateOwnHealth(){
        EditText ownHealth = (EditText) findViewById(R.id.lifeTotalEditText);
        ownHealth.setText(Integer.toString(getPlayerHealth(playerName)));
    }

    private void setPlayerHealth(String player, int newHealth){
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

    private int getPlayerHealth(String player){
        for (int i = 0; i < client_players.length; i++){
            if (client_players[i].equals(player)){
                return client_health[i];
            }
        }

        throw new Resources.NotFoundException("GET PLAYER HEALTH Error Player " + player + " not found");
    }

    private String getPlayerIP(String player){
        for (int i = 0; i < client_players.length; i++){
            if (client_players[i].equals(player)){
                return client_ip[i];
            }
        }
        throw new Resources.NotFoundException("GET PLAYER IP Error Player " + player + " not found");
    }

    private void printPlayerInfo() {
        for (int i = 0; i < client_players.length; i++) {
            String msg = "Player: " + client_players[i] + " IP: " + client_ip[i];
            Toast toast = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT);
            toast.show();
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
