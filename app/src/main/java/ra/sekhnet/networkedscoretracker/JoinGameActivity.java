package ra.sekhnet.networkedscoretracker;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

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

        TextView status = (TextView) findViewById(R.id.gameSearchStatusLabel);
        status.setText("Searching for games");
    }
    @Override
    protected void onStart(){
        super.onStart();

        searchForGame();
    }
    public void searchForGame(){
        TextView status = (TextView) findViewById(R.id.gameSearchStatusLabel);
        boolean foundGame = false;

        // here we would actually be searching for games, but we are just doing this now
        //TODO: GUI ISSUE: message does not show screen until onCreate is finished doing this
        try {
            Thread.sleep(5000);
            foundGame = true;
        } catch (InterruptedException e) {
            System.out.println("Here we are, none of this matters");
        }

        status.setText("Found game!\nWaiting for game to start.");

        // here we handle connecting to the game and such
        // for now we just go to the inGameClientActivity
    }
}
