package mustachelets;

import mustachelet.annotations.Controller;
import mustachelet.annotations.HttpMethod;
import mustachelet.annotations.Path;
import mustachelet.annotations.Template;

import static mustachelet.annotations.HttpMethod.*;
import static mustachelet.annotations.HttpMethod.Type.*;

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
  @Controller
  boolean exists() {
    return true;
  }

  String name() {
    return "Sam";
  }
}
