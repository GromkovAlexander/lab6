package zookeeper;

import java.util.ArrayList;

public class TestClass {
    public static void main(String[] args) {

        ArrayList<String> serversList = new ArrayList<>();

        serversList.add("5050");
        serversList.add("5060");
        serversList.add("5070");

        String msg = "5050";

        for (int i = 0; i < 10; i++) {
            int serverPort = (int) (Math.random() * (serversList.size() - 1));
            while (msg == serversList.get(serverPort)) {
                serverPort = (int) (Math.random() * (serversList.size() - 1));
            }

            System.out.println(serverPort);
        }


    }
}
