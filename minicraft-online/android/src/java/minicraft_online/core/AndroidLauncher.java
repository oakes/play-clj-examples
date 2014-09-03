package minicraft_online.core;

import clojure.lang.RT;
import clojure.lang.Symbol;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.Game;

public class AndroidLauncher extends AndroidApplication {
	public void onCreate (android.os.Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
          RT.var("clojure.core", "require").invoke(Symbol.intern("minicraft-online.core"));
		try {
			Game game = (Game) RT.var("minicraft-online.core", "minicraft-online").deref();
			initialize(game);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
