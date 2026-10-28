package pt.flawless.fkits.service;

public record ClaimResult(Status status, long remainingMillis) {
    public enum Status { OK, COOLDOWN, FULL, BUSY }
}
