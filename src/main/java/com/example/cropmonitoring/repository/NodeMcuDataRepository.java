package com.example.cropmonitoring.repository;

import com.example.cropmonitoring.model.NodeMcuData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NodeMcuDataRepository extends JpaRepository<NodeMcuData, Long> {
    NodeMcuData findTopByOrderByTimestampDesc();
}
