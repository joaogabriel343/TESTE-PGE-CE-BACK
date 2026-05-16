package br.gov.pgece.rides.service;

import br.gov.pgece.rides.dto.RideResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public static final String TOPIC_RIDES = "/topic/rides";

    public void notifyDrivers(RideResponse ride) {
        try {
            messagingTemplate.convertAndSend(TOPIC_RIDES, ride);
            log.info("Motoristas notificados via WebSocket: corrida id={}", ride.getId());
        } catch (Exception e) {
            log.error("Falha ao notificar motoristas via WebSocket: {}", e.getMessage(), e);
        }
    }
}
