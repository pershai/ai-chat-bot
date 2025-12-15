package com.example.aichatbot.security;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ValidateInput is a custom annotation that is used to validate the input of a method.
 * It is used in combination with the ValidateInputAspect to validate the input of a method.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidateInput {
}
