package mustachelets;

import mustachelet.annotations.Controller;
import mustachelet.annotations.HttpMethod;
import mustachelet.annotations.Path;
import mustachelet.annotations.Template;
import mustachelet.pusher.RequestB;
import mustachelet.pusher.RequestP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Matcher;

/**
 * Post / redirect handling
 * <p/>
 * User: sam
 * Date: 12/21/10
 * Time: 3:49 PM
 */
@Path("/post(/(.*))?")
@Template("post.html")
@HttpMethod({HttpMethod.Type.GET, HttpMethod.Type.POST})
public class Post {
  @RequestP(RequestB.HTTP_METHOD)
  HttpMethod.Type method;

  @RequestP(RequestB.RESPONSE)
  HttpServletResponse response;

  @RequestP(RequestB.REQUEST)
  HttpServletRequest request;

  @RequestP(RequestB.MATCHER)
  Matcher m;

  @Controller
  boolean post() throws IOException {
    if (method == HttpMethod.Type.POST) {
      response.sendRedirect("/post/" + request.getParameter("value"));
      return false;
    }
    return true;
  }

  String value() {
    return m.group(2);
  }
}
