package org.falmdev.survivalRealms.domain.repository;

import org.falmdev.survivalRealms.domain.model.Warp;

import java.util.List;
import java.util.Optional;

public interface WarpRepository {
    void initialize() throws Exception;
    void save(Warp warp) throws Exception;
    void delete(String id) throws Exception;
    Optional<Warp> findById(String id) throws Exception;
    Optional<Warp> findByName(String name) throws Exception;
    List<Warp> findAll() throws Exception;
}