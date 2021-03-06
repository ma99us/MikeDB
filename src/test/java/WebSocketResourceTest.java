//import org.jboss.arquillian.container.test.api.Deployment;
//import org.jboss.arquillian.container.test.api.RunAsClient;
//import org.jboss.arquillian.junit.Arquillian;
//import org.jboss.arquillian.test.api.ArquillianResource;
//import org.jboss.shrinkwrap.api.ShrinkWrap;
//import org.jboss.shrinkwrap.api.spec.WebArchive;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.maggus.mikedb.DbWebSocketResource;
//
//import javax.websocket.ClientEndpointConfig;
//import javax.websocket.CloseReason;
//import javax.websocket.ContainerProvider;
//import javax.websocket.Endpoint;
//import javax.websocket.EndpointConfig;
//import javax.websocket.MessageHandler.Whole;
//import javax.websocket.Session;
//import java.io.File;
//import java.net.URI;
//import java.net.URL;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.atomic.AtomicReference;
//
//import static java.util.Arrays.asList;
//import static javax.xml.bind.DatatypeConverter.printBase64Binary;
//import static org.junit.Assert.assertEquals;
//
//@RunAsClient
//@RunWith(Arquillian.class)
//public class WebSocketResourceTest {
//
//    private static final int PORT = 8443;
//
//    @ArquillianResource()
//    private URL url;
//
//    @Deployment(testable = false)
//    public static final WebArchive app() {
//        return ShrinkWrap.create(WebArchive.class, "example.war")
//                .addClasses(DbWebSocketResource.class)
//                //.addAsWebInfResource(new File("src/main/webapp/WEB-INF/web.xml"), "web.xml")
//                ;
//    }
//
//    @Test
//    public void sayHi() throws Exception {
//
//        final URI uri = url.toURI();
//
//        final AtomicReference<String> message = new AtomicReference<>();
//        final CountDownLatch latch = new CountDownLatch(1);
//
//        Endpoint endpoint = new Endpoint() {
//            @Override
//            public void onClose(final Session session,
//                                final CloseReason closeReason) {
//                super.onClose(session, closeReason);
//                System.out.println("onClose: " + closeReason);
//
//            }
//
//            @Override
//            public void onError(final Session session,
//                                final Throwable throwable) {
//                super.onError(session, throwable);
//                System.out.println("onError: " + throwable);
//            }
//
//            @Override
//            public void onOpen(final Session session,
//                               final EndpointConfig endpointConfig) {
//                session.addMessageHandler(new Whole<String>() {
//                    @Override
//                    public void onMessage(final String content) {
//                        message.set(content);
//                        latch.countDown();
//                    }
//                });
//            }
//        };
//
//        ClientEndpointConfig.Configurator configurator = new ClientEndpointConfig.Configurator() {
//            public void beforeRequest(Map<String, List<String>> headers) {
//                headers.put("Authorization", asList("Basic " + printBase64Binary("tomee:tomee".getBytes())));
//            }
//        };
//
//        ClientEndpointConfig authorizationConfiguration = ClientEndpointConfig.Builder.create()
//                .configurator(configurator)
//                .build();
//
//        //use same keystore as the server
//        authorizationConfiguration.getUserProperties().put("org.apache.tomcat.websocket.SSL_TRUSTSTORE",
//                "src/main/conf/keystore.jks");
//        authorizationConfiguration.getUserProperties().put("org.apache.tomcat.websocket.SSL_TRUSTSTORE_PWD",
//                "123456");
//
//        Session session = ContainerProvider.getWebSocketContainer()
//                .connectToServer(
//                        endpoint,
//                        authorizationConfiguration,
////                      When using the keystore.jks.ca-cert certificate
////                      new URI("wss", uri.getUserInfo(), "www.example.org", PORT, "/example/socket",
////                              null, null)
//                        new URI("wss", uri.getUserInfo(), "localhost", PORT, "/example/socket",
//                                null, null)
//                );
//
//        latch.await(1, TimeUnit.MINUTES);
//        session.close();
//
//        assertEquals("Hello tomee", message.get());
//    }
//}