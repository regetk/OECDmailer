package org.oecd.server;

import java.util.Map;
import org.oecd.messagebeans.StatusMessageJSON;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.platform.Verticle;
import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

/**
 *
 * @author reget.kalamees
 */
public class OecdVertxServer extends Verticle {

    private Logger logger;

    @Override
    public void start() {
        super.start();
        logger = container.logger();
        JsonObject appConfig = container.config();
        JsonObject mailerConfig = appConfig.getObject("mailer_config");
        container.deployVerticle("org.oecd.epms.SendMailService", mailerConfig);
        HttpServer server = vertx.createHttpServer().requestHandler(new Handler<HttpServerRequest>() {
            @Override
            public void handle(final HttpServerRequest req) {
                req.response().sendFile("index.html");
            }
        });

        //mail eventbus
        JsonObject config = new JsonObject().putString("prefix", "/mailbus");

        JsonArray inboundPermitted = new JsonArray();
        JsonObject inboundPermittedRule = new JsonObject().putString("address", "epms.email.in");
        inboundPermitted.add(inboundPermittedRule);

        JsonArray outboundPermitted = new JsonArray();
         
        JsonObject outboundPermitted1 = new JsonObject().putString("address_re", "epms.email.out.*");
        outboundPermitted.add(outboundPermitted1);
        
        vertx.createSockJSServer(server).bridge(config, inboundPermitted, outboundPermitted);
        server.listen(8080);
    }

    @Override
    public void stop() {
        logger.info("Http stop");
    }

}
