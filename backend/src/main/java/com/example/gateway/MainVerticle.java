package com.example.gateway;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.BodyHandler;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainVerticle extends AbstractVerticle {

  private ExecutorService executors = Executors.newCachedThreadPool();

  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    JsonObject config = config();
    String host1 = config.getString("host1", "localhost");
    String host2 = config.getString("host2", "localhost");
    System.out.println("config: " + config);
    System.out.println("host1: " + host1);
    System.out.println("host2: " + host2);
    vertx.deployVerticle(new GetPredictionVerticle(),
            new DeploymentOptions()
            .setWorker(true)
            .setConfig(config));

    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());
    router.post("/phase-3/prob-2/predict")
        .handler(routingContext -> submitTaskToPool(routingContext, this::handleModel1));
    router.post("/phase-3/prob-2/predict")
        .handler(routingContext -> submitTaskToPool(routingContext, this::handleModel2));

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

  // WORKER VERTICLE

  public void handleModel1(RoutingContext routingContext) {
    vertx.eventBus().request("get.prediction", routingContext.body().asJsonObject(), reply -> {
      if (reply.succeeded()) {
        routingContext.response()
            .putHeader("content-type", "application/json")
            .end(reply.result().body().toString());
      } else {
        routingContext.response()
            .putHeader("content-type", "application/json")
            .end(new JsonObject().put("message", "failed").encodePrettily());
      }
    });
  }

  public void handleModel2(RoutingContext routingContext) {
    vertx.eventBus().request("get.prediction2", routingContext.body().asJsonObject(), reply -> {
      if (reply.succeeded()) {
        routingContext.response()
            .putHeader("content-type", "application/json")
            .end(reply.result().body().toString());
      } else {
        routingContext.response()
            .putHeader("content-type", "application/json")
            .end(new JsonObject().put("message", "failed").encodePrettily());
      }
    });
  }

  public interface RoutingContextHandler extends Handler<RoutingContext> {
    @Override
    void handle(RoutingContext routingContext);
  }

  private void submitTaskToPool(RoutingContext context, RoutingContextHandler handler) {
    executors.submit(() -> {
      try {
        handler.handle(context);
      } catch (Exception e) {
        e.printStackTrace();
        context.response().setStatusCode(504).end(
            new JsonObject()
                .put("message", "Handled error")
                .put("success", false)
                .toString());
      }
    });
  }

}
