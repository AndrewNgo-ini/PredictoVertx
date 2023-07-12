// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: bentoml/grpc/v1/service.proto

package com.bentoml.grpc.v1;

public interface RequestOrBuilder extends
    // @@protoc_insertion_point(interface_extends:bentoml.grpc.v1.Request)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <pre>
   * api_name defines the API entrypoint to call.
   * api_name is the name of the function defined in bentoml.Service.
   * Example:
   *
   *     &#64;svc.api(input=NumpyNdarray(), output=File())
   *     def predict(input: NDArray[float]) -&gt; bytes:
   *         ...
   *
   *     api_name is "predict" in this case.
   * </pre>
   *
   * <code>string api_name = 1;</code>
   * @return The apiName.
   */
  java.lang.String getApiName();
  /**
   * <pre>
   * api_name defines the API entrypoint to call.
   * api_name is the name of the function defined in bentoml.Service.
   * Example:
   *
   *     &#64;svc.api(input=NumpyNdarray(), output=File())
   *     def predict(input: NDArray[float]) -&gt; bytes:
   *         ...
   *
   *     api_name is "predict" in this case.
   * </pre>
   *
   * <code>string api_name = 1;</code>
   * @return The bytes for apiName.
   */
  com.google.protobuf.ByteString
      getApiNameBytes();

  /**
   * <pre>
   * NDArray represents a n-dimensional array of arbitrary type.
   * </pre>
   *
   * <code>.bentoml.grpc.v1.NDArray ndarray = 3;</code>
   * @return Whether the ndarray field is set.
   */
  boolean hasNdarray();
  /**
   * <pre>
   * NDArray represents a n-dimensional array of arbitrary type.
   * </pre>
   *
   * <code>.bentoml.grpc.v1.NDArray ndarray = 3;</code>
   * @return The ndarray.
   */
  com.bentoml.grpc.v1.NDArray getNdarray();
  /**
   * <pre>
   * NDArray represents a n-dimensional array of arbitrary type.
   * </pre>
   *
   * <code>.bentoml.grpc.v1.NDArray ndarray = 3;</code>
   */
  com.bentoml.grpc.v1.NDArrayOrBuilder getNdarrayOrBuilder();

  /**
   * <pre>
   * DataFrame represents any tabular data type. We are using
   * DataFrame as a trivial representation for tabular type.
   * </pre>
   *
   * <code>.bentoml.grpc.v1.DataFrame dataframe = 5;</code>
   * @return Whether the dataframe field is set.
   */
  boolean hasDataframe();
  /**
   * <pre>
   * DataFrame represents any tabular data type. We are using
   * DataFrame as a trivial representation for tabular type.
   * </pre>
   *
   * <code>.bentoml.grpc.v1.DataFrame dataframe = 5;</code>
   * @return The dataframe.
   */
  com.bentoml.grpc.v1.DataFrame getDataframe();
  /**
   * <pre>
   * DataFrame represents any tabular data type. We are using
   * DataFrame as a trivial representation for tabular type.
   * </pre>
   *
   * <code>.bentoml.grpc.v1.DataFrame dataframe = 5;</code>
   */
  com.bentoml.grpc.v1.DataFrameOrBuilder getDataframeOrBuilder();

  /**
   * <pre>
   * Series portrays a series of values. This can be used for
   * representing Series types in tabular data.
   * </pre>
   *
   * <code>.bentoml.grpc.v1.Series series = 6;</code>
   * @return Whether the series field is set.
   */
  boolean hasSeries();
  /**
   * <pre>
   * Series portrays a series of values. This can be used for
   * representing Series types in tabular data.
   * </pre>
   *
   * <code>.bentoml.grpc.v1.Series series = 6;</code>
   * @return The series.
   */
  com.bentoml.grpc.v1.Series getSeries();
  /**
   * <pre>
   * Series portrays a series of values. This can be used for
   * representing Series types in tabular data.
   * </pre>
   *
   * <code>.bentoml.grpc.v1.Series series = 6;</code>
   */
  com.bentoml.grpc.v1.SeriesOrBuilder getSeriesOrBuilder();

  /**
   * <pre>
   * File represents for any arbitrary file type. This can be
   * plaintext, image, video, audio, etc.
   * </pre>
   *
   * <code>.bentoml.grpc.v1.File file = 7;</code>
   * @return Whether the file field is set.
   */
  boolean hasFile();
  /**
   * <pre>
   * File represents for any arbitrary file type. This can be
   * plaintext, image, video, audio, etc.
   * </pre>
   *
   * <code>.bentoml.grpc.v1.File file = 7;</code>
   * @return The file.
   */
  com.bentoml.grpc.v1.File getFile();
  /**
   * <pre>
   * File represents for any arbitrary file type. This can be
   * plaintext, image, video, audio, etc.
   * </pre>
   *
   * <code>.bentoml.grpc.v1.File file = 7;</code>
   */
  com.bentoml.grpc.v1.FileOrBuilder getFileOrBuilder();

  /**
   * <pre>
   * Text represents a string inputs.
   * </pre>
   *
   * <code>.google.protobuf.StringValue text = 8;</code>
   * @return Whether the text field is set.
   */
  boolean hasText();
  /**
   * <pre>
   * Text represents a string inputs.
   * </pre>
   *
   * <code>.google.protobuf.StringValue text = 8;</code>
   * @return The text.
   */
  com.google.protobuf.StringValue getText();
  /**
   * <pre>
   * Text represents a string inputs.
   * </pre>
   *
   * <code>.google.protobuf.StringValue text = 8;</code>
   */
  com.google.protobuf.StringValueOrBuilder getTextOrBuilder();

  /**
   * <pre>
   * JSON is represented by using google.protobuf.Value.
   * see https://github.com/protocolbuffers/protobuf/blob/main/src/google/protobuf/struct.proto
   * </pre>
   *
   * <code>.google.protobuf.Value json = 9;</code>
   * @return Whether the json field is set.
   */
  boolean hasJson();
  /**
   * <pre>
   * JSON is represented by using google.protobuf.Value.
   * see https://github.com/protocolbuffers/protobuf/blob/main/src/google/protobuf/struct.proto
   * </pre>
   *
   * <code>.google.protobuf.Value json = 9;</code>
   * @return The json.
   */
  com.google.protobuf.Value getJson();
  /**
   * <pre>
   * JSON is represented by using google.protobuf.Value.
   * see https://github.com/protocolbuffers/protobuf/blob/main/src/google/protobuf/struct.proto
   * </pre>
   *
   * <code>.google.protobuf.Value json = 9;</code>
   */
  com.google.protobuf.ValueOrBuilder getJsonOrBuilder();

  /**
   * <pre>
   * Multipart represents a multipart message.
   * It comprises of a mapping from given type name to a subset of aforementioned types.
   * </pre>
   *
   * <code>.bentoml.grpc.v1.Multipart multipart = 10;</code>
   * @return Whether the multipart field is set.
   */
  boolean hasMultipart();
  /**
   * <pre>
   * Multipart represents a multipart message.
   * It comprises of a mapping from given type name to a subset of aforementioned types.
   * </pre>
   *
   * <code>.bentoml.grpc.v1.Multipart multipart = 10;</code>
   * @return The multipart.
   */
  com.bentoml.grpc.v1.Multipart getMultipart();
  /**
   * <pre>
   * Multipart represents a multipart message.
   * It comprises of a mapping from given type name to a subset of aforementioned types.
   * </pre>
   *
   * <code>.bentoml.grpc.v1.Multipart multipart = 10;</code>
   */
  com.bentoml.grpc.v1.MultipartOrBuilder getMultipartOrBuilder();

  /**
   * <pre>
   * serialized_bytes is for data serialized in BentoML's internal serialization format.
   * </pre>
   *
   * <code>bytes serialized_bytes = 2;</code>
   * @return Whether the serializedBytes field is set.
   */
  boolean hasSerializedBytes();
  /**
   * <pre>
   * serialized_bytes is for data serialized in BentoML's internal serialization format.
   * </pre>
   *
   * <code>bytes serialized_bytes = 2;</code>
   * @return The serializedBytes.
   */
  com.google.protobuf.ByteString getSerializedBytes();

  com.bentoml.grpc.v1.Request.ContentCase getContentCase();
}
