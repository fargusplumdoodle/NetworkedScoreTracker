package ra.sekhnet.networkedscoretracker;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;

import ra.sekhnet.networkedscoretracker.NSAClient;

import static ra.sekhnet.networkedscoretracker.MainActivity.PLAYER_NAME;

public class JoinGameActivity extends AppCompatActivity {
    private String playerName;
    private String hostIP;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_game);

        // getting player name
        Intent intent = getIntent();
        playerName = intent.getStringExtra(PLAYER_NAME);

        // setting player name
        TextView playerNameTextView = (TextView) findViewById(R.id.playerNameLabelJoinGame);
        playerNameTextView.setText(playerName);

        searchForGame();

    }

    public void searchForGame(){
        TextView status = (TextView) findViewById(R.id.gameSearchStatusLabel);

        status.setText("Searching for games");


        status.setText("Found game!\nWaiting for game to start.");

        // here we handle connecting to the game and such
        hostIP = "TEMP";

        // passing the IP of the game
        Intent intent = new Intent(this, inGameClientActivity.class);

        intent.putExtra("HOST_IP", hostIP);
        intent.putExtra("PLAYER_NAME", playerName);

        startActivity(intent);
    }
}
