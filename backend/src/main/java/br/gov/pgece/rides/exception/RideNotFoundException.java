package br.gov.pgece.rides.exception;

public class RideNotFoundException extends RuntimeException {

    public RideNotFoundException(Long id) {
        super("Corrida não encontrada: id=" + id);
    }
}
