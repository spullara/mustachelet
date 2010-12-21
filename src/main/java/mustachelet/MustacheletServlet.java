package mustachelet;

import com.sampullara.mustache.Mustache;
import com.sampullara.mustache.MustacheCompiler;
import com.sampullara.mustache.MustacheException;
import com.sampullara.mustache.Scope;
import com.sampullara.util.FutureWriter;
import mustachelet.annotations.Exists;
import mustachelet.annotations.Path;
import mustachelet.annotations.Template;
import mustachelet.pusher.ConfigB;
import mustachelet.pusher.ConfigP;
import mustachelet.pusher.RequestB;
import mustachelet.pusher.RequestP;
import thepusher.Pusher;
import thepusher.PusherBase;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This servlet handles serving Mustachelets.
 * <p/>
 * User: sam
 * Date: 12/21/10
 * Time: 2:13 PM
 */
public class MustacheletServlet extends HttpServlet {

  @ConfigP(ConfigB.PUSHER)
  Pusher mustacheletPusher;

  @ConfigP(ConfigB.MUSTACHELETS)
  List<Class> mustachelets;

  @ConfigP(ConfigB.MUSTACHE_ROOT)
  File root;

  @Override
  public void init() throws ServletException {
    MustacheCompiler mc = new MustacheCompiler(root);
    for (Class<?> mustachelet : mustachelets) {
      Path annotation = mustachelet.getAnnotation(Path.class);
      if (annotation == null) {
        throw new ServletException("No Path annotation present on: " + mustachelet.getCanonicalName());
      }
      Template template = mustachelet.getAnnotation(Template.class);
      if (template == null) {
        throw new ServletException("You must specify a template on: " + mustachelet.getCanonicalName());
      }
      String regex = annotation.value();
      Class put = pathMap.put(Pattern.compile(regex), mustachelet);
      if (put != null) {
        throw new ServletException("Duplicate path: " + mustachelet + " and " + put);
      }
      try {
        File file = new File(root, template.value());
        if (!file.exists()) {
          throw new ServletException("Template file does not exist: " + file);
        }
        Mustache mustache = mc.compile(new BufferedReader(new FileReader(file)));
        mustacheMap.put(mustachelet, mustache);
      } catch (Exception e) {
        throw new ServletException("Failed to compile template: " + template.value(), e);
      }
      for (Method method : mustachelet.getDeclaredMethods()) {
        if (method.getAnnotation(Exists.class) != null) {
          method.setAccessible(true);
          existsMap.put(mustachelet, method);
          break;
        }
      }
    }
  }

  private Map<Pattern, Class> pathMap = new HashMap<Pattern, Class>();
  private Map<Class, Mustache> mustacheMap = new HashMap<Class, Mustache>();
  private Map<Class, Method> existsMap = new HashMap<Class, Method>();

  @Override
  protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    resp.setHeader("Server", "Mustachelet 0.1");
    Pusher<RequestB> requestPusher = PusherBase.create(RequestB.class, RequestP.class);
    requestPusher.bindInstance(RequestB.REQUEST, req);

    String requestURI = req.getRequestURI();
    if (requestURI == null || requestURI.equals("")) {
      requestURI = "/";
    }
    for (Map.Entry<Pattern, Class> entry : pathMap.entrySet()) {
      Matcher matcher = entry.getKey().matcher(requestURI);
      if (matcher.matches()) {
        requestPusher.bindInstance(RequestB.MATCHER, matcher);
        Class mustachelet = entry.getValue();
        Object o = mustacheletPusher.create(mustachelet);
        requestPusher.push(o);
        Method method = existsMap.get(mustachelet);
        if (method != null) {
          Object invoke;
          try {
            invoke = method.invoke(o);
            if (invoke instanceof Boolean && !((Boolean)invoke)) {
              resp.setStatus(404);
              return;
            }
          } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(500);
            return;
          }
        }
        Mustache mustache = mustacheMap.get(mustachelet);
        FutureWriter fw = new FutureWriter(resp.getWriter());
        try {
          mustache.execute(fw, new Scope(o));
          resp.setStatus(200);
          fw.flush();
        } catch (MustacheException e) {
          resp.setStatus(500);
          e.printStackTrace();
          return;
        }
      }
    }
  }
}
