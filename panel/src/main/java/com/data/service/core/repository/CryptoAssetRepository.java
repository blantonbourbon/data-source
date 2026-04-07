package com.data.service.core.repository;

import com.data.service.core.model.CryptoAssetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface CryptoAssetRepository extends JpaRepository<CryptoAssetEntity, Long>, JpaSpecificationExecutor<CryptoAssetEntity> {
}
