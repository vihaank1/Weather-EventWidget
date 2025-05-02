package cs1302.api;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.concurrent.Task;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import java.util.Properties;
import java.io.InputStream;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Event Forecast App - Displays weather and local events for any city.
 */
public class ApiApp extends Application {
    private Stage stage;
    private Scene scene;
    private VBox root;
    private TextField cityInput;
    private Button searchButton;
    private Label weatherLabel;
    private ListView<String> eventsList;
    private Label statusLabel;
    private String openWeatherKey;
    private String ticketmasterKey;
    private boolean apiKeysValid = true;

    private final HttpClient client = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    /**
     * Represents the JSON response structure from OpenWeatherMap API. Extracts coordinates for
     * Ticketmaster.
     */
    public static class WeatherResponse {
        public String name;
        public Coord coord;
        public Main main;

        /**
         * Geographic coordinates from OpenWeatherMap.
         */
        public static class Coord {
            public double lat;
            public double lon;
        }

        /**
         * Contains temperature data from OpenWeatherMap.
         */
        public static class Main {
            public double temp;
        }
    }


    /**
     * Represents the JSON response structure from Ticketmaster API.
     */
    public static class EventsResponse {
        @SerializedName("_embedded")
        public Embedded embedded;

        /**
         * Here is a wrapper for an array of events.
         */
        public static class Embedded {
            public Event[] events;
        }

        /**
         * Here are Individual event details from Ticketmaster.
         */
        public static class Event {
            public String name;
            public Dates dates;
            public Venue venue;
            @SerializedName("_embedded")
            public EventEmbedded embedded;
        }

        /**
         * Contains venue array for venues.
         */
        public static class EventEmbedded {
            public Venue[] venues;
        }

        /**
         * This contains event date information.
         */
        public static class Dates {
            public Start start;
        }

        /**
         * This contains localized start date info.
         */
        public static class Start {
            public String localDate;
        }

        /**
         * Here are venue details as well.
         */
        public static class Venue {
            public String name;
        }
    }


    /**
     * Constructs an {@code ApiApp} object. This default (i.e., no argument)
     * constructor is executed in Step 2 of the JavaFX Application Life-Cycle.
     */
    public ApiApp() {
        root = new VBox();
    } // ApiApp


    /**
     * Initializes API keys from config.properties file. We disable search functionality if
     * keys aren't there or don't work.
     */
    @Override
    public void init() {
        try (InputStream input = getClass().getResourceAsStream("/config.properties")) {
            Properties prop = new Properties();
            prop.load(input);
            openWeatherKey = prop.getProperty("openweather.key");
            ticketmasterKey = prop.getProperty("ticketmaster.key");

            if (openWeatherKey == null || openWeatherKey.isBlank() ||
                 ticketmasterKey == null || ticketmasterKey.isBlank()) {
                apiKeysValid = false;
            }
        } catch (Exception e) {
            apiKeysValid = false;
        }
    }


    /** {@inheritDoc} */
    @Override
    public void start(Stage stage) {

        this.stage = stage;

        // demonstrate how to load local asset using "file:resources/"
        Image bannerImage = new Image("file:resources/readme-banner.png");
        ImageView banner = new ImageView(bannerImage);
        banner.setPreserveRatio(true);
        banner.setFitWidth(640);

        // some labels to display information
        this.cityInput = new TextField();
        this.cityInput.setPromptText("Format: for US: City,State Code,Country Code"
                         + "(e.g., Athens,GA,US) and International: City,Country Code (Calgary,CA");
        this.searchButton = new Button("Search");
        this.weatherLabel = new Label();
        this.eventsList = new ListView<>();
        this.statusLabel = new Label("Enter a city to find weather and events!!");

        if (!apiKeysValid) {
            statusLabel.setText("Error: API keys not loaded");
            searchButton.setDisable(true);
        }

        searchButton.setOnAction(e -> {
            String input = cityInput.getText().trim();
            if (!input.isEmpty()) {
                fetchWeather(input);
            }
        });

        // setup scene
        root.getChildren().addAll(banner, cityInput, searchButton, weatherLabel, eventsList,
                                  statusLabel);
        scene = new Scene(root, 640, 650);

        // setup stage
        stage.setTitle("Event Forecast!");
        stage.setScene(scene);
        stage.setOnCloseRequest(event -> Platform.exit());
        stage.sizeToScene();
        stage.show();

    } // start


