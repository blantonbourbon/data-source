<% def calculatedAttrs = attributes.findAll { it.isCalculated } %>package ${packageName};
<% if (!calculatedAttrs.isEmpty()) { %>
import ${entityPackage}.${entityClass};
<% } %>
import org.springframework.stereotype.Component;

/**
 * Mapper Implementation.
 * You can customize logic here. This file will not be overwritten if it exists.
 */
@Component
public class ${className} extends ${baseClassName} {
<% calculatedAttrs.each { attr -> %>
    @Override
    protected ${attr.type} calculate${attr.capitalizedName}(${entityClass} entity) {
        // TODO: Implement custom logic for ${attr.name}
        return null;
    }
<% } %>
}
