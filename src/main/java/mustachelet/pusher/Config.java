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
 * Time: 2:32 PM
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Config {
  B value();

  public enum B {
    MUSTACHELETS, // List<Class>, List of mustachelet classes
    MUSTACHE_ROOT, // MustacheCompiler root, For compiling templates
    PUSHER, // Pusher, Global pusher from the client
  }
}
