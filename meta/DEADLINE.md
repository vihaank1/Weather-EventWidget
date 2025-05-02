# Deadline

Modify this file to satisfy a submission requirement related to the project
deadline. Please keep this file organized using Markdown. If you click on
this file in your GitHub repository website, then you will see that the
Markdown is transformed into nice-looking HTML.

## Part 1.1: App Description

> Please provide a friendly description of your app, including
> the primary functions available to users of the app. Be sure to
> describe exactly what APIs you are using and how they are connected
> in a meaningful way.

> **Also, include the GitHub `https` URL to your repository.**

**Event Forecast** is a JavaFX application that helps users discover local weather
conditions and nearby events. By integrating two external APIs:
1. **OpenWeatherMap API**: Provides real-time weather data and geographic coordinates
2. **Ticketmaster Discovery API**: Finds events near the coordinates from the weather data

Users can search using:
- International cities: `City,CountryCode` (e.g., `Paris,FR`)
- US cities: `City,StateCode,US` (e.g., `Atlanta,GA,US`)

GitHub Repository:
https://github.com/vihaank1/cs1302-api-app



## Part 1.2: APIs

> For each RESTful JSON API that your app uses (at least two are required),
> include an example URL for a typical request made by your app. If you
> need to include additional notes (e.g., regarding API keys or rate
> limits), then you can do that below the URL/URI. Placeholders for this
> information are provided below. If your app uses more than two RESTful
> JSON APIs, then include them with similar formatting.

### API 1: OpenWeatherMap Current Weather

**Example Requests**:

- International City
```
https://api.openweathermap.org/data/2.5/weather?q=London,GB&units=metric&APPID=f524984b71ee33e111e4a315da9bc139
```
- US City
```
https://api.openweathermap.org/data/2.5/weather?q=Austin,TX,US&units=metric&APPID=f524984b71ee33e111e4a315da9bc139
```

> Returns coordinates (e.g., London: "lon":-0.1257,"lat":51.5085). Rate limit: 60 calls/minute (free tier).


### API 2: Ticketmaster Discovery

**Example Requests**:

- Events in London

```
https://app.ticketmaster.com/discovery/v2/events.json?latlong=51.5074,-0.1278&radius=15&unit=miles&classificationName=
Family,Music,Sports&size=15&sort=date,asc&apikey=ycntiTB7iRIoiqThruMhKk9K3ipSvaWV
```
- Events in Austin

```
https://app.ticketmaster.com/discovery/v2/events.json?latlong=30.2672,-97.7431&radius=15&unit=miles&classificationName=
Family,Music,Sports&size=15&sort=date,asc&apikey=ycntiTB7iRIoiqThruMhKk9K3ipSvaWV
```

> latlong values come directly from OpenWeatherMap response. classificationName=Family,Music,Sports filters events to family-friendly, music,
and sports. size=15 returns up to 15 events. sort=date,asc lists events chronologically (soonest first).


## Part 2: New

> What is something new and/or exciting that you learned from working
> on this project?

I learned about Secure API Key Management: Storing keys in config.properties to avoid hardcoding and using environment variables for deployment,
Input Validation: Ensuring users enter City,CountryCode (e.g., Paris,FR) or City,StateCode,US (e.g., Austin,TX,US) to avoid ambiguous results,
Threaded API Calls: Using JavaFX Task to fetch weather and events without freezing the UI, and
ISO Code Usage: How to structure API requests with ISO 3166 codes (e.g., FR for France, CA for Canada), etc.
I must say typing London,GB and seeing many fun events pop up in London was thrilling!


## Part 3: Retrospect

> If you could start the project over from scratch, what do
> you think might do differently and why?

I feel that I would add a help section: Users shouldn’t need to memorize codes. A dropdown showing US state codes (e.g., GA=Georgia, TX=Texas)
and common country codes (e.g., FR=France) would reduce errors; incorporate a auto-suggest feature for cities: Typing "Athen" could suggest
Athens,GA,US (Georgia), making searches faster and less error-prone; track API usage: Displaying "Ticketmaster: 45/5000 requests left today"
would help avoid hitting rate limits for users; and allow a retry for failed requests: Automatically retrying after 5 seconds would handle
temporary issues (e.g., Wi-Fi drops) for users.



### ISO Code Clarification
Users can find valid codes through:
- Official [ISO 3166 Country Codes](https://www.iso.org/obp/ui/#search) portal
- US state codes follow [ANSI US State Codes](https://www.census.gov/library/reference/code-lists/ansi.html) standard
- Common code references (e.g., FR=France, CA=Canada, AU=Australia)
- Built-in validation in the app that rejects invalid codes

The app now includes strict input validation that:
- Requires country codes for all searches
- Enforces state codes for US locations

  London,GB → London, United Kingdom

  Montreal,CA → Montreal, Canada

  Austin,TX,US → Austin, Texas, US
