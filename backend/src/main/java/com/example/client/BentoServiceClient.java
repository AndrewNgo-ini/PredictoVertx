package com.example.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.bentoml.grpc.v1.BentoServiceGrpc;
import com.bentoml.grpc.v1.BentoServiceGrpc.BentoServiceBlockingStub;
import com.bentoml.grpc.v1.BentoServiceGrpc.BentoServiceFutureStub;
import com.bentoml.grpc.v1.BentoServiceGrpc.BentoServiceStub;
import com.bentoml.grpc.v1.NDArray;
import com.bentoml.grpc.v1.Request;
import com.bentoml.grpc.v1.RequestOrBuilder;
import com.bentoml.grpc.v1.Response;

import io.vertx.core.json.JsonArray;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

// make this class a client for MainVerticle to use
public class BentoServiceClient {
    private static final Logger logger = Logger.getLogger(BentoServiceClient.class.getName());
    private String host;
    private Integer port;
    private ManagedChannel channel;
    private BentoServiceFutureStub stub;
    private String apiName = "inference";
    private ExecutorService executors = Executors.newCachedThreadPool();

    static Iterable<Integer> convert(int[] array) {
        return () -> Arrays.stream(array).iterator();
    }

    public void init(String host, Integer port) {
        if (host == null || host.isEmpty()) {
            this.host = "localhost";
        } 
        else {
            this.host = host;
        }
        this.port = port;
        this.channel = ManagedChannelBuilder.forAddress(this.host, this.port).usePlaintext().build();
        this.stub = BentoServiceGrpc.newFutureStub(channel);
    }

    public static List<Float> flattenJsonArray(JsonArray jsonArray) {
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
        return flattenedList;
    }

    public ListenableFuture<NDArray> getPrediction(JsonArray rows) {
        // Get shape of the array
        List<Integer> shapeIterable = new ArrayList<Integer>();
        shapeIterable.add(rows.size());
        shapeIterable.add(rows.getJsonArray(0).size());

        List<Float> arrayIterable = flattenJsonArray(rows);

        // Access a service running on the local machine on port 50051
        NDArray.Builder builder = NDArray.newBuilder()
                .addAllShape(shapeIterable)
                .addAllFloatValues(arrayIterable)
                .setDtype(NDArray.DType.DTYPE_FLOAT);

        Request req = Request.newBuilder().setApiName(apiName).setNdarray(builder).build();

        return Futures.transform(
                stub.call(req),
                response -> response.getNdarray(),
                executors);
    }
}