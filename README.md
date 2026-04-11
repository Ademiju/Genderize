# Gender Classification API (Spring Boot)

A RESTful API built with Spring Boot that classifies a given name by gender using the Genderize.io API. The service processes the external API response and returns a structured, validated, and enriched result.

---

## 🚀 Features

* Calls external Genderize API
* Processes and transforms response data
* Confidence scoring based on rules
* ISO 8601 UTC timestamps
* Robust input validation
* Standardized error handling
* CORS enabled (`Access-Control-Allow-Origin: *`)
* Handles concurrent requests efficiently

---

## 📌 Endpoint

### `GET /api/classify`

Classifies a given name.

### 🔹 Query Parameters

| Parameter | Type   | Required | Description             |
| --------- | ------ | -------- |-------------------------|
| name      | string | Yes      | Name to classify gender |

---

## ✅ Success Response

```json
{
  "status": "success",
  "data": {
    "name": "michael",
    "gender": "male",
    "probability": 0.99,
    "sample_size": 1234,
    "is_confident": true,
    "processed_at": "2026-04-01T12:00:00Z"
  }
}
```

---

## ⚙️ Processing Rules

The API transforms the Genderize response as follows:

* Extracts:

    * `gender`
    * `probability`
    * `count` → renamed to `sample_size`
* Computes:

    * `is_confident = true` if:

        * `probability >= 0.7`
        * AND `sample_size >= 100`
* Adds:

    * `processed_at` (UTC, ISO 8601, seconds precision)

---

## ❌ Error Handling

All errors follow this structure:

```json
{
  "status": "error",
  "message": "Error message here"
}
```

---

### 🔹 Error Cases

| Scenario                | Status Code |
| ----------------------- | ----------- |
| Missing or empty `name` | 400         |
| Non-string `name`       | 422         |
| No prediction available | 200         |
| External API failure    | 502         |
| Internal server error   | 500         |

---

### Example: No Prediction

```json
{
  "status": "error",
  "message": "No prediction available for the provided name"
}
```

---

## 🌐 CORS

CORS is enabled globally:

```
Access-Control-Allow-Origin: *
```

---

## ⏱ Performance

* Response time under **500ms** (excluding external API latency)
* Stateless and thread-safe design
* Handles multiple concurrent requests

---

## 🛠 Tech Stack

* Java 17+
* Spring Boot
* Spring Web
* Jackson (JSON processing)

---

## 📦 Setup & Run

### 1. Clone the repository

```bash
git clone https://github.com/your-username/gender-classification-api.git
```

### 2. Build the project

```bash
mvn clean install
```

### 3. Run the application

```bash
mvn spring-boot:run
```

---

## 🔍 Example Request

```bash
curl "http://localhost:8050/api/classify?name=michael"
```

---

## 🧪 Testing Tips

* Try names with low data (e.g., rare names)
* Try empty query:

  ```
  /api/classify
  ```

---

## 📚 Notes

* `processed_at` is dynamically generated per request
* Timestamp is always UTC and truncated to seconds
* API is designed for reliability and clarity of output

---

## 📄 License

This project is open-source and available under the MIT License.
