import os
import bentoml
import mlflow
import yaml
import pandas as pd
import pickle
import json

from bentoml.io import JSON, NumpyNdarray
from raw_data_processor import RawDataProcessor
from utils import AppConfig
from problem_config import ProblemConst

PREDICTOR_API_PORT = 8000

def load_nessecary_config(config_file_path):
    with open(config_file_path + "model-1.yaml", "r") as f:
        config = yaml.safe_load(f)
    with open(config_file_path + "category_index.pickle", "rb") as f:
        category_index = pickle.load(f)
    with open(config_file_path + "features_config.json") as f:
        categorical_cols = json.load(f).get("category_columns")
    
    return config, category_index, categorical_cols


def save_model() -> bentoml.Model:
    mlflow.set_tracking_uri(AppConfig.MLFLOW_TRACKING_URI)

    # load model
    model_uri = os.path.join(
        "models:/", config["model_name"], str(config["model_version"])
    )
    #model = mlflow.sklearn.load_model(model_uri)
    
    # save model using bentoml
    bentoml_model = bentoml.mlflow.import_model(
        config["model_name"],
        model_uri,
        # model signatures for runner inference
        signatures={
            "predict": {
                "batchable": False,
            },
        },
    )
    return bentoml_model

path = f"model_config/{ProblemConst.PHASE2}/{ProblemConst.PROB1}/"
config, category_index, categorical_cols = load_nessecary_config(path)
bentoml_model = save_model()
bentoml_runner = bentoml.mlflow.get(bentoml_model.tag).to_runner()
svc = bentoml.Service(bentoml_model.tag.name, runners=[bentoml_runner])



@svc.api(input=JSON(), 
        output=NumpyNdarray(),
        route="/phase-2/prob-1/predict")
def inference(data: dict) -> dict:
    try:
        raw_df = pd.DataFrame(data["rows"], columns=data["columns"])
        feature_df = RawDataProcessor.apply_category_features(
            raw_df=raw_df,
            categorical_cols=categorical_cols,
            category_index=category_index,
        )
        result = bentoml_runner.predict.run(feature_df)
        print(result)
        return result
    except Exception as e:
        print(e)


    
