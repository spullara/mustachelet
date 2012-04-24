package mustachelet;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.internal.Nullable;
import com.google.inject.name.Named;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheException;
import com.github.mustachejava.MustacheFactory;
import mustachelet.annotations.Controller;
import mustachelet.annotations.HttpMethod;
import mustachelet.annotations.Path;
import mustachelet.annotations.Template;

/**
 * This servlet handles serving Mustachelets.
 * <p/>
 * User: sam
 * Date: 12/21/10
 * Time: 2:13 PM
 */
public class MustacheletService extends HttpServlet implements Filter {

  @Inject
  Logger logger;

  @Inject
  List<Class<?>> mustachelets;

  @Inject
  @Nullable
  @Named("root")
  File root;

  @Inject
  Injector injector;

  private Map<Pattern, Map<HttpMethod.Type, Class>> pathMap = new HashMap<Pattern, Map<HttpMethod.Type, Class>>();
  private Map<Class, Mustache> mustacheMap = new HashMap<Class, Mustache>();
  private Map<Class, Map<HttpMethod.Type, Method>> controllerMap = new HashMap<Class, Map<HttpMethod.Type, Method>>();
  private FilterConfig filterConfig;

  @Override
  protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    if (execute(resp, req)) return;
    resp.sendError(404, "Not found");
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    this.filterConfig = filterConfig;
    init();
  }

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
  }

  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    HttpServletResponse resp = (HttpServletResponse) response;
    HttpServletRequest req = (HttpServletRequest) request;
    if (execute(resp, req)) return;
    chain.doFilter(request, response);
  }

  private boolean execute(final HttpServletResponse resp, final HttpServletRequest req) throws IOException {
    resp.setHeader("Server", "Mustachelet/0.1");
    resp.setContentType("text/html");
    resp.setCharacterEncoding("UTF-8");

    String requestURI = req.getRequestURI();
    if (requestURI == null || requestURI.equals("")) {
      requestURI = "/";
    }
    for (Map.Entry<Pattern, Map<HttpMethod.Type, Class>> entry : pathMap.entrySet()) {
      final Matcher matcher = entry.getKey().matcher(requestURI);
      if (matcher.matches()) {
        Map<HttpMethod.Type, Class> methodClassMap = entry.getValue();
        String httpMethod = req.getMethod();
        final boolean head;
        if (httpMethod.equals("HEAD")) {
          head = true;
          httpMethod = "GET";
        } else head = false;
        HttpMethod.Type type = HttpMethod.Type.valueOf(httpMethod);
        Class mustachelet = methodClassMap.get(type);
        Injector ci = injector.createChildInjector(new Module() {
          @Override
          public void configure(Binder binder) {
            binder.bind(Matcher.class).toInstance(matcher);
            binder.bind(HttpServletRequest.class).toInstance(req);
            binder.bind(HttpServletResponse.class).toInstance(resp);
          }
        });

        Object o = ci.getInstance(mustachelet);
        if (o != null) {
          Map<HttpMethod.Type, Method> typeMethodMap = controllerMap.get(mustachelet);
          if (typeMethodMap != null) {
            Method method = typeMethodMap.get(type);
            if (method != null) {
              Object invoke;
              try {
                invoke = method.invoke(o);
                if (invoke instanceof Boolean && !((Boolean) invoke)) {
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
          try {
            Writer writer = resp.getWriter();
            writer = mustache.execute(writer, o);
            resp.setStatus(200);
            writer.close();
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

  public void init() throws ServletException {
    String moduleClass;
    if (filterConfig != null) {
      moduleClass = filterConfig.getInitParameter("module");
    } else {
      moduleClass = getInitParameter("module");
    }
    if (moduleClass != null) {
      try {
        Guice.createInjector((Module) Class.forName(moduleClass).newInstance()).injectMembers(this);
      } catch (Exception e) {
        throw new ServletException("Failed to initialize", e);
      }
    }
    if (root == null) {
      ServletContext servletContext;
      if (filterConfig != null) {
        servletContext = filterConfig.getServletContext();
      } else {
        servletContext = getServletContext();
      }
      String realPath = servletContext.getRealPath("/");
      root = new File(realPath);
    }
    MustacheFactory mc = new DefaultMustacheFactory(root);
    for (Class<?> mustachelet : mustachelets) {
      Path annotation = mustachelet.getAnnotation(Path.class);
      if (annotation == null) {
        throw new ServletException(
                "No Path annotation present on: " + mustachelet.getCanonicalName());
      }
      Template template = mustachelet.getAnnotation(Template.class);
      if (template == null) {
        throw new ServletException(
                "You must specify a template on: " + mustachelet.getCanonicalName());
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
          throw new ServletException("Template file does not exist: " + file.getAbsolutePath());
        }
        Mustache mustache = mc.compile(template.value());
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
