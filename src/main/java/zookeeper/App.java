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


import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

public class App extends AllDirectives {

    private static Http http;
    private static int serverPort;

    private final static String ROUTES = "routes";
    private final static String LOCALHOST = "127.0.0.1";

    private final static int TIME_OUT_MILLS = 10000;

    public static void main(String[] args)  {

        serverPort = Integer.parseInt(args[0]);

        ActorSystem system = ActorSystem.create(ROUTES);
        ActorRef storageActor = system.actorOf(Props.create(StorageActor.class));

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


    CompletionStage<HttpResponse> fetchToServer(String url, int port, int count) {
        String req = "http://localhost:" + port + "/?url=" + url + "&count=" + count;
        try {
            return http.singleRequest(HttpRequest.create(req));
        } catch (Exception e) {
            return CompletableFuture.completedFuture(HttpResponse.create().withEntity("ERROR 404"));
        }
    }

    CompletionStage<HttpResponse> fetch(String url) {
        try {
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
                                            new GetRandomServer(count),
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
