package com.example.gateway;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.core.json.JsonArray;

import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import com.bentoml.grpc.v1.NDArray;
import com.example.client.BentoServiceClient;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Value;

import java.nio.file.Files;
import com.google.common.util.concurrent.FutureCallback;

public class MainVerticleCallGrpc extends AbstractVerticle {

  private BentoServiceClient bentoServiceClient = new BentoServiceClient();
  private BentoServiceClient bentoServiceClient2 = new BentoServiceClient();
  private ExecutorService executors = Executors.newCachedThreadPool();
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
    
    bentoServiceClient.init(this.host1, 3000);
    bentoServiceClient2.init(this.host2, 3100);

    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());
    router.post("/phase-3/prob-2/predict")
        .handler(routingContext -> submitTaskToPool(routingContext, this::handleModel1GRPC));
    router.post("/phase-3/prob-2/predict")
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
          // predictions.add(v.getStringValue());
          // }
          return jsobj.getListValue().getValuesList()
              .stream()
              .map(Value::getStringValue)
              .collect(Collectors.toCollection(ArrayList::new));
        }, executors);
  }

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

  private ListenableFuture<List<Long>> callModelGRPC1(String apiName, JsonArray rows) {
    ListenableFuture<List<Integer>> future1 = prepareShape(rows);
    ListenableFuture<List<Float>> future2 = prepareArray(rows);
    ListenableFuture<NDArray> future4 = Futures.transformAsync(
        Futures.allAsList(future1, future2),
        input -> {
          List<Integer> shapeIterable = (List<Integer>) input.get(0);
          List<Float> arrayIterable = (List<Float>) input.get(1);
          return bentoServiceClient.getNDArrayPredictionFromNdArray(apiName, shapeIterable, arrayIterable);
        }, executors);
    return Futures.transform(
        future4,
        ndarray -> {
          return ndarray.getInt64ValuesList();
        }, executors);
  }

  private ListenableFuture<List<String>> callModelGRPC2(String apiName, JsonArray rows) {
    ListenableFuture<List<Integer>> future1 = prepareShape(rows);
    ListenableFuture<List<Float>> future2 = prepareArray(rows);
    ListenableFuture<Value> future4 = Futures.transformAsync(
        Futures.allAsList(future1, future2),
        input -> {
          List<Integer> shapeIterable = (List<Integer>) input.get(0);
          List<Float> arrayIterable = (List<Float>) input.get(1);
          return bentoServiceClient2.getJsonPredictionFromNdArray(apiName, shapeIterable, arrayIterable);
        }, executors);
    return Futures.transform(
        future4,
        jsobj -> {
          return jsobj.getListValue().getValuesList()
              .stream()
              .map(Value::getStringValue)
              .collect(Collectors.toCollection(ArrayList::new));
        }, executors);
  }


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


  private void handleModel1GRPC(RoutingContext routingContext) {
    JsonObject input = routingContext.body().asJsonObject();

    Futures.addCallback(
        Futures.transform(
            callModelGRPC1("inference", input.toString()),
            predictions -> {
              JsonObject response = new JsonObject()
                  .put("id", input.getString("id", ""))
                  .put("predictions", postProcessLightGBM(predictions))
                  .put("drift", 0);
              return response;
            }, executors),
        new FutureCallback<JsonObject>() {
          @Override
          public void onSuccess(JsonObject resp) {
            System.out.println("success 1");
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
            // callModelGRPC2("inference2", newRows),
            predictions -> {
              // System.out.println("predictions: " + predictions);
              JsonObject response = new JsonObject()
                  .put("id", input.getString("id", ""))
                  .put("predictions", predictions)
                  .put("drift", 0);
              return response;
            }, executors),
        new FutureCallback<JsonObject>() {
          @Override
          public void onSuccess(JsonObject resp) {
            System.out.println("success 2");
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
