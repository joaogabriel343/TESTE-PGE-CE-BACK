package br.gov.pgece.rides.service;

import br.gov.pgece.rides.model.Ride;
import br.gov.pgece.rides.model.RideStatus;
import br.gov.pgece.rides.repository.RideRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RideExpirationSchedulerTest {

    @Mock private RideRepository rideRepository;
    @InjectMocks private RideExpirationScheduler scheduler;

    @Test
    @DisplayName("deve cancelar corridas PENDING com mais de 2 minutos")
    void shouldCancelExpiredPendingRides() {
        Ride expiredRide = Ride.builder()
                .id(1L).userId("user-1")
                .pickupAddress("Av. Beira Mar, 100")
                .destinationAddress("Av. Washington Soares, 200")
                .status(RideStatus.PENDING)
                .createdAt(LocalDateTime.now().minusMinutes(5))
                .build();

        when(rideRepository.findByStatusAndCreatedAtBefore(eq(RideStatus.PENDING), any()))
                .thenReturn(List.of(expiredRide));

        scheduler.expireOldPendingRides();

        assertThat(expiredRide.getStatus()).isEqualTo(RideStatus.CANCELLED);
        verify(rideRepository).saveAll(List.of(expiredRide));
    }

    @Test
    @DisplayName("não deve salvar nada quando não há corridas expiradas")
    void shouldDoNothingWhenNoExpiredRides() {
        when(rideRepository.findByStatusAndCreatedAtBefore(eq(RideStatus.PENDING), any()))
                .thenReturn(List.of());

        scheduler.expireOldPendingRides();

        verify(rideRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("deve usar threshold de 2 minutos atrás para busca")
    void shouldQueryWithCorrectThreshold() {
        when(rideRepository.findByStatusAndCreatedAtBefore(eq(RideStatus.PENDING), any()))
                .thenReturn(List.of());

        LocalDateTime before = LocalDateTime.now();
        scheduler.expireOldPendingRides();
        LocalDateTime after = LocalDateTime.now();

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(rideRepository).findByStatusAndCreatedAtBefore(eq(RideStatus.PENDING), captor.capture());

        LocalDateTime threshold = captor.getValue();
        assertThat(threshold).isBefore(before.minusMinutes(1));
        assertThat(threshold).isAfter(after.minusMinutes(3));
    }

    @Test
    @DisplayName("deve cancelar múltiplas corridas de uma vez")
    void shouldCancelMultipleExpiredRides() {
        Ride ride1 = Ride.builder().id(1L).status(RideStatus.PENDING)
                .createdAt(LocalDateTime.now().minusMinutes(3)).build();
        Ride ride2 = Ride.builder().id(2L).status(RideStatus.PENDING)
                .createdAt(LocalDateTime.now().minusMinutes(10)).build();
        Ride ride3 = Ride.builder().id(3L).status(RideStatus.PENDING)
                .createdAt(LocalDateTime.now().minusMinutes(5)).build();

        when(rideRepository.findByStatusAndCreatedAtBefore(eq(RideStatus.PENDING), any()))
                .thenReturn(List.of(ride1, ride2, ride3));

        scheduler.expireOldPendingRides();

        assertThat(ride1.getStatus()).isEqualTo(RideStatus.CANCELLED);
        assertThat(ride2.getStatus()).isEqualTo(RideStatus.CANCELLED);
        assertThat(ride3.getStatus()).isEqualTo(RideStatus.CANCELLED);
        verify(rideRepository).saveAll(argThat(rides -> {
            List<Ride> list = (List<Ride>) rides;
            return list.size() == 3;
        }));
    }
}
