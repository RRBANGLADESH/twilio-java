package com.twilio.sdk.http;

import com.twilio.sdk.Twilio;
import com.twilio.sdk.exceptions.ApiConnectionException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class NetworkHttpClient extends HttpClient {

    /**
     * Make a request.
     *
     * @param request request to make
     * @return Response of the HTTP request
     */
    public Response makeRequest(final Request request) {
        try {
            URL url = request.constructURL();
            HttpMethod method = request.getMethod();

            // TODO If we support proxying, plumb it through here.
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.addRequestProperty("X-Twilio-Client", "java-" + Twilio.VERSION);
            connection.addRequestProperty("User-Agent", "twilio-java/" + Twilio.VERSION);
            connection.addRequestProperty("Accept", "application/json");
            connection.addRequestProperty("Accept-Encoding", "utf-8");

            connection.setAllowUserInteraction(false);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestMethod(method.toString());

            if (request.requiresAuthentication()) {
                connection.setRequestProperty("Authorization", request.getAuthString());
            }

            if (method == HttpMethod.POST) {
                connection.setDoOutput(true);
                connection.addRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            }

            // TODO set up timeouts, caching, etc
            connection.connect();

            if (method == HttpMethod.POST) {
                sendPostBody(request, connection);
            }

            int responseCode = connection.getResponseCode();
            InputStream errorStream = connection.getErrorStream();

            if (errorStream != null) {
                return new Response(errorStream, responseCode);
            }

            InputStream stream = connection.getInputStream();
            return new Response(stream, responseCode);

        } catch (final IOException e) {
            throw new ApiConnectionException("IOException during API request to Twilio", e);
        }
    }

    private void sendPostBody(final Request request, final HttpURLConnection conn) {
        String postBody = request.encodeFormBody();
        try {
            OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
            writer.write(postBody);
            writer.close();
        } catch (final IOException e) {
            throw new ApiConnectionException("IOException during API request to Twilio", e);
        }
    }

}