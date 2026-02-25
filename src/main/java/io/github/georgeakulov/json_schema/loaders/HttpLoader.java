package io.github.georgeakulov.json_schema.loaders;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.georgeakulov.json_schema.common.JsonUtils;
import io.github.georgeakulov.json_schema.common.SchemaCompileException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.stream.Stream;

import static io.github.georgeakulov.json_schema.common.SchemaCompileException.create;

public class HttpLoader implements IResourceLoader {

    @Override
    public Stream<String> getSupportedSchemes() {
        return Stream.of("https", "http");
    }

    @Override
    public JsonNode loadResource(URI byUri) {

        try(HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build()) {

            return load(client, byUri);
        }
        catch(SchemaCompileException sce) {
            throw sce;
        }
        catch(Exception e) {
            throw create(e, "Error on load schema: {0}", byUri);
        }
    }

    private JsonNode load(HttpClient client, URI byUri) throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder()
                .GET()
                .uri(byUri)
                .build();

        var hr = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

        if(hr.statusCode() == HttpURLConnection.HTTP_MOVED_PERM || hr.statusCode() == HttpURLConnection.HTTP_MOVED_TEMP) {
            String location = hr.headers().firstValue("Location")
                    .orElseThrow(() -> create("Response code {0} without Location header", hr.statusCode()));

            return onLocation(client, location);
        }

        if(hr.statusCode() != 200) {
            throw create("<< Status: {0}  from: {1}", hr.statusCode(), byUri);
        }
        try(InputStream is = hr.body()) {
            return JsonUtils.parse(is);
        }
        catch(IOException e) {
            throw create(e, "Error read json from {0}", byUri);
        }
    }

    private JsonNode onLocation(HttpClient client, String location) throws IOException, InterruptedException {
        try {
            return load(client, URI.create(location));
        }
        catch(IllegalArgumentException e) {
            throw create(e, "Wrong Location header uri: {0}", location);
        }
    }
}
