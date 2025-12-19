package org.rostislav.curiokeep.providers;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ProviderKnowledgeBase {

    private final Map<String, ProviderProfile> profiles;

    public ProviderKnowledgeBase() {
        Map<String, ProviderProfile> map = new LinkedHashMap<>();
        map.put("anilist", profile(
                "anilist",
                "AniList",
                "GraphQL-powered anime and manga metadata, including seasons, titles, and character information.",
                "https://anilist.co",
                "https://anilist.gitbook.io/ApiV2-GraphQL-Docs",
                "Anime/manga titles, synonyms, cover art, episodes, studios, and demographic tags.",
                "Returns GraphQL payloads with cover URLs and airing schedules",
                "Good for anime/manga titles across seasons and arcs"
        ));
        map.put("boardgamegeek", profile(
                "boardgamegeek",
                "Board Game Geek",
                "The community-driven board game catalog with ratings, mechanics, player counts, and designers.",
                "https://boardgamegeek.com",
                "https://boardgamegeek.com/wiki/page/BGG_XML_API2",
                "Board game metadata (title, year, designers, player range, playtime, family), plus user statistics.",
                "Includes designer/publisher, category, and mechanic tags",
                "Exposes average rating and complexity"
        ));
        map.put("brickset", profile(
                "brickset",
                "Brickset",
                "Official LEGO set data with release dates, piece counts, and minifigure lists.",
                "https://brickset.com",
                "https://brickset.com/api/v3.asmx",
                "Set metadata (name, theme, pieces, year), minifigure roster, and alternate box art.",
                "Lists set number, theme, and subtheme",
                "Highlights minifigure and inventory information"
        ));
        map.put("coverartarchive", profile(
                "coverartarchive",
                "Cover Art Archive",
                "Open source artwork repository tied to MusicBrainz release groups.",
                "https://coverartarchive.org",
                "https://coverartarchive.org/documentation/",
                "Album cover art URLs, artist/label thumbnail metadata, and release image types.",
                "Supports front/back/spine/artist photos",
                "Backed by MusicBrainz release IDs"
        ));
        map.put("discogs", profile(
                "discogs",
                "Discogs",
                "Marketplace-grade music release metadata with formats, styles, and label data.",
                "https://discogs.com",
                "https://www.discogs.com/developers/",
                "Release metadata (titles, artists, year, genres, formats) plus label/pressing information.",
                "Returns artist and label credits",
                "Includes format, country, and catalog numbers"
        ));
        map.put("googlebooks", profile(
                "googlebooks",
                "Google Books",
                "Global book catalog with bibliographic metadata, previews, and thumbnails.",
                "https://books.google.com",
                "https://www.googleapis.com/books/v1/volumes",
                "Book details (title, subtitle, authors, publisher, ISBNs, publish year, pages, language, cover art).",
                "Normalizes ISBN10/ISBN13 and authors",
                "Provides publisher/published year"
        ));
        map.put("igdb", profile(
                "igdb",
                "IGDB",
                "Video game database maintained by Twitch with deep metadata and screenshots.",
                "https://www.igdb.com",
                "https://api-docs.igdb.com",
                "Game metadata (name, summary, release date, platforms, genres, age ratings, cover art).",
                "RESTful JSON with localized summaries",
                "Requires Twitch/IGDB client token"
        ));
        map.put("internetarchive", profile(
                "internetarchive",
                "Internet Archive",
                "Digital library of books, audio, video, and software with rich metadata and download mirrors.",
                "https://archive.org",
                "https://archive.org/docs/api/",
                "Item metadata (title, creator, description, subjects, download files, external identifiers).",
                "Returns media formats and mirrors",
                "Includes contributor and collection tags"
        ));
        map.put("musicbrainz", profile(
                "musicbrainz",
                "MusicBrainz",
                "Open music encyclopedia that tracks recordings, releases, and artists with persistent MBIDs.",
                "https://musicbrainz.org",
                "https://musicbrainz.org/doc/MusicBrainz_API",
                "Release/recording metadata (titles, dates, length, artist credits) plus MBIDs for linking resources.",
                "Use MBIDs to join other services",
                "Returns multiple release versions and artists"
        ));
        map.put("openlibrary", profile(
                "openlibrary",
                "Open Library",
                "Internet Archive's open catalog with edition-level bibliographic data and cover images.",
                "https://openlibrary.org",
                "https://openlibrary.org/dev/docs/api/books",
                "Book metadata (title, authors, subjects, publish dates, languages, covers, ISBNs).",
                "Provides subjects/genres",
                "Exposes edition identifiers and cover URLs"
        ));
        map.put("openproduct", profile(
                "openproduct",
                "Open Product Data",
                "World Open Food Facts product metadata (branded food, beverage, and wellness items).",
                "https://world.openfoodfacts.org",
                "https://world.openfoodfacts.org/api/v2/product/{barcode}",
                "Product metadata (brands, ingredients, categories, allergens, packaging, images).",
                "Includes labeled ingredients and allergens",
                "Surface packaging and brand info"
        ));
        map.put("pokeapi", profile(
                "pokeapi",
                "PokéAPI",
                "Community-maintained Pokémon data with sprites, stats, types, and evolution chains.",
                "https://pokeapi.co",
                "https://pokeapi.co/docs/v2",
                "Pokémon species metadata (names, stats, abilities, sprites, types, evolution paths).",
                "Covers sprites on multiple generations",
                "Returns base stats and typing"
        ));
        map.put("rawg", profile(
                "rawg",
                "RAWG",
                "Gaming database aggregating releases, platforms, screenshots, and store links.",
                "https://rawg.io",
                "https://rawg.io/apidocs",
                "Video game metadata (titles, platforms, publishers, release dates, tags, ratings) and media.",
                "Includes store/URL references",
                "Ranks games by popularity and trending tags"
        ));
        map.put("rebrickable", profile(
                "rebrickable",
                "Rebrickable",
                "LEGO enthusiast site tracking sets, minifigs, parts, and alternate models.",
                "https://rebrickable.com",
                "https://rebrickable.com/api/v3/docs",
                "Set metadata, part inventories, color info, alternate build references, and user comments.",
                "Supports set/part searches",
                "Lists inventory counts and alternate models"
        ));
        map.put("scryfall", profile(
                "scryfall",
                "Scryfall",
                "Magic: The Gathering card database with pricing, art, and rulings.",
                "https://scryfall.com",
                "https://scryfall.com/docs/api",
                "Card metadata (name, mana cost, colors, type line, oracle text, legalities, prices, art).",
                "Surface card art variants and prices",
                "Exposes rulings and printings"
        ));
        map.put("tmdb", profile(
                "tmdb",
                "TMDb",
                "The Movie Database's open API for films and television metadata.",
                "https://www.themoviedb.org",
                "https://developers.themoviedb.org/3",
                "Movie/TV metadata (titles, overview, release dates, cast, crew, runtime, posters).",
                "Supports trending and popular queries",
                "Returns full cast/crew credits"
        ));
        map.put("tvmaze", profile(
                "tvmaze",
                "TVMaze",
                "TV show and episode metadata plus schedule, network, and cast details.",
                "https://www.tvmaze.com",
                "https://www.tvmaze.com/api",
                "Show metadata (name, summary, genres), seasons, episodes, and cast credits.",
                "Includes airtime/schedule information",
                "Exposes cast & crew data"
        ));
        profiles = Collections.unmodifiableMap(map);
    }

    public ProviderProfile profileFor(String key) {
        return profiles.get(key);
    }

    public Collection<ProviderProfile> all() {
        return profiles.values();
    }

    private static ProviderProfile profile(String key,
                                           String displayName,
                                           String summary,
                                           String websiteUrl,
                                           String apiUrl,
                                           String dataReturned,
                                           String... highlights) {
        return new ProviderProfile(key, displayName, summary, websiteUrl, apiUrl, dataReturned, List.of(highlights));
    }
}
