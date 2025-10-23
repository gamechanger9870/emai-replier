package com.email.writer.dtos;

import lombok.Data;

@Data
public class EmailRequest {
    private String emailContent;
    private String tone;
}
