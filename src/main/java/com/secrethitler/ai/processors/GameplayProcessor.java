package com.secrethitler.ai.processors;

import java.util.Optional;

import com.secrethitler.ai.dtos.ParticipantGameNotification;

public interface GameplayProcessor {
	Optional<String> getMessageToSend(final ParticipantGameNotification notification);
}
