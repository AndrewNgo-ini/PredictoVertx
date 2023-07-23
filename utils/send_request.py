#read data from parquet file and translate it to json and send to localhost 5040
import requests
import sys
import pandas as pd
import json

def send_request(path: str):
    df = pd.read_parquet(path=path)
    #print(df.columns.values.tolist())
    json_data = {
        "id": "1",
        "rows": df.values.tolist(),
        "columns": df.columns.values.tolist(),
    }
    data = requests.post('http://34.143.204.143:5040/phase-2/prob-1/predict', json=json_data)
    print(data.text)

def send_request_json(path):
    f = open(path, "r")
    obj = json.loads(f.read())
    json_data = obj["map"]
    data = requests.post('http://34.143.204.143:5040/phase-2/prob-1/predict', json=json_data)
    print(data.text)


if __name__ == "__main__":
    if len(sys.argv) >= 2:
        #send_request(sys.argv[1])
        send_request_json(sys.argv[1])
    else:
        print("missing path")
        exit(1)