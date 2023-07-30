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
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.example.client.BentoServiceClient;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.Value;
import java.nio.file.Files;
import com.google.common.util.concurrent.FutureCallback;

public class MainVerticle extends AbstractVerticle {

  private BentoServiceClient bentoServiceClient = new BentoServiceClient();
  private BentoServiceClient bentoServiceClient2 = new BentoServiceClient();
  private ExecutorService executors = Executors.newCachedThreadPool();
  private static final Logger logger = Logger.getLogger(MainVerticle.class.getName());
  private WebClient webClient;
  private String host1;
  private String host2;
  private JsonObject labelMapping = new JsonObject()
      .put("0", "setosa")
      .put("1", "versicolor")
      .put("2", "virginica");

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    JsonObject config = config();
    String host1 = config.getString("host1");
    String host2 = config.getString("host2");
    this.host1 = host1;
    this.host2 = host2;
    System.out.println("config: " + config);
    System.out.println("host1: " + host1);
    System.out.println("host2: " + host2);
    // vertx.deployVerticle(new GetPredictionVerticle(), 
    //       new DeploymentOptions()
    //       .setWorker(true)
    //       .setConfig(config));
    bentoServiceClient.init(this.host1, 3000);
    bentoServiceClient2.init(this.host2, 3100);
    webClient = WebClient.create(vertx);

    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());
    router.post("/phase-2/prob-1/predict")
        .handler(routingContext -> submitTaskToPool(routingContext, this::handleModel1GRPC));
    router.post("/phase-2/prob-2/predict")
        .handler(routingContext -> submitTaskToPool(routingContext, this::handleModel2GRPC));

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

  public void eventBusModel1Rest(RoutingContext routingContext) {
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


  // NOT USING WORKER VERTICLE

  // -------------------------------- GRPC --------------------------------
  private ListenableFuture<List<Long>> callModelGRPC1(String apiName, String jsonObj) {
    return Futures.transform(
            bentoServiceClient.getPredictionGRPC(apiName, jsonObj),
            ndarray -> {
                return ndarray.getInt64ValuesList();
            }, executors);
  }

  private ListenableFuture<List<String>> callModelGRPC2(String apiName, String jsonObj) {
    return Futures.transform(
            bentoServiceClient2.getPredictionJSON(apiName, jsonObj),
            jsobj -> {
                // ArrayList<String> predictions = new ArrayList<String>();
                // for (Value v : jsobj.getListValue().getValuesList()) {
                //     predictions.add(v.getStringValue());
                // }
                ArrayList<String> predictions = jsobj.getListValue().getValuesList()
                  .stream()
                  .map(Value::getStringValue)
                  .collect(Collectors.toCollection(ArrayList::new));
                // System.out.println("predictions: " + predictions);
                return predictions;
            }, executors);
  }

  private void handleModel1GRPC(RoutingContext routingContext) {
    JsonObject input = routingContext.body().asJsonObject();
    Futures.addCallback(
            Futures.transform(
                    callModelGRPC1("inference", input.toString()),
                    predictions -> {
                        JsonObject response = new JsonObject()
                                .put("id", input.getString("id", ""))
                                .put("predictions", predictions)
                                .put("drift", 0);
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

  private void handleModel2GRPC(RoutingContext routingContext) {
    JsonObject input = routingContext.body().asJsonObject();
    Futures.addCallback(
            Futures.transform(
                    callModelGRPC2("inference2", input.toString()),
                    predictions -> {
                        //System.out.println("predictions: " + predictions);
                        JsonObject response = new JsonObject()
                                .put("id", input.getString("id", ""))
                                .put("predictions", predictions)
                                .put("drift", 0);
                        return response;
                    }, executors),
            new FutureCallback<JsonObject>() {
                @Override
                public void onSuccess(JsonObject resp) {
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



  // -------------------------------- REST --------------------------------

  // private JsonArray reOrder(JsonArray rows, JsonArray columns) {
  //   // re-order the rows and columns from ["feaure 3", "feature 2"] to ["feature 2", "feature 3"]
  //   JsonArray newRows = new JsonArray();
  //   for (int i = 0; i < rows.size(); i++) {
  //     JsonArray row = rows.getJsonArray(i);
  //     JsonArray newRow = new JsonArray();
  //     for (int j = 0; j < columns.size(); j++) {
  //       newRow.add(row.getValue(columns.getInteger(j)));
  //     }
  //     newRows.add(newRow);
  //   }
  // }

  private SettableFuture<List<Long>> modelCommunicator(JsonArray rows, JsonArray columns, String host, Integer port, String endpoint) {
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
                                    .put("drift", 0);
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
                                    .put("drift", 0);
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
