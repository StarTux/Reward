package com.winthier.reward.sql;

import com.avaje.ebean.validation.Length;
import com.avaje.ebean.validation.NotEmpty;
import com.avaje.ebean.validation.NotNull;
import java.util.Date;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "dailies",
       uniqueConstraints = @UniqueConstraint(columnNames = {"uuid", "name"}))
@Getter
@Setter
public class Daily {
    @Id Integer id;
    @Version Date version;
    @NotNull UUID uuid;
    @NotNull @Length(max = 255) String name;
    @NotNull Date day;

    public Daily() {}
    
    public Daily(UUID uuid, String name, Date day) {
        setUuid(uuid);
        setName(name);
        setDay(day);
    }
}
