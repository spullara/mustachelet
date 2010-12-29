package mustachelet;

import com.google.common.collect.Lists;
import mustachelet.pusher.Config;
import mustachelet.pusher.PTest;
import mustachelet.pusher.TestPush;
import mustachelets.Index;
import mustachelets.Post;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import thepusher.Pusher;
import thepusher.PusherBase;

import java.io.File;

import static mustachelet.pusher.Config.B.MUSTACHELETS;
import static mustachelet.pusher.Config.B.MUSTACHE_ROOT;
import static mustachelet.pusher.Config.B.PUSHER;

/**
 * Simple Jetty Test Server
 * <p/>
 * User: sam
 * Date: 12/21/10
 * Time: 2:16 PM
 */
public class MustacheletTestServer {
  public static void main(String[] args) throws Exception {
    Server server = new Server(9000);
    server.setGracefulShutdown(1000);

    final ContextHandlerCollection contexts = new ContextHandlerCollection();
    server.setHandler(contexts);

    Pusher<Config.B> pusher = PusherBase.create(Config.B.class, Config.class);
    File root = new File("src/test/resources");
    pusher.bindInstance(MUSTACHELETS, Lists.newArrayList(Index.class, Post.class));
    pusher.bindInstance(MUSTACHE_ROOT, root);
    pusher.bindInstance(PUSHER, PusherBase.create(PTest.class, TestPush.class));

    final ServletContextHandler mainHandler = new ServletContextHandler(contexts, "/", true, false);
    mainHandler.addServlet(new ServletHolder(pusher.create(MustacheletService.class)), "/");
    server.start();
  }
}
