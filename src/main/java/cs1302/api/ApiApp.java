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
     * ....
     */
    public static class WeatherResponse {
        public String name;
        public Coord coord;
        public Main main;

        /**
         * ...
         */
        public static class Coord {
            public double lat;
            public double lon;
        }

        /**
         * ...
         */
        public static class Main {
            public double temp;
        }
    }


    /**
     * ....
     */
    public static class EventsResponse {
        @SerializedName("_embedded")
        public Embedded embedded;

        /**
         * ...
         */
        public static class Embedded {
            public Event[] events;
        }

        /**
         * ...
         */
        public static class Event {
            public String name;
            public Dates dates;
            public Venue venue;
            @SerializedName("_embedded")
            public EventEmbedded embedded;
        }

        /**
         * ...
         */
        public static class EventEmbedded {
            public Venue[] venues;
        }

        /**
         * ...
         */
        public static class Dates {
            public Start start;
        }

        /**
         * ...
         */
        public static class Start {
            public String localDate;
        }

        /**
         * ...
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
     * ...
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
        this.searchButton = new Button("Search");
        this.weatherLabel = new Label();
        this.eventsList = new ListView<>();
        this.statusLabel = new Label("Enter a city to find weather and events");

        if (!apiKeysValid) {
            statusLabel.setText("Error: API keys not loaded");
            searchButton.setDisable(true);
        }

        searchButton.setOnAction(e -> {
            String city = cityInput.getText().trim();
            if (!city.isEmpty()) {
                fetchWeather(city);
            }
        });

        // setup scene
        root.getChildren().addAll(banner, cityInput, searchButton, weatherLabel, eventsList,
                                  statusLabel);
        scene = new Scene(root, 650, 650);

        // setup stage
        stage.setTitle("Event Forecast!");
        stage.setScene(scene);
        stage.setOnCloseRequest(event -> Platform.exit());
        stage.sizeToScene();
        stage.show();

    } // start


    /**
     * ...
     * @param city
     */
    private void fetchWeather(String city) {
        try {
            String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8);
            String url = "https://api.openweathermap.org/data/2.5/weather?q=" + encodedCity +
                "&units=metric&APPID=" + openWeatherKey;

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
                    Platform.runLater(() -> statusLabel.setText("Invalid city or no coord"));
                    return;
                }

                Platform.runLater(() -> {
                    weatherLabel.setText(String.format("%s: %.1f°C",
                                                                 weather.name, weather.main.temp));
                });
                fetchEvents(weather.coord.lat, weather.coord.lon);
            });


            task.setOnFailed(e -> {
                Platform.runLater(() -> statusLabel.setText("Error fetching weather data"));
            });

            new Thread(task).start();
        } catch (Exception e) {
            Platform.runLater(() ->
                               statusLabel.setText("This isn't a valid city format"));
        }

    }

    /**
     * ...
     * @param lat ....
     * @param lon ...
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
                statusLabel.setText("No events found, try major US cities");
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
                statusLabel.setText("Found " + eventsList.getItems().size() + " events"));
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
