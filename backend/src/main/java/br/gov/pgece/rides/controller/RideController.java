package br.gov.pgece.rides.controller;

import br.gov.pgece.rides.dto.AcceptRideRequest;
import br.gov.pgece.rides.dto.CreateRideRequest;
import br.gov.pgece.rides.dto.RejectRideRequest;
import br.gov.pgece.rides.dto.RideResponse;
import br.gov.pgece.rides.service.RideService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Corridas", description = "Gerenciamento do ciclo de vida das corridas")
@RestController
@RequestMapping("/api/rides")
@RequiredArgsConstructor
public class RideController {

    private final RideService rideService;

    @Operation(
        summary = "Criar corrida",
        description = "Persiste a corrida como PENDING e publica na fila RabbitMQ para notificar motoristas via WebSocket"
    )
    @ApiResponse(responseCode = "201", description = "Corrida criada")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    @PostMapping
    public ResponseEntity<RideResponse> create(@Valid @RequestBody CreateRideRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rideService.createRide(request));
    }

    @Operation(summary = "Listar todas as corridas")
    @GetMapping
    public ResponseEntity<List<RideResponse>> listAll() {
        return ResponseEntity.ok(rideService.listAll());
    }

    @Operation(summary = "Listar corridas pendentes", description = "Retorna apenas corridas com status PENDING")
    @GetMapping("/pending")
    public ResponseEntity<List<RideResponse>> listPending() {
        return ResponseEntity.ok(rideService.listPending());
    }

    @Operation(summary = "Buscar corrida por ID", description = "Consulta Redis primeiro; fallback no PostgreSQL")
    @ApiResponse(responseCode = "404", description = "Corrida não encontrada")
    @GetMapping("/{id}")
    public ResponseEntity<RideResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(rideService.findById(id));
    }

    @Operation(
        summary = "Aceitar corrida",
        description = "Muda status para IN_PROGRESS, vincula motorista e grava no Redis com TTL de 24h"
    )
    @ApiResponse(responseCode = "409", description = "Corrida já aceita")
    @PostMapping("/{id}/accept")
    public ResponseEntity<RideResponse> accept(
            @PathVariable Long id,
            @Valid @RequestBody AcceptRideRequest request) {
        return ResponseEntity.ok(rideService.acceptRide(id, request));
    }

    @Operation(
        summary = "Rejeitar corrida",
        description = "Motorista recusa a corrida; ela permanece PENDING e visível para outros motoristas"
    )
    @ApiResponse(responseCode = "409", description = "Corrida não está mais disponível para rejeição")
    @PostMapping("/{id}/reject")
    public ResponseEntity<RideResponse> reject(
            @PathVariable Long id,
            @Valid @RequestBody RejectRideRequest request) {
        return ResponseEntity.ok(rideService.rejectRide(id, request));
    }

    @Operation(
        summary = "Finalizar corrida",
        description = "Motorista finaliza a corrida: muda status para COMPLETED e remove do cache Redis"
    )
    @ApiResponse(responseCode = "409", description = "Corrida não está em andamento")
    @PostMapping("/{id}/complete")
    public ResponseEntity<RideResponse> complete(@PathVariable Long id) {
        return ResponseEntity.ok(rideService.completeRide(id));
    }
}
