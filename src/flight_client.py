# client_store.py

import click
import pyarrow.flight as flight
import pyarrow as pa
import pandas as pd

@click.command()
@click.option('--server', default="grpc://0.0.0.0:5050", help='Flight server URL')
@click.option('--action', type=click.Choice(['health_check', 'do_exchange']), default='health_check', help='Choose action')
def main(server, action):
    """Perform an action on the Flight server."""
    client = flight.FlightClient(server)
    if action == 'health_check':
        health_check(client)
    elif action == 'do_exchange':
        do_exchange(client)

def health_check(client):
    """Perform a health check on the Flight server."""
    action = flight.Action("health_check", b"")
    results = client.do_action(action)
    for result in results:
        print("Health Status:", result.body.to_pybytes().decode("utf-8"))

def do_exchange(client):
    """Perform a data exchange on the Flight server."""
    descriptor = flight.FlightDescriptor.for_command("do_exchange")
    writer, reader = client.do_exchange(descriptor)
    print("Client sending descriptor command:", descriptor.command)
    print(reader)
    print(writer)

    # Create a RecordBatch from the DataFrame
    record_batch = pa.RecordBatch.from_pandas(pd.DataFrame({"data": [1, 2, 3]}))

    # Convert the RecordBatch to a Table
    table = pa.Table.from_batches([record_batch])
    print(table)

    # Initialize the writer with the schema
    writer.begin(table.schema)

    # Write the Table to the server
    writer.write_table(table)
    writer.done_writing()

    # Read and process the response from the server
    response = reader.read_all()
    print(response)


if __name__ == '__main__':
    main()
