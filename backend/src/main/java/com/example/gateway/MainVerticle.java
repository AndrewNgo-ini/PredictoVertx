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

// import com.bentoml.grpc.v1.NDArray;
import com.example.client.AdhocFlightClient;
// import com.example.client.BentoServiceClient;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
// import com.google.gson.Gson;
// import com.google.gson.GsonBuilder;
// import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.Value;
import java.nio.file.Files;
import com.google.common.util.concurrent.FutureCallback;

public class MainVerticle extends AbstractVerticle {

  // private BentoServiceClient bentoServiceClient = new BentoServiceClient();
  // private BentoServiceClient bentoServiceClient2 = new BentoServiceClient();
  private ExecutorService executors = Executors.newCachedThreadPool();
  private static final Logger logger = Logger.getLogger(MainVerticle.class.getName());
  private WebClient webClient;
  private String host1;
  private String host2;
  private JsonObject categoryIndexProb1 = loadJsonFile("category_index.json");
  private JsonObject categoryIndexProb2 = loadJsonFile("category_index.json");
  private List<String> orderFeatureName;
  private AdhocFlightClient adhocFlightClient;
  
  // #['Denial of Service' 'Exploits' 'Information Gathering' 'Malware' 'Normal', 'Other']
  private JsonObject labelMapping = new JsonObject()
    .put("0", "Denial of Service")
    .put("1", "Exploits")
    .put("2", "Information Gathering")
    .put("3", "Malware")
    .put("4", "Normal")
    .put("5", "Other");

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    orderFeatureName = new ArrayList<String>();
    for (int i = 1; i <= 41; i++) {
      orderFeatureName.add("feature" + i);
    }

