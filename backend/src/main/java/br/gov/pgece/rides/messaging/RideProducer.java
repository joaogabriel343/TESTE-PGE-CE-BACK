package br.gov.pgece.rides.messaging;

import br.gov.pgece.rides.config.RabbitMQConfig;
import br.gov.pgece.rides.dto.RideResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class RideProducer {

    private final RabbitTemplate rabbitTemplate;


    public void publishRideCreated(RideResponse ride) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    RabbitMQConfig.ROUTING_KEY,
                    ride
            );
            log.info("Corrida id={} publicada na fila", ride.getId());
        } catch (Exception e) {
            log.error("Falha ao publicar corrida id={} na fila: {}", ride.getId(), e.getMessage(), e);
        }
    }
}
