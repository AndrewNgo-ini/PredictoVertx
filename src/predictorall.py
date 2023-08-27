import bentoml
import numpy as np
import pandas as pd
from bentoml.io import NumpyNdarray, JSON
import catboost as cbt

runner = bentoml.catboost.get("catboost1:latest").to_runner()
svc = bentoml.Service("cancer_clf", runners=[runner])


@svc.api(input=JSON(), 
        output=JSON(),
        route="/phase-2/prob-1/predict")
def inference(data: np.ndarray) -> dict:
    try:
        print("Data: ", data)
        df = pd.DataFrame(columns=data['columns'], data=data['rows'])
        p = cbt.Pool(df, cat_features=[0,1,2])
        print("Pool: ", p)
        result = runner.predict.run(p)
        print("Result: ", result)
        return {
            "prediction": result
        }
    except Exception as e:
        print(e)


@svc.api(input=JSON(), 
        output=NumpyNdarray(),
        route="/phase-2/prob-2/predict")
async def inference2(data: np.ndarray) -> dict:
    try:
        result = await runner.predict.async_run(data)
        #print(result)
        return result
    except Exception as e:
        print(e)