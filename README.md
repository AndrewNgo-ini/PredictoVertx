# PredictoVertx

This document provides an overview of the technology stack employed in the project, including backend components, models, and services. The project leverages a variety of tools and frameworks to efficiently handle requests, process data, and deliver results.

## Backend Gateway

The Backend Gateway serves as the entry point for incoming requests, responsible for handling and directing them to appropriate services for further processing. It utilizes the following technologies:

* Vert.x: A versatile, event-driven, and non-blocking toolkit that aids in building reactive applications.
* Flight RPC: A framework that enables efficient data exchange between services using gRPC, a high-performance remote procedure call protocol.
* BentoML Client gRPC: A library that allows the Backend Gateway to interact with BentoML services and make predictions using deployed machine learning models.

## Model

The core of the project's functionality lies in the model, which makes predictions based on the provided data. The model stack comprises the following components:

* CatBoost: A powerful gradient boosting library that excels in handling categorical features and achieving high predictive accuracy.
* Bento Service: A concept offered by BentoML for packaging and deploying machine learning models in a containerized environment.
* Flight RPC Service: A service that facilitates communication and interaction with the CatBoost model using the Flight RPC framework.

## Overall Workflow

1. Incoming requests are received by the Backend Gateway.
2. The Backend Gateway uses Vert.x to manage the asynchronous nature of these requests.
3. The Backend Gateway communicates with the CatBoost model using Flight RPC and the BentoML Client gRPC library.
4. The CatBoost model, deployed as a Bento Service, processes the incoming data and generates predictions.
5. The predictions are sent back to the Backend Gateway via the Flight RPC service.
6. The Backend Gateway returns the predictions to the requester.

## Conclusion

The project's technology stack combines the power of reactive programming, efficient communication via Flight RPC, the predictive capabilities of CatBoost, and the deployment ease of BentoML. This stack enables the creation of a robust and responsive system capable of handling requests, processing data, and delivering accurate predictions.