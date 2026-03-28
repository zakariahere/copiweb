package com.elzakaria.copiweb.repository;

import com.elzakaria.copiweb.model.WorkflowCommand;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkflowCommandRepository extends JpaRepository<WorkflowCommand, Long> {
    List<WorkflowCommand> findAllByOrderByBuiltInDescNameAsc();
}
