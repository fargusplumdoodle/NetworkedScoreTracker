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
/*
This would be the in game activity for the server.

It will not be finished.

This project has turned out to have too many technical issues to be properly completed on time.

This has been a valuable learning experience for me. I knew how to do all of the threading and
networking in Python and a bit in Java, so I assumed there wouldn't be much issues integrating that
with Android studio.

The Python server/client do work perfectly.

There have been a lot of hiccups on the android studio side. Most stem from the fact that I have
never made Android app before. For example its quite difficult/finicky to get my laptop to be able
to access the IP of the android emulator, and that has not been accomplished. So when making the
client that did break a portion of it, but I was able to rewrite the protocol to make it work.
Unfortunately that means that my app will not be compatable with  Codys... which was the whole point.

But of course it would be impossible to test the server if I cannot connect to the IP of the emulator.

I have learned that for the next time that I am working on a project that contains some parts that
I dont know how to do. I should figure those out BEFORE working on the parts of the project that I
do know about.

In this case I did enough research to convince myself that it was possible, and even did a test with
an app that ran a simple client/server locally with sockets which worked perfectly fine. But I should
have done more testing with connecting to the emulator from my laptop.

So hopefully I dont fail this course. I still have put in more than 20 hours on this project.
I have learned alot about Android development, planning projects, and Java. So fail or pass, this
has been a valuable learning experience.

- Isaac Thiessen 2019
 */
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

        // making text input start minimized
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

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
