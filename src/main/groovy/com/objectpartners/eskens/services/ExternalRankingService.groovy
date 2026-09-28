package com.objectpartners.eskens.services

import com.objectpartners.eskens.entities.Person
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

/**
 * This is to mimic calls to an external 3rd party service that you wouldn't want to test locally.
 * The URL and API key come from the environment (RANKING_API_URL / RANKING_API_KEY, see .env.example).
 * When no URL is configured a default "Unranked" rank is returned so the app still runs locally.
 * Created by derek on 4/10/17.
 */
@Service
class ExternalRankingService {

    static final Rank UNRANKED = new Rank(level: 0, classification: 'Unranked')

    final RestClient restClient

    final String apiUrl

    ExternalRankingService(RestClient.Builder restClientBuilder,
                           @Value('${ranking.api.url:}') String apiUrl,
                           @Value('${ranking.api.key:}') String apiKey) {
        this.apiUrl = apiUrl
        this.restClient = restClientBuilder
                .defaultHeader('X-API-KEY', apiKey)
                .build()
    }

    Rank getRank(Person person) {
        if (!apiUrl) {
            return UNRANKED
        }
        restClient.get()
                .uri("$apiUrl/ranks/{id}", person.id)
                .retrieve()
                .body(Rank)
    }
}
