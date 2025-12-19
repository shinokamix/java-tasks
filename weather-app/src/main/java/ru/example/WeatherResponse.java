package ru.example;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WeatherResponse {

    public String name;
    public Main main;
    public List<Weather> weather;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Main {
        public double temp;
        public double feels_like;
        public int humidity;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Weather {
        public String description;
    }
}
