from locust import HttpUser, task, between
import random
import pandas as pd

# Define the number of rows and features
num_rows = 2000
numeric_features = [
    "feature1", "feature5", "feature6", "feature7", "feature8", "feature9", "feature10", 
    "feature11", "feature12", "feature13", "feature14", "feature15", "feature16", "feature17", "feature18", "feature19", "feature20", 
    "feature21", "feature22", "feature23", "feature24", "feature25", "feature26", "feature27", "feature28", "feature29", "feature30", 
    "feature31", "feature32", "feature33", "feature34", "feature35", "feature36", "feature37", "feature38", "feature39", "feature40", 
    "feature41"
]
categorical_features = [
    "feature2", "feature3", "feature4"
]

# Generate random numeric and categorical feature (feature2, feature3, feature4) the rest are all numeric
num_numeric_features = len(numeric_features)
num_categorical_features = len(categorical_features)
feature2 = "tcp"
feature3 = "http"
feature4 = "FIN"


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
        for i in range(num_categorical_features):
            if i == 0:
                row_data.append(feature2)
            elif i == 1:
                row_data.append(feature3)
            elif i == 2:
                row_data.append(feature4) 

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
        endpoint_url1 = "/phase-2/prob-1/predict"
        endpoint_url2 = "/phase-2/prob-2/predict"
        # random endpoint
        test_endpoint = random.choice([endpoint_url1, endpoint_url2])

        # Generate random data for each request
        data = generate_random_data()

        # Send the batch request
        with self.client.post(test_endpoint, json=data, catch_response=True) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Request failed with status code: {response.status_code}")
