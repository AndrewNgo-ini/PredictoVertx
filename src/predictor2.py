import bentoml
import numpy as np
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


category_index, numeric_encoder, standard_scaler, numeric_columns, category_columns = load_meterials("model_config/phase-2/prob-2/")
columns_order = [
    "feature1", "feature2", "feature3", "feature4", "feature5", "feature6", "feature7", "feature8", "feature9", "feature10", 
    "feature11", "feature12", "feature13", "feature14", "feature15", "feature16", "feature17", "feature18", "feature19", "feature20", 
    "feature21", "feature22", "feature23", "feature24", "feature25", "feature26", "feature27", "feature28", "feature29", "feature30", 
    "feature31", "feature32", "feature33", "feature34", "feature35", "feature36", "feature37", "feature38", "feature39", "feature40", 
    "feature41"
  ]


runner = bentoml.mlflow.get("model2:latest").to_runner()

svc = bentoml.Service("service2", runners=[runner])


@svc.api(input=JSON(), 
        output=NumpyNdarray(),
        route="/phase-2/prob-2/predict")
def inference2(data: dict):
    try:
        raw_df = pd.DataFrame(data["rows"], columns=data["columns"])
        order_df = raw_df[columns_order]
        processed_data = preprocess(order_df, numeric_encoder, standard_scaler, numeric_columns, category_columns, category_index)
        result = runner.run(processed_data)
        return result
    except Exception as e:
        print(e)



# @svc.api(input=NumpyNdarray(), 
#         output=NumpyNdarray(),
#         route="/phase-2/prob-1/predict")
# async def inference(data: np.ndarray) -> dict:
#     try:
#         result = await runner.predict.async_run(data)
#         return result
#     except Exception as e:
#         print(e)


# @svc.api(input=NumpyNdarray(), 
#         output=NumpyNdarray(),
#         route="/phase-2/prob-2/predict")
# async def inference2(data: np.ndarray) -> dict:
#     try:
#         result = await runner.predict.async_run(data)
#         #print(result)
#         return result
#     except Exception as e:
#         print(e)

    
