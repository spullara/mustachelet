package mustachelet;

import com.sampullara.mustache.Mustache;
import com.sampullara.mustache.MustacheCompiler;
import com.sampullara.mustache.MustacheException;
import com.sampullara.mustache.Scope;
import com.sampullara.util.FutureWriter;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache partials for performance.
 * <p/>
 * User: sam
 * Date: 4/25/11
 * Time: 6:03 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class Mustachelet extends Mustache {
  private static Map<String, Mustache> cache = new ConcurrentHashMap<String, Mustache>();

  @Override
  protected void partial(FutureWriter writer, Scope s, String name) throws MustacheException {
    Mustache m;
    synchronized (name.intern()) {
      m = cache.get(name);
      if (m == null) {
        String parentDir = new File(getPath()).getParent();
        String filename = (parentDir == null ? "" : parentDir + "/") + name + ".html";
        MustacheCompiler c = new MustacheCompiler(getRoot());
        c.setSuperclass(Mustachelet.class.getName());
        Trace.Event event = null;
        if (trace) {
          Object parent = s.getParent();
          String traceName = parent == null ? s.getClass().getName() : parent.getClass().getName();
          event = Trace.addEvent("partial compile: " + name, traceName);
        }
        m = c.parseFile(filename);
        cache.put(name, m);
        if (trace) {
          event.end();
        }
      }
    }
    Object parent = s.get(name);
    final Scope scope = parent == null ? s : new Scope(parent, s);
    Trace.Event event = null;
    if (trace) {
      Object parentObject = s.getParent();
      String traceName = parentObject == null ? s.getClass().getName() : parentObject.getClass().getName();
      event = Trace.addEvent("partial execute: " + name, traceName);
    }
    m.execute(writer, scope);
    if (trace) {
      event.end();
    }
  }
}
