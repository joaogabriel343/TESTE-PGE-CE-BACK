package br.gov.pgece.rides.exception;

public class RideAlreadyAcceptedException extends RuntimeException {

    public RideAlreadyAcceptedException(Long id) {
        super("Corrida id=" + id + " não está disponível para aceitação");
    }
}
