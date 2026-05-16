package br.gov.pgece.rides.service;

import br.gov.pgece.rides.model.Ride;
import br.gov.pgece.rides.model.RideStatus;
import br.gov.pgece.rides.repository.RideRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RideExpirationScheduler {

    private static final long EXPIRATION_MINUTES = 2;

    private final RideRepository rideRepository;

    @Transactional
    @Scheduled(fixedDelay = 30_000)
    public void expireOldPendingRides() {
        LocalDateTime expirationThreshold = LocalDateTime.now().minusMinutes(EXPIRATION_MINUTES);

        List<Ride> expired = rideRepository.findByStatusAndCreatedAtBefore(
                RideStatus.PENDING, expirationThreshold);

        if (expired.isEmpty()) return;

        expired.forEach(ride -> ride.setStatus(RideStatus.CANCELLED));
        rideRepository.saveAll(expired);

        log.warn("Expiração automática: {} corrida(s) cancelada(s) por timeout (>{} min sem aceitação)",
                expired.size(), EXPIRATION_MINUTES);
    }
}
