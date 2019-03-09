package ra.sekhnet.networkedscoretracker;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ArrayAdapter;
import android.widget.ListView;
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
}
