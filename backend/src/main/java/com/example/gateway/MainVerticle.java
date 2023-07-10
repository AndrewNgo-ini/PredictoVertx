package com.example.gateway;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Future;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.mongo.*;
import io.vertx.ext.web.client.WebClient;
import io.vertx.core.json.JsonArray;
import java.util.List;


public class MainVerticle extends AbstractVerticle {

  private MongoClient mongoClient;
  private WebClient webClient;
  

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());
    
    JsonObject config = new JsonObject()
      .put("connection_string", "mongodb://root:example@localhost:27017/")
      .put("db_name", "request");
    mongoClient = MongoClient.createShared(vertx, config);
    webClient = WebClient.create(vertx);
    
    router.post("/phase-2/prob-1/predict")
      .handler(this::handleModel1);  
    
    // router.post("/phase-1/prob-2/predict")
    //   .handler(this::handleModel2);  
        

    vertx.createHttpServer()
      .requestHandler(router)
      .listen(5040, http -> {
      if (http.succeeded()) {
        startPromise.complete();
        System.out.println("HTTP server started on port 8888");
      } else {
        startPromise.fail(http.cause());
      }
    });
  }

  private Future<List<Long>> callModel1(JsonArray rows, JsonArray columns) {
    Promise<List<Long>> promise = Promise.promise();
    webClient.post(3000, "python_service", "/phase-2/prob-1/predict")
        .sendJsonObject(new JsonObject()
            .put("rows", rows)
            .put("columns", columns))
        .onSuccess(response -> {
            List<Long> result = response.bodyAsJsonArray().getList();
            System.out.println("Received response with status code " + response.statusCode());
            promise.complete(result);
        })
        .onFailure(error -> {
            error.printStackTrace();
            System.out.println("Something went wrong " + error.getMessage());
            promise.fail(error);
        });
    return promise.future();
  }

  

  private void saveData(JsonObject data) {
    mongoClient.save("sample", data).onComplete(res -> {
      if (res.succeeded()) {
        String id = res.result();
        System.out.println("Saved book with id " + id);
      } else {
        res.cause().printStackTrace();
      }
    });
  }

  public void handleModel1(RoutingContext routingContext) {
    JsonObject input = routingContext.getBodyAsJson();
    
    vertx.executeBlocking(promise -> {
        JsonArray rows = input.getJsonArray("rows");
        JsonArray columns = input.getJsonArray("columns");
        callModel1(rows, columns)
            .onSuccess(result -> promise.complete(result))
            .onFailure(error -> promise.fail(error));
    }).onComplete(res -> {
        if (res.succeeded()) {
            List<Long> result = (List<Long>) res.result();
            routingContext.response()
                .putHeader("content-type", "application/json")
                .end(new JsonObject()
                    .put("id", input.getString("id", ""))
                    .put("predictions", result)
                    .put("drift", 0)
                    .encodePrettily());
        } else {
            routingContext.response()
                .putHeader("content-type", "application/json")
                .end(new JsonObject().put("message", "failed").encodePrettily());
        }
    });
}

}