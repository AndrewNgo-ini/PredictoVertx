from sklearn.preprocessing import QuantileTransformer
from sklearn.preprocessing import StandardScaler
import pandas as pd
import json
from sklearn.model_selection import train_test_split
from lightgbm import LGBMClassifier
from sklearn.metrics import roc_auc_score, accuracy_score
import pickle
import joblib
import random
import bentoml
import mlflow
import functools
random.seed(42)



def build_category_features(data, categorical_cols=None):
    if categorical_cols is None:
        categorical_cols = []
    category_index = {}
    if len(categorical_cols) == 0:
        return data, category_index

    df = data.copy()
    # process category features
    for col in categorical_cols:
        df[col] = df[col].astype("category")
        category_index[col] = df[col].cat.categories
        df[col] = df[col].cat.codes
    return df, category_index

def apply_category_features(
        raw_df, categorical_cols=None, category_index: dict = None
    ):
    if categorical_cols is None:
        categorical_cols = []
    if len(categorical_cols) == 0:
        return raw_df

    for col in categorical_cols:
        raw_df[col] = raw_df[col].astype("category")
        raw_df[col] = pd.Categorical(
            raw_df[col],
            categories=category_index[col],
        ).codes
    return raw_df

def fit_preprocessor(path, X, numeric_encoder, standard_scaler, numeric_columns, category_columns):
    X, category_index = build_category_features(X, category_columns)
    numeric_encoder.fit(X[numeric_columns])
    standard_scaler.fit(X)
    save_preprocessor(path, category_index, numeric_encoder, standard_scaler)
    return category_index, numeric_encoder, standard_scaler

def preprocess(X, numeric_encoder, standard_scaler, numeric_columns, category_columns, category_index):
    X = apply_category_features(X, category_columns, category_index)
    #X[numeric_columns] = numeric_encoder.transform(X[numeric_columns])
    #X = standard_scaler.transform(X)
    return X

async def a_preprocess(X, numeric_encoder, standard_scaler, numeric_columns, category_columns, category_index):
    X = apply_category_features(X, category_columns, category_index)
    X[numeric_columns] = await numeric_encoder.transform(X[numeric_columns])
    X = await standard_scaler.transform(X)
    return X

def save_preprocessor(path, category_index, numeric_encoder, standard_scaler):
    with open(path + "category_index.pkl", "wb") as f:
        pickle.dump(category_index, f)
    # turn category_index to dict and save a json
    category_index_json = {k: {v: i for i, v in enumerate(category_index[k])} for k in category_index}
    with open(path + "category_index.json", "w") as f:
        json.dump(category_index_json, f)
    joblib.dump(numeric_encoder, path + 'numeric_encoder.pkl')
    joblib.dump(standard_scaler, path + 'standard_scaler.pkl')

def load_preprocessor(path):
    with open(path + "category_index.pkl", "rb") as f:
        category_index = pickle.load(f)
    numeric_encoder = joblib.load(path + 'numeric_encoder.pkl')
    standard_scaler = joblib.load(path + 'standard_scaler.pkl')
    return category_index, numeric_encoder, standard_scaler

def train(train_path, config_path):
    model_config = json.load(open(config_path + "features_config.json", "rb"))
    numeric_columns = model_config["numeric_columns"]
    category_columns = model_config["category_columns"]
    numeric_encoder = QuantileTransformer(output_distribution='normal')
    standard_scaler = StandardScaler()

    df = pd.read_parquet(train_path)
    train, dev = train_test_split(df, train_size=0.8, random_state=42)
    X = train.drop(["label"], axis=1)
    Y = train[["label"]]
    X_test = dev.drop(["label"], axis=1)
    Y_test = dev[["label"]]

    category_index, numeric_encoder, standard_scaler = fit_preprocessor(config_path, X, numeric_encoder, standard_scaler, numeric_columns, category_columns)

    X = preprocess(X, numeric_encoder, standard_scaler, numeric_columns, category_columns, category_index)
    X_test = preprocess(X_test, numeric_encoder, standard_scaler, numeric_columns, category_columns, category_index)
    print(type(X))
    print(X)
    mlflow.lightgbm.autolog()
    with mlflow.start_run():
        if model_config["ml_type"] == "classification":
            model = LGBMClassifier(is_unbalance = True)
            model.fit(X, Y)
            score = roc_auc_score(Y_test, model.predict_proba(X_test)[:,1])
            print("test", model.predict(X_test))
            print("test2", model.predict_proba(X_test))
            model_uri = mlflow.get_artifact_uri("model")
            bento_model = bentoml.mlflow.import_model(
                "model1", model_uri
            )
            print("Model imported to BentoML: %s" % bento_model)
        elif model_config["ml_type"] == "multiclass":
            model = LGBMClassifier(objective='multiclass', 
                                    n_estimators=1000, 
                                    max_depth=4, 
                                    learning_rate=0.1,
                                    reg_lambda=1, 
                                    random_state=101)
            model.fit(X, Y)
            score = accuracy_score(Y_test, model.predict(X_test))
            model_uri = mlflow.get_artifact_uri("model")
            bento_model = bentoml.mlflow.import_model(
                "model2", model_uri
            )
            print("Model classes", model.classes_)
            print("Model imported to BentoML: %s" % bento_model)
    print(score)



if __name__ == "__main__":
    train_path = "../data/raw_data/phase-2/prob-1/raw_train.parquet"
    config_path = "../src/model_config/phase-2/prob-1/"
    train(train_path, config_path)

    train_path = "../data/raw_data/phase-2/prob-2/raw_train.parquet"
    config_path = "../src/model_config/phase-2/prob-2/"
    train(train_path, config_path)

    # df = pd.read_parquet(train_path)


    # # # Transform
    # X = apply_category_features(X, category_columns, category_index)
    # X[numeric_columns] = numeric_encoder.transform(X[numeric_columns])
    # X = standard_scaler.transform(X)
    # print(type(X))
    # print(X)
