package br.gov.pgece.rides.repository;

import br.gov.pgece.rides.model.Ride;
import br.gov.pgece.rides.model.RideStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RideRepository extends JpaRepository<Ride, Long> {

    List<Ride> findByStatus(RideStatus status);

    List<Ride> findByUserId(String userId);

    List<Ride> findByStatusAndCreatedAtBefore(RideStatus status, LocalDateTime before);
}
