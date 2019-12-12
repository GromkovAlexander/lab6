package zookeeper;

import akka.actor.AbstractActor;
import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.japi.pf.ReceiveBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

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
                        GetRandomServer.class, msg -> {

                            Random rand = new Random();
                            int serversListSize = serversList.size();

                            int serverPortIndex = rand.nextInt(serversListSize);
                            while (msg.getServer().equals(serversList.get(serverPortIndex))) {
                                serverPortIndex = rand.nextInt(serversListSize);
                            }
                            getSender().tell(serversList.get(serverPortIndex), ActorRef.noSender());
                        }

                )
                .build();
    }
}
