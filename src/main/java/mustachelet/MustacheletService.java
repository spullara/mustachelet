package mustachelet;

import com.sampullara.mustache.Mustache;
import com.sampullara.mustache.MustacheBuilder;
import com.sampullara.mustache.MustacheException;
import com.sampullara.mustache.Scope;
import com.sampullara.util.FutureWriter;
import mustachelet.annotations.Controller;
import mustachelet.annotations.HttpMethod;
import mustachelet.annotations.Path;
import mustachelet.annotations.Template;
import mustachelet.pusher.Config;
import mustachelet.pusher.Request;
import thepusher.Pusher;
import thepusher.PusherBase;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static mustachelet.pusher.Config.Bind.LOGGER;
import static mustachelet.pusher.Config.Bind.MUSTACHELETS;
import static mustachelet.pusher.Config.Bind.MUSTACHE_ROOT;
import static mustachelet.pusher.Config.Bind.PUSHER;
import static mustachelet.pusher.Request.Bind.REQUEST;
import static mustachelet.pusher.Request.Bind.RESPONSE;

/**
 * This servlet handles serving Mustachelets.
 * <p/>
 * User: sam
 * Date: 12/21/10
 * Time: 2:13 PM
 */
public class MustacheletService extends HttpServlet implements Filter {

  @Config(LOGGER)
  Logger logger;

  @Config(PUSHER)
  Pusher mustacheletPusher;

  @Config(MUSTACHELETS)
  List<Class> mustachelets;

  @Config(MUSTACHE_ROOT)
  File root;

  private Map<Pattern, Map<HttpMethod.Type, Class>> pathMap = new HashMap<Pattern, Map<HttpMethod.Type, Class>>();
  private Map<Class, Mustache> mustacheMap = new HashMap<Class, Mustache>();
  private Map<Class, Map<HttpMethod.Type, Method>> controllerMap = new HashMap<Class, Map<HttpMethod.Type, Method>>();

  @Override
  protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    if (execute(resp, req)) return;
    resp.sendError(404, "Not found");
  }

  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    HttpServletResponse resp = (HttpServletResponse) response;
    HttpServletRequest req = (HttpServletRequest) request;
    if (execute(resp, req)) return;
    chain.doFilter(request, response);
  }

  private boolean execute(HttpServletResponse resp, HttpServletRequest req) throws IOException {
    resp.setHeader("Server", "Mustachelet/0.1");
    resp.setContentType("text/html");
    resp.setCharacterEncoding("UTF-8");
    Pusher<Request.Bind> requestPusher = PusherBase.create(Request.Bind.class, Request.class);
    requestPusher.bindInstance(REQUEST, req);
    requestPusher.bindInstance(RESPONSE, resp);

    String requestURI = req.getRequestURI();
    if (requestURI == null || requestURI.equals("")) {
      requestURI = "/";
    }
    for (Map.Entry<Pattern, Map<HttpMethod.Type, Class>> entry : pathMap.entrySet()) {
      Matcher matcher = entry.getKey().matcher(requestURI);
      if (matcher.matches()) {
        requestPusher.bindInstance(Request.Bind.MATCHER, matcher);
        Map<HttpMethod.Type, Class> methodClassMap = entry.getValue();
        String httpMethod = req.getMethod();
        boolean head;
        if (httpMethod.equals("HEAD")) {
          head = true;
          httpMethod = "GET";
        } else head = false;
        HttpMethod.Type type = HttpMethod.Type.valueOf(httpMethod);
        requestPusher.bindInstance(Request.Bind.HTTP_METHOD, type);
        Class mustachelet = methodClassMap.get(type);
        Object o = mustacheletPusher.create(mustachelet);
        if (o != null) {
          requestPusher.push(o);
          Map<HttpMethod.Type, Method> typeMethodMap = controllerMap.get(mustachelet);
          if (typeMethodMap != null) {
            Method method = typeMethodMap.get(type);
            if (method != null) {
              Object invoke;
              try {
                invoke = method.invoke(o);
                if (invoke instanceof Boolean && !((Boolean)invoke)) {
                  return true;
                }
              } catch (Exception e) {
                e.printStackTrace();
                resp.setStatus(500);
                return true;
              }
            }
          }
          if (head) {
            resp.setStatus(200);
            return true;
          }
          Mustache mustache = mustacheMap.get(mustachelet);
          FutureWriter fw = new FutureWriter(resp.getWriter());
          try {
            mustache.execute(fw, new Scope(o));
            resp.setStatus(200);
            fw.flush();
            return true;
          } catch (MustacheException e) {
            resp.setStatus(500);
            e.printStackTrace();
            return true;
          }
        } else {
          resp.setStatus(405);
        }
      }
    }
    return false;
  }

  public void init(FilterConfig filterConfig) throws ServletException {
    init();
  }

  public void init() throws ServletException {
    MustacheBuilder mc = new MustacheBuilder(root);
    for (Class<?> mustachelet : mustachelets) {
      Path annotation = mustachelet.getAnnotation(Path.class);
      if (annotation == null) {
        throw new ServletException("No Path annotation present on: " + mustachelet.getCanonicalName());
      }
      Template template = mustachelet.getAnnotation(Template.class);
      if (template == null) {
        throw new ServletException("You must specify a template on: " + mustachelet.getCanonicalName());
      }
      HttpMethod httpMethod = mustachelet.getAnnotation(HttpMethod.class);
      String regex = annotation.value();
      Map<HttpMethod.Type, Class> methodClassMap = new HashMap<HttpMethod.Type, Class>();
      if (httpMethod == null) {
        methodClassMap.put(HttpMethod.Type.GET, mustachelet);
      } else {
        for (HttpMethod.Type type : httpMethod.value()) {
          methodClassMap.put(type, mustachelet);
        }
      }
      Map<HttpMethod.Type, Class> put = pathMap.put(Pattern.compile(regex), methodClassMap);
      if (put != null) {
        throw new ServletException("Duplicate path: " + mustachelet + " and " + put);
      }
      try {
        File file = new File(root, template.value());
        if (!file.exists()) {
          throw new ServletException("Template file does not exist: " + file);
        }
        Mustache mustache = mc.parseFile(template.value());
        mustacheMap.put(mustachelet, mustache);
      } catch (Exception e) {
        throw new ServletException("Failed to compile template: " + template.value(), e);
      }
      for (Method method : mustachelet.getDeclaredMethods()) {
        Controller controller = method.getAnnotation(Controller.class);
        if (controller != null) {
          method.setAccessible(true);
          Map<HttpMethod.Type, Method> typeMethodMap = controllerMap.get(mustachelet);
          if (typeMethodMap == null) {
            typeMethodMap = new HashMap<HttpMethod.Type, Method>();
            controllerMap.put(mustachelet, typeMethodMap);
          }
          for (HttpMethod.Type type : controller.value()) {
            typeMethodMap.put(type, method);
          }
        }
      }
    }
  }

  public void destroy() {
  }
}
