package br.gov.pgece.rides.dto;

import br.gov.pgece.rides.model.Ride;
import br.gov.pgece.rides.model.RideStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RideResponse {

    private Long id;
    private String userId;
    private String pickupAddress;
    private String destinationAddress;
    private RideStatus status;
    private String driverId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static RideResponse from(Ride ride) {
        return RideResponse.builder()
                .id(ride.getId())
                .userId(ride.getUserId())
                .pickupAddress(ride.getPickupAddress())
                .destinationAddress(ride.getDestinationAddress())
                .status(ride.getStatus())
                .driverId(ride.getDriverId())
                .createdAt(ride.getCreatedAt())
                .updatedAt(ride.getUpdatedAt())
                .build();
    }
}
