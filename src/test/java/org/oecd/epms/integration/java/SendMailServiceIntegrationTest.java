package org.oecd.epms.integration.java;

import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.testtools.TestVerticle;

import static org.vertx.testtools.VertxAssert.*;

public class SendMailServiceIntegrationTest extends TestVerticle {

    @Test
    public void testInvalidInput() {
        container.logger().info("in test SendMailService");
        JsonObject emailObj = new JsonObject();
        emailObj.putString("to", "jaanratas");
        vertx.eventBus().send("epms.email.in", emailObj, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> reply) {
                assertEquals("the validation errors on input", reply.body().getString("message"));

                testComplete();
            }
        });
    }

    @Test
    public void testConnectionError() {

      container.logger().info("in test SendMailService");
        JsonObject emailObj = new JsonObject();
        emailObj.putString("to", "reget.kalamees@helmes.ee");
        emailObj.putString("subject", "Int test");
        emailObj.putString("body", "test");
        
        vertx.eventBus().send("epms.email.in", emailObj, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> reply) {
                assertEquals("Your email could not be sent, please try again later", reply.body().getString("message"));
                testComplete();
            }
        });
    }

    @Override
    public void start() {
        // Make sure we call initialize() - this sets up the assert stuff so assert functionality works correctly
        initialize();
        JsonObject appConfig = container.config();
        JsonObject mailerConfig = appConfig.getObject("mailer_config");
        container.deployVerticle("org.oecd.epms.SendMailService", mailerConfig, new AsyncResultHandler<String>() {
            @Override
            public void handle(AsyncResult<String> asyncResult) {
                assertTrue(asyncResult.succeeded());
                assertNotNull("deploymentID should not be null", asyncResult.result());
                startTests();
            }
        });
    }

}
