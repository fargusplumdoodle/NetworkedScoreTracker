package ra.sekhnet.networkedscoretracker;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

public class inGameServerActivity extends AppCompatActivity {

    private String playerName;

    public String[] client_players;
    public String[] client_ip;
    public int[] client_health;

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

        this.printPlayerInfo();
    }
    private void printPlayerInfo() {
        for (int i = 0; i < client_players.length; i++) {
            String msg = "Player: " + client_players[i] + " IP: " + client_ip[i];
            Toast toast = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT);
            toast.show();
        }
    }


}
