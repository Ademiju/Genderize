# Genderize Profile API

Spring Boot API for building and serving enriched person profiles from a name. The application combines:

* `Genderize` for gender prediction
* `Agify` for age prediction
* `Nationalize` for country prediction

It supports:

* profile creation from a name
* profile lookup by ID
* filtered, sorted, and paginated profile listing
* natural-language search over profiles
* startup database seeding from a JSON file

## Base URL

Hosted API base URL:

`https://genderize-production-e6cd.up.railway.app`

## What The Project Does

When a client submits a name to `POST /api/profiles`, the service calls three external APIs, merges the results, classifies the age into an age group, and stores a normalized profile in the database.

Stored profile fields:

* `id`
* `name`
* `gender`
* `gender_probability`
* `age`
* `age_group`
* `country_id`
* `country_probability`
* `created_at`

Names are normalized to lowercase before storage, and the `name` column is unique.

## Features

* profile creation using `Genderize`, `Agify`, and `Nationalize`
* direct gender classification endpoint via `GET /api/classify`
* JSON-based startup seed loader
* upsert-style seeding by normalized name
* filtering, sorting, and pagination in one `GET /api/profiles` request
* rule-based natural-language search via `GET /api/profiles/search`
* standardized JSON response format
* PostgreSQL persistence with Spring Data JPA

## Profile Creation Flow

`POST /api/profiles` accepts a request body like:

```json
{
  "name": "Michael"
}
```

The service then:

1. normalizes the name to lowercase
2. checks whether the profile already exists
3. calls:
   `https://api.genderize.io`
   `https://api.agify.io`
   `https://api.nationalize.io`
4. selects the top predicted country from `Nationalize`
5. derives `age_group` from `age`
6. stores the assembled profile

Age-group classification:

* `0-12` -> `child`
* `13-19` -> `teenager`
* `20-59` -> `adult`
* `60+` -> `senior`

If the normalized name already exists, the API returns the existing profile instead of creating a duplicate.

## API Endpoints

### `POST /api/profiles`

Create a profile from a name.

Example:

```bash
curl -X POST https://genderize-production-e6cd.up.railway.app/api/profiles \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"Michael\"}"
```

### `GET /api/profiles/{id}`

Fetch a single stored profile by UUID.

### `GET /api/profiles`

List profiles with filtering, sorting, and pagination.

Supported filters:

* `gender`
* `age_group`
* `country_id`
* `min_age`
* `max_age`
* `min_gender_probability`
* `min_country_probability`

Sorting:

* `sort_by` -> `age` | `created_at` | `gender_probability`
* `order` -> `asc` | `desc`

Pagination:

* `page` default: `1`
* `limit` default: `10`
* `limit` max: `50`

Example:

```bash
curl "https://genderize-production-e6cd.up.railway.app/api/profiles?gender=male&country_id=NG&min_age=18&sort_by=age&order=desc&page=1&limit=10"
```

### `GET /api/profiles/search`

Natural-language profile search. This endpoint parses a plain-English query into the same profile filters used by `GET /api/profiles`.

Example:

```bash
curl "https://genderize-production-e6cd.up.railway.app/api/profiles/search?q=young males from nigeria&page=1&limit=10"
```

### `DELETE /api/profiles/{id}`

Delete a stored profile by UUID.

### `GET /api/classify`

Direct gender-only classification using `Genderize`.

Example:

```bash
curl "https://genderize-production-e6cd.up.railway.app/api/classify?name=michael"
```

## Filtering, Sorting, And Pagination Behavior

The `GET /api/profiles` endpoint composes all supported filters into a single database query using JPA specifications.

Validation rules:

* `page` must be `>= 1`
* `limit` must be between `1` and `50`
* `min_age` cannot be greater than `max_age`
* `sort_by` must be one of `age`, `created_at`, or `gender_probability`
* `order` must be `asc` or `desc`

Response pagination fields:

* `page`
* `limit`
* `total`
* `data`

## Startup JSON Seeding

On application startup, the project loads seed data from JSON and writes it to the database.

Supported file locations, in lookup order:

* `file:seed_profile.json`
* `classpath:seed_profile.json`
* `classpath:seed_profiles.json`

The current repository includes:

* `src/main/resources/seed_profiles.json`

Expected shape:

```json
{
  "profiles": [
    {
      "name": "Awino Hassan",
      "gender": "female",
      "gender_probability": 0.66,
      "age": 68,
      "age_group": "senior",
      "country_id": "TZ",
      "country_probability": 0.60
    }
  ]
}
```

