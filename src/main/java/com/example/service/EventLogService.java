package com.example.service;

import com.example.model.EventLog;
import com.example.repository.EventLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EventLogService {

    @Autowired
    private EventLogRepository eventLogRepository;

    public void log(String eventType, Long jobId, Long jobseekerId, Long recruiterId, String description) {
        EventLog log = new EventLog();
        log.setEventType(eventType);
        log.setDescription(description != null ? description : "JobId: " + jobId + ", JobseekerId: " + jobseekerId + ", RecruiterId: " + recruiterId);
        eventLogRepository.save(log);
    }
}