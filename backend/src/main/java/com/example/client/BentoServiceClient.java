package com.example.client;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.bentoml.grpc.v1.BentoServiceGrpc;
import com.bentoml.grpc.v1.BentoServiceGrpc.BentoServiceBlockingStub;
import com.bentoml.grpc.v1.BentoServiceGrpc.BentoServiceStub;
import com.bentoml.grpc.v1.NDArray;
import com.bentoml.grpc.v1.Request;
import com.bentoml.grpc.v1.RequestOrBuilder;
import com.bentoml.grpc.v1.Response;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.vertx.core.json.JsonArray;

// make this class a client for MainVerticle to use
public class BentoServiceClient {
    private static final Logger logger = Logger.getLogger(BentoServiceClient.class.getName());
    private String target = "localhost:3000";
    private ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
    private BentoServiceBlockingStub blockingStub = BentoServiceGrpc.newBlockingStub(channel);
    private String apiName = "inference";

    static Iterable<Integer> convert(int[] array) {
        return () -> Arrays.stream(array).iterator();
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

    public NDArray getPrediction(JsonArray rows) {
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

        try {
            Response resp = blockingStub.call(req);
            Response.ContentCase contentCase = resp.getContentCase();
            if (contentCase != Response.ContentCase.NDARRAY) {
                throw new Exception("Currently only support NDArray response");
            }
            NDArray output = resp.getNdarray();
            logger.info("Response: " + resp.toString());
            return output;
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
            return null;
        } catch (Exception e) {
            logger.log(Level.WARNING, "RPC failed: {0}", e.getMessage());
            return null;
        }
    }
}