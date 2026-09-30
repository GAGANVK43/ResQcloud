package com.resqcloud.common.exception;

public class InvalidIncidentStateException extends RuntimeException {
    public InvalidIncidentStateException(String message) {
        super(message);
    }

    public InvalidIncidentStateException(String currentStatus, String targetStatus, String reason) {
        super(String.format("Cannot transition incident from state '%s' to '%s': %s", currentStatus, targetStatus, reason));
    }
}
