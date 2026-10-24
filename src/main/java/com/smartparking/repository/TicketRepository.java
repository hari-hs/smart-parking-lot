package com.smartparking.repository;

import com.smartparking.model.Ticket;

import java.util.Optional;

public interface TicketRepository {

    void save(Ticket ticket);

    Optional<Ticket> findById(String ticketId);
}
