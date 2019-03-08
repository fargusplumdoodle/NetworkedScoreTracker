package ra.sekhnet.networkedscoretracker;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

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
                "ShellieNolton",
                "BrettHankin",
                "LashayMounts",
                "ConsuelaSlaybaugh",
                "YenStille",
                "DanicaShephard",
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
}
