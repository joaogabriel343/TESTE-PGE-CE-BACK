package br.gov.pgece.rides.exception;

public class RideNotInProgressException extends RuntimeException {

    public RideNotInProgressException(Long id) {
        super("Corrida id=" + id + " não está em andamento e não pode ser finalizada");
    }
}
