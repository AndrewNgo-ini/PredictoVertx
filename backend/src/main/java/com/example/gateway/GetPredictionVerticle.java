package com.example.gateway;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.bentoml.grpc.v1.NDArray;
import com.example.client.BentoServiceClient;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.eventbus.Message;

public class GetPredictionVerticle extends AbstractVerticle {

    private BentoServiceClient bentoServiceClient = new BentoServiceClient();
    private BentoServiceClient bentoServiceClient2 = new BentoServiceClient();
    private ExecutorService executors = Executors.newCachedThreadPool();

    @Override
    public void start() throws Exception {
        JsonObject config = config();
        String host1 = config.getString("host1");
        String host2 = config.getString("host2");
        System.out.println("config: " + config);
        System.out.println("host1: " + host1);
        System.out.println("host2: " + host2);
        bentoServiceClient.init(host1, 3000);
        bentoServiceClient2.init(host2, 3100);

        vertx.eventBus().consumer("get.prediction", message -> {
            this.handleModel1(message);
        });

        vertx.eventBus().consumer("get.prediction2", message -> {
            this.handleModel2(message);
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

    private ListenableFuture<List<Long>> callModelGrpc(String apiName, JsonArray rows) {
        ListenableFuture<List<Integer>> future1 = prepareShape(rows);
        ListenableFuture<List<Float>> future2 = prepareArray(rows);
        ListenableFuture<NDArray> future4 = Futures.transformAsync(
                Futures.allAsList(future1, future2),
                input -> {
                    List<Integer> shapeIterable = (List<Integer>) input.get(0);
                    List<Float> arrayIterable = (List<Float>) input.get(1);
                    return bentoServiceClient.getPrediction(apiName, shapeIterable, arrayIterable);
                }, executors);
        return Futures.transform(
                future4,
                ndarray -> {
                    return ndarray.getInt64ValuesList();
                }, executors);
    }

    private ListenableFuture<List<Long>> callModelGrpc2(String apiName, JsonArray rows) {
        ListenableFuture<List<Integer>> future1 = prepareShape(rows);
        ListenableFuture<List<Float>> future2 = prepareArray(rows);
        ListenableFuture<NDArray> future4 = Futures.transformAsync(
                Futures.allAsList(future1, future2),
                input -> {
                    List<Integer> shapeIterable = (List<Integer>) input.get(0);
                    List<Float> arrayIterable = (List<Float>) input.get(1);
                    return bentoServiceClient2.getPrediction(apiName, shapeIterable, arrayIterable);
                }, executors);
        return Futures.transform(
                future4,
                ndarray -> {
                    return ndarray.getInt64ValuesList();
                }, executors);
    }

    private void handleModel1(Message<Object> message) {
        JsonObject input = (JsonObject) message.body();

        JsonArray rows = fakePreProcess(input.getJsonArray("rows"));
        JsonArray columns = input.getJsonArray("columns");

        Futures.addCallback(
                Futures.transform(
                        callModelGrpc("inference", rows),
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
                        message.reply(resp);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        t.printStackTrace();
                        message.fail(500, t.getMessage());
                    }
                }, executors);
    }

    private void handleModel2(Message<Object> message) {
        JsonObject input = (JsonObject) message.body();

        JsonArray rows = fakePreProcess(input.getJsonArray("rows"));
        JsonArray columns = input.getJsonArray("columns");

        Futures.addCallback(
                Futures.transform(
                        callModelGrpc2("inference2", rows),
                        predictions -> {
                            JsonObject response = new JsonObject()
                                    .put("id", input.getString("id", ""))
                                    .put("predictions", fakePostProcess(predictions))
                                    .put("drift", 0);
                            return response;
                        }, executors),
                new FutureCallback<JsonObject>() {
                    @Override
                    public void onSuccess(JsonObject resp) {
                        // logger.info("Received response from model 1: " + System.currentTimeMillis());
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
