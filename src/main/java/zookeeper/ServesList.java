package zookeeper;

import java.util.ArrayList;

public class ServesList {

    private ArrayList<Integer> serversList;

    public ServesList(ArrayList<Integer> serversList) {
        this.serversList = serversList;
    }

    public ArrayList<Integer> getServersList() {
        return serversList;
    }

}
