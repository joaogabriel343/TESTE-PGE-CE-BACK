package br.gov.pgece.rides.service;

import br.gov.pgece.rides.dto.AcceptRideRequest;
import br.gov.pgece.rides.dto.CreateRideRequest;
import br.gov.pgece.rides.dto.RejectRideRequest;
import br.gov.pgece.rides.dto.RideResponse;
import br.gov.pgece.rides.exception.RideAlreadyAcceptedException;
import br.gov.pgece.rides.exception.RideNotFoundException;
import br.gov.pgece.rides.exception.RideNotInProgressException;
import br.gov.pgece.rides.messaging.RideProducer;
import br.gov.pgece.rides.model.Ride;
import br.gov.pgece.rides.model.RideStatus;
import br.gov.pgece.rides.repository.RideRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RideServiceTest {

    @Mock private RideRepository rideRepository;
    @Mock private RideProducer rideProducer;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private RideService rideService;

    private Ride pendingRide;
    private Ride inProgressRide;

    @BeforeEach
    void setUp() {
        pendingRide = Ride.builder()
                .id(1L)
                .userId("user-1")
                .pickupAddress("Av. Beira Mar, 100")
                .destinationAddress("Av. Washington Soares, 200")
                .status(RideStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        inProgressRide = Ride.builder()
                .id(1L)
                .userId("user-1")
                .pickupAddress("Av. Beira Mar, 100")
                .destinationAddress("Av. Washington Soares, 200")
                .status(RideStatus.IN_PROGRESS)
                .driverId("driver-1")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Nested
    @DisplayName("createRide")
    class CreateRide {

        @Test
        @DisplayName("deve persistir corrida com status PENDING e publicar na fila")
        void shouldSaveAndPublish() {
            CreateRideRequest request = new CreateRideRequest();
            request.setUserId("user-1");
            request.setPickupAddress("Av. Beira Mar, 100");
            request.setDestinationAddress("Av. Washington Soares, 200");

            when(rideRepository.save(any(Ride.class))).thenReturn(pendingRide);

            RideResponse response = rideService.createRide(request);

            assertThat(response.getStatus()).isEqualTo(RideStatus.PENDING);
            assertThat(response.getUserId()).isEqualTo("user-1");
            verify(rideRepository).save(any(Ride.class));
            verify(rideProducer).publishRideCreated(any(RideResponse.class));
        }
    }

    @Nested
    @DisplayName("acceptRide")
    class AcceptRide {

        @Test
        @DisplayName("deve mudar status para IN_PROGRESS, vincular motorista e cachear no Redis")
        void shouldUpdateStatusAndCacheInRedis() {
            AcceptRideRequest request = new AcceptRideRequest();
            request.setDriverId("driver-1");

            when(rideRepository.findById(1L)).thenReturn(Optional.of(pendingRide));
            when(rideRepository.save(any(Ride.class))).thenReturn(inProgressRide);

            RideResponse response = rideService.acceptRide(1L, request);

            assertThat(response.getStatus()).isEqualTo(RideStatus.IN_PROGRESS);
            assertThat(response.getDriverId()).isEqualTo("driver-1");
            verify(valueOperations).set(eq("ride:in_progress:1"), any(RideResponse.class), any());
        }

        @Test
        @DisplayName("deve continuar normalmente quando Redis falha (resiliência)")
        void shouldContinueWhenRedisFails() {
            AcceptRideRequest request = new AcceptRideRequest();
            request.setDriverId("driver-1");

            when(rideRepository.findById(1L)).thenReturn(Optional.of(pendingRide));
            when(rideRepository.save(any(Ride.class))).thenReturn(inProgressRide);
            doThrow(new RuntimeException("Redis unavailable"))
                    .when(valueOperations).set(any(), any(), any());

            assertThatCode(() -> rideService.acceptRide(1L, request))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("deve lançar RideNotFoundException quando corrida não existe")
        void shouldThrowWhenRideNotFound() {
            when(rideRepository.findById(99L)).thenReturn(Optional.empty());

            AcceptRideRequest request = new AcceptRideRequest();
            request.setDriverId("driver-1");

            assertThatThrownBy(() -> rideService.acceptRide(99L, request))
                    .isInstanceOf(RideNotFoundException.class)
                    .hasMessageContaining("99");
        }

        @Test
        @DisplayName("deve lançar RideAlreadyAcceptedException quando corrida não está PENDING")
        void shouldThrowWhenRideNotPending() {
            when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));

            AcceptRideRequest request = new AcceptRideRequest();
            request.setDriverId("driver-2");

            assertThatThrownBy(() -> rideService.acceptRide(1L, request))
                    .isInstanceOf(RideAlreadyAcceptedException.class);
        }
    }

    @Nested
    @DisplayName("rejectRide")
    class RejectRide {

        @Test
        @DisplayName("deve retornar corrida PENDING sem alterar status")
        void shouldReturnPendingRideUnchanged() {
            RejectRideRequest request = new RejectRideRequest();
            request.setDriverId("driver-1");

            when(rideRepository.findById(1L)).thenReturn(Optional.of(pendingRide));

            RideResponse response = rideService.rejectRide(1L, request);

            assertThat(response.getStatus()).isEqualTo(RideStatus.PENDING);
            verify(rideRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lançar RideNotFoundException quando corrida não existe")
        void shouldThrowWhenNotFound() {
            when(rideRepository.findById(99L)).thenReturn(Optional.empty());

            RejectRideRequest request = new RejectRideRequest();
            request.setDriverId("driver-1");

            assertThatThrownBy(() -> rideService.rejectRide(99L, request))
                    .isInstanceOf(RideNotFoundException.class);
        }

        @Test
        @DisplayName("deve lançar RideAlreadyAcceptedException quando corrida não está PENDING")
        void shouldThrowWhenNotPending() {
            when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));

            RejectRideRequest request = new RejectRideRequest();
            request.setDriverId("driver-1");

            assertThatThrownBy(() -> rideService.rejectRide(1L, request))
                    .isInstanceOf(RideAlreadyAcceptedException.class);
        }
    }

    // ══ completeRide ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("completeRide")
    class CompleteRide {

        @Test
        @DisplayName("deve mudar status para COMPLETED e remover do Redis")
        void shouldUpdateStatusToCompleted() {
            Ride completedRide = Ride.builder()
                    .id(1L).userId("user-1")
                    .pickupAddress("Av. Beira Mar, 100")
                    .destinationAddress("Av. Washington Soares, 200")
                    .status(RideStatus.COMPLETED).driverId("driver-1")
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .build();

            when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));
            when(rideRepository.save(any(Ride.class))).thenReturn(completedRide);

            RideResponse response = rideService.completeRide(1L);

            assertThat(response.getStatus()).isEqualTo(RideStatus.COMPLETED);
            verify(redisTemplate).delete("ride:in_progress:1");
        }

        @Test
        @DisplayName("deve continuar normalmente quando deleção no Redis falha")
        void shouldContinueWhenRedisDeleteFails() {
            Ride completedRide = Ride.builder()
                    .id(1L).status(RideStatus.COMPLETED).driverId("driver-1")
                    .createdAt(LocalDateTime.now()).build();

            when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));
            when(rideRepository.save(any(Ride.class))).thenReturn(completedRide);
            doThrow(new RuntimeException("Redis unavailable")).when(redisTemplate).delete(anyString());

            assertThatCode(() -> rideService.completeRide(1L)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("deve lançar RideNotFoundException quando corrida não existe")
        void shouldThrowWhenNotFound() {
            when(rideRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> rideService.completeRide(99L))
                    .isInstanceOf(RideNotFoundException.class);
        }

        @Test
        @DisplayName("deve lançar RideNotInProgressException quando corrida não está IN_PROGRESS")
        void shouldThrowWhenNotInProgress() {
            when(rideRepository.findById(1L)).thenReturn(Optional.of(pendingRide));

            assertThatThrownBy(() -> rideService.completeRide(1L))
                    .isInstanceOf(RideNotInProgressException.class);
        }
    }

    // ══ listAll / listPending ═════════════════════════════════════════════════

    @Nested
    @DisplayName("listAll e listPending")
    class ListAndPending {

        @Test
        @DisplayName("listAll: deve retornar todas as corridas")
        void listAll_shouldReturnAllRides() {
            when(rideRepository.findAll()).thenReturn(List.of(pendingRide, inProgressRide));

            List<RideResponse> result = rideService.listAll();

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("listPending: deve retornar somente corridas PENDING")
        void listPending_shouldReturnOnlyPendingRides() {
            when(rideRepository.findByStatus(RideStatus.PENDING)).thenReturn(List.of(pendingRide));

            List<RideResponse> result = rideService.listPending();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo(RideStatus.PENDING);
        }

        @Test
        @DisplayName("listPending: deve retornar lista vazia quando não há corridas pendentes")
        void listPending_shouldReturnEmptyWhenNoPending() {
            when(rideRepository.findByStatus(RideStatus.PENDING)).thenReturn(List.of());

            List<RideResponse> result = rideService.listPending();

            assertThat(result).isEmpty();
        }
    }

    // ══ findById ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("deve retornar corrida do Redis quando disponível (cache hit)")
        void shouldReturnCachedRideFromRedis() {
            RideResponse cached = RideResponse.from(pendingRide);
            when(valueOperations.get("ride:in_progress:1")).thenReturn(cached);

            RideResponse result = rideService.findById(1L);

            assertThat(result.getId()).isEqualTo(1L);
            verify(rideRepository, never()).findById(any());
        }

        @Test
        @DisplayName("deve buscar no banco quando Redis não tem (cache miss)")
        void shouldFallbackToDatabaseOnCacheMiss() {
            when(valueOperations.get("ride:in_progress:1")).thenReturn(null);
            when(rideRepository.findById(1L)).thenReturn(Optional.of(pendingRide));

            RideResponse result = rideService.findById(1L);

            assertThat(result.getId()).isEqualTo(1L);
            verify(rideRepository).findById(1L);
        }

        @Test
        @DisplayName("deve lançar RideNotFoundException quando não existe em cache nem no banco")
        void shouldThrowWhenNotFoundAnywhere() {
            when(valueOperations.get("ride:in_progress:1")).thenReturn(null);
            when(rideRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> rideService.findById(1L))
                    .isInstanceOf(RideNotFoundException.class)
                    .hasMessageContaining("1");
        }
    }
}
