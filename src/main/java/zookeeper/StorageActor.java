package zookeeper;

import akka.actor.AbstractActor;
import akka.actor.Actor;
import akka.japi.pf.ReceiveBuilder;

import java.util.ArrayList;

public class StorageActor extends AbstractActor {

    private ArrayList<Integer> serversList;

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(
                        ServesList.class, msg -> {
                            serversList = msg.getServersList();
                        }
                )
                .match(

                )
                .build();
    }
}
