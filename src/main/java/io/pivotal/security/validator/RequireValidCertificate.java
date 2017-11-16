package io.pivotal.security.validator;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({TYPE, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy = {CertificateValidator.class})
public @interface RequireValidCertificate {

  String message();

  String[] fields();
  Class<? extends Payload>[] payload() default {};

  Class<?>[] groups() default {};
}
