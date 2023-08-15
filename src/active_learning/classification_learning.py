import matplotlib.pyplot as plt
import numpy as np
from modAL.models import ActiveLearner
from sklearn.datasets import load_iris
from sklearn.decomposition import PCA
from sklearn.neighbors import KNeighborsClassifier

def main():
    # load the iris dataset
    iris = load_iris()

    # initialize the active learner with a small amount of labeled data
    learner = ActiveLearner(
        estimator=KNeighborsClassifier(n_neighbors=3),
        X_training=iris['data'][:3],
        y_training=iris['target'][:3]
    )

    # create a pool of unlabeled data
    X_pool = iris['data'][3:]
    y_pool = iris['target'][3:]

    # iterate over the pool of unlabeled data
    for idx in range(len(X_pool)):
        # query the most informative instance from the pool
        query_idx, query_instance = learner.query(X_pool)

        # assign a label to the queried instance using a labeler
        # (in this case, we will just use the majority vote of the active learner)
        y_query = learner.predict(query_instance).reshape(1, )

        # add the queried instance to the training set
        learner.teach(X=query_instance.reshape(1, -1), y=y_query)

        # remove the queried instance from the pool
        X_pool = np.delete(X_pool, query_idx, axis=0)
        y_pool = np.delete(y_pool, query_idx)

        # plot the learner's predictions
        with plt.style.context('seaborn-white'):
            plt.figure(figsize=(7, 7))
            prediction = learner.predict(iris['data'])
            plt.scatter(x=iris['data'][:, 0], y=iris['data'][:, 1], c=prediction, cmap='viridis', s=50)
            plt.title('Classification accuracy: %f' % learner.score(iris['data'], iris['target']))
            plt.show()

if __name__ == '__main__':
    main()
