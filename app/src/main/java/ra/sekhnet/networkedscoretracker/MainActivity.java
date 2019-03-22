package ra.sekhnet.networkedscoretracker;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    // declaring playername
    public static final String PLAYER_NAME = "ra.sekhnet.NetworkedScoreApp.PLAYERNAME";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // generating example name
        Random rand = new Random();
        String[] exampleNames = new String[]{"Robyn Kircher",
                "YolandoBerndt",
                "JuniorSettles",
                "DorindaCoombes",
                "MickiOrso",
                "AntonettaHocking",
                "YerSitler",
                "CorrinBilodeau",
                "Manfish",
                "Dordle",
                "Fargus",
                "Plumtastrophy",
                "ShellieNolton",
                "LashayMounts",
                "ConsuelaSlaybaugh",
                "YenStille",
                "YukiVanmatre",
                "JosefLaberge",
                "TovaMccardell",
                "TandraHanselman",
                "GillianFujimoto",
                "SpringBailon"};

        String defaultPlayerName = exampleNames[rand.nextInt(exampleNames.length - 1)] + rand.nextInt(100);
        TextView playerName = findViewById(R.id.PlayerNameEditText);
        playerName.setText(defaultPlayerName);
    }

    public void newGame(View view){
        Intent intent = new Intent(this, NewGameActivity.class);
        EditText playerNameEditText = (EditText) findViewById(R.id.PlayerNameEditText);

        // passing player name to newGame page
        intent.putExtra(PLAYER_NAME, playerNameEditText.getText().toString());
        startActivity(intent);
    }

    public void joinGame(View view){
        Intent intent = new Intent(this, JoinGameActivity.class);
        EditText playerNameEditText = (EditText) findViewById(R.id.PlayerNameEditText);

        // passing player name to newGame page
        intent.putExtra(PLAYER_NAME, playerNameEditText.getText().toString());
        startActivity(intent);
    }
}
