package com.app.Genderize.service;

import com.app.Genderize.config.SystemProperties;
import com.app.Genderize.response.GenderizeResponse;
import com.app.Genderize.response.GenericResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class GenderizeService {
    private final SystemProperties systemProperties;
    private final RestTemplate restTemplate;

    public ResponseEntity<GenericResponse<Object>> classifyGenderByName(String name){
        log.info("Starting classifyGenderByName service");
        try{
            String genderizeClassifyUrl = String.format(systemProperties.getGenderizeBaseUrl().concat("?name=%s"), name);
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            log.info("Sending request to: {}", genderizeClassifyUrl);
            ResponseEntity<String> responseEntity = restTemplate.exchange(genderizeClassifyUrl, HttpMethod.GET, entity, String.class);
            log.info("Genderize response: {}", responseEntity);
            GenderizeResponse genderizeResponse = new GenderizeResponse();
            if(responseEntity.getStatusCode().is2xxSuccessful()){
                log.info("Genderize responseBody: {}", responseEntity.getBody());
                JSONObject responseJson = new JSONObject(responseEntity.getBody());
                if(responseJson.isEmpty() || responseJson.optString("gender").isEmpty() || responseJson.getInt("count") == 0){
                    return ResponseEntity.ok(GenericResponse.builder().status("error").message("No prediction available for the provided name").build());
                }
                genderizeResponse.setGender(responseJson.optString("gender", null));
                genderizeResponse.setName(responseJson.optString("name", null));
                genderizeResponse.setSampleSize(responseJson.optInt("count", 0));
                genderizeResponse.setProbability(responseJson.optDouble("probability",0.0));
                genderizeResponse.setIsConfident(genderizeResponse.getSampleSize() >= 100 && genderizeResponse.getProbability() >= 0.7);
                genderizeResponse.setProcessedAt(Instant.now().truncatedTo(ChronoUnit.SECONDS));
                GenericResponse<Object> finalResponse = GenericResponse.builder().status("success").data(genderizeResponse).build();
                log.info("Final response: {}", finalResponse);
                return ResponseEntity.ok(finalResponse);
            }else {
                log.error("Genderize API returned non-2xx status: {}", responseEntity.getStatusCode());
                return ResponseEntity.status(502)
                        .body(GenericResponse.builder()
                                .status("error")
                                .message("External API error")
                                .build());
            }


        } catch (RestClientException ex) {
            log.error("External API error: ", ex);
            return ResponseEntity.status(502)
                    .body(GenericResponse.builder()
                            .status("error")
                            .message("External API error: ".concat(ex.getMessage()))
                            .build());


        }catch (Exception e){
            log.error("Error occurred :", e);
            return ResponseEntity.internalServerError().body(GenericResponse.builder().status("error").message(e.getMessage()).build());
        }
    }
}
