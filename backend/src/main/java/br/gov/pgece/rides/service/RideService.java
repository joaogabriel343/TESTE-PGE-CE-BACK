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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RideService {

    private static final String REDIS_KEY_PREFIX = "ride:in_progress:";
    private static final Duration REDIS_TTL = Duration.ofHours(24);

    private final RideRepository rideRepository;
    private final RideProducer rideProducer;
    private final RedisTemplate<String, Object> redisTemplate;

    @Transactional
    public RideResponse createRide(CreateRideRequest request) {
        Ride ride = Ride.builder()
                .userId(request.getUserId())
                .pickupAddress(request.getPickupAddress())
                .destinationAddress(request.getDestinationAddress())
                .status(RideStatus.PENDING)
                .build();

        Ride saved = rideRepository.save(ride);
        log.info("Corrida criada: id={} userId={}", saved.getId(), saved.getUserId());

        RideResponse response = RideResponse.from(saved);
        rideProducer.publishRideCreated(response);

        return response;
    }

    @Transactional
    public RideResponse acceptRide(Long rideId, AcceptRideRequest request) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RideNotFoundException(rideId));

        if (ride.getStatus() != RideStatus.PENDING) {
            throw new RideAlreadyAcceptedException(rideId);
        }

        ride.setStatus(RideStatus.IN_PROGRESS);
        ride.setDriverId(request.getDriverId());

        Ride updated = rideRepository.save(ride);
        log.info("Corrida id={} aceita pelo motorista {}", rideId, request.getDriverId());

        RideResponse response = RideResponse.from(updated);

        try {
            redisTemplate.opsForValue().set(REDIS_KEY_PREFIX + rideId, response, REDIS_TTL);
            log.info("Corrida id={} gravada no Redis (TTL=24h)", rideId);
        } catch (Exception e) {
            log.error("Falha ao gravar corrida id={} no Redis (operação continua): {}", rideId, e.getMessage());
        }

        return response;
    }

    @Transactional
    public RideResponse rejectRide(Long rideId, RejectRideRequest request) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RideNotFoundException(rideId));

        if (ride.getStatus() != RideStatus.PENDING) {
            throw new RideAlreadyAcceptedException(rideId);
        }

        log.info("Corrida id={} rejeitada pelo motorista {}", rideId, request.getDriverId());
        return RideResponse.from(ride);
    }

    @Transactional
    public RideResponse completeRide(Long rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RideNotFoundException(rideId));

        if (ride.getStatus() != RideStatus.IN_PROGRESS) {
            throw new RideNotInProgressException(rideId);
        }

        ride.setStatus(RideStatus.COMPLETED);
        Ride updated = rideRepository.save(ride);
        log.info("Corrida id={} finalizada pelo motorista {}", rideId, ride.getDriverId());

        try {
            redisTemplate.delete(REDIS_KEY_PREFIX + rideId);
        } catch (Exception e) {
            log.error("Falha ao remover corrida id={} do Redis: {}", rideId, e.getMessage());
        }

        return RideResponse.from(updated);
    }

    @Transactional(readOnly = true)
    public List<RideResponse> listAll() {
        return rideRepository.findAll()
                .stream()
                .map(RideResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RideResponse> listPending() {
        return rideRepository.findByStatus(RideStatus.PENDING)
                .stream()
                .map(RideResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public RideResponse findById(Long rideId) {
        String redisKey = REDIS_KEY_PREFIX + rideId;
        Object cached = redisTemplate.opsForValue().get(redisKey);
        if (cached instanceof RideResponse cachedRide) {
            log.debug("Cache hit Redis: corrida id={}", rideId);
            return cachedRide;
        }

        return rideRepository.findById(rideId)
                .map(RideResponse::from)
                .orElseThrow(() -> new RideNotFoundException(rideId));
    }
}
