package mustachelets;

import mustachelet.annotations.Controller;
import mustachelet.annotations.HttpMethod;
import mustachelet.annotations.Path;
import mustachelet.annotations.Template;

/**
 * Index page.
 * <p/>
 * User: sam
 * Date: 12/21/10
 * Time: 2:22 PM
 */
@Path("/")
@Template("index.html")
@HttpMethod(HttpMethod.Type.GET)
public class Index {
  @Controller
  boolean exists() {
    return true;
  }

  String name() {
    return "Sam";
  }
}
