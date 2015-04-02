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
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

/**
 *
 * @author reget.kalamees
 */
public class HttpServer extends Verticle {

    private Logger logger;

    @Override
    public void start() {
        super.start();
        logger = container.logger();
        JsonObject appConfig = container.config();
        JsonObject mailerConfig = appConfig.getObject("mailer_config");
        container.deployVerticle("org.oecd.epms.SendMailService", mailerConfig);
        //container.deployVerticle("org.oecd.epms.DummyConsumer");
        vertx.createHttpServer().requestHandler(new Handler<HttpServerRequest>() {
            @Override
            public void handle(final HttpServerRequest req) {

                if ((req.path().equals("/")) || (req.path().equals("/index.html"))) {
                    req.response().sendFile("index.html");
                    return;
                }

                if (req.method().equalsIgnoreCase("POST")) {
                    req.expectMultiPart(true);
                    req.endHandler(new VoidHandler() {
                        @Override
                        public void handle() {
                            JsonObject emailObj = new JsonObject();
                            MultiMap attrs = req.formAttributes();
                            for (Map.Entry<String, String> entry : attrs.entries()) {
                                switch (entry.getKey()) {
                                    case "email":
                                        emailObj.putString("to", entry.getValue());
                                        break;
                                    case "subject":
                                        emailObj.putString("subject", entry.getValue());
                                        break;
                                    case "email_text":
                                        emailObj.putString("body", entry.getValue());
                                        break;
                                    default:
                                }

                            }
                            
                            EventBus eb = vertx.eventBus();
                            eb.sendWithTimeout("epms.email.in", emailObj, 20000, new Handler<AsyncResult<Message<JsonObject>>>() {
                                @Override
                                public void handle(AsyncResult<Message<JsonObject>> result) {
                                    if (result.succeeded()) {
                                        req.response().end(result.result().body().encode());
                                    } else {
                                        StatusMessageJSON timeoutObj=new StatusMessageJSON();
                                        timeoutObj.setError();
                                        timeoutObj.setMessage("timeout");
                                        req.response().end(timeoutObj.getJsonObject().encode());
                                    }
                                }
                            });
                        }
                    });

                } else {
                    req.response().end("got request");
                }
            }
        }).listen(8080);
    }

    @Override
    public void stop() {
        logger.info("Http stop");
    }

}
