package br.gov.pgece.rides.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RejectRideRequest {

    @NotBlank(message = "driverId é obrigatório")
    private String driverId;
}
