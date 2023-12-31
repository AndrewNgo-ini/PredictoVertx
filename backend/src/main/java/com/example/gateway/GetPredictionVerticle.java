package com.example.gateway;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import com.bentoml.grpc.v1.NDArray;
import com.example.client.BentoServiceClient;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Value;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.eventbus.Message;

public class GetPredictionVerticle extends AbstractVerticle {

    private BentoServiceClient bentoServiceClient = new BentoServiceClient();
    private BentoServiceClient bentoServiceClient2 = new BentoServiceClient();
    private ExecutorService executors = Executors.newCachedThreadPool();
    private String host1;
    private String host2;

    @Override
    public void start() throws Exception {
        JsonObject config = config();
        String host1 = config.getString("host1");
        String host2 = config.getString("host2");
        this.host1 = host1;
        this.host2 = host2;
        System.out.println("config: " + config);
        System.out.println("host1: " + host1);
        System.out.println("host2: " + host2);
        bentoServiceClient.init(this.host1, 3000);
        bentoServiceClient2.init(this.host2, 3100);

        vertx.eventBus().consumer("get.prediction", message -> {
            this.handleModel1GRPC(message);
        });

        vertx.eventBus().consumer("get.prediction2", message -> {
            this.handleModel2GRPC(message);
        });
    }

    private JsonArray fakePreProcess(JsonArray rows) {
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

    private List<String> fakePostProcess(List<Long> predictions) {
        // Return list of "Normal" base on length of predictions
        List<String> result = new ArrayList<String>();
        for (int i = 0; i < predictions.size(); i++) {
            result.add("Normal");
        }
        return result;
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

    private ListenableFuture<List<Long>> callModelGRPC2(String apiName, JsonArray rows) {
        ListenableFuture<List<Integer>> future1 = prepareShape(rows);
        ListenableFuture<List<Float>> future2 = prepareArray(rows);
        ListenableFuture<NDArray> future4 = Futures.transformAsync(
                Futures.allAsList(future1, future2),
                input -> {
                    List<Integer> shapeIterable = (List<Integer>) input.get(0);
                    List<Float> arrayIterable = (List<Float>) input.get(1);
                    return bentoServiceClient2.getNDArrayPredictionFromNdArray(apiName, shapeIterable, arrayIterable);
                }, executors);
        return Futures.transform(
                future4,
                ndarray -> {
                    return ndarray.getInt64ValuesList();
                }, executors);
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

    private void handleModel1GRPC(Message<Object> message) {
        JsonObject input = (JsonObject) message.body();

        // JsonArray rows = fakePreProcess(input.getJsonArray("rows"));
        // JsonArray columns = input.getJsonArray("columns");

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
                        //System.out.println("Received response from model 1: response: " + resp);
                        message.reply(resp);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        t.printStackTrace();
                        message.fail(500, t.getMessage());
                    }
                }, executors);
    }

    private void handleModel2GRPC(Message<Object> message) {
        JsonObject input = (JsonObject) message.body();

        JsonArray rows = fakePreProcess(input.getJsonArray("rows"));
        // JsonArray columns = input.getJsonArray("columns");

        Futures.addCallback(
                Futures.transform(
                        callModelGRPC2("inference2", rows),
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
                        //System.out.println("Received response from model 2: response: " + resp);
                        message.reply(resp);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        t.printStackTrace();
                        message.fail(500, t.getMessage());
                    }
                }, executors);
    }
}
