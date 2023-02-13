package cl.fonasa.claveunica.controllers;

import io.vertx.core.json.JsonObject;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping({"/resources"})
@CrossOrigin(
        origins = {"*"},
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.DELETE, RequestMethod.PATCH})
public class ClaveunicaController {
    private String urlClaveUnica="https://accounts.claveunica.gob.cl/";

    // Estas variables contienen los datos de autenticacion del aplicativo con clave unica
    //produccion
/*
    private String clientId="sssssssss";   // Cliente Id entregado por claveunica
    private String clientSecret="aaaaaaaaaa"; // Client Secret entregado por claveunica
*/

    //desarrollo sanbox
    private String clientId="d73c00fb6b6f4739a12c4181e2d046a5";   // Cliente Id entregado por claveunica
    private String clientSecret="c8d8b3a66685401c99de9bf580c09885"; // Client Secret entregado por claveunica

    //produccion
    //private String redirectUri="https://beneficiarios.fonasa.cl/Fonasa-beneficiarios";  // URL a la cual se debe redirigir cuando la autenticacion es exitosa

    //desarrollo sanbox
    private String redirectUri="http://127.0.0.1:8000/Fonasa-beneficiarios";  // URL a la cual se debe redirigir cuando la autenticacion es exitosa
    private String strscope ="openid%20run%20name";


    @GET
    @GetMapping(value = "/authorize", produces = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @ResponseBody
    public String getAuth() {
        String csrfToken= String.valueOf(UUID.randomUUID());
        String uri ="";
        uri = urlClaveUnica + "openid/authorize" +
                "?client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&response_type=code" +
                "&scope=" + strscope +
                "&state=" + csrfToken;
        return uri;
    }

    @POST
    @PostMapping(value = "/token"
            , produces = MediaType.APPLICATION_FORM_URLENCODED_VALUE
            , consumes = "application/x-www-form-urlencoded;charset=UTF-8"
    )
    @ResponseBody
    public String token(@RequestParam Map<String, String> data) throws IOException  {
        // data array (0.0)-->(content, value)-->representa los valores en headers
        try {
            String accessToken = "";
            String code = data.get("code");
            String state = data.get("state");
            String localUri = data.get("redirect_uri");

            String query = "client_id=" + clientId;
            query += "&";
            query += "client_secret=" + clientSecret;
            query += "&";
            query += "redirect_uri=" + localUri;
            query += "&";
            query += "grant_type=authorization_code";
            query += "&";
            query += "code=" + code;
            query += "&";
            query += "state=" + state;

            String urlstr=urlClaveUnica + "openid/token/";
            URL myUrl = new URL(null, urlstr, new sun.net.www.protocol.https.Handler());
            HttpsURLConnection con = (HttpsURLConnection) myUrl.openConnection();

            con.setRequestMethod("POST");

            con.setRequestProperty("Content-length", String.valueOf(query.length()));
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            con.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0;Windows98;DigExt)");
            con.setDoOutput(true);
            con.setDoInput(true);

            DataOutputStream output = new DataOutputStream(con.getOutputStream());
            output.write(query.getBytes(StandardCharsets.UTF_8));
            output.flush();
            output.close();

            InputStream is;
            if (con.getResponseCode() >= 400) {
                is = con.getErrorStream();
            } else {
                is = con.getInputStream();
            }

            BufferedReader input = new BufferedReader(new InputStreamReader(is));
            String inputLine;
            StringBuilder response = new StringBuilder();
            for (inputLine = input.readLine(); inputLine != null; inputLine = input.readLine()) {
                response.append(inputLine);
            }
            input.close();

            JsonObject json = new JsonObject(response.toString());
            accessToken = json.getString("access_token");
            return accessToken;
        }
        catch (Exception  e){
            return(e.toString());
        }
    }

    @POST
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @PostMapping(value="/info")
    public ResponseEntity info(@RequestHeader("Authorization") String code) {

        try {
            ResponseEntity<String> response = null;

            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
            headers.add("Authorization", code);

            HttpEntity<String> request = new HttpEntity<>(headers);

            String accessTokenUrl = urlClaveUnica + "openid/userinfo/";
            accessTokenUrl += "?code=" + code;
            accessTokenUrl += "&grant_type=authorization_code";
            accessTokenUrl += "&redirect_uri=" + redirectUri;

            SSLContext context = SSLContext.getInstance("TLSv1.2");
            context.init(null, null, null);

            CloseableHttpClient httpClient = HttpClientBuilder.create().setSslcontext(context).build();
            HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
            RestTemplate restTemplate = new RestTemplate(factory);

            response = restTemplate.exchange(accessTokenUrl, HttpMethod.POST, request, String.class);

            return response;
        }
        catch (Exception e){
             return new ResponseEntity<String>(e.toString(), HttpStatus.NOT_IMPLEMENTED);

        }
    }
}
