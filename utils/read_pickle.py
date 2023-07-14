import pickle

with open("src/model_config/phase-2/prob-1/category_index.pickle", "rb") as f:
    category_index = pickle.load(f)
    print(category_index)