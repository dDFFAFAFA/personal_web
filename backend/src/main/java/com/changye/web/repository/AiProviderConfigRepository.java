package com.changye.web.repository;

import com.changye.web.model.AiProviderConfig;
import com.changye.web.model.enums.AiProvider;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiProviderConfigRepository extends JpaRepository<AiProviderConfig, AiProvider> {
}
