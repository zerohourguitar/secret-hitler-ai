package com.secrethitler.ai.processors;

import java.util.Optional;

import com.secrethitler.ai.dtos.GameplayAction;
import com.secrethitler.ai.dtos.ParticipantGameNotification;

public interface GameplayProcessor {
	Optional<GameplayAction> getActionToTake(final ParticipantGameNotification notification);
}
