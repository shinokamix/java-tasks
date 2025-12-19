package ru.example;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Main {

    private static final String API_KEY = "fd00ff995a9a937e048699f3c363cabb";

    public static void main(String[] args) throws Exception {
        String url = "https://api.openweathermap.org/data/2.5/weather"
                + "?q=Kazan"
                + "&appid=" + API_KEY
                + "&units=metric"
                + "&lang=en";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request =
                HttpRequest.newBuilder(URI.create(url)).GET().build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        ObjectMapper mapper = new ObjectMapper();
        WeatherResponse w =
                mapper.readValue(response.body(), WeatherResponse.class);

        System.out.println("City: " + w.name);
        System.out.println("Description: " + w.weather.get(0).description);
        System.out.println("Temperature: " + w.main.temp + " °C");
        System.out.println("Feels like: " + w.main.feels_like + " °C");
        System.out.println("Humidity: " + w.main.humidity + " %");
    }
}
