package bullyrmi;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import sun.misc.IOUtils;

public class Bully {

    public static String id;
    public static Map<String, String> ips;

    public static void main(String[] args) throws Exception {
        ips = new Hashtable<>();
        //ips.put("1", "192.168.20.105");
        //ips.put("2", "192.168.20.104");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        //enter all ips in network
        System.out.println("Podaj ID oraz adresy innych wezlow w sieci: (END - konczy wpisywanie)");
        String s = br.readLine();
        int a = 0;
        while (!s.equals("END")) {
            
            String key = s;
            String val = br.readLine();
            ips.put(key, val);
            if(a==0){
                id = key; 
                a++;
            }
            s = br.readLine();
        }

        for (Map.Entry<String, String> entry : ips.entrySet()) {
            System.out.println(entry.getKey() + " " + entry.getValue());
        }

        System.out.print("Czy mam zacząć elekcję? (y/n)");
        String el = br.readLine();

        if ("y".equals(el)) {
            electionStart();
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/bully", new MyHandler());
        server.setExecutor(null); // creates a default executor
        server.start();
    }

    public static String excutePost(String targetURL, String urlParameters) {
        HttpURLConnection connection = null;
        try {
            //Create connection
            URL url = new URL(targetURL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            //Send request
            DataOutputStream wr = new DataOutputStream(
                    connection.getOutputStream());
            wr.writeBytes(urlParameters);
            wr.close();

            //Get Response  
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            StringBuilder response = new StringBuilder(); // or StringBuffer if not Java 5+ 
            String line;
            while ((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            rd.close();
            return response.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    static void announceLeader() throws ProtocolException {
        boolean bigger = false;
        for (String sid : ips.keySet()) {
            excutePost("http://" + ips.get(sid) + ":8000/bully", "Jestem jebanym bosem");
        }
    }

    static void electionStart() throws ProtocolException {
        boolean bigger = false;
        for (String sid : ips.keySet()) {
            if (sid.compareTo(id) > 0) {
                String response = excutePost("http://" + ips.get(sid) + ":8000/bully", id);
                if (response != null) {
                    bigger = true;
                    break;
                }
            }
        }
        if (!bigger) {
            System.out.println("Nie ma procesu o większym id - zostaje bosem");
            announceLeader();
        }
    }

    static class MyHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange t) throws IOException {
            InputStream inputStream = t.getRequestBody();
            String senderID = convertStreamToString(inputStream);
            System.out.printf("Wiadomosc od innego procesu:" + senderID);
            String response = "";
            if (senderID.matches("-?\\d+(\\.\\d+)?") && id.compareTo(senderID) > 0) {
                response = "Is bigger";
                electionStart();
            }
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

}
