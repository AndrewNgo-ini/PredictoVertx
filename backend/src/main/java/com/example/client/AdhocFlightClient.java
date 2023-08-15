package com.example.client;

import org.apache.arrow.flight.FlightClient;
import org.apache.arrow.flight.FlightDescriptor;
import org.apache.arrow.flight.FlightStream;
import org.apache.arrow.flight.Location;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.BigIntVector;
import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.Float8Vector;
import org.apache.arrow.vector.IntVector;
import org.apache.arrow.vector.VarCharVector;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.types.FloatingPointPrecision;
import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.Schema;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class AdhocFlightClient {
    // Create a new root allocator
    RootAllocator allocator = new RootAllocator();
    private final FlightClient client;

    public AdhocFlightClient(String host, int port) {
        // Connect to the Flight server
        client = FlightClient.builder(allocator, Location.forGrpcInsecure(host, port)).build();
    }

    private List<Field> prepareFields() {
        List<Field> fields = new ArrayList<>();
        for (int i = 0; i < 41; i++) {
            // feature 2, 3, 4
            if (i == 1 || i == 2 || i == 3) {
                Field feature = Field.notNullable("feature" + (i + 1), new ArrowType.Utf8());
                fields.add(feature);
            } else {
                Field feature = Field.notNullable("feature" + (i + 1),
                        new ArrowType.FloatingPoint(FloatingPointPrecision.DOUBLE));
                fields.add(feature);
            }
        }
        return fields;

    }

    private List<FieldVector> prepareVectors(JsonObject input, VectorSchemaRoot root) {
        List<FieldVector> vectors = new ArrayList<>();
        JsonArray arr = input.getJsonArray("rows");
        JsonArray cols = input.getJsonArray("columns");
        for (int i = 0; i < cols.size(); i++) {
            String featureName = cols.getString(i);
            // minus 1 from feature 2, 3,4
            if (featureName.equals("feature2") || featureName.equals("feature3") || featureName.equals("feature4")) {
                VarCharVector feature = (VarCharVector) root.getVector(featureName);
                for (int j = 0; j < arr.size(); j++) {
                    // System.out.println("i: " + i + " j: " + j);
                    // System.out.println("value: " + arr.getJsonArray(j).getString(i));
                    feature.set(j, arr.getJsonArray(j).getString(i).getBytes(StandardCharsets.UTF_8));
                }
                vectors.add(feature);
            } else {
                Float8Vector feature = (Float8Vector) root.getVector(featureName);
                for (int j = 0; j < arr.size(); j++) {
                    // System.out.println("i: " + i + " j: " + j);
                    // System.out.println("value: " + arr.getJsonArray(j).getString(i));
                    feature.set(j, arr.getJsonArray(j).getDouble(i));
                }
                vectors.add(feature);
            }
            root.setRowCount(arr.size());

        }
        return vectors;
    }

    public ListenableFuture<List<Integer>> doExchange(JsonObject input) {
        FlightDescriptor descriptor = FlightDescriptor.command("do_exchange".getBytes(StandardCharsets.UTF_8));

        // Create a VectorSchemaRoot from the input
        Schema schema = new Schema(prepareFields());
        VectorSchemaRoot root = VectorSchemaRoot.create(schema, allocator);
        // Create a VectorSchemaRoot from the input
        root.allocateNew();
        prepareVectors(input, root);

        // Perform the exchange
        try (FlightClient.ExchangeReaderWriter exchange = client.doExchange(descriptor)) {
            // Read and process the response from the server
            exchange.getWriter().start(root);
            exchange.getWriter().putNext();
            exchange.getWriter().completed();

            FlightStream reader = exchange.getReader();
            VectorSchemaRoot responseRoot = reader.getRoot();
            reader.next();
            BigIntVector responseDataVector = (BigIntVector) responseRoot.getVector("predictions");
            System.out.println(responseDataVector.toString());
            List<Integer> responseData = new ArrayList<>();
            for (int i = 0; i < responseDataVector.getValueCount(); i++) {
                responseData.add((int) responseDataVector.get(i));
            }
            return Futures.immediateFuture(responseData);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
