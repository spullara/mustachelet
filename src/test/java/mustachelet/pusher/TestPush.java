package mustachelet.pusher;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * TODO: Edit this
 * <p/>
 * User: sam
 * Date: 12/21/10
 * Time: 2:54 PM
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface TestPush {
  PTest value();
}
