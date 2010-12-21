package mustachelet.pusher;

/**
 * Things that you can push into Mustachelet
 * <p/>
 * User: sam
 * Date: 12/21/10
 * Time: 2:32 PM
 */
public enum ConfigB {
  MUSTACHELETS, // List<Class>, List of mustachelet classes
  MUSTACHE_ROOT, // MustacheCompiler root, For compiling templates
  PUSHER, // Pusher, Global pusher from the client
}
