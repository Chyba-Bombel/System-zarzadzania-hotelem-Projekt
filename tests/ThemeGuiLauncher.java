public final class ThemeGuiLauncher {
    public static void main(String[] args) throws Exception {
        TestDatabase.wymagajTestowejBazy();
        javafx.application.Application.launch(ThemeGuiProbe.class,args);
    }
}
