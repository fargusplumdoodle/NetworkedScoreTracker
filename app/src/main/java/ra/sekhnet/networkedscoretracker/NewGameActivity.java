package ra.sekhnet.networkedscoretracker;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import static ra.sekhnet.networkedscoretracker.MainActivity.PLAYER_NAME;

public class NewGameActivity extends AppCompatActivity {

    public String playerName;
    public String[] client_players;
    public String[] client_ip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_game);

        // getting player name
        Intent intent = getIntent();
        playerName = intent.getStringExtra(PLAYER_NAME);

        TextView playerNameTextView = (TextView) findViewById(R.id.PlayerNameViewServerNewGame);
        playerNameTextView.setText(playerName);

        // populating initial player list
        this.getPlayerList();

    }

    private void getPlayerList(){
        // this just gets each player who has joined the game and puts it in a list
        String[] clientNames = {"Dylan", "Liyani", "Cody"};

        // populating players list
        client_players = new String[ clientNames.length];
        client_ip = new String[ clientNames.length];

        for (int i = 0; i <  clientNames.length; i++){
            client_players[i] =  clientNames[i];
            client_ip[i] = "fake_ip";
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1,
                 clientNames);
        ListView listView = (ListView) findViewById(R.id.joinedPlayerListView);
        listView.setAdapter(adapter);
    }

    public void startGame(View view){
        Intent intent = new Intent(this, inGameServerActivity.class);

        // passing player name
        intent.putExtra(PLAYER_NAME, playerName);

        // passing in clients
        intent.putExtra("CLIENT_PLAYER_LIST", client_players);
        intent.putExtra("CLIENT_IP_LIST", client_ip);

        startActivity(intent);
    }
}
