package com.glazr.authentication.infrastructure;

import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class UserSessionService {

    private final FindByIndexNameSessionRepository<? extends Session> sessions;

    public UserSessionService(FindByIndexNameSessionRepository<? extends Session> sessions) {
        this.sessions = sessions;
    }

    public void deleteSessions(UUID userId) {
        Map<String, ? extends Session> found = sessions.findByPrincipalName(userId.toString());
        found.keySet().forEach(sessions::deleteById);
    }
}
