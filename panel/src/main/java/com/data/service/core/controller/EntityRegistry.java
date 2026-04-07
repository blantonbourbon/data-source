package com.data.service.core.controller;

import com.data.service.core.mapper.EntityMapper;
import com.data.service.core.service.GenericService;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import jakarta.persistence.EntityManager;

/**
 * Auto-discovers all EntityMapper beans and creates corresponding
 * GenericService instances.
 * This eliminates the need for concrete Service classes for each entity.
 */
@Component
public class EntityRegistry {

    private final Map<String, GenericService<?, ?>> serviceMap = new HashMap<>();

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public EntityRegistry(ApplicationContext context, EntityManager entityManager) {
        // Find all beans that implement EntityMapper
        Map<String, EntityMapper> mappers = context.getBeansOfType(EntityMapper.class);

        for (EntityMapper mapper : mappers.values()) {
            // Inspect the generic types of the Mapper: EntityMapper<Model, Entity>
            ResolvableType type = ResolvableType.forClass(mapper.getClass()).as(EntityMapper.class);
            Class<?> modelClass = type.getGeneric(0).resolve();
            Class<?> entityClass = type.getGeneric(1).resolve();

            if (modelClass == null || entityClass == null) {
                continue; // Skip if types cannot be resolved
            }

            // Convention: Entity Name "TradeEntity" -> repository bean "tradeRepository"
            String repoBeanName = IntrospectorUtils.decapitalize(entityClass.getSimpleName().replace("Entity", ""))
                    + "Repository";

            if (context.containsBean(repoBeanName)) {
                Object repoBean = context.getBean(repoBeanName);
                if (repoBean instanceof JpaRepository && repoBean instanceof JpaSpecificationExecutor) {
                    // Create the GenericService dynamically
                    GenericService service = new GenericService(
                            (JpaRepository) repoBean,
                            (JpaSpecificationExecutor) repoBean,
                            mapper,
                            (Class) modelClass,
                            (Class) entityClass,
                            entityManager);

                    // Register it: "Trade" -> "trades"
                    String entityKey = modelClass.getSimpleName().toLowerCase() + "s";
                    serviceMap.put(entityKey, service);
                }
            }
        }
    }

    public GenericService<?, ?> getService(String entityName) {
        return serviceMap.get(entityName);
    }

    public boolean hasEntity(String entityName) {
        return serviceMap.containsKey(entityName);
    }

    // Helper to avoid java.beans dependency if not present, though Spring has
    // similar utils
    private static class IntrospectorUtils {
        public static String decapitalize(String name) {
            if (name == null || name.length() == 0) {
                return name;
            }
            if (name.length() > 1 && Character.isUpperCase(name.charAt(1)) &&
                    Character.isUpperCase(name.charAt(0))) {
                return name;
            }
            char chars[] = name.toCharArray();
            chars[0] = Character.toLowerCase(chars[0]);
            return new String(chars);
        }
    }
}
