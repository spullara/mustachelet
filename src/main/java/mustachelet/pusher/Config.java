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
  Bind value();

  public enum Bind {
    MUSTACHELETS, // List<Class>, List of mustachelet classes
    MUSTACHE_ROOT, // MustacheCompiler root, For compiling templates
    PUSHER, // Pusher, Global pusher from the client
    LOGGER, // Instance of java.util.logging.Logger
  }
}
