package com.shofiniakol.catalog;

import com.shofiniakol.common.AuditedEntity;
import com.shofiniakol.common.Lang;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "categories")
public class Category extends AuditedEntity {

    @Column(nullable = false, unique = true, length = 40)
    private String slug;

    @Column(name = "name_ar", nullable = false, length = 80)
    private String nameAr;

    @Column(name = "name_en", nullable = false, length = 80)
    private String nameEn;

    @Column(name = "name_fr", nullable = false, length = 80)
    private String nameFr;

    @Column(length = 16)
    private String emoji;

    @Column(name = "icon_url", length = 300)
    private String iconUrl;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private boolean active = true;

    public String localizedName() {
        return Lang.pick(nameAr, nameEn, nameFr);
    }
}
