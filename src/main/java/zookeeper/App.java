package zookeeper;

public class App {

    private final static String ROUTES = "routes";
    private final static String LOCALHOST = "localhost";
    private final static int LOCALHOST_PORT = 8080;
    private final static String SERVER_ONLINE_MESSAGE = "Server online at http://localhost:" + LOCALHOST_PORT + "/\nPress RETURN to stop...";
    private final static String POST_MESSAGE = "Message posted";

    private final static String PACKAGE_ID = "packageId";
    private final static int TIME_OUT_MILLS = 10000;

    public static void main(String[] args) throws IOException {
        ActorSystem system = ActorSystem.create(ROUTES);
        ActorRef mainActor = system.actorOf(Props.create(MainActor.class));

        final Http http = Http.get(system);
        final ActorMaterializer materializer = ActorMaterializer.create(system);

        TesterJS testerJS = new TesterJS();

        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = testerJS.testJsRoute(mainActor).flow(system, materializer);
        final CompletionStage<ServerBinding> binding = http.bindAndHandle(
                routeFlow,
                ConnectHttp.toHost(LOCALHOST, LOCALHOST_PORT),
                materializer
        );

        System.out.println(SERVER_ONLINE_MESSAGE);
        System.in.read();

        binding
                .thenCompose(ServerBinding::unbind)
                .thenAccept(unbound -> system.terminate());

}
