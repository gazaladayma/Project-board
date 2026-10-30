package com.javaproject.beans;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ErrorMessage {
    private static final String status = "error";

    private String message;
}
