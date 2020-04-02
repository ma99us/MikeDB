package org.maggus.mikedb;

import javax.websocket.Extension;
import javax.websocket.server.ServerEndpointConfig;
import java.util.Collections;
import java.util.List;

public class WebSocketConfigurator extends ServerEndpointConfig.Configurator {

    @Override
    public List<Extension> getNegotiatedExtensions(List<Extension> installed,
                                                   List<Extension> requested) {
        return Collections.emptyList(); // disable all websocket extensions, especially 'permessage-deflate'
    }
}
