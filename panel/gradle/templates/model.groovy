package ${packageName};

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ${className} {
<% attributes.each { attr -> %>
    private ${attr.type} ${attr.name};
<% } %>
}
