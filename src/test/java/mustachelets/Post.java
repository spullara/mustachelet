package mustachelets;

import com.google.inject.Inject;
import mustachelet.annotations.Controller;
import mustachelet.annotations.HttpMethod;
import mustachelet.annotations.Path;
import mustachelet.annotations.Template;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Matcher;

import static mustachelet.annotations.HttpMethod.Type.*;

/**
 * Post / redirect handling
 * <p/>
 * User: sam
 * Date: 12/21/10
 * Time: 3:49 PM
 */
@Path("/post(/(.*))?")
@Template("post.html")
@HttpMethod({GET, POST})
public class Post {
  @Inject
  HttpServletResponse response;

  @Inject
  HttpServletRequest request;

  @Controller(POST)
  boolean redirectPostData() throws IOException {
    response.sendRedirect("/post/" + request.getParameter("value"));
    return false;
  }

  @Inject
  Matcher m;

  String value() {
    return m.group(2);
  }
}
