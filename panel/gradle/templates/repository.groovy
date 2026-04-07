package ${packageName};

import ${entityPackage}.${entityName}Entity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ${className} extends JpaRepository<${entityName}Entity, Long>, JpaSpecificationExecutor<${entityName}Entity> {
}
