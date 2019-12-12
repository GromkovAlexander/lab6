package zookeeper;

import akka.actor.AbstractActor;
import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.japi.pf.ReceiveBuilder;

import java.util.ArrayList;
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

                            int serverPortIndex = rand.nextInt(serversList.size());
                            System.out.println(msg.getServer());
                            System.out.println(serversList.get(serverPortIndex));
                            while (msg.getServer().equals(serversList.get(serverPortIndex))) {
                                serverPortIndex = rand.nextInt(serversList.size());
                                System.out.println(serversList.get(serverPortIndex));
                            }
                            getSender().tell(serversList.get(serverPortIndex), ActorRef.noSender());
                        }

                )
                .build();
    }
}