Seeding behavior:

* names are normalized to lowercase before matching
* existing rows are matched by normalized `name`
* if a profile already exists, its seed-managed fields are updated
* if a profile does not exist, a new row is inserted
* existing `id` and `created_at` are preserved for updated rows

This makes the seed file suitable for both first-time population and later corrections to profile data.

## Natural-Language Search Parsing

The `GET /api/profiles/search` endpoint uses a rule-based parser only. It does not use AI, embeddings, or LLMs.

The parser:

* lowercases the query
* removes extra commas and spaces
* detects supported gender, age-group, age-bound, and country patterns
* converts those patterns into structured profile filters
* reuses the same pagination rules as `GET /api/profiles`

If the query cannot be interpreted, the endpoint returns:

```json
{
  "status": "error",
  "message": "Unable to interpret query"
}
```

Supported keyword mappings:

Gender:

* `male`, `males` -> `gender=male`
* `female`, `females` -> `gender=female`
* if both appear in one query, no gender filter is applied

Age groups:

* `child`, `children`, `kid`, `kids` -> `age_group=child`
* `teen`, `teenager`, `teenagers` -> `age_group=teenager`
* `adult`, `adults` -> `age_group=adult`
* `senior`, `seniors`, `elderly` -> `age_group=senior`

Age bounds:

* `young` -> `min_age=16`, `max_age=24`
* `above 30`, `over 30`, `older than 30` -> `min_age=30`
* `below 20`, `under 20`, `younger than 20` -> `max_age=20`

Country mapping:

* countries are resolved from Java locale country names into ISO alpha-2 codes
* examples:
  `from nigeria` -> `country_id=NG`
  `from kenya` -> `country_id=KE`
  `from angola` -> `country_id=AO`
* supported aliases include `usa`, `us`, `uk`, `dr congo`, and `congo`

Example interpretations:

* `young males` -> `gender=male`, `min_age=16`, `max_age=24`
* `females above 30` -> `gender=female`, `min_age=30`
* `people from angola` -> `country_id=AO`
* `adult males from kenya` -> `gender=male`, `age_group=adult`, `country_id=KE`
* `male and female teenagers above 17` -> `age_group=teenager`, `min_age=17`

## Parser Limitations

The natural-language parser intentionally supports a small rule set. Current limitations:

* no typo correction or fuzzy matching
* no semantic understanding beyond the supported keywords
* no support for phrases like `between 20 and 30`, `at least 18`, or `exactly 25`
* no support for probability-based natural-language phrases
* no support for natural-language sort instructions like `oldest first`
* no negation handling such as `not male`
* no multi-country logic such as `from nigeria and kenya`
* no OR filtering for mixed-gender phrases; mixed gender simply removes the gender filter
* no conflict resolution for logically inconsistent phrases

## Response Shape

Successful list/search responses include pagination metadata:

```json
{
  "status": "success",
  "page": 1,
  "limit": 10,
  "total": 2026,
  "data": [
    {
      "id": "7bb7f9ef-2b25-4db5-b0f2-7dca8c1bc3ef",
      "name": "michael",
      "gender": "male",
      "gender_probability": 0.99,
      "age": 34,
      "age_group": "adult",
      "country_id": "NG",
      "country_probability": 0.72,
      "created_at": "2026-04-22T14:00:00Z"
    }
  ]
}
```

Error responses follow:

```json
{
  "status": "error",
  "message": "Error message here"
}
```

## Tech Stack

* Java 21
* Spring Boot 3
* Spring Web
* Spring Data JPA
* PostgreSQL
* Jackson
* Lombok
* Maven

## Configuration

Main application settings live in:

* `src/main/resources/application.properties`
* `src/main/resources/application-dev.properties`
* `src/main/resources/application-prod.properties`

The app uses PostgreSQL and expects datasource configuration through properties or environment-backed placeholders, depending on the active profile.

## Setup And Run

### 1. Clone the repository

```bash
git clone https://github.com/Ademiju/Genderize.git
cd Genderize
```

### 2. Build the project

```bash
mvn clean install
```

### 3. Run the application

```bash
mvn spring-boot:run
```

### 4. Run tests

```bash
mvn test
```

## Notes

* timestamps are stored as `Instant` in UTC ISO 8601 format
* profile creation depends on external API availability
* `country_id` is stored as an ISO alpha-2 country code
* the JSON seed file can be used to bulk-populate or refresh the database on startup

## License

This project is open-source and available under the MIT License.
