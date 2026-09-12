# Custom Views are referenced from XML layouts and need their constructors preserved.
-keep public class com.halahasneen.theguest.ui.components.** { public <init>(...); }
-keep public class com.halahasneen.theguest.ui.game.GameCanvasView { public <init>(...); }
