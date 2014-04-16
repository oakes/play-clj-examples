package breakout.core;

import clojure.lang.RT;
import clojure.lang.Symbol;

import com.badlogic.gdx.backends.iosrobovm.*;
import com.badlogic.gdx.*;

import org.robovm.apple.foundation.*;
import org.robovm.apple.uikit.*;

public class IOSLauncher extends IOSApplication.Delegate {
	protected IOSApplication createApplication() {
		IOSApplicationConfiguration config = new IOSApplicationConfiguration();
		RT.var("clojure.core", "require").invoke(Symbol.intern("breakout.core"));
		try {
			Game game = (Game) RT.var("breakout.core", "breakout").deref();
			return new IOSApplication(game, config);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void main(String[] argv) {
		NSAutoreleasePool pool = new NSAutoreleasePool();
		UIApplication.main(argv, null, IOSLauncher.class);
		pool.close();
	}
}
