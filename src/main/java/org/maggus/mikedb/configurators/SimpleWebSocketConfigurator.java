package org.maggus.mikedb.configurators;

import javax.websocket.Extension;
import javax.websocket.server.ServerEndpointConfig;
import java.util.Collections;
import java.util.List;

public class SimpleWebSocketConfigurator extends ServerEndpointConfig.Configurator {

    @Override
    public List<Extension> getNegotiatedExtensions(List<Extension> installed,
                                                   List<Extension> requested) {
        return Collections.emptyList(); // disable all websocket extensions, especially 'permessage-deflate'
    }
}