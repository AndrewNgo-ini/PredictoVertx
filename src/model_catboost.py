import logging
logging.basicConfig(level = logging.INFO)
import catboost
import pandas as pd
from sklearn.metrics import roc_auc_score
from sklearn.model_selection import train_test_split
from mlflow.models.signature import infer_signature
import bentoml
#from sklearn import preprocessing
#le = preprocessing.LabelEncoder()

def get_dataset(path):
    df = pd.read_parquet(path + "raw_train.parquet")
    train, dev = train_test_split(df, test_size=0.2, random_state=42)
    train_x = train.drop(["label"], axis=1)
    train_y = train[["label"]]
    test_x = dev.drop(["label"], axis=1)
    test_y = dev[["label"]]
    return train_x, train_y, test_x, test_y

def train(path):
    # load train data
    train_x, train_y, test_x, test_y = get_dataset(path)
    #cat_features = ["feature_2", "feature_3", "feature_4"]
    train_x = train_x.to_numpy()
    train_y = train_y.to_numpy()
    logging.info(f"loaded {len(train_x)} samples")

    model = catboost.CatBoostClassifier(iterations=5,learning_rate=0.1,cat_features=[1,2,3])
    model.fit(train_x, train_y)

    # evaluate
    predictions = model.predict(test_x)
    auc_score = roc_auc_score(test_y, predictions)
    metrics = {"test_auc": auc_score}
    logging.info(f"metrics: {metrics}")

    #model.save_model(path + "model.cbm")
    signature = infer_signature(test_x, predictions)
    bento_model = bentoml.catboost.save_model("catboost1", model)
    logging.info("finish train_model")

if __name__ == "__main__":
    train("/Users/ngohieu/MLOps/data/raw_data/phase-2/prob-1/")