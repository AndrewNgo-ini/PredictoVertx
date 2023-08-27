PredictoVertx

This document provides an overview of the technology stack employed in the project, including backend components, models, and services. The project leverages a variety of tools and frameworks to efficiently handle requests, process data, and deliver results.

Backend Gateway
The Backend Gateway serves as the entry point for incoming requests, responsible for handling and directing them to appropriate services for further processing. It utilizes the following technologies:

Vert.x: Vert.x is a versatile, event-driven, and non-blocking toolkit that aids in building reactive applications. In this project, Vert.x is used to manage the asynchronous nature of incoming requests and to ensure optimal resource utilization.

Flight RPC: Flight RPC is a framework that enables efficient data exchange between services using gRPC, a high-performance remote procedure call protocol. The Backend Gateway employs Flight RPC to communicate with other services seamlessly.

BentoML Client gRPC: BentoML is a platform used for deploying, managing, and serving machine learning models. The BentoML Client gRPC library allows the Backend Gateway to interact with BentoML services and make predictions using deployed machine learning models.

Model
The core of the project's functionality lies in the model, which makes predictions based on the provided data. The model stack comprises the following components:

CatBoost: CatBoost is a powerful gradient boosting library that excels in handling categorical features and achieving high predictive accuracy. In this project, CatBoost is the chosen machine learning algorithm due to its effectiveness in a variety of scenarios.

Bento Service: BentoML offers the concept of a Bento Service, which is a containerized environment for packaging and deploying machine learning models. The CatBoost model is transformed into a Bento Service, enabling easy deployment, versioning, and scaling.

Flight RPC Service: To facilitate communication and interaction with the CatBoost model, a Flight RPC service is implemented. This service allows the model to receive input data and return predictions efficiently using the Flight RPC framework.

Overall Workflow
Incoming requests are received by the Backend Gateway, which uses Vert.x to manage the asynchronous nature of these requests.

The Backend Gateway communicates with the CatBoost model using Flight RPC and the BentoML Client gRPC library.

The CatBoost model, deployed as a Bento Service, processes the incoming data and generates predictions.

The predictions are sent back to the Backend Gateway via the Flight RPC service.

The Backend Gateway returns the predictions to the requester.

Conclusion
The project's technology stack combines the power of reactive programming, efficient communication via Flight RPC, the predictive capabilities of CatBoost, and the deployment ease of BentoML. This stack enables the creation of a robust and responsive system capable of handling requests, processing data, and delivering accurate predictions.