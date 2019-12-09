package zookeeper;

import akka.actor.AbstractActor;
import akka.actor.Actor;
import akka.actor.ActorRef;
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
                        GetRandomServer.class, msg -> {
                            int serverPort = (int) (Math.random() * (serversList.size() - 1));
                            while (serverPort == msg.getServer()) {
                                serverPort = (int) (Math.random() * (serversList.size() - 1));
                            }
                            getSender().tell(serverPort, ActorRef.noSender());
                        }

                )
                .build();
    }
}
