package com.example.gateway;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.client.WebClient;
import io.vertx.core.json.JsonArray;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.FutureCallback;

public class MainVerticleCallRest extends AbstractVerticle {

  private ExecutorService executors = Executors.newCachedThreadPool();
  private WebClient webClient;
  private String host1;
  private String host2;

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
    
    webClient = WebClient.create(vertx);

    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());
    router.post("/phase-3/prob-2/predict")
        .handler(routingContext -> submitTaskToPool(routingContext, this::handleModel1REST));
    router.post("/phase-3/prob-2/predict")
        .handler(routingContext -> submitTaskToPool(routingContext, this::handleModel2REST));

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


  private SettableFuture<List<Long>> modelCommunicator(JsonArray rows, JsonArray columns, String host, Integer port,
      String endpoint) {
    SettableFuture<List<Long>> future = SettableFuture.create();
    webClient.post(port, host, endpoint)
        .sendJsonObject(new JsonObject()
            .put("rows", rows)
            .put("columns", columns))
        .onSuccess(response -> {
          List<Long> result = response.bodyAsJsonArray().getList();
          System.out.println("Received response with status code " + response.statusCode());
          future.set(result);
        })
        .onFailure(error -> {
          error.printStackTrace();
          System.out.println("Something went wrong " + error.getMessage());
          future.setException(error);
        });
    return future;
  }

  public void handleModel1REST(RoutingContext routingContext) {
    JsonObject input = routingContext.getBodyAsJson();
    JsonArray rows = input.getJsonArray("rows");
    JsonArray columns = input.getJsonArray("columns");
    Futures.addCallback(
        Futures.transform(
            modelCommunicator(rows, columns, this.host1, 3000, "/phase-2/prob-1/predict"),
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

  public void handleModel2REST(RoutingContext routingContext) {
    JsonObject input = routingContext.getBodyAsJson();
    JsonArray rows = input.getJsonArray("rows");
    JsonArray columns = input.getJsonArray("columns");
    Futures.addCallback(
        Futures.transform(
            modelCommunicator(rows, columns, this.host2, 3100, "/phase-2/prob-2/predict"),
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
