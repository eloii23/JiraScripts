package util

import com.atlassian.jira.util.json.JSONObject

public class HttpRequest {

    public static final GET = "GET"
    public static final POST = "POST"
    public static final PUT = "PUT"
    public static final DELETE = "DELETE"

    public static final FORM_DATA = "multipart/form-data"
    public static final FORM_URL_ENCODED = "application/x-www-form-urlencoded"
    public static final APP_JSON = "application/json;charset=utf-8"

    /**
     *
     * @param url
     * @param postContent
     * @param user_token
     * @param content_type
     * @param oLog
     * @return
     */
    static HashMap post(String url, String postContent, String user_token, String content_type, Log oLog) {
        HashMap response = this.call(url, user_token, POST, oLog, postContent, content_type)

        return response
    }

    /**
     *
     * @param url
     * @param user_token
     * @param oLog
     * @return
     */
    static HashMap post(String url, String user_token, Log oLog){
        HashMap response = this.call(url, user_token, POST, oLog)
        return response
    }

    /**
     *
     * @param url
     * @param user_token
     * @param oLog
     * @return
     */
    static HashMap get(String url, String user_token, Log oLog) {
        HashMap response = this.call(url, user_token, GET, oLog)

        return response
    }

    /**
     *
     * @param url
     * @param postContent
     * @param user_token
     * @param content_type
     * @param oLog
     * @return
     */
    static HashMap put(String url, String postContent, String user_token, String content_type, Log oLog) {
        HashMap response = this.call(url, user_token, PUT, oLog, postContent, content_type)
        return response
    }

    /**
     *
     * @param url
     * @param user_token
     * @param method
     * @param oLog
     * @param postContent
     * @param content_type
     * @return
     */
    private static HashMap call(String url, String user_token, String method, Log oLog, String postContent = null, String content_type = null){
        oLog.escribir("Se ejecuta HttpRequest." + method)

        URL urlCall = new URL(url.replace(" ", "%20"))
        HttpURLConnection conCall = (HttpURLConnection) urlCall.openConnection()
        conCall.setRequestMethod(method)
        conCall.setDoInput(true)
        conCall.setRequestProperty("Authorization", "Basic " + user_token)
        conCall.setRequestProperty("charset", "utf-8")

        if (content_type != null) {
            conCall.setRequestProperty("Content-Type", content_type)
        }
        if(postContent != null){
            conCall.setDoOutput(true)
            DataOutputStream wr = new DataOutputStream(conCall.getOutputStream())
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(wr, "UTF-8"))
            writer.write(postContent)
            writer.close()
            wr.writeBytes(postContent)
            wr.flush()
        }

        HashMap response = new HashMap()

        try {
            Integer statusCode = conCall.getResponseCode()

            response.put("statusCode", statusCode)
            response.put("message", conCall.getResponseMessage())

            if (statusCode == 200 || statusCode == 201 || statusCode == 202) {

                response.put("content", conCall.getInputStream().getText().toString())
                response.put("headersGetted", conCall.getHeaderFields())
                response.put("responseCookies", conCall.getHeaderField("Set-Cookie"))

            }
        } catch (IOException e) {
            try {
                ((HttpURLConnection)conCall).errorStream.text
            } catch (Exception ignored) {
                throw e
            }
        }

        oLog.escribir("Finaliza " + method)

        return response
    }
}

