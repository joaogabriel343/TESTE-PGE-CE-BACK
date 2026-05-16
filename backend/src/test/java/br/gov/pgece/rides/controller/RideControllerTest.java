package br.gov.pgece.rides.controller;

import br.gov.pgece.rides.dto.AcceptRideRequest;
import br.gov.pgece.rides.dto.CreateRideRequest;
import br.gov.pgece.rides.dto.RejectRideRequest;
import br.gov.pgece.rides.dto.RideResponse;
import br.gov.pgece.rides.exception.GlobalExceptionHandler;
import br.gov.pgece.rides.exception.RideAlreadyAcceptedException;
import br.gov.pgece.rides.exception.RideNotFoundException;
import br.gov.pgece.rides.exception.RideNotInProgressException;
import br.gov.pgece.rides.model.RideStatus;
import br.gov.pgece.rides.service.RideService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RideController.class)
@Import(GlobalExceptionHandler.class)
class RideControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean  private RideService rideService;

    private RideResponse buildResponse(Long id, RideStatus status) {
        return RideResponse.builder()
                .id(id)
                .userId("user-1")
                .pickupAddress("Av. Beira Mar, 100")
                .destinationAddress("Av. Washington Soares, 200")
                .status(status)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private RideResponse buildResponseWithDriver(Long id, String driverId) {
        return RideResponse.builder()
                .id(id).userId("user-1")
                .pickupAddress("Av. Beira Mar, 100")
                .destinationAddress("Av. Washington Soares, 200")
                .status(RideStatus.IN_PROGRESS)
                .driverId(driverId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("POST /api/rides")
    class CreateRide {

        @Test
        @DisplayName("deve retornar 201 com corrida criada")
        void shouldReturn201() throws Exception {
            CreateRideRequest request = new CreateRideRequest();
            request.setUserId("user-1");
            request.setPickupAddress("Av. Beira Mar, 100");
            request.setDestinationAddress("Av. Washington Soares, 200");

            when(rideService.createRide(any())).thenReturn(buildResponse(1L, RideStatus.PENDING));

            mockMvc.perform(post("/api/rides")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.userId").value("user-1"));
        }

        @Test
        @DisplayName("deve retornar 400 quando userId está em branco")
        void shouldReturn400WhenUserIdBlank() throws Exception {
            CreateRideRequest request = new CreateRideRequest();
            request.setUserId("");
            request.setPickupAddress("Av. Beira Mar, 100");
            request.setDestinationAddress("Av. Washington Soares, 200");

            mockMvc.perform(post("/api/rides")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("deve retornar 400 quando pickupAddress está em branco")
        void shouldReturn400WhenPickupBlank() throws Exception {
            CreateRideRequest request = new CreateRideRequest();
            request.setUserId("user-1");
            request.setPickupAddress("");
            request.setDestinationAddress("Av. Washington Soares, 200");

            mockMvc.perform(post("/api/rides")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 400 quando destinationAddress está em branco")
        void shouldReturn400WhenDestinationBlank() throws Exception {
            CreateRideRequest request = new CreateRideRequest();
            request.setUserId("user-1");
            request.setPickupAddress("Av. Beira Mar, 100");
            request.setDestinationAddress("");

            mockMvc.perform(post("/api/rides")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ══ GET /api/rides ════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/rides")
    class ListAllRides {

        @Test
        @DisplayName("deve retornar 200 com lista de corridas")
        void shouldReturn200WithList() throws Exception {
            when(rideService.listAll()).thenReturn(
                    List.of(buildResponse(1L, RideStatus.PENDING), buildResponse(2L, RideStatus.IN_PROGRESS)));

            mockMvc.perform(get("/api/rides"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));
        }
    }

    // ══ GET /api/rides/pending ════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/rides/pending")
    class ListPendingRides {

        @Test
        @DisplayName("deve retornar 200 com somente corridas PENDING")
        void shouldReturn200WithPendingOnly() throws Exception {
            when(rideService.listPending()).thenReturn(List.of(buildResponse(1L, RideStatus.PENDING)));

            mockMvc.perform(get("/api/rides/pending"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].status").value("PENDING"));
        }
    }

    // ══ GET /api/rides/{id} ═══════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/rides/{id}")
    class GetRideById {

        @Test
        @DisplayName("deve retornar 200 com a corrida")
        void shouldReturn200() throws Exception {
            when(rideService.findById(1L)).thenReturn(buildResponse(1L, RideStatus.PENDING));

            mockMvc.perform(get("/api/rides/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1));
        }

        @Test
        @DisplayName("deve retornar 404 quando corrida não existe")
        void shouldReturn404() throws Exception {
            when(rideService.findById(99L)).thenThrow(new RideNotFoundException(99L));

            mockMvc.perform(get("/api/rides/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").exists());
        }
    }

    // ══ POST /api/rides/{id}/accept ═══════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/rides/{id}/accept")
    class AcceptRide {

        @Test
        @DisplayName("deve retornar 200 com corrida IN_PROGRESS e motorista vinculado")
        void shouldReturn200() throws Exception {
            AcceptRideRequest request = new AcceptRideRequest();
            request.setDriverId("driver-1");

            when(rideService.acceptRide(eq(1L), any())).thenReturn(buildResponseWithDriver(1L, "driver-1"));

            mockMvc.perform(post("/api/rides/1/accept")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                    .andExpect(jsonPath("$.driverId").value("driver-1"));
        }

        @Test
        @DisplayName("deve retornar 404 quando corrida não existe")
        void shouldReturn404() throws Exception {
            AcceptRideRequest request = new AcceptRideRequest();
            request.setDriverId("driver-1");

            when(rideService.acceptRide(eq(99L), any())).thenThrow(new RideNotFoundException(99L));

            mockMvc.perform(post("/api/rides/99/accept")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("deve retornar 409 quando corrida já foi aceita")
        void shouldReturn409WhenAlreadyAccepted() throws Exception {
            AcceptRideRequest request = new AcceptRideRequest();
            request.setDriverId("driver-2");

            when(rideService.acceptRide(eq(1L), any())).thenThrow(new RideAlreadyAcceptedException(1L));

            mockMvc.perform(post("/api/rides/1/accept")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("deve retornar 400 quando driverId está em branco")
        void shouldReturn400WhenDriverIdBlank() throws Exception {
            AcceptRideRequest request = new AcceptRideRequest();
            request.setDriverId("");

            mockMvc.perform(post("/api/rides/1/accept")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ══ POST /api/rides/{id}/reject ═══════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/rides/{id}/reject")
    class RejectRide {

        @Test
        @DisplayName("deve retornar 200 mantendo corrida PENDING")
        void shouldReturn200() throws Exception {
            RejectRideRequest request = new RejectRideRequest();
            request.setDriverId("driver-1");

            when(rideService.rejectRide(eq(1L), any())).thenReturn(buildResponse(1L, RideStatus.PENDING));

            mockMvc.perform(post("/api/rides/1/reject")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @Test
        @DisplayName("deve retornar 404 quando corrida não existe")
        void shouldReturn404() throws Exception {
            RejectRideRequest request = new RejectRideRequest();
            request.setDriverId("driver-1");

            when(rideService.rejectRide(eq(99L), any())).thenThrow(new RideNotFoundException(99L));

            mockMvc.perform(post("/api/rides/99/reject")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("deve retornar 409 quando corrida não está mais PENDING")
        void shouldReturn409WhenNotPending() throws Exception {
            RejectRideRequest request = new RejectRideRequest();
            request.setDriverId("driver-1");

            when(rideService.rejectRide(eq(1L), any())).thenThrow(new RideAlreadyAcceptedException(1L));

            mockMvc.perform(post("/api/rides/1/reject")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }
    }

    // ══ POST /api/rides/{id}/complete ══════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/rides/{id}/complete")
    class CompleteRide {

        @Test
        @DisplayName("deve retornar 200 com corrida COMPLETED")
        void shouldReturn200() throws Exception {
            RideResponse completed = RideResponse.builder()
                    .id(1L).userId("user-1")
                    .pickupAddress("Av. Beira Mar, 100")
                    .destinationAddress("Av. Washington Soares, 200")
                    .status(RideStatus.COMPLETED).driverId("driver-1")
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .build();

            when(rideService.completeRide(1L)).thenReturn(completed);

            mockMvc.perform(post("/api/rides/1/complete"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("COMPLETED"));
        }

        @Test
        @DisplayName("deve retornar 404 quando corrida não existe")
        void shouldReturn404() throws Exception {
            when(rideService.completeRide(99L)).thenThrow(new RideNotFoundException(99L));

            mockMvc.perform(post("/api/rides/99/complete"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("deve retornar 409 quando corrida não está IN_PROGRESS")
        void shouldReturn409WhenNotInProgress() throws Exception {
            when(rideService.completeRide(1L)).thenThrow(new RideNotInProgressException(1L));

            mockMvc.perform(post("/api/rides/1/complete"))
                    .andExpect(status().isConflict());
        }
    }
}
