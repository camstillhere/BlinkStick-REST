package com.cameron.stiller.blinkStick;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final ExecutorService httpThreadPool = Executors.newFixedThreadPool(500);
    private static final Controller controller = new Controller();


    public static void main(String[] args) throws IOException {
        System.out.println("Starting up the controller");
        controller.getConnectedDevices().forEach(System.out::println);
        System.out.println("Starting up the http server");

        String httpPrefix = "http";
        int port = 80;
        HttpServer server;
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        HttpContext apiContext = server.createContext("/");
        apiContext.setHandler(Server::apiHandler);
        HttpContext htmlContext = server.createContext("/web");
        htmlContext.setHandler(Server::htmlHandleRequest);
        server.setExecutor(httpThreadPool);
        server.start();
        System.out.println(httpPrefix + "://" + InetAddress.getLocalHost().getHostAddress() + ":" + server.getAddress().getPort() + "/list");
        System.out.println(httpPrefix + "://" + InetAddress.getLocalHost().getHostAddress() + ":" + server.getAddress().getPort() + "/web/index.html");
        System.out.println("All finished");
    }

    //Basic HTML serving web service... nothing special really.
    static void htmlHandleRequest(HttpExchange exchange) throws IOException {
        //System.out.println("htmlHandleRequest");
        OutputStream os;

        URI requestURI = exchange.getRequestURI();
        String resourcePath=requestURI.getPath();
        if(resourcePath.length() < 2)
        {
            resourcePath= "/web/index.html";
        }
        os = exchange.getResponseBody();
        InputStream res = Controller.class.getResourceAsStream(resourcePath);
        try {
            if(res == null)
            {
                resourcePath= "/web/404.html";
                res=Controller.class.getResourceAsStream(resourcePath);
            }
            if(resourcePath.endsWith(".html"))
            {
                exchange.getResponseHeaders().set("Content-Type", "text/html");
            }
            else
            {
                if(resourcePath.endsWith(".svg"))
                {
                    exchange.getResponseHeaders().set("Content-Type", "image/svg+xml");
                }
                else
                {
                    if(resourcePath.endsWith(".css"))
                    {
                        exchange.getResponseHeaders().set("Content-Type", "text/css");
                    }
                    else
                    {
                        exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
                    }
                }
            }
            exchange.sendResponseHeaders(200, res.available());
            if(os != null)
            {
                res.transferTo(os);
            }
            res.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(os!=null)
        {
            os.close();
        }
    }

    private static void apiHandler(HttpExchange exchange) {
        //TODO: This should be configurable, at least to specify the host and what methods they wish to allow
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

        if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            //TODO: Validate the method is in fact supported by the method being called.
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type,Authorization");
            try {
                exchange.sendResponseHeaders(204, -1);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        String actualEndPoint = exchange.getRequestURI().getPath();
        Map<String, List<String>> actualParams = getQueryMap(exchange.getRequestURI().getRawQuery());
        String actualPayLoad = "";
        OutputStream os = exchange.getResponseBody();
        BufferedReader originalPayload = new java.io.BufferedReader(new java.io.InputStreamReader(exchange.getRequestBody()));
        String payloadLine;

        try {
            payloadLine = originalPayload.readLine();

            StringBuilder originalBuffer = new StringBuilder();
            while (payloadLine != null) {
                originalBuffer.append(payloadLine).append("\n");
                payloadLine = originalPayload.readLine();
            }
            actualPayLoad = originalBuffer.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            List<String> pathPoints = new LinkedList<>(Arrays.asList(actualEndPoint.split("/")));
            JSONObject output = new JSONObject();
            if (pathPoints.size() == 0) {
                output.put("Server", "BlinkStick Server");
                JSONArray methods = new JSONArray();
                methods.put("getColors");
                methods.put("list");
                methods.put("setAllRandom");
                methods.put("setBrightness");
                methods.put("setColor");
                methods.put("setColors");
                methods.put("setFrameRate");
                methods.put("setRandom");
                methods.put("test");
                output.put("Methods",methods);
            } else {
                //BlinkStick stuff
                pathPoints.remove(0);
                switch (pathPoints.get(0)) {
                    case "list":
                        //output.put("method","list");
                        controller.getConnectedDevices().forEach(deviceId ->
                        {
                            JSONObject details = new JSONObject();
                            details.put("maxLEDs", controller.determineMaxLeds(deviceId));
                            output.put(deviceId, details);
                        });
                        break;
                    case "getColors":
                        int index = 0;
                        for (HashMap<Character, Integer> point : controller.getColors(actualParams.get("deviceId").get(0))) {

                            output.put(String.valueOf(index),new JSONObject(point));
                            index=index+1;
                        }
                        break;
                    case "setColors":
                        JSONObject missingSetColorParams = new JSONObject();
                        if (!actualParams.containsKey("deviceId")) {
                            missingSetColorParams.put("deviceId", "The unique instance of the device");
                            output.put("Missing Params", missingSetColorParams);
                        }
                        JSONArray points = new JSONArray(actualPayLoad);
                        ArrayList<HashMap<Character, Integer>> ledPayload = new ArrayList<>();
                        for (Object point : points) {
                            JSONObject element = (JSONObject) point;
                            HashMap<Character, Integer> ledBundle = new HashMap<>();
                            ledBundle.put('r', element.getInt("r"));
                            ledBundle.put('g', element.getInt("g"));
                            ledBundle.put('b', element.getInt("b"));
                            ledPayload.add(ledBundle);
                        }
                        controller.setIndexedColors(actualParams.get("deviceId").get(0), ledPayload);
                        break;
                    case "setFrameRate":
                        JSONObject missingFrameRateParams = new JSONObject();
                        if (!actualParams.containsKey("deviceId")) {
                            missingFrameRateParams.put("deviceId", "The unique instance of the device");
                        }
                        if (!actualParams.containsKey("rate")) {
                            missingFrameRateParams.put("rate", "The breathing time between commands. Lower rate is faster, higher rate is slower.");
                        }
                        if(missingFrameRateParams.keySet().size() > 0) {
                            output.put("Missing Params", missingFrameRateParams);
                        }
                        else
                        {
                            controller.setFrameRate( actualParams.get("deviceId").get(0),  Integer.parseInt(actualParams.get("rate").get(0)));
                        }
                        break;
                    case "test":
                        JSONObject missingTestParams = new JSONObject();
                        if (!actualParams.containsKey("deviceId")) {
                            missingTestParams.put("deviceId", "The unique instance of the device");
                            output.put("Missing Params", missingTestParams);
                        } else {
                            String deviceId = actualParams.get("deviceId").get(0);

                            controller.setFrameRate(deviceId, 10);
                            for (double i = 0; i < 1.0; i += 0.01) {
                                controller.setBrightness(deviceId, i);
                                int r;
                                int g;
                                int b = 0;
                                if (i <= 0.5) {
                                    r = 255;
                                    g = (int) (i * 512);
                                } else {
                                    g = 255;
                                    r = (int) ((1.0 - i) * 512);
                                }
                                controller.setColor(deviceId, r, g, b);
                            }
                            controller.setBrightness(deviceId, 0.1);

                            ArrayList<HashMap<Character, Integer>> ledPackage = new ArrayList<>();
                            for (int i = 0; i < controller.determineMaxLeds(deviceId); i += 1) {
                                double percentage = (double) i / controller.determineMaxLeds(deviceId);
                                HashMap<Character, Integer> ledBundle = new HashMap<>();
                                ledBundle.put('r', 0);
                                ledBundle.put('g', 0);
                                ledBundle.put('b', 0);
                                if (percentage <= 0.5) {
                                    ledBundle.put('r', 255);
                                    ledBundle.put('g', (int) (percentage * 510));
                                } else {
                                    ledBundle.put('g', 255);
                                    ledBundle.put('r', (int) ((1.0 - percentage) * 510));
                                }
                                ledPackage.add(ledBundle);
                            }


                            controller.setColor(deviceId, 0, 0, 0);
                            int max = controller.determineMaxLeds(deviceId);
                            HashMap<Character, Integer> off = new HashMap<>();
                            off.put('r', 0);
                            off.put('g', 0);
                            off.put('b', 0);
                            for (int maxLed = max; maxLed >= -1; maxLed -= 1) {
                                ArrayList<HashMap<Character, Integer>> ledBank = new ArrayList<>();
                                for (int i = 0; i < max; i += 1) {
                                    if (i >= maxLed) {
                                        ledBank.add(ledPackage.get(i));
                                    } else {
                                        ledBank.add(off);
                                    }
                                }
                                for (int i = 0; i < maxLed; i += 1) {
                                    ledBank.set(i, ledPackage.get(i));
                                    if (i > 0) {
                                        ledBank.set(i - 1, off);
                                    }
                                    controller.setIndexedColors(deviceId, ledBank);
                                }
                            }
                            controller.setFrameRate(deviceId, 100);
                            controller.setBrightness(deviceId, 1.0);
                            for (int i = 0; i < 100; i += 1) {
                                controller.setRandomColors(deviceId);
                            }
                            controller.setColor(deviceId, 0, 0, 0);
                        }
                        break;
                    case "setBrightness":
                        JSONObject missingBrightnessParams = new JSONObject();
                        if (!actualParams.containsKey("deviceId")) {
                            missingBrightnessParams.put("deviceId", "The unique instance of the device");
                        }
                        if (!actualParams.containsKey("percent")) {
                            missingBrightnessParams.put("percent", "value from 0.00 to 1.00 representing the brightness");
                        }
                        if (missingBrightnessParams.keySet().size() == 0) {
                            controller.setBrightness(
                                    actualParams.get("deviceId").get(0), Double.parseDouble(actualParams.get("percent").get(0)));
                        } else {
                            output.put("Missing Params", missingBrightnessParams);
                        }
                        break;
                    case "setAllRandom":
                        JSONObject randomParms = new JSONObject();
                        if (!actualParams.containsKey("deviceId")) {
                            randomParms.put("deviceId", "The unique instance of the device");
                            output.put("Missing Params", randomParms);
                        }
                        else
                        {
                            controller.setRandomColors(actualParams.get("deviceId").get(0));
                        }
                        break;
                    case "setRandom":
                        JSONObject missingRandomParams = new JSONObject();
                        if (!actualParams.containsKey("deviceId")) {
                            missingRandomParams.put("deviceId", "The unique instance of the device");
                        }
                        if (missingRandomParams.keySet().size() > 0) {
                            if (!actualParams.containsKey("index")) {
                                missingRandomParams.put("index", "OPTIONAL, the specific LED index to change");
                            }
                            output.put("Missing Params", missingRandomParams);
                        } else {
                            if (missingRandomParams.keySet().size() == 0) {
                                if (actualParams.containsKey("index")) {
                                    controller.setRandomColor(
                                            actualParams.get("deviceId").get(0), Integer.parseInt(actualParams.get("index").get(0)));
                                } else {
                                    controller.setRandomColor(
                                            actualParams.get("deviceId").get(0));
                                }
                            } else {
                                output.put("Missing Params", missingRandomParams);
                            }


                        }
                        break;
                    case "setColor":
                        //output.put("method","setColor");
                        JSONObject missingParams = new JSONObject();
                        int r = 0;
                        int g = 0;
                        int b = 0;
                        int i = -1;
                        if (!actualParams.containsKey("deviceId")) {
                            missingParams.put("deviceId", "The unique instance of the device");
                        }
                        if (!actualParams.containsKey("r")) {
                            missingParams.put("r", "red from 0-255");
                        }
                        try {
                            r = Integer.parseInt(actualParams.get("r").get(0));
                        } catch (Exception ex) {
                            missingParams.put("r", "red from 0-255");
                        }
                        if (!actualParams.containsKey("g")) {
                            missingParams.put("g", "green from 0-255");
                        }
                        try {
                            g = Integer.parseInt(actualParams.get("g").get(0));
                        } catch (Exception ex) {
                            missingParams.put("g", "green from 0-255");
                        }
                        if (!actualParams.containsKey("b")) {
                            missingParams.put("b", "blue from 0-255");
                        }
                        try {
                            b = Integer.parseInt(actualParams.get("b").get(0));
                        } catch (Exception ex) {
                            missingParams.put("b", "blue from 0-255");
                        }
                        if (missingParams.keySet().size() > 0) {
                            output.put("ERROR", "Missing a required Paramater:" + pathPoints.get(0));
                            if (!actualParams.containsKey("index")) {
                                missingParams.put("index", "OPTIONAL, the specific LED index to change");
                            }
                            output.put("Missing Params", missingParams);
                        } else {
                            try {
                                if (!(actualParams.containsKey("index"))) {

                                    controller.setColor(
                                            actualParams.get("deviceId").get(0),
                                            r,
                                            g,
                                            b);
                                } else {
                                    try {
                                        i = Integer.parseInt(actualParams.get("index").get(0));

                                    } catch (Exception ex) {
                                        missingParams.put("index", "OPTIONAL, the specific LED index to change");
                                    }
                                    //output.put("method", "setIndexedColor");
                                    controller.setColor(
                                            actualParams.get("deviceId").get(0),
                                            i,
                                            r,
                                            g,
                                            b);
                                }
                                output.put("success", true);
                            } catch (Exception ex) {
                                output.put("ERROR_METHOD", ex.getMessage());
                                ex.printStackTrace();
                            }
                        }
                        break;
                    default:
                        output.put("method", "ERROR");
                        output.put("ERROR", "Not a known method:" + pathPoints.get(0));
                        output.put("pathpoints", pathPoints);
                }

            }
            String response = output.toString(4);
            exchange.getResponseHeaders().put("Content-Type", Collections.singletonList("application/json"));
            if (output.keySet().contains("ERROR")) {
                exchange.sendResponseHeaders(500, response.length());
            } else {
                exchange.sendResponseHeaders(200, response.length());
            }
            os.write(response.getBytes());
            os.close();
        }
        /*catch(java.net.ConnectException ex)
        {
            //probably connection refused... don't print
        }*/ catch (Exception | java.lang.Error ex) {
            String response = ex.getMessage();
            System.out.println(response);
            boolean headersNotSent = false;
            try {
                if (ex.getMessage().startsWith("404\tEndpoint not found")) //bad error status... it should be 501
                {
                    exchange.sendResponseHeaders(501, response.length());
                } else {
                    exchange.sendResponseHeaders(500, response.length());
                }
                headersNotSent = true;
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            if (headersNotSent) {

                if (response.length() > 0) //status 204 NO CONTENT, successful but no response.
                {
                    byte[] theBytes = response.getBytes();
                    try {
                        os.write(theBytes);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }  //TODO: headers or data already sent but some other issue was raised... let it run through

        }
    }


    public static Map<String, List<String>> getQueryMap(String query) {
        Map<String, List<String>> map = new HashMap<>();
        if (query == null) {
            return map;
        }
        if (query.equals("")) {
            return map;
        }
        if (query.trim().equals("")) {
            return map;
        }
        if (URLDecoder.decode(query, StandardCharsets.UTF_8).trim().equals("")) {
            return map;
        }
        try {
            String[] params = query.split("&");

            for (String param : params) {
                String[] parts = param.split("=");
                String name = parts[0];
                if (!map.containsKey(name)) {
                    map.put(name, new ArrayList<>());
                }
                if (parts.length > 1) {
                    map.get(name).add(java.net.URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return map;
    }
}