    JsonObject config = config();
    String host1 = config.getString("host1", "localhost");
    String host2 = config.getString("host2", "localhost");
    this.host1 = host1;
    this.host2 = host2;
    System.out.println("config: " + config);
    System.out.println("host1: " + host1);
    System.out.println("host2: " + host2);
    // vertx.deployVerticle(new GetPredictionVerticle(),
    //         new DeploymentOptions()
    //         .setWorker(true)
    //         .setConfig(config));
    // bentoServiceClient.init(this.host1, 3000);
    // bentoServiceClient2.init(this.host2, 3100);
    webClient = WebClient.create(vertx);
    adhocFlightClient = new AdhocFlightClient(host1, 3000);
    //adhocFlightClient.init();

    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());
    router.post("/phase-2/prob-1/predict")
        .handler(routingContext -> submitTaskToPool(routingContext, this::handleTestFlight));
    router.post("/phase-2/prob-2/predict")
        .handler(routingContext -> submitTaskToPool(routingContext, this::handleModel2));
    // router.post("/test_flight")
    //     .handler(routingContext -> submitTaskToPool(routingContext, this::handleTestFlight));

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

  // Load JsonFile that maps "feature 1" with value "http" to 0
  private JsonObject loadJsonFile(String path) {
    String content = "";
    try {
      content = new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return new JsonObject(content);
  }

  // turn value of column "feature X" value "Y" to index from loaded jsonobject
  private Integer getIndex(JsonObject categoryIndex, String feature, String value) {
    JsonObject featureIndex = categoryIndex.getJsonObject(feature);
    return featureIndex.getInteger(value, 0);
  }

  private JsonArray transformCateJsonArray(JsonArray rows, JsonArray columns) {
    JsonObject featureNameWithRespectiveIndex = new JsonObject();

    // get index of feature 2, 3, 4
    for (int i = 0; i < columns.size(); i++) {
      String featureName = columns.getString(i);
      if (featureName.equals("feature2") || featureName.equals("feature3") || featureName.equals("feature4")) {
        featureNameWithRespectiveIndex.put(featureName, i);
      }
    }

    JsonArray newRows = new JsonArray();
    for (int i = 0; i < rows.size(); i++) {
      JsonArray row = rows.getJsonArray(i);
      for (int j = 0; j < columns.size(); j++) {
        String featureName = columns.getString(j);
        if (featureName.equals("feature2") || featureName.equals("feature3") || featureName.equals("feature4")) {
          Integer index = featureNameWithRespectiveIndex.getInteger(featureName);
          row.set(index, getIndex(categoryIndexProb2, featureName, row.getString(index)));
        } else {
          row.set(j, row.getValue(j));
        }
      }
      newRows.add(row);
    }
    return newRows;
  }

  private JsonArray transformCateAndReOrderJsonArray(JsonArray rows, JsonArray columns) {
    JsonObject featureNameWithRespectiveIndex = new JsonObject();

    // get index of all feature
    for (int i = 0; i < columns.size(); i++) {
      featureNameWithRespectiveIndex.put(columns.getString(i), i);
    }

    // System.out.println("featureNameWithRespectiveIndex: " +
    //     featureNameWithRespectiveIndex);

    JsonArray newRows = new JsonArray();
    // create new rows with ordered index: feature 1, feature 2, ... feature n
    for (int i = 0; i < rows.size(); i++) {
      JsonArray row = rows.getJsonArray(i);
      JsonArray newRow = new JsonArray();
      for (int j = 0; j < orderFeatureName.size(); j++) {
        String featureName = orderFeatureName.get(j);
        // System.out.println("featureName: " + featureName);
        Integer index = featureNameWithRespectiveIndex.getInteger(featureName);
        if (featureName.equals("feature2") || featureName.equals("feature3") || featureName.equals("feature4")) {
          newRow.add(getIndex(categoryIndexProb2, featureName, row.getString(index)));
        } else {
          newRow.add(row.getValue(index));
        }
        // System.out.println("newRow: " + newRow);
      }
      newRows.add(newRow);
    }
    return newRows;
  }

  // private ListenableFuture<List<Long>> callModelGRPC1(String apiName, String jsonObj) {
  //   return Futures.transform(
  //       bentoServiceClient.getPredictionGRPC(apiName, jsonObj),
  //       ndarray -> {
  //         return ndarray.getInt64ValuesList();
  //       }, executors);
  // }

  // private ListenableFuture<List<String>> callModelGRPC2(String apiName, String jsonObj) {
  //   return Futures.transform(
  //       bentoServiceClient2.getPredictionJSON(apiName, jsonObj),
  //       jsobj -> {
  //         // ArrayList<String> predictions = new ArrayList<String>();
  //         // for (Value v : jsobj.getListValue().getValuesList()) {
  //         // predictions.add(v.getStringValue());
  //         // }
  //         return jsobj.getListValue().getValuesList()
  //             .stream()
  //             .map(Value::getStringValue)
  //             .collect(Collectors.toCollection(ArrayList::new));
  //       }, executors);
  // }

  public ListenableFuture<List<Float>> prepareArray(JsonArray jsonArray) {
    List<Float> flattenedList = new ArrayList<>();
    for (Object outerElement : jsonArray) {
      if (outerElement instanceof JsonArray) {
        for (Object innerElement : (JsonArray) outerElement) {
          flattenedList.add(((Number) innerElement).floatValue());
        }
      } else {
        flattenedList.add(((Number) outerElement).floatValue());
      }
    }
    return Futures.immediateFuture(flattenedList);
  }

  private ListenableFuture<List<Integer>> prepareShape(JsonArray rows) {
    List<Integer> shapeIterable = new ArrayList<Integer>();
    shapeIterable.add(rows.size());
    shapeIterable.add(rows.getJsonArray(0).size());
    return Futures.immediateFuture(shapeIterable);
  }

  // private ListenableFuture<List<Long>> callModelGRPC1(String apiName, JsonArray rows) {
  //   ListenableFuture<List<Integer>> future1 = prepareShape(rows);
  //   ListenableFuture<List<Float>> future2 = prepareArray(rows);
  //   ListenableFuture<NDArray> future4 = Futures.transformAsync(
  //       Futures.allAsList(future1, future2),
  //       input -> {
  //         List<Integer> shapeIterable = (List<Integer>) input.get(0);
  //         List<Float> arrayIterable = (List<Float>) input.get(1);
  //         return bentoServiceClient.getNDArrayPredictionFromNdArray(apiName, shapeIterable, arrayIterable);
  //       }, executors);
  //   return Futures.transform(
  //       future4,
  //       ndarray -> {
  //         return ndarray.getInt64ValuesList();
  //       }, executors);
  // }

  // private ListenableFuture<List<String>> callModelGRPC2(String apiName, JsonArray rows) {
  //   ListenableFuture<List<Integer>> future1 = prepareShape(rows);
  //   ListenableFuture<List<Float>> future2 = prepareArray(rows);
  //   ListenableFuture<Value> future4 = Futures.transformAsync(
  //       Futures.allAsList(future1, future2),
  //       input -> {
  //         List<Integer> shapeIterable = (List<Integer>) input.get(0);
  //         List<Float> arrayIterable = (List<Float>) input.get(1);
  //         return bentoServiceClient2.getJsonPredictionFromNdArray(apiName, shapeIterable, arrayIterable);
  //       }, executors);
  //   return Futures.transform(
  //       future4,
  //       jsobj -> {
  //         return jsobj.getListValue().getValuesList()
  //             .stream()
  //             .map(Value::getStringValue)
  //             .collect(Collectors.toCollection(ArrayList::new));
  //       }, executors);
  // }

  // private ListenableFuture<List<List<Float>>> callModelGRPC2ReturnRaw(String apiName, JsonArray rows) {
  //   ListenableFuture<List<Integer>> future1 = prepareShape(rows);
  //   ListenableFuture<List<Float>> future2 = prepareArray(rows);
  //   ListenableFuture<NDArray> future4 = Futures.transformAsync(
  //       Futures.allAsList(future1, future2),
  //       input -> {
  //         List<Integer> shapeIterable = (List<Integer>) input.get(0);
  //         List<Float> arrayIterable = (List<Float>) input.get(1);
  //         return bentoServiceClient2.getNDArrayPredictionFromNdArray(apiName, shapeIterable, arrayIterable);
  //       }, executors);
  //   return Futures.transform(
  //       future4,
  //       ndarray -> {
  //         return ndarray.getFloatValuesList()
  //             .stream()
  //             .map(value -> {
  //               List<Float> row = new ArrayList<Float>();
  //               row.add(value);
  //               return row;
  //             })
  //             .collect(Collectors.toCollection(ArrayList::new));
  //       }, executors);
  // }

  private ListenableFuture<List<Float>> postProcessLightGBM(List<Long> predictions) {
    List<Float> newPredictions = new ArrayList<Float>();
    // 1 If the probability of the first class is greater than 0.5, then the
    // prediction is 0
    for (int i = 0; i < predictions.size(); i++) {
      if (predictions.get(i) > 0.5) {
        newPredictions.add(0.0f);
      } else {
        newPredictions.add(1.0f);
      }
    }
    return Futures.immediateFuture(newPredictions);
  }

  private ListenableFuture<List<String>> postProcessLabelLightGBM(List<List<Float>> predictions) {
    List<String> newPredictions = new ArrayList<String>();
    // Argmax then label mapping
    for (int i = 0; i < predictions.size(); i++) {
      List<Float> prediction = predictions.get(i);
      Float max = prediction.get(0);
      Integer maxIndex = 0;
      for (int j = 1; j < prediction.size(); j++) {
        if (prediction.get(j) > max) {
          max = prediction.get(j);
          maxIndex = j;
        }
      }
      newPredictions.add(this.labelMapping.getString(maxIndex.toString(), "Other"));
    }
    return Futures.immediateFuture(newPredictions);
  }


  // private void handleModel1GRPC(RoutingContext routingContext) {
  //   JsonObject input = routingContext.body().asJsonObject();
  //   // System.out.println("loaded json: " + categoryIndexProb2);

  //   // JsonArray rows = input.getJsonArray("rows");
  //   // System.out.println("Before: " + rows);
  //   // JsonArray columns = input.getJsonArray("columns");

  //   // JsonArray newRows = transformCateJsonArray(rows, columns);
  //   // input.put("rows", newRows);
  //   // input.put("columns", this.orderFeatureName);
  //   // System.out.println("After: " + input.getJsonArray("rows"));


  //   Futures.addCallback(
  //       Futures.transform(
  //           callModelGRPC1("inference", input.toString()),
  //           predictions -> {
  //             JsonObject response = new JsonObject()
  //                 .put("id", input.getString("id", ""))
  //                 .put("predictions", postProcessLightGBM(predictions))
  //                 .put("drift", 0);
  //             return response;
  //           }, executors),
  //       new FutureCallback<JsonObject>() {
  //         @Override
  //         public void onSuccess(JsonObject resp) {
  //           System.out.println("success 1");
  //           routingContext.response()
  //               .putHeader("content-type", "application/json")
  //               .end(resp.toString());
  //         }

  //         @Override
  //         public void onFailure(Throwable t) {
  //           t.printStackTrace();
  //           routingContext.response()
  //               .putHeader("content-type", "application/json")
  //               .end(new JsonObject().put("message", "failed").encodePrettily());
  //         }
  //       }, executors);
  // }

  // private void handleModel2GRPC(RoutingContext routingContext) {
  //   JsonObject input = routingContext.body().asJsonObject();

  //   // System.out.println("loaded json: " + categoryIndexProb2);

  //   // JsonArray rows = input.getJsonArray("rows");
  //   // System.out.println("Before: " + rows);
  //   // JsonArray columns = input.getJsonArray("columns");

  //   // JsonArray newRows = transformCateJsonArray(rows, columns);
  //   // input.put("rows", newRows);
  //   // input.put("columns", this.orderFeatureName);
  //   // System.out.println("After: " + input.getJsonArray("rows"));

  //   Futures.addCallback(
  //       Futures.transform(
  //           callModelGRPC2("inference2", input.toString()),
  //           // callModelGRPC2("inference2", newRows),
  //           predictions -> {
  //             // System.out.println("predictions: " + predictions);
  //             JsonObject response = new JsonObject()
  //                 .put("id", input.getString("id", ""))
  //                 .put("predictions", predictions)
  //                 .put("drift", 0);
  //             return response;
  //           }, executors),
  //       new FutureCallback<JsonObject>() {
  //         @Override
  //         public void onSuccess(JsonObject resp) {
  //           System.out.println("success 2");
  //           routingContext.response()
  //               .putHeader("content-type", "application/json")
  //               .end(resp.toString());
  //         }

  //         @Override
  //         public void onFailure(Throwable t) {
  //           t.printStackTrace();
  //           routingContext.response()
  //               .putHeader("content-type", "application/json")
  //               .end(new JsonObject().put("message", "failed").encodePrettily());
  //         }
  //       }, executors);
  // }

  // -------------------------------- REST --------------------------------

  // private JsonArray reOrder(JsonArray rows, JsonArray columns) {
  // // re-order the rows and columns from ["feaure 3", "feature 2"] to ["feature
  // 2", "feature 3"]
  // JsonArray newRows = new JsonArray();
  // for (int i = 0; i < rows.size(); i++) {
  // JsonArray row = rows.getJsonArray(i);
  // JsonArray newRow = new JsonArray();
  // for (int j = 0; j < columns.size(); j++) {
  // newRow.add(row.getValue(columns.getInteger(j)));
  // }
  // newRows.add(newRow);
  // }
  // }

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
