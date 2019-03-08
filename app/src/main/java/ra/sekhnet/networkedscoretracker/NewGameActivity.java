package ra.sekhnet.networkedscoretracker;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class NewGameActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_game);

        // getting playername
        Intent intent = getIntent();
        String playerName = intent.getStringExtra(MainActivity.PLAYER_NAME);

        TextView playerNameTextView = (TextView) findViewById(R.id.PlayerNameViewServerNewGame);
        playerNameTextView.setText(playerName);

    }
}
