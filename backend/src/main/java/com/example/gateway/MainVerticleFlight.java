package com.example.gateway;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.BodyHandler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.example.client.AdhocFlightClient;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.FutureCallback;

public class MainVerticleFlight extends AbstractVerticle {

  private ExecutorService executors = Executors.newCachedThreadPool();
  private AdhocFlightClient adhocFlightClient;
  String host1;
  String host2;
  
  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    JsonObject config = config();
    String host1 = config.getString("host1", "localhost");
    String host2 = config.getString("host2", "localhost");
    this.host1 = host1;
    this.host2 = host2;
    System.out.println("config: " + config);
    System.out.println("host1: " + host1);
    System.out.println("host2: " + host2);
    adhocFlightClient = new AdhocFlightClient(host1, 3000);

    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());
    router.post("/phase-3/prob-1/predict")
        .handler(this::handleTestFlight);

    vertx.createHttpServer()
        .requestHandler(router)
        .listen(5040, http -> {
          if (http.succeeded()) {
            startPromise.complete();
            System.out.println("HTTP server started on port 5040");
          } else {
            startPromise.fail(http.cause());
          }
        });
  }


  public void handleTestFlight(RoutingContext routingContext) {
    JsonObject input = routingContext.body().asJsonObject();
    Futures.addCallback(
      Futures.transform(
          this.adhocFlightClient.doExchange(input),
          predictions -> {
            JsonObject response = new JsonObject()
                .put("id", input.getString("id", ""))
                .put("predictions", predictions)
                .put("drift", 1);
            return response;
          }, executors),
      new FutureCallback<JsonObject>() {
        @Override
        public void onSuccess(JsonObject resp) {
          // logger.info("Received response from model 1: " + System.currentTimeMillis());
          routingContext.response()
              .putHeader("content-type", "application/json")
              .end(resp.toString());
        }

        @Override
        public void onFailure(Throwable t) {
          t.printStackTrace();
          routingContext.response()
              .putHeader("content-type", "application/json")
              .end(new JsonObject().put("message", "failed").encodePrettily());
        }
      }, executors);
  }
}
