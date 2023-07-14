import os
import bentoml
import mlflow
import yaml
import pandas as pd
import pickle
import json
import numpy as np

from bentoml.io import JSON, NumpyNdarray

# def load_nessecary_config(config_file_path):
#     with open(config_file_path + "model-1.yaml", "r") as f:
#         config = yaml.safe_load(f)
#     with open(config_file_path + "category_index.pickle", "rb") as f:
#         category_index = pickle.load(f)
#     with open(config_file_path + "features_config.json") as f:
#         categorical_cols = json.load(f).get("category_columns")
    
#     return config, category_index, categorical_cols

# path = f"model_config/{ProblemConst.PHASE2}/{ProblemConst.PROB1}/"
# config, category_index, categorical_cols = load_nessecary_config(path)
runner = bentoml.catboost.get("catboost_cancer_clf:latest").to_runner()
svc = bentoml.Service("cancer_clf", runners=[runner])


@svc.api(input=NumpyNdarray(), 
        output=NumpyNdarray(),
        route="/phase-2/prob-1/predict")
def inference(data: np.ndarray) -> dict:
    try:
        result = runner.predict.run(data)
        print(result)
        return result
    except Exception as e:
        print(e)

    
