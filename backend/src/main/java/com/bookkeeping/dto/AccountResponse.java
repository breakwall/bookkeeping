package com.bookkeeping.dto;

import com.bookkeeping.entity.Account;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class AccountResponse {
    private Long id;
    private String name;
    private String type;
    private String note;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static AccountResponse fromEntity(Account account) {
        return new AccountResponse(
            account.getId(),
            account.getName(),
            account.getType(),
            account.getNote(),
            account.getStatus().name(),
            account.getCreatedAt(),
            account.getUpdatedAt()
        );
    }
}
