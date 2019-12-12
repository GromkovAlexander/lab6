package zookeeper;

import java.util.ArrayList;
import java.util.Random;

public class TestClass {
    public static void main(String[] args) {

        ArrayList<String> serversList = new ArrayList<>();

        serversList.add("5050");
        serversList.add("5060");
        serversList.add("5070");

        String msg = "5050";

        for (int i = 0; i < 10; i++) {

            Random rand = new Random();

            int serverPort = rand.nextInt(serversList.size());
            while (msg == serversList.get(serverPort)) {
                serverPort = rand.nextInt(serversList.size());
            }

            System.out.println(serverPort);
        }


    }
}
