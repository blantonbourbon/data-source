package com.data.service.core.mapper;

/**
 * Generic mapper interface for converting between Model (DTO) and Entity (JPA)
 * types.
 *
 * @param <M> the model/DTO type
 * @param <E> the JPA entity type
 */
public interface EntityMapper<M, E> {

    M toModel(E entity);

    E toEntity(M model);
}
