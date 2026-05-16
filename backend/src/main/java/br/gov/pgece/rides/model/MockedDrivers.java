package br.gov.pgece.rides.model;

import java.util.List;

public class MockedDrivers {

    public record Driver(String id, String name, String license) {}

    public static final List<Driver> ALL = List.of(
        new Driver("driver-1", "Carlos Eduardo Silva",   "ABC-1234"),
        new Driver("driver-2", "Ana Luiza Souza",        "DEF-5678"),
        new Driver("driver-3", "Roberto Lima Neto",      "GHI-9012"),
        new Driver("driver-4", "Francisca Bezerra",      "JKL-3456"),
        new Driver("driver-5", "Antônio Gomes Júnior",   "MNO-7890"),
        new Driver("driver-6", "Maria das Graças Freire","PQR-1122")
    );

    public static Driver findById(String id) {
        return ALL.stream()
                  .filter(d -> d.id().equals(id))
                  .findFirst()
                  .orElseThrow(() -> new IllegalArgumentException("Motorista não encontrado: " + id));
    }

    private MockedDrivers() {}
}
