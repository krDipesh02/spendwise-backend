package com.spendwise.dto.response;

import com.spendwise.dto.entity.Category;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDto {

    private String id;
    private String name;
    private String icon;
    private boolean active;

    public static CategoryDto from(Category category) {
        return new CategoryDto(
                category.getId().toString(),
                category.getName(),
                category.getIcon(),
                category.isActive()
        );
    }
}
