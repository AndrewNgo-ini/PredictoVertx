package com.example.gateway;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.client.WebClient;
import io.vertx.core.json.JsonArray;

import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import com.example.client.BentoServiceClient;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import com.google.common.util.concurrent.FutureCallback;

public class MainVerticle extends AbstractVerticle {

  private ExecutorService executors = Executors.newCachedThreadPool();
  private static final Logger logger = Logger.getLogger(MainVerticle.class.getName());

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    JsonObject config = config();
    vertx.deployVerticle(new GetPredictionVerticle(), new DeploymentOptions().setConfig(config));

    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());
    router.post("/phase-2/prob-1/predict")
        .handler(routingContext -> submitTaskToPool(routingContext, this::handleModel1));
    router.post("/phase-2/prob-2/predict")
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
// JsonObject input = routingContext.getBodyAsJson();

// // Create a Gson object to convert the JSON object to a string
// // Gson gson = new GsonBuilder().setPrettyPrinting().create();

// // // Convert the JSON object to a string
// // String json = gson.toJson(input);

// // // Write the string to a file
// // try {
// // Files.write(Paths.get("output.json"),
// json.getBytes(StandardCharsets.UTF_8));
// // } catch (Exception e) {
// // e.printStackTrace();
// // }
