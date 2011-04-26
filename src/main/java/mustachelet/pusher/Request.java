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
public @interface Request {
  Bind value();

  public enum Bind {
    REQUEST, // HttpServletRequest
    RESPONSE, // HttpServletResponse
    MATCHER, // Matcher
    HTTP_METHOD, // Http Method, HttpMethod.Type.*
  }
}
