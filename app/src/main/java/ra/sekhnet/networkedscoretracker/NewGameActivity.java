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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_game);

        // getting playername
        Intent intent = getIntent();
        playerName = intent.getStringExtra(PLAYER_NAME);

        TextView playerNameTextView = (TextView) findViewById(R.id.PlayerNameViewServerNewGame);
        playerNameTextView.setText(playerName);

        // populating initial player list
        this.getPlayerList();

    }

    private void getPlayerList(){
        // this just gets each player who has joined the game and puts it in a list
        String[] players = {"Dylan", "Liyani", "Cody"};

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1,
                players);
        ListView listView = (ListView) findViewById(R.id.joinedPlayerListView);
        listView.setAdapter(adapter);
        /*
        List<HashMap<String, String>> aList = new ArrayList<HashMap<String, String>>();

        for (int i = 0; i < players.length; i++){
            // populating list, you could add the players health
            HashMap<String, String> hm = new HashMap<String, String>();
            hm.put("PlayerName", players[i]);
            aList.add(hm);
        }

        String[] from = {"PlayerName"};
        int[] to = {R.id.joinedPlayerListView};

        SimpleAdapter theAdapter = new SimpleAdapter(getBaseContext(), aList, R.layout.activity_new_game, from, to);
        ListView theListView = (ListView) findViewById(R.id.joinedPlayerListView);
        theListView.setAdapter(theAdapter);
        */
    }

    public void startGame(View view){
        Intent intent = new Intent(this, inGame.class);

        intent.putExtra(PLAYER_NAME, playerName);
        startActivity(intent);
    }
}
