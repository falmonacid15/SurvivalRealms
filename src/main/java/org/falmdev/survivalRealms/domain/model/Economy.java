package org.falmdev.survivalRealms.domain.model;

import java.util.UUID;

public class Economy {

    private final UUID playerUuid;
    private double balance;

    public Economy(UUID playerUuid, double balance) {
        this.playerUuid = playerUuid;
        this.balance    = balance;
    }

    public UUID   getPlayerUuid() { return playerUuid; }
    public double getBalance()    { return balance; }
    public void   setBalance(double balance) { this.balance = balance; }

    public boolean canAfford(double amount) { return balance >= amount; }

    public void deposit(double amount) { this.balance += amount; }

    public void withdraw(double amount) {
        if (amount > balance) throw new IllegalArgumentException("Saldo insuficiente");
        this.balance -= amount;
    }
}