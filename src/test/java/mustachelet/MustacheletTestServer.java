package mustachelet;

import com.google.common.collect.Lists;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import mustachelets.Index;
import mustachelets.Post;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.io.File;
import java.util.List;

/**
 * Simple Jetty Test Server
 * <p/>
 * User: sam
 * Date: 12/21/10
 * Time: 2:16 PM
 */
public class MustacheletTestServer {
  public static void main(String[] args) throws Exception {
    Server server = new Server(9090);
    server.setGracefulShutdown(1000);

    final ContextHandlerCollection contexts = new ContextHandlerCollection();
    server.setHandler(contexts);

    Injector injector = Guice.createInjector(new Module() {
      @Override
      public void configure(Binder binder) {
        binder.bind(new TypeLiteral<List<Class<?>>>() {
        }).toInstance(Lists.newArrayList(Index.class, Post.class));
        binder.bind(File.class).annotatedWith(Names.named("root")).toInstance(new File("src/test/resources"));
      }
    });

    final ServletContextHandler mainHandler = new ServletContextHandler(contexts, "/", true, false);
    mainHandler.addServlet(new ServletHolder(injector.getInstance(MustacheletService.class)), "/");
    server.start();
  }
}
