import bentoml
import pandas as pd
import json

from bentoml.io import NumpyNdarray, JSON
from model_lightgbm import load_preprocessor, preprocess

def load_meterials(path):
    config_path = path
    category_index, numeric_encoder, standard_scaler = load_preprocessor(config_path)
    model_config = json.load(open(config_path + "features_config.json", "rb"))
    numeric_columns = model_config["numeric_columns"]
    category_columns = model_config["category_columns"]
    return category_index, numeric_encoder, standard_scaler, numeric_columns, category_columns


category_index, numeric_encoder, standard_scaler, numeric_columns, category_columns = load_meterials("model_config/phase-2/prob-1/")
category_index2, numeric_encoder2, standard_scaler2, numeric_columns2, category_columns2 = load_meterials("model_config/phase-2/prob-2/")

runner = bentoml.mlflow.get("model1:latest").to_runner()
runner2 = bentoml.mlflow.get("model2:latest").to_runner()

svc = bentoml.Service("model", runners=[runner, runner2])


@svc.api(input=JSON(), 
        output=NumpyNdarray(),
        route="/phase-2/prob-1/predict")
def inference(data: dict):
    try:
        raw_df = pd.DataFrame(data["rows"], columns=data["columns"])
        processed_data = preprocess(raw_df, numeric_encoder, standard_scaler, numeric_columns, category_columns, category_index)
        result = runner.run(processed_data)
        print(result)
        return result
    except Exception as e:
        print(e)


@svc.api(input=JSON(), 
        output=NumpyNdarray(),
        route="/phase-2/prob-2/predict")
def inference2(data: dict):
    try:
        raw_df = pd.DataFrame(data["rows"], columns=data["columns"])
        processed_data = preprocess(raw_df, numeric_encoder2, standard_scaler2, numeric_columns2, category_columns2, category_index2)
        result = runner2.run(processed_data)
        return result
    except Exception as e:
        print(e)