package org.example.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.entity.Service;
import org.example.enums.Category;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServiceCreationResponse {
    String name;
    Category category;
    String token;
    String owner;
    Long createdAt;
    Long warningDuration;
}
