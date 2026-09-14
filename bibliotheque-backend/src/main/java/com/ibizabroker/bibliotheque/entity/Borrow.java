package com.ibizabroker.bibliotheque.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import java.util.Date;

@Data
@Entity @EntityListeners(AuditingEntityListener.class)
@Table(name = "Borrow")
public class Borrow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer borrowId;
    Integer bookId;
    Integer userId;

    @Temporal(TemporalType.TIMESTAMP)
    @JsonSerialize(using = JsonDataSerializer.class)
    @JsonDeserialize(using = JsonDataDeserializer.class)
    @JsonFormat(pattern = "dd-MM-yyyy")
    Date issueDate;

    @Temporal(TemporalType.TIMESTAMP)
    @JsonSerialize(using = JsonDataSerializer.class)
    @JsonDeserialize(using = JsonDataDeserializer.class)
    @JsonFormat(pattern = "dd-MM-yyyy")
    Date returnDate;

    @Temporal(TemporalType.TIMESTAMP)
    @JsonSerialize(using = JsonDataSerializer.class)
    @JsonDeserialize(using = JsonDataDeserializer.class)
    @JsonFormat(pattern = "dd-MM-yyyy")
    Date dueDate;

}
