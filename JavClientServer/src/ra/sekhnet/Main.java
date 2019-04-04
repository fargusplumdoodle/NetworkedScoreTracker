package ra.sekhnet;

import java.io.IOException;
import java.net.UnknownHostException;

public class Main {

    public static void main(String[] args) throws UnknownHostException, IOException, ClassNotFoundException, InterruptedException {
        NSAClient c = new NSAClient("Manfish", 12349);
        //SocketClientExample.main(args);
        //String json = "{\"for\": \"20\", \"Manfish\": \"20\", \"Manfish\": \"20\", \"Dordle\": \"21\"}";
        //c.get_player_life_totals_from_json(json);
        System.out.println("YAY");
    }
}
