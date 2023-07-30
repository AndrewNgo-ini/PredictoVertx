# Base image
#FROM maven:3.8.3-openjdk-11-slim AS build

# # Set the working directory in the container
# WORKDIR /app

# # Copy the pom.xml and install dependencies
# COPY ./backend/pom.xml .

# # Copy the source code
# COPY ./backend ./src/

# # Build the application
# RUN mvn -f pom.xml clean package

# Runtime image
FROM openjdk:11-jre-slim

# Set the working directory in the container
WORKDIR /app

# Copy the built JAR file from the build stage
#COPY --from=build /app/target/gateway-1.0.0-SNAPSHOT-fat.jar ./gateway-1.0.0-SNAPSHOT-fat.jar
COPY ./backend/target/gateway-1.0.0-SNAPSHOT-fat.jar ./gateway-1.0.0-SNAPSHOT-fat.jar

COPY ./backend/conf_prod.json ./conf_prod.json
COPY ./backend/category_index.json ./category_index.json

# Expose the port if necessary
EXPOSE 5040

# Set the entrypoint command to run the application
ENTRYPOINT ["java", "-jar", "gateway-1.0.0-SNAPSHOT-fat.jar", "--conf", "conf_prod.json"]
