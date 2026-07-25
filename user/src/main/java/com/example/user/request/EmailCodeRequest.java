package com.example.user.request;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
@Getter @Setter public class EmailCodeRequest { @NotBlank @Email @Size(max = 255) private String email; }
