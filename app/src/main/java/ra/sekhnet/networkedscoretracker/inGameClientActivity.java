package ra.sekhnet.networkedscoretracker;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

public class inGameClientActivity extends AppCompatActivity {

    private String playerName;

    // this is for the hosts index on the client arrays
    private int me;

    public String[] client_players;
    public String host_ip;
    public int[] client_health;

    private int START_HEALTH = 20;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_in_game_client);

        // making text input start minimized
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);


        // getting player name
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


        this.updatePlayerInfo();
        this.printGameInfo();
    }

    private void getPlayersTEMP(){
        client_players = new String[] {"Liyani", "Dylan", "Cody", playerName};
        client_health = new int[] {20, 20, 19, 20};

    }

    private void printGameInfo() {
            String msg = "Connected to: " + host_ip;
            Toast toast = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT);
            toast.show();
    }

    public void loseGame(View view){
        setPlayerHealth(playerName, 0);
        updateOwnHealth();
        updatePlayerInfo();
    }

    public void submitHealth(View view){
        EditText life = (EditText) findViewById(R.id.lifeTotalEditText);
        int newHealth = Integer.parseInt(life.getText().toString());
        setPlayerHealth(playerName, newHealth);
        updateOwnHealth();
        updatePlayerInfo();
    }

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
        EditText ownHealth = (EditText) findViewById(R.id.lifeTotalEditText);
        ownHealth.setText(Integer.toString(getPlayerHealth(playerName)));
    }

    private int getPlayerHealth(String player){
        for (int i = 0; i < client_players.length; i++){
            if (client_players[i].equals(player)){
                return client_health[i];
            }
        }

        throw new Resources.NotFoundException("GET PLAYER HEALTH Error Player " + player + " not found");
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
