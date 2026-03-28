package com.elzakaria.copiweb.service;

import com.elzakaria.copiweb.model.WorkflowCommand;
import com.elzakaria.copiweb.repository.WorkflowCommandRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WorkflowCommandService {

    private final WorkflowCommandRepository commandRepo;

    public List<WorkflowCommand> listCommands() {
        return commandRepo.findAllByOrderByBuiltInDescNameAsc();
    }

    public WorkflowCommand getCommand(Long id) {
        return commandRepo.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Workflow command not found: " + id));
    }

    public String assemblePrompt(Long id, Map<String, String> params) {
        var command = getCommand(id);
        String template = command.getPromptTemplate();
        if (template == null || template.isBlank()) return "";
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String value = entry.getValue() != null ? entry.getValue() : "";
            template = template.replace("{{" + entry.getKey() + "}}", value);
        }
        return template;
    }
}
