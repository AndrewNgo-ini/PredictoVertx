from locust import HttpUser, task, between
import random
import pandas as pd

# Define the number of rows and features
num_rows = 2000
num_numeric_features = 40
num_categorical_features = 10

# Generate random numeric and categorical feature names
numeric_features = [f"feature{i}" for i in range(1, num_numeric_features + 1)]
categorical_features = [f"feature{i}" for i in range(num_numeric_features + 1, num_numeric_features + num_categorical_features + 1)]

def generate_random_data():
    # Generate a random id for each request
    data_id = str(random.randint(1, 1000))

    # Generate random data for the rows
    rows_data = []
    for _ in range(num_rows):
        row_data = []

        # Generate random numeric values
        for _ in range(num_numeric_features):
            row_data.append(random.random())

        # Generate random categorical values (replace ["a", "b", "c"] with your actual categories)
        for _ in range(num_categorical_features):
            row_data.append(random.choice(["a", "b", "c"]))

        rows_data.append(row_data)

    # Create the final data in the required format
    data = {
        "id": data_id,
        "rows": rows_data,
        "columns": numeric_features + categorical_features
    }

    return data

class APILoadTestUser(HttpUser):
    wait_time = between(1, 2)  # Time between consecutive requests from the same user

    @task
    def send_batch_request(self):
        # You can put your API endpoint URL here
        endpoint_url = "/phase-2/prob-1/predict"

        # Generate random data for each request
        data = generate_random_data()

        # Send the batch request
        with self.client.post(endpoint_url, json=data, catch_response=True) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Request failed with status code: {response.status_code}")
