import pyarrow as pa
import pyarrow.flight as flight
import pathlib
import pandas as pd
from catboost import CatBoostClassifier

class FlightServer(pa.flight.FlightServerBase):
    def __init__(self, model_path, location="grpc://0.0.0.0:5050", **kwargs):
        super(FlightServer, self).__init__(location, **kwargs)
        self._location = location
        self.model = CatBoostClassifier()
        self.model.load_model(model_path)

    def list_actions(self, context):
        return [flight.ActionType("health_check", "Check the system's health")]

    def do_action(self, context, action):
        if action.type == "health_check":
            health_status = "OK"
            return iter([flight.Result(health_status.encode("utf-8"))])
        raise flight.FlightUnavailableError("Unknown action!")

    def do_exchange(self, context, descriptor, reader, writer):
        print("Server received descriptor command:", descriptor.command)
        
        #print(reader)
        #print(writer)
        # Read the incoming data
        incoming_table = reader.read_all()
        #print('\n+ Incoming Table\n', incoming_table)

        # Process the data
        predictions = self._process_exchange(incoming_table)

        table = pa.Table.from_arrays([predictions], ['predictions'])
        print('\n+ Result Table\n', table)

        #print('\nreader.schema', reader.schema)
        #print('\ntable.schema', table.schema)

        # Initialize the writer with the schema
        writer.begin(table.schema)
        # Write the Table to the client
        writer.write_table(table)

    def _process_exchange(self, incoming_table):
        # Convert the incoming table to a Pandas DataFrame
        incoming_df = incoming_table.to_pandas()

        predictions = self.model.predict(incoming_df)
        #print("predictions", predictions)
        #print("type(predictions)", type(predictions))

        # Return the predictions equal to length of the incoming data
        return pa.array(predictions)

def serve(model_path):
    location = "grpc://0.0.0.0:3000"
    server = FlightServer(model_path, location)
    print("Server is running on:", location)
    server.serve()

if __name__ == "__main__":
    import sys
    model_path = sys.argv[1]
    serve(model_path)
