package zookeeper;

import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.server.Route;
import akka.pattern.Patterns;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import akka.http.javadsl.server.AllDirectives;
import org.apache.zookeeper.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;


public class App extends AllDirectives {

    private static Http http;
    private static int serverPort;
    private static ActorRef storageActor;
    private static ZooKeeper zoo;

    private final static String ROUTES = "routes";
    private final static String LOCALHOST = "127.0.0.1";
    private final static String ZOO_LOCALHOST = "127.0.0.1:2181";

    private final static String HOME_DIR = "/zooop";
    private final static String CHILD_DIR = HOME_DIR + "/";

    private final static int TIME_OUT_MILLS = 10000;

    public static void main(String[] args)  {

        Scanner sc = new Scanner(System.in);
        serverPort = Integer.parseInt(sc.nextLine());

        ActorSystem system = ActorSystem.create(ROUTES);
        storageActor = system.actorOf(Props.create(StorageActor.class));
        createZoo();
        http = Http.get(system);
        final ActorMaterializer materializer = ActorMaterializer.create(system);

        App testerJS = new App();

        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = testerJS.route(storageActor).flow(system, materializer);
        final CompletionStage<ServerBinding> binding = http.bindAndHandle(
                routeFlow,
                ConnectHttp.toHost(LOCALHOST, serverPort),
                materializer
        );

        System.out.println("Server online at http://" + LOCALHOST + ":" + serverPort);

        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }

        binding
                .thenCompose(ServerBinding::unbind)
                .thenAccept(unbound -> system.terminate());

    }

    public static class ZooWatcher implements Watcher {

        @Override
        public void process(WatchedEvent watchedEvent) {
                List<String> ports = new ArrayList<>();

                try {
                    ports = zoo.getChildren(HOME_DIR, this);
                } catch (KeeperException e) {

                    System.out.println("Создаю дочернюю директорию");
                    try {
                        zoo.create(HOME_DIR,
                                "parent".getBytes(),
                                ZooDefs.Ids.OPEN_ACL_UNSAFE,
                                CreateMode.PERSISTENT
                        );

                        ports = zoo.getChildren(HOME_DIR, this);

                    } catch (KeeperException | InterruptedException ex) {
                        ex.printStackTrace();
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();

                }

                List<String> portsData = new ArrayList<>();

                for (String port : ports) {
                    byte[] data = new byte[0];
                    try {
                        data = zoo.getData(CHILD_DIR + port, false, null);
                    } catch (KeeperException | InterruptedException e) {
                        e.printStackTrace();
                    }
                    portsData.add(new String(data));
                }

                ArrayList<Integer> portsDataInt = new ArrayList<>();
                for (String port : portsData) {
                    portsDataInt.add(Integer.parseInt(port));
                }

                storageActor.tell(new ServesList(portsDataInt), ActorRef.noSender());

        }
    }

    private static void createZoo() {
        try {
            zoo = new ZooKeeper(
                    ZOO_LOCALHOST,
                    TIME_OUT_MILLS,
                    new ZooWatcher()
            );
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            zoo.create(CHILD_DIR + serverPort,
                    Integer.toString(serverPort).getBytes(),
                    ZooDefs.Ids.OPEN_ACL_UNSAFE,
                    CreateMode.EPHEMERAL
            );
        } catch (KeeperException e) {
//            try {
//                System.out.println("Создаю дочернюю директорию");
//                zoo.create(HOME_DIR,
//                        "parent".getBytes(),
//                        ZooDefs.Ids.OPEN_ACL_UNSAFE,
//                        CreateMode.PERSISTENT
//                );
//
//                zoo.create(CHILD_DIR + serverPort,
//                        Integer.toString(serverPort).getBytes(),
//                        ZooDefs.Ids.OPEN_ACL_UNSAFE,
//                        CreateMode.EPHEMERAL
//                );
//            } catch (KeeperException | InterruptedException ex) {
//                ex.printStackTrace();
//            }
            e.printStackTrace();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            zoo.getChildren(
                    HOME_DIR,
                    new ZooWatcher()
            );
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }

    }


    CompletionStage<HttpResponse> fetchToServer(String url, int port, int count) {
        String req = "http://localhost:" + port + "/?url=" + url + "&count=" + count;
        System.out.println(req);
        try {
            return http.singleRequest(HttpRequest.create(req));
        } catch (Exception e) {
            return CompletableFuture.completedFuture(HttpResponse.create().withEntity("ERROR 404"));
        }
    }

    CompletionStage<HttpResponse> fetch(String url) {
        try {
            System.out.println(url);
            return http.singleRequest(HttpRequest.create(url));
        } catch (Exception e) {
            return CompletableFuture.completedFuture(HttpResponse.create().withEntity("ERROR 404"));
        }
    }

    private Route route(ActorRef storageActor) {
        return get(
                () -> parameter("url", url ->
                        parameter("count", notParsedCount -> {
                                int count = Integer.parseInt(notParsedCount);
                                if (count != 0) {
                                    CompletionStage<HttpResponse> randomPort = Patterns.ask(
                                            storageActor,
                                            new GetRandomServer(serverPort),
                                            java.time.Duration.ofMillis(TIME_OUT_MILLS)
                                    ).thenCompose(
                                            port ->
                                                fetchToServer(
                                                        url,
                                                        (int) port,
                                                        count - 1
                                                )
                                    );
                                    return completeWithFuture(randomPort);
                                } else {
                                    try {
                                        return complete(fetch(url).toCompletableFuture().get());
                                    } catch (InterruptedException | ExecutionException e) {
                                        e.printStackTrace();
                                        return complete("ERROR 404");
                                    }
                                }
                            }
                        )
                )
        );
    }

}
