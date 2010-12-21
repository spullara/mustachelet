package mustachelets;

import mustachelet.annotations.Exists;
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
public class Index {
  @Exists
  boolean exists() {
    return true;
  }

  String name() {
    return "Sam";
  }
}
