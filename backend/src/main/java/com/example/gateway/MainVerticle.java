package com.example.gateway;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Future;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.impl.pool.Executor;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.mongo.*;
import io.vertx.ext.web.client.WebClient;
import io.vertx.core.json.JsonArray;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import com.example.client.BentoServiceClient;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.bentoml.grpc.v1.NDArray;
import com.google.common.util.concurrent.FutureCallback;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


public class MainVerticle extends AbstractVerticle {

  private MongoClient mongoClient;
  private WebClient webClient;
  private BentoServiceClient bentoServiceClient;
  private ExecutorService executors = Executors.newCachedThreadPool();
  private static final Logger logger = Logger.getLogger(MainVerticle.class.getName());

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    JsonObject config = config();
    String host = config.getString("host");
    System.out.println("config: " + config);
    System.out.println("host: " + host);

    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());

    // JsonObject config = new JsonObject()
    // .put("connection_string", "mongodb://root:example@localhost:27017/")
    // .put("db_name", "request");
    // mongoClient = MongoClient.createShared(vertx, config);
    webClient = WebClient.create(vertx);
    bentoServiceClient = new BentoServiceClient();
    bentoServiceClient.init(host, 3000);

    router.post("/phase-2/prob-1/predict")
        .handler(this::handleModel1);

    // router.post("/phase-1/prob-2/predict")
    // .handler(this::handleModel2);

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

  private ListenableFuture<List<Long>> callModel1Grpc(JsonArray rows) {
    return Futures.transform(
        bentoServiceClient.getPrediction(rows), ndarray -> ndarray.getInt64ValuesList(), executors);
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

  private JsonArray fakePreprocess(JsonArray rows) {
    // Iterate over each element in rows 
    for (int i = 0; i < rows.size(); i++) {
      JsonArray element = rows.getJsonArray(i);

      // Iterate over if String then turn it to 1.0
      for (int j = 0; j < element.size(); j++) {
        if (element.getValue(j) instanceof String) {
          element.set(j, 1.0);
        }
      }
    }
    return rows;
  }

  public void handleModel1(RoutingContext routingContext) {
    JsonObject input = routingContext.getBodyAsJson();

    // // Create a Gson object to convert the JSON object to a string
    // Gson gson = new GsonBuilder().setPrettyPrinting().create();

    // // Convert the JSON object to a string
    // String json = gson.toJson(input);

    // // Write the string to a file
    // try {
    //   Files.write(Paths.get("output.json"), json.getBytes(StandardCharsets.UTF_8));
    // } catch (IOException e) {
    //   e.printStackTrace();
    // }


    JsonArray rows = fakePreprocess(input.getJsonArray("rows"));
    JsonArray columns = input.getJsonArray("columns");
    
    Futures.addCallback(
        callModel1Grpc(rows),
        new FutureCallback<List<Long>>() {
          @Override
          public void onSuccess(List<Long> result) {
            logger.info("Received response from model 1: " + result);
            routingContext.response()
                .putHeader("content-type", "application/json")
                .end(new JsonObject()
                    .put("id", input.getString("id", ""))
                    .put("predictions", result)
                    .put("drift", 0)
                    .encodePrettily());
          }

          @Override
          public void onFailure(Throwable t) {
            logger.info("Failed to get response from model 1" + t.getMessage());
            t.printStackTrace();
            routingContext.response()
                .putHeader("content-type", "application/json")
                .end(new JsonObject().put("message", "failed").encodePrettily());
          }
        }, executors);

  }

}