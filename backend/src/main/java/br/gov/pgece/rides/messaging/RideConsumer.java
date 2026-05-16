package br.gov.pgece.rides.messaging;

import br.gov.pgece.rides.config.RabbitMQConfig;
import br.gov.pgece.rides.dto.RideResponse;
import br.gov.pgece.rides.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RideConsumer {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void onRideCreated(RideResponse ride) {
        try {
            log.info("Mensagem recebida da fila: corrida id={}", ride.getId());
            notificationService.notifyDrivers(ride);
        } catch (Exception e) {
            log.error("Falha ao processar mensagem da fila para corrida id={}: {}",
                      ride.getId(), e.getMessage(), e);
        }
    }
}
