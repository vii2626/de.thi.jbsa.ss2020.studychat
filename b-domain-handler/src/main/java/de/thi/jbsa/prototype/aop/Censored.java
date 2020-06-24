package de.thi.jbsa.prototype.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by Balazs Endredi <balazs.endredi@beskgroup.com> on 06.05.2020
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface Censored {

  String value() default "default_bad_terms";
}
