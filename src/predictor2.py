import bentoml
import numpy as np

from bentoml.io import NumpyNdarray

runner = bentoml.catboost.get("catboost_cancer_clf:latest").to_runner()
svc = bentoml.Service("cancer_clf", runners=[runner])


@svc.api(input=NumpyNdarray(), 
        output=NumpyNdarray(),
        route="/phase-2/prob-2/predict")
def inference2(data: np.ndarray) -> dict:
    try:
        result = runner.predict.run(data)
        print(result)
        return result
    except Exception as e:
        print(e)

    
