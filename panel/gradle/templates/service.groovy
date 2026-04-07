package ${packageName};

import ${modelPackage}.${entityName};
import ${entityPackage}.${entityName}Entity;
import ${repositoryPackage}.${repositoryName};
import ${mapperPackage}.${mapperName};
import org.springframework.stereotype.Service;

@Service
public class ${className} extends GenericService<${entityName}, ${entityName}Entity> {

    public ${className}(${repositoryName} repository, ${mapperName} mapper) {
        super(repository, repository, mapper, ${entityName}.class);
    }
}