    /**
     * Fetches weather info from OpenWeatherMap API. City name is used as input.
     * @param input Target city name for weather search (state code and country where needed).
     */
    private void fetchWeather(String input) {
        try {
            String[] best = new String[3];

            // Validate Input
            if (!validateInput(input, best)) {
                return;
            }

            String city = best[0], state = best[1], country = best[2];

            String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8);
            String encodedState = URLEncoder.encode(state, StandardCharsets.UTF_8);
            String encodedCountry = URLEncoder.encode(country, StandardCharsets.UTF_8);

            String url = "https://api.openweathermap.org/data/2.5/weather?q=" + encodedCity
                + (!state.isEmpty() ? "," + encodedState : "")
                + (!country.isEmpty() ? "," + encodedCountry : "")
                + "&units=metric&APPID=" + openWeatherKey;

            Task<WeatherResponse> task = new Task<>() {
                    @Override
                     protected WeatherResponse call() throws Exception {
                    HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
                    HttpResponse<String> response = client.send(request,
                                                           HttpResponse.BodyHandlers.ofString());

                        return gson.fromJson(response.body(), WeatherResponse.class);
                    }
                };

            task.setOnSucceeded(e -> {
                WeatherResponse weather = task.getValue();
                if (weather.coord == null) {
                    Platform.runLater(() -> statusLabel.setText("Invalid format for US City. " +
                                                 "Use: City,State,US (e.g., Atlanta,GA,US)"));
                    return;
                }

                Platform.runLater(() -> {
                    weatherLabel.setText(String.format("%s: %.1f°F",
                                                                 weather.name,
                                                       (weather.main.temp * 9.0 / 5.0 + 32)));
                });
                fetchEvents(weather.coord.lat, weather.coord.lon);
            });


            task.setOnFailed(e -> {
                Platform.runLater(() -> statusLabel.setText("Error fetching weather data"));
            });

            new Thread(task).start();
        } catch (Exception e) {
            Platform.runLater(() ->
                               statusLabel.setText("Invalid format. Use: City,State(US only),"
                                                   + "Country"));
        }

    }  // fetchWeather


    /**
     * Validates and parses city/state/country input.
     * @param input city location
     * @param result Output array for city, state (US input only), country
     * @return true if the input is valid, false otherwise
     */
    private boolean validateInput(String input, String[] result) {
        String[] parts = input.split(",");
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim();
        }
        // City + Country (e.g., London,GB)
        if (parts.length == 2) {
            result[0] = parts[0]; // City
            result[2] = parts[1]; // Country
            result[1] = "";       // State (empty)
        } else if (parts.length == 3 && parts[2].equalsIgnoreCase("US")) {
            result[0] = parts[0]; // City
            result[1] = parts[1]; // State
            result[2] = parts[2]; // Country (US)
            //  City + State + US (e.g., Atlanta,GA,US)
        } else {
            Platform.runLater(() -> statusLabel.setText(
                "Invalid input. Example inputs allowed: Paris,FR (International) or Houston,TX,US"
            ));
            return false;
        } // Invalid format

        // Validate country
        if (result[2].isEmpty()) {
            Platform.runLater(() -> statusLabel.setText("Missing country code (e.g., London,GB)"));
            return false;
        }

        // Validate US states
        if (result[2].equalsIgnoreCase("US") && result[1].isEmpty()) {
            Platform.runLater(() -> statusLabel.setText(
                                  "For US cities, include state code (e.g., Austin,TX,US)"
            ));
            return false;
        }

        // Validate non-US states
        if (!result[2].equalsIgnoreCase("US") && !result[1].isEmpty()) {
            Platform.runLater(() -> statusLabel.setText(
                                  "State codes only allowed for US (e.g., Paris,FR)"
            ));
            return false;
        }

        return true;
    }  // validateInput


    /**
     * Queries Ticketmaster Discovery API using geographic coordinates. It makes requests with
     * particular parameters like radius, event type, etc.
     * @param lat Latitude from OpenWeatherMap response
     * @param lon Longitude from OpenWeatherMap response
     */
    private void fetchEvents(double lat, double lon) {
        String url = "https://app.ticketmaster.com/discovery/v2/events.json" +
            "?latlong=" + lat + "," + lon +
            "&radius=15" + // 15 mile radius from coordinates
            "&unit=miles" +
            "&classificationName=Family,Music,Sports" + //family
            "&size=15&sort=date,asc" +  // 15 events and Nearest dates first
            "&apikey=" + ticketmasterKey;

        Task<EventsResponse> task = new Task<>() {
            @Override
            protected EventsResponse call() throws Exception {
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
                HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());

                return gson.fromJson(response.body(), EventsResponse.class);
                }
            };
        task.setOnSucceeded(e -> {
            eventsList.getItems().clear();
            EventsResponse response = task.getValue();

            if (response == null || response.embedded == null ||
                response.embedded.events == null) {
                statusLabel.setText("No events found sorry :(");
                return;
            }

            for (EventsResponse.Event event : response.embedded.events) {
                String date = "Date N/A", name = "Unnamed event", venue = "Unknown venue";

                if (event.dates != null && event.dates.start != null) {
                    date = event.dates.start.localDate;
                }
                if (event.name != null) {
                    name = event.name;
                }
                if (event.embedded != null && event.embedded.venues != null
                    && event.embedded.venues.length > 0) {
                    venue = event.embedded.venues[0].name;
                } else if (event.venue != null && event.venue.name != null) {
                    venue = event.venue.name;
                }

                String entry = String.format("%s - %s @ %s", date, name, venue);
                Platform.runLater(() -> eventsList.getItems().add(entry));
            }
            Platform.runLater(() ->
                statusLabel.setText("Here are " + eventsList.getItems().size() + " events :)"));
        });

        task.setOnFailed(e -> {
            Platform.runLater(() -> statusLabel.setText("Error fetching events- check API/Key"));
            if (e.getSource().getException() != null) {
                e.getSource().getException().printStackTrace(); // For error logging
            }
        });
        new Thread(task).start();
    }  //fetchEvents

} // ApiApp
