package ${packageName};

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "${tableName}")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ${className} {
<% attributes.each { attr -> %><% if (attr.isId) { %>
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private ${attr.type} ${attr.name};
<% } else { %>
    private ${attr.type} ${attr.name};
<% } %><% } %>
}
