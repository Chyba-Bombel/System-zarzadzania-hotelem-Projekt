public final class SettingsGuiLauncher {
    public static void main(String[] args) throws Exception {
        TestDatabase.wymagajTestowejBazy();
        javafx.application.Application.launch(SettingsGuiProbe.class, args);
    }
}
