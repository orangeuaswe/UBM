package com.UBM.BMW.config;
import java.net.http.HttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import com.UBM.BMW.client.AlertSender;
import com.UBM.BMW.client.LoggingAlertSender;
import com.UBM.BMW.client.NhtsaRecallClient;
import com.UBM.BMW.client.RecallClient;
import com.UBM.BMW.client.VinDecoder;
import com.UBM.BMW.client.WebhookAlertSender;

@Configuration
public class ClientConfig
{
    @Bean
    RestClient toolkitRestClient(NhtsaProperties property)
    {
        HttpClient http = HttpClient.newBuilder().connectTimeout(property.timeout()).build();
        JdkClientHttpRequestFactor factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(property.timeout());
        return RestClient.builder().requestFactory(factory).defaultHeader("Accept", "application/json").build();
    }
    @Bean
    VinDecoder decoder(RestClient toolkitRestClient, NhtsaProperties properties)
    {
        return new NhtsaVpicClient(toolkitRestClient, properties.vpicBaseUrl());
    }
    @Bean
    RecallClient recallClient(RestClient toolkitRestClient, NhtsaProperties properties)
    {
        return new NthsaRecallClient(toolkitRestClient, properties.recallBaseUrl());
    }
    @Bean
    AlertSender sender(RestClient toolkitRestClient, AlertProperties properties)
    {
        return properties.hasWebhook() ? new WebhoolAlertSender(toolkitRestClient, properties.webhookUrl()) : new LoggingAlertSender();
    }
}