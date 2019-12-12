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
                            int serverPortIndex = (int) (Math.random() * (serversList.size() - 1));
                            System.out.println("serverPortIndex = " + serverPortIndex + " msg.getServer() = " + msg.getServer() +
                                    " serversList.get(serverPortIndex) = " + serversList.get(serverPortIndex));
                            while (msg.getServer() == serversList.get(serverPortIndex)) {
                                serverPortIndex = (int) (Math.random() * (serversList.size() - 1));
                            }
                            getSender().tell(serversList.get(serverPortIndex), ActorRef.noSender());
                        }

                )
                .build();
    }
}
