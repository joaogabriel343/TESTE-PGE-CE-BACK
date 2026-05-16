package br.gov.pgece.rides.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PGE Rides API")
                        .description("Sistema de Gerenciamento de Corridas Corporativas — " +
                                     "Procuradoria Geral do Estado do Ceará")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("PGE-CE")
                                .url("https://www.pge.ce.gov.br")));
    }
}
