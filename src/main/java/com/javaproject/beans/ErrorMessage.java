package com.javaproject.beans;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ErrorMessage {
    private static final String STATUS = "error";

    private String message;
}
