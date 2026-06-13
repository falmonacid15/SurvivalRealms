package org.falmdev.survivalRealms.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Team {

    private final UUID         id;
    private       String       name;
    private       String       description;
    private       double       bankBalance;
    private       TeamBanner   banner;
    private final List<TeamMember> members;
    private final Instant      createdAt;

    public Team(UUID id, String name, String description,
                double bankBalance, TeamBanner banner,
                List<TeamMember> members, Instant createdAt) {
        this.id          = id;
        this.name        = name;
        this.description = description;
        this.bankBalance = bankBalance;
        this.banner      = banner;
        this.members     = members != null ? members : new ArrayList<>();
        this.createdAt   = createdAt;
    }

    public UUID             getId()          { return id; }
    public String           getName()        { return name; }
    public String           getDescription() { return description; }
    public double           getBankBalance() { return bankBalance; }
    public TeamBanner       getBanner()      { return banner; }
    public List<TeamMember> getMembers()     { return members; }
    public Instant          getCreatedAt()   { return createdAt; }

    public void setName(String name)             { this.name        = name; }
    public void setDescription(String desc)      { this.description = desc; }
    public void setBankBalance(double balance)   { this.bankBalance = balance; }
    public void setBanner(TeamBanner banner)     { this.banner      = banner; }

    public boolean canBankAfford(double amount)  { return bankBalance >= amount; }

    public void depositToBank(double amount)     { this.bankBalance += amount; }

    public void withdrawFromBank(double amount) {
        if (amount > bankBalance) throw new IllegalArgumentException("Saldo del banco insuficiente");
        this.bankBalance -= amount;
    }

    public TeamMember getMember(UUID uuid) {
        return members.stream()
                .filter(m -> m.getPlayerUuid().equals(uuid))
                .findFirst()
                .orElse(null);
    }

    public boolean hasMember(UUID uuid) {
        return getMember(uuid) != null;
    }

    public TeamMember getAdmin() {
        return members.stream()
                .filter(TeamMember::isAdmin)
                .findFirst()
                .orElse(null);
    }

    public int getMemberCount() { return members.size(); }
}