package minimal_3d.core;

import clojure.lang.RT;
import clojure.lang.Symbol;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.Game;

public class AndroidLauncher extends AndroidApplication {
	public void onCreate (android.os.Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
          RT.var("clojure.core", "require").invoke(Symbol.intern("minimal-3d.core"));
		try {
			Game game = (Game) RT.var("minimal-3d.core", "minimal-3d").deref();
			initialize(game);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
