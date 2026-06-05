package org.falmdev.survivalRealms.database.warp;

import org.falmdev.survivalRealms.model.Warp;

import java.util.List;
import java.util.Optional;

public interface WarpRepository {
    void initialize() throws Exception;
    void saveWarp(Warp warp) throws Exception;
    void deleteWarp(String id) throws Exception;
    Optional<Warp> findById(String id) throws Exception;
    Optional<Warp> findByName(String name) throws Exception;
    List<Warp> findAll() throws Exception;
}