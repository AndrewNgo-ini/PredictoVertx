import bentoml
import numpy as np

from bentoml.io import NumpyNdarray

runner = bentoml.catboost.get("catboost_cancer_clf:latest").to_runner()
svc = bentoml.Service("cancer_clf", runners=[runner])


@svc.api(input=NumpyNdarray(), 
        output=NumpyNdarray(),
        route="/phase-2/prob-1/predict")
async def inference(data: np.ndarray) -> dict:
    try:
        result = await runner.predict.async_run(data)
        return result
    except Exception as e:
        print(e)


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

    
