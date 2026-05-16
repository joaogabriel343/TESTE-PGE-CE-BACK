package br.gov.pgece.rides.controller;

import br.gov.pgece.rides.model.MockedDrivers;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/drivers")
public class DriverController {

    @GetMapping
    public ResponseEntity<List<MockedDrivers.Driver>> listDrivers() {
        return ResponseEntity.ok(MockedDrivers.ALL);
    }
}
