package com.elzakaria.copiweb.repository;

import com.elzakaria.copiweb.model.AgentProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentProfileRepository extends JpaRepository<AgentProfile, Long> {
    List<AgentProfile> findAllByOrderByBuiltInDescNameAsc();
}
