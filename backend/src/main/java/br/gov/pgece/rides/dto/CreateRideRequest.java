package br.gov.pgece.rides.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateRideRequest {

    @NotBlank(message = "userId é obrigatório")
    private String userId;

    @NotBlank(message = "Endereço de partida é obrigatório")
    @Size(min = 10, max = 255, message = "Endereço de partida deve ter entre 10 e 255 caracteres")
    private String pickupAddress;

    @NotBlank(message = "Endereço de destino é obrigatório")
    @Size(min = 10, max = 255, message = "Endereço de destino deve ter entre 10 e 255 caracteres")
    private String destinationAddress;
}
